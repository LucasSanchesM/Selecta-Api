package com.unisal.selectaapi.chat.service;

import com.unisal.selectaapi.candidatura.entity.Candidatura;
import com.unisal.selectaapi.candidatura.repository.CandidaturaRepository;
import com.unisal.selectaapi.chat.dtos.CandidaturaEUsarioDto;
import com.unisal.selectaapi.chat.dtos.CreateMessageDto;
import com.unisal.selectaapi.chat.dtos.HistoricoMessageResponseDto;
import com.unisal.selectaapi.chat.dtos.MensagemLidaDto;
import com.unisal.selectaapi.chat.entity.HistoricoMensagens;
import com.unisal.selectaapi.chat.repository.HistoricoMensagensRepository;
import com.unisal.selectaapi.company.entity.CompanyMember;
import com.unisal.selectaapi.company.repository.CompanyMemberRepository;
import com.unisal.selectaapi.company.service.CompanyMemberService;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.notificacoes.NotificacoesRole;
import com.unisal.selectaapi.notificacoes.dtos.PublishNotificacaoDto;
import com.unisal.selectaapi.notificacoes.service.NotificacaoService;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.UserRole;
import com.unisal.selectaapi.user.UserService;
import com.unisal.selectaapi.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
public class HistoricoMensagensService {

    private final HistoricoMensagensRepository historicoMensagensRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final NotificacaoService notificacaoService;
    private final CompanyMemberService companyMemberService;
    private final UserService userService;
    private final CompanyMemberRepository companyMemberRepository;

    public HistoricoMensagensService(HistoricoMensagensRepository historicoMensagensRepository, CandidaturaRepository candidaturaRepository, UserRepository userRepository, SimpMessagingTemplate simpMessagingTemplate, NotificacaoService notificacaoService, CompanyMemberService companyMemberService, UserService userService, CompanyMemberRepository companyMemberRepository) {
        this.historicoMensagensRepository = historicoMensagensRepository;
        this.candidaturaRepository = candidaturaRepository;
        this.userRepository = userRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.notificacaoService = notificacaoService;
        this.companyMemberService = companyMemberService;
        this.userService = userService;
        this.companyMemberRepository = companyMemberRepository;
    }

    private CandidaturaEUsarioDto pegarEValidarCandidaturaEUser(UUID userId, UUID candidaturaId){
        var user = userRepository.findById(userId).orElseThrow(
                () -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario nao encontrado")
        );
        Candidatura candidatura;
        if(UserRole.CANDIDATE.name().equals(user.getRole())){
            candidatura = candidaturaRepository.findByIdAndCandidatoUserId(candidaturaId, userId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Candidatura buscada nao existe"));
        }

        else if(UserRole.COMPANY.name().equals((user.getRole()))){
            var company = companyMemberService.pegarCompanyPeloUserId(userId);
            candidatura = candidaturaRepository.findByIdAndVagaEmpresaId(candidaturaId, company.getId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Candidatura buscada nao existe"));
        }

        else{
            throw new BusinessException(HttpStatus.FORBIDDEN, "Erro, nao foi possivel aceitar a role de usuario");
        }

        return new CandidaturaEUsarioDto(candidatura, user);
    }

    private boolean isCandidato(CandidaturaEUsarioDto dados) {
        return UserRole.CANDIDATE.name().equals(dados.user().getRole());
    }

    @Transactional(readOnly = true)
    public List<HistoricoMessageResponseDto> pegarListaDeMensagens(UUID userId, UUID candidaturaId){
        var dados = pegarEValidarCandidaturaEUser(userId, candidaturaId);
        var lista = historicoMensagensRepository.findByCandidaturaOrderByCreatedTimeStampAsc(dados.candidatura());
        boolean souCandidato = isCandidato(dados);
        UUID candidatoUserId = dados.candidatura().getCandidato().getUser().getId();

        return lista.stream().map(registro -> {
            var autor = registro.getUser();
            boolean doMeuLado = souCandidato
                    ? autor != null && autor.getId().equals(userId)
                    : autor == null || !autor.getId().equals(candidatoUserId);
            String nome = autor != null ? autor.getName() : "Usuário removido";
            return new HistoricoMessageResponseDto(nome, registro.getMensagem(), doMeuLado,
                    registro.getCreatedTimeStamp(), registro.getLida());
        }).toList();
    }

    @Transactional
    public HistoricoMessageResponseDto registrarMensagem(CreateMessageDto dto, UUID userId, UUID candidaturaId){
        var dadosMensagem = pegarEValidarCandidaturaEUser(userId, candidaturaId);
        boolean souCandidato = isCandidato(dadosMensagem);
        if(dadosMensagem.candidatura().getChatIniciadoEm() == null && souCandidato){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Apenas empresas podem iniciar um chat");
        }else if(dadosMensagem.candidatura().getChatIniciadoEm() == null){
            dadosMensagem.candidatura().setChatIniciadoEm(LocalDateTime.now());
        }
        var registro = new HistoricoMensagens();
        registro.setCandidatura(dadosMensagem.candidatura());
        registro.setMensagem(dto.message());
        registro.setUser(dadosMensagem.user());
        registro.setLida(false);
        var registroSaved = historicoMensagensRepository.save(registro);

        var candidatura = dadosMensagem.candidatura();
        var empresaUsers = getAllUsuariosDaEmpresa(candidatura);
        UUID candidatoUserId = candidatura.getCandidato().getUser().getId();

        var nome = registroSaved.getUser().getName();
        var dtoMesmoLado = new HistoricoMessageResponseDto(nome, registroSaved.getMensagem(), true,
                registroSaved.getCreatedTimeStamp(), registroSaved.getLida());
        var dtoOutroLado = new HistoricoMessageResponseDto(nome, registroSaved.getMensagem(), false,
                registroSaved.getCreatedTimeStamp(), registroSaved.getLida());

        if (souCandidato) {
            empresaUsers.forEach(u -> enviarSocket(u.getId(), dtoOutroLado));
        } else {
            empresaUsers.stream()
                    .filter(u -> !u.getId().equals(userId))
                    .forEach(u -> enviarSocket(u.getId(), dtoMesmoLado));
            enviarSocket(candidatoUserId, dtoOutroLado);
        }

        UUID destinatarioNotificacao = souCandidato
                ? candidatura.getVaga().getEmpresa().getUser().getId()   // dono da empresa
                : candidatoUserId;

        if (historicoMensagensRepository.quantidadeDeMensagensNaoLidasPeloIdDaCandidatura(candidaturaId, userId) <= 1) {
            notificacaoService.publicarNotificacao(destinatarioNotificacao,
                    new PublishNotificacaoDto("Mensagens não lidas", "Há mensagens não lidas",
                            NotificacoesRole.CHAT, candidaturaId.toString()));
        }

        return dtoMesmoLado;
    }

    @Transactional
    public void marcarMensagensComoLidas(UUID userId, UUID candidaturaId, Instant lidaEm){
            var dados = pegarEValidarCandidaturaEUser(userId, candidaturaId);
        var candidatura = dados.candidatura();
        boolean souCandidato = isCandidato(dados);

        int atualizadas = souCandidato
                ? historicoMensagensRepository.marcarComoLidas(candidaturaId, userId, lidaEm)
                : historicoMensagensRepository.marcarComoLidasDoAutor(candidaturaId, candidatura.getCandidato().getUser().getId(), lidaEm);

        if(atualizadas>0){
            var aviso = new MensagemLidaDto(candidaturaId, lidaEm);
            if (souCandidato) {
                getAllUsuariosDaEmpresa(candidatura).forEach(user ->
                        simpMessagingTemplate.convertAndSendToUser(user.getId().toString(), "/queue/leituras", aviso));
            } else {
                simpMessagingTemplate.convertAndSendToUser(candidatura.getCandidato().getUser().getId().toString(), "/queue/leituras", aviso);
            }
        }
    }

    private List<User> getAllUsuariosDaEmpresa(Candidatura candidatura) {
        var listaDeUsuarios = companyMemberRepository.findAllUsersByCompanyId(candidatura.getVaga().getEmpresa().getId());
        listaDeUsuarios.add(candidatura.getVaga().getEmpresa().getUser());
        return listaDeUsuarios;
    }

    private void enviarSocket(UUID destinatarioId, HistoricoMessageResponseDto dto) {
        simpMessagingTemplate.convertAndSendToUser(destinatarioId.toString(), "/queue/mensagens", dto);
        System.out.println("Enviando mensagem WebSocket");
        System.out.println("Destinatário UUID: " + destinatarioId);
    }
}
