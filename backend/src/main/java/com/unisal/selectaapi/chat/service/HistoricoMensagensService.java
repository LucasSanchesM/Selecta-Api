package com.unisal.selectaapi.chat.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.candidatura.entity.Candidatura;
import com.unisal.selectaapi.candidatura.repository.CandidaturaRepository;
import com.unisal.selectaapi.chat.dtos.CandidaturaEUsarioDto;
import com.unisal.selectaapi.chat.dtos.CreateMessageDto;
import com.unisal.selectaapi.chat.dtos.HistoricoMessageResponseDto;
import com.unisal.selectaapi.chat.dtos.MensagemLidaDto;
import com.unisal.selectaapi.chat.entity.HistoricoMensagens;
import com.unisal.selectaapi.chat.repository.HistoricoMensagensRepository;
import com.unisal.selectaapi.company.service.CompanyAccessService;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.notificacoes.NotificacoesRole;
import com.unisal.selectaapi.notificacoes.dtos.PublishNotificacaoDto;
import com.unisal.selectaapi.notificacoes.service.NotificacaoService;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.UserRole;

@Service
public class HistoricoMensagensService {

    private final HistoricoMensagensRepository historicoMensagensRepository;
    private final CandidaturaRepository candidaturaRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final CompanyAccessService companyAccessService;
    private final NotificacaoService notificacaoService;

    public HistoricoMensagensService(
            HistoricoMensagensRepository historicoMensagensRepository,
            CandidaturaRepository candidaturaRepository,
            UserRepository userRepository,
            SimpMessagingTemplate simpMessagingTemplate,
            CompanyAccessService companyAccessService,
            NotificacaoService notificacaoService) {

        this.historicoMensagensRepository = historicoMensagensRepository;
        this.candidaturaRepository = candidaturaRepository;
        this.userRepository = userRepository;
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.companyAccessService = companyAccessService;
        this.notificacaoService = notificacaoService;
    }

    private CandidaturaEUsarioDto pegarEValidarCandidaturaEUser(
            UUID userId,
            UUID candidaturaId) {

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "Usuario nao encontrado"));

        Candidatura candidatura;

        if (UserRole.CANDIDATE.name().equals(user.getRole())) {
            candidatura = candidaturaRepository
                    .findByIdAndCandidatoUserId(candidaturaId, userId)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "Candidatura buscada nao existe"));

        } else if (UserRole.COMPANY.name().equals(user.getRole())) {
            UUID empresaId = companyAccessService
                    .resolveCompany(userId)
                    .getId();

            candidatura = candidaturaRepository
                    .findByIdAndVagaEmpresaId(candidaturaId, empresaId)
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "Candidatura buscada nao existe"));

        } else {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Erro, nao foi possivel aceitar a role de usuario");
        }

        return new CandidaturaEUsarioDto(candidatura, user);
    }

    private boolean isCandidato(CandidaturaEUsarioDto dados) {
        return UserRole.CANDIDATE.name().equals(dados.user().getRole());
    }

    private UUID candidatoUserId(Candidatura candidatura) {
        return candidatura.getCandidato().getUser().getId();
    }

    private List<UUID> usuariosDaEmpresa(Candidatura candidatura) {
        return companyAccessService.activeUserIdsOf(
                candidatura.getVaga().getEmpresa());
    }

    @Transactional(readOnly = true)
    public List<HistoricoMessageResponseDto> pegarListaDeMensagens(
            UUID userId,
            UUID candidaturaId) {

        var dados = pegarEValidarCandidaturaEUser(userId, candidaturaId);
        var candidatura = dados.candidatura();

        boolean souCandidato = isCandidato(dados);
        UUID candidatoId = candidatoUserId(candidatura);

        var lista = historicoMensagensRepository
                .findByCandidaturaOrderByCreatedTimeStampAsc(candidatura);

        return lista.stream()
                .map(registro -> {
                    var autor = registro.getUser();

                    boolean doMeuLado = souCandidato
                            ? autor != null && autor.getId().equals(userId)
                            : autor == null || !autor.getId().equals(candidatoId);

                    String nome = autor != null
                            ? autor.getName()
                            : "Usuário removido";

                    return new HistoricoMessageResponseDto(
                            nome,
                            registro.getMensagem(),
                            doMeuLado,
                            registro.getCreatedTimeStamp(),
                            registro.getLida());
                })
                .toList();
    }

    @Transactional
    public HistoricoMessageResponseDto registrarMensagem(
            CreateMessageDto dto,
            UUID userId,
            UUID candidaturaId) {

        var dados = pegarEValidarCandidaturaEUser(userId, candidaturaId);
        var candidatura = dados.candidatura();

        boolean souCandidato = isCandidato(dados);

        if (candidatura.getChatIniciadoEm() == null && souCandidato) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "Apenas empresas podem iniciar um chat");
        }

        if (candidatura.getChatIniciadoEm() == null) {
            candidatura.setChatIniciadoEm(LocalDateTime.now());
        }

        var registro = new HistoricoMensagens();
        registro.setCandidatura(candidatura);
        registro.setMensagem(dto.message());
        registro.setUser(dados.user());
        registro.setLida(false);

        var registroSaved = historicoMensagensRepository.save(registro);

        String nome = registroSaved.getUser().getName();

        var paraMeuLado = new HistoricoMessageResponseDto(
                nome,
                registroSaved.getMensagem(),
                true,
                registroSaved.getCreatedTimeStamp(),
                registroSaved.getLida());

        var paraOutroLado = new HistoricoMessageResponseDto(
                nome,
                registroSaved.getMensagem(),
                false,
                registroSaved.getCreatedTimeStamp(),
                registroSaved.getLida());

        List<UUID> empresaUsuarios = usuariosDaEmpresa(candidatura);

        if (souCandidato) {
            empresaUsuarios.forEach(
                    id -> enviarSocket(id, paraOutroLado));
        } else {
            enviarSocket(
                    candidatoUserId(candidatura),
                    paraOutroLado);

            empresaUsuarios.stream()
                    .filter(id -> !id.equals(userId))
                    .forEach(id -> enviarSocket(id, paraMeuLado));
        }

        UUID destinatarioNotificacao = souCandidato
                ? candidatura.getVaga().getEmpresa().getUser().getId()
                : candidatoUserId(candidatura);

        Long quantidadeNaoLidas = historicoMensagensRepository
                .quantidadeDeMensagensNaoLidasPeloIdDaCandidatura(
                        candidaturaId,
                        userId);

        if (quantidadeNaoLidas <= 1) {
            notificacaoService.publicarNotificacao(
                    destinatarioNotificacao,
                    new PublishNotificacaoDto(
                            "Mensagens não lidas",
                            "Há mensagens não lidas",
                            NotificacoesRole.CHAT,
                            candidaturaId.toString()));
        }

        return paraMeuLado;
    }

    @Transactional
    public void marcarMensagensComoLidas(
            UUID userId,
            UUID candidaturaId,
            Instant lidaEm) {

        var dados = pegarEValidarCandidaturaEUser(userId, candidaturaId);
        var candidatura = dados.candidatura();

        boolean souCandidato = isCandidato(dados);

        int atualizadas = souCandidato
                ? historicoMensagensRepository.marcarComoLidas(
                        candidaturaId,
                        userId,
                        lidaEm)
                : historicoMensagensRepository.marcarComoLidasDoAutor(
                        candidaturaId,
                        candidatoUserId(candidatura),
                        lidaEm);

        if (atualizadas > 0) {
            var aviso = new MensagemLidaDto(candidaturaId, lidaEm);

            if (souCandidato) {
                usuariosDaEmpresa(candidatura).forEach(
                        id -> simpMessagingTemplate.convertAndSendToUser(
                                id.toString(),
                                "/queue/leituras",
                                aviso));
            } else {
                simpMessagingTemplate.convertAndSendToUser(
                        candidatoUserId(candidatura).toString(),
                        "/queue/leituras",
                        aviso);
            }
        }
    }

    private void enviarSocket(
            UUID destinatarioId,
            HistoricoMessageResponseDto dto) {

        simpMessagingTemplate.convertAndSendToUser(
                destinatarioId.toString(),
                "/queue/mensagens",
                dto);
    }
}