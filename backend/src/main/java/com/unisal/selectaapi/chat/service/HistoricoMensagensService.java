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

    public List<HistoricoMessageResponseDto> pegarListaDeMensagens(UUID userId, UUID candidaturaId){

        var dados = pegarEValidarCandidaturaEUser(userId, candidaturaId);

        var lista = historicoMensagensRepository.findByCandidaturaOrderByCreatedTimeStampAsc(dados.candidatura());
        boolean souCandidato = isCandidato(dados);

        return lista.stream()
                .map(registro -> {
                    UUID autorId = registro.getUser().getId();
                    // "Minha" = enviada pelo meu lado da conversa. Para a empresa, mensagens
                    // de qualquer colega da equipe também aparecem como do lado da empresa.
                    boolean doMeuLado = souCandidato ? autorId.equals(userId) : !autorId.equals(dados.candidatura().getCandidato().getId());
                    return new HistoricoMessageResponseDto(
                    registro.getUser().getName(),
                    registro.getMensagem(), doMeuLado,
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
        //var destinatario = UserRole.CANDIDATE.name().equals(dadosMensagem.user().getRole()) ?
        //        dadosMensagem.candidatura().getVaga().getEmpresa().getUser().getId():
        //        dadosMensagem.candidatura().getCandidato().getUser().getId();
        //simpMessagingTemplate.convertAndSendToUser(destinatario.toString(),
          //                                         "/queue/mensagens",
            //                                        new HistoricoMessageResponseDto(registroSaved.getUser().getName(), registroSaved.getMensagem(), false, registroSaved.getCreatedTimeStamp(), registroSaved.getLida())
        //);

        var dtoEnviadoParaSiMesmo = new HistoricoMessageResponseDto(registroSaved.getUser().getName(),
                                                                    registroSaved.getMensagem(),
                                                    false,
                                                                    registroSaved.getCreatedTimeStamp(),
                                                                    registroSaved.getLida());

        var dtoEnviadoParaOutros = new HistoricoMessageResponseDto(registroSaved.getUser().getName(),
                registroSaved.getMensagem(),
                true,
                registroSaved.getCreatedTimeStamp(),
                registroSaved.getLida());

        if(souCandidato){
            //Enviar para todos os usuarios da empresa online
            getAllUsuariosDaEmpresa(dadosMensagem.candidatura()).forEach(usuario -> {
                simpMessagingTemplate.convertAndSendToUser(usuario.getId().toString(),
                                                    "/queue/mensagens",
                                                            dtoEnviadoParaOutros
                                                    );
            });
        }else{
            //Envia pro candidato e para outros usuarios da empresa online
            var usersToSend = getAllUsuariosDaEmpresa(dadosMensagem.candidatura()).stream().filter(user -> !user.getId().equals(userId)).toList();
            usersToSend.forEach(usuario ->
                    simpMessagingTemplate.convertAndSendToUser(usuario.getId().toString(),
                                                    "/queue/mensagens",
                                                                dtoEnviadoParaSiMesmo
                                                    )
            );
            simpMessagingTemplate.convertAndSendToUser(dadosMensagem.candidatura().getCandidato().getUser().getId().toString(),
                                                        "/queue/mensagens",
                                                        dtoEnviadoParaOutros);

        }

        var destinatario = UserRole.CANDIDATE.name().equals(dadosMensagem.user().getRole()) ? dadosMensagem.candidatura().getVaga().getEmpresa().getUser().getId()
                                :dadosMensagem.candidatura().getCandidato().getUser().getId();

        if(historicoMensagensRepository.quantidadeDeMensagensNaoLidasPeloIdDaCandidatura(registroSaved.getCandidatura().getId(), registroSaved.getUser().getId()) <= 1){
            notificacaoService.publicarNotificacao(destinatario, new PublishNotificacaoDto("Mensagens não lidas",
                                                                                  "Há mensagens nao lidas",
                                                                                            NotificacoesRole.CHAT,
                                                                                            registro.getIdMensagem().toString()
                    ));
        }

        return new HistoricoMessageResponseDto(registroSaved.getUser().getName(), registroSaved.getMensagem(), true, registroSaved.getCreatedTimeStamp(), registroSaved.getLida());
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


}
