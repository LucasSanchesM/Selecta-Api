package com.unisal.selectaapi.candidatura.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.unisal.selectaapi.company.service.CompanyMemberService;
import com.unisal.selectaapi.notificacoes.NotificacoesRole;
import com.unisal.selectaapi.notificacoes.dtos.PublishNotificacaoDto;
import com.unisal.selectaapi.notificacoes.service.NotificacaoService;
import com.unisal.selectaapi.vagas.entity.Vaga;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.candidate.entity.CandidateProfile;
import com.unisal.selectaapi.candidate.repository.CandidateProfileRepository;
import com.unisal.selectaapi.candidatura.CandidaturaSpecification;
import com.unisal.selectaapi.candidatura.dto.AvancarEtapaDto;
import com.unisal.selectaapi.candidatura.dto.CandidaturaDetalheResponse;
import com.unisal.selectaapi.candidatura.dto.CandidaturaResponse;
import com.unisal.selectaapi.candidatura.dto.CreateCandidatura;
import com.unisal.selectaapi.candidatura.dto.DecidirCandidaturaDto;
import com.unisal.selectaapi.candidatura.entity.Candidatura;
import com.unisal.selectaapi.candidatura.entity.CandidaturaAgendamento;
import com.unisal.selectaapi.candidatura.entity.CandidaturaHistorico;
import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.candidatura.entity.StatusCandidatura;
import com.unisal.selectaapi.candidatura.repository.CandidaturaFeedbackRepository;
import com.unisal.selectaapi.candidatura.repository.CandidaturaHistoricoRepository;
import com.unisal.selectaapi.candidatura.repository.CandidaturaAgendamentoRepository;
import com.unisal.selectaapi.candidatura.repository.CandidaturaRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.entity.User;
import com.unisal.selectaapi.vagas.repository.VagaRepository;

@Service
public class CandidaturaService {

    private final CandidaturaRepository candidaturaRepository;
    private final CandidaturaHistoricoRepository historicoRepository;
    private final CandidaturaFeedbackRepository feedbackRepository;
    private final CandidaturaAgendamentoRepository agendamentoRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final VagaRepository vagaRepository;
    private final UserRepository userRepository;
    private final CandidaturaMapper mapper;
    private final RabbitTemplate rabbitTemplate;
    private final NotificacaoService notificacaoService;
    private final CompanyMemberService companyMemberService;

    public CandidaturaService(CandidaturaRepository candidaturaRepository,
                              CandidaturaHistoricoRepository historicoRepository,
                              CandidaturaFeedbackRepository feedbackRepository,
                              CandidaturaAgendamentoRepository agendamentoRepository,
                              CandidateProfileRepository candidateProfileRepository,
                              VagaRepository vagaRepository,
                              UserRepository userRepository,
                              CandidaturaMapper mapper,
                              RabbitTemplate rabbitTemplate, NotificacaoService notificacaoService, CompanyMemberService companyMemberService) {
        this.candidaturaRepository = candidaturaRepository;
        this.historicoRepository = historicoRepository;
        this.feedbackRepository = feedbackRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.vagaRepository = vagaRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.rabbitTemplate = rabbitTemplate;
        this.notificacaoService = notificacaoService;
        this.companyMemberService = companyMemberService;
    }

    @Transactional
    public CandidaturaResponse candidatar(UUID userId, CreateCandidatura dto) {

        Vaga vaga = vagaRepository.findById(dto.vagaId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Vaga não encontrada com o id: " + dto.vagaId()));

        if (Boolean.FALSE.equals(vaga.getAtiva())) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Esta vaga não está mais recebendo candidaturas");
        }

        CandidateProfile candidato = findOrCreateCandidateProfile(userId);

        if (candidaturaRepository.existsByVagaIdAndCandidatoUserId(vaga.getId(), userId)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Você já se candidatou para esta vaga");
        }

        var candidatura = new Candidatura(vaga, candidato, dto.cartaApresentacao(), dto.curriculoUrl());
        var salva = candidaturaRepository.save(candidatura);

        registrarHistorico(
                salva,
                null,
                EtapaProcesso.INSCRICAO,
                null,
                StatusCandidatura.EM_ANDAMENTO,
                "Candidatura enviada pelo candidato",
                candidato.getUser().getId(),
                candidato.getUser().getName());

        return mapper.toResponse(salva, 0);
    }

    @Transactional(readOnly = true)
    public List<CandidaturaResponse> listarMinhasCandidaturas(UUID userId) {
        return candidaturaRepository.findByCandidatoUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(candidatura -> mapper.toResponse(
                        candidatura,
                        feedbackRepository.findByCandidaturaIdOrderByCreatedAtAsc(candidatura.getId()).size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CandidaturaDetalheResponse detalharComoCandidato(UUID userId, UUID candidaturaId) {
        Candidatura candidatura = buscarComoCandidato(userId, candidaturaId);
        return montarDetalhe(candidatura);
    }

    @Transactional
    public CandidaturaResponse cancelar(UUID userId, UUID candidaturaId) {
        Candidatura candidatura = buscarComoCandidato(userId, candidaturaId);

        if (candidatura.getStatus().isFinalizada()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Esta candidatura já foi finalizada e não pode ser cancelada");
        }

        StatusCandidatura statusAnterior = candidatura.getStatus();
        candidatura.setStatus(StatusCandidatura.CANCELADA);
        candidatura.setFinalizadaEm(Instant.now());
        candidatura.setMotivoEncerramento("Cancelada pelo candidato");

        Candidatura salva = candidaturaRepository.save(candidatura);
        User autor = candidatura.getCandidato().getUser();

        registrarHistorico(
                salva,
                salva.getEtapaAtual(),
                salva.getEtapaAtual(),
                statusAnterior,
                StatusCandidatura.CANCELADA,
                "Candidatura cancelada pelo candidato",
                autor.getId(),
                autor.getName());

        return mapper.toResponse(salva, contarFeedbacks(salva.getId()));
    }

    // lado que a empresa usa

    @Transactional(readOnly = true)
    public List<CandidaturaResponse> listarCandidaturasDaEmpresa(UUID empresaId,
            UUID vagaId,
            StatusCandidatura status,
            EtapaProcesso etapa,
            String candidatoNome) {

        var spec = CandidaturaSpecification.withFilters(empresaId, vagaId, status, etapa, candidatoNome);

        return candidaturaRepository.findAll(spec)
                .stream()
                .map(candidatura -> mapper.toResponse(candidatura, contarFeedbacks(candidatura.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CandidaturaDetalheResponse detalharComoEmpresa(UUID empresaUserId, UUID candidaturaId) {
        Candidatura candidatura = buscarComoEmpresa(empresaUserId, candidaturaId);
        return montarDetalhe(candidatura);
    }

    @Transactional
    public CandidaturaResponse avancarEtapa(UUID empresaUserId, UUID candidaturaId, AvancarEtapaDto dto) {
        Candidatura candidatura = buscarComoEmpresa(empresaUserId, candidaturaId);

        if (candidatura.getStatus().isFinalizada()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Não é possível alterar a etapa de uma candidatura finalizada");
        }

        EtapaProcesso etapaAnterior = candidatura.getEtapaAtual();
        List<EtapaProcesso> fluxo = candidatura.getVaga().getEtapas().stream()
            .map(etapa -> etapa.getEtapa())
            .toList();

        if (fluxo.isEmpty()) {
            fluxo = EtapaProcesso.etapasConfiguraveisPadrao();
        }

        int indiceEtapaAtual = fluxo.indexOf(etapaAnterior);
        EtapaProcesso proximaEtapa = indiceEtapaAtual + 1 < fluxo.size()
            ? fluxo.get(indiceEtapaAtual + 1)
            : null;

        if (proximaEtapa == null || dto.etapa() != proximaEtapa) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                "A próxima etapa deve ser " + (proximaEtapa == null
                    ? "inexistente, pois o fluxo já foi concluído"
                    : proximaEtapa.getDescricao()));
        }

        candidatura.setEtapaAtual(dto.etapa());
        Candidatura salva = candidaturaRepository.save(candidatura);
        User autor = buscarUsuario(empresaUserId);

        registrarHistorico(
                salva,
                etapaAnterior,
                dto.etapa(),
                salva.getStatus(),
                salva.getStatus(),
                dto.observacao(),
                autor.getId(),
                autor.getName());

        notificarCandidato(salva, dto.observacao());

        return mapper.toResponse(salva, contarFeedbacks(salva.getId()));
    }

    @Transactional
    public CandidaturaResponse decidir(UUID empresaUserId, UUID candidaturaId, DecidirCandidaturaDto dto) {
        Candidatura candidatura = buscarComoEmpresa(empresaUserId, candidaturaId);

        if (candidatura.getStatus().isFinalizada()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Esta candidatura já foi finalizada");
        }

        if (dto.status() != StatusCandidatura.APROVADA && dto.status() != StatusCandidatura.REPROVADA) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "A empresa só pode definir o status como APROVADA ou REPROVADA");
        }

        StatusCandidatura statusAnterior = candidatura.getStatus();
        EtapaProcesso etapaAnterior = candidatura.getEtapaAtual();

        candidatura.setStatus(dto.status());
        candidatura.setMotivoEncerramento(dto.motivo());
        candidatura.setFinalizadaEm(Instant.now());

        if (dto.status() == StatusCandidatura.APROVADA) {
            candidatura.setEtapaAtual(EtapaProcesso.CONTRATACAO);
        }

        Candidatura salva = candidaturaRepository.save(candidatura);
        User autor = buscarUsuario(empresaUserId);

        registrarHistorico(
                salva,
                etapaAnterior,
                salva.getEtapaAtual(),
                statusAnterior,
                dto.status(),
                dto.motivo(),
                autor.getId(),
                autor.getName());

        notificarCandidato(salva, dto.motivo());

        return mapper.toResponse(salva, contarFeedbacks(salva.getId()));
    }

    public Candidatura buscarComoCandidato(UUID userId, UUID candidaturaId) {
        return candidaturaRepository.findByIdAndCandidatoUserId(candidaturaId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Candidatura não encontrada para este candidato"));
    }

    public Candidatura buscarComoEmpresa(UUID empresaUserId, UUID candidaturaId) {
        return candidaturaRepository.findByIdAndVagaEmpresaUserId(candidaturaId, empresaUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Candidatura não encontrada para as vagas desta empresa"));
    }

    public void notificarCandidato(Candidatura candidatura, String mensagem) {
        notificarCandidato(candidatura, mensagem, null);
    }

    public void notificarCandidato(Candidatura candidatura, String mensagem, CandidaturaAgendamento agendamento) {
        User candidato = candidatura.getCandidato().getUser();

        var request = new CandidaturaEmailRequest(
                candidato.getEmail(),
                candidato.getName(),
                candidatura.getVaga().getTitulo(),
                candidatura.getVaga().getEmpresa().getCompanyName(),
                candidatura.getEtapaAtual().getDescricao(),
                candidatura.getStatus().getDescricao(),
                mensagem,
                agendamento != null ? agendamento.getTitulo() : null,
                agendamento != null ? agendamento.getInicio() : null,
                agendamento != null ? agendamento.getDuracaoMinutos() : null,
                agendamento != null ? agendamento.getLink() : null);

        rabbitTemplate.convertAndSend("email.exchange", "candidatura.atualizacao.key", request);

        boolean temAgendamento = agendamento != null;
        if(temAgendamento) {
            notificacaoService.publicarNotificacao(candidatura.getCandidato().getUser().getId(),
                    new PublishNotificacaoDto("Agendamento de conversa",
                            mensagem + " Vaga: " + candidatura.getVaga().getTitulo() + ".",
                            NotificacoesRole.AGENDAMENTO,
                            agendamento.getId().toString()));
        }
        else{
            String descricao = (mensagem == null || mensagem.isBlank())
                    ? "Sua candidatura agora está na etapa: " + candidatura.getEtapaAtual().getDescricao() + "."
                    : mensagem;
            notificacaoService.publicarNotificacao(candidatura.getCandidato().getUser().getId(),
                    new PublishNotificacaoDto("Atualização na Candidatura",
                            descricao , NotificacoesRole.CANDIDATURA_ATUALIZACAO, candidatura.getId().toString()));
        }
    }

    private CandidaturaDetalheResponse montarDetalhe(Candidatura candidatura) {
        return mapper.toDetalheResponse(
                candidatura,
                historicoRepository.findByCandidaturaIdOrderByCreatedAtAsc(candidatura.getId()),
                feedbackRepository.findByCandidaturaIdOrderByCreatedAtAsc(candidatura.getId()),
                agendamentoRepository.findByCandidaturaIdOrderByInicioAsc(candidatura.getId()));
    }

    private long contarFeedbacks(UUID candidaturaId) {
        return feedbackRepository.findByCandidaturaIdOrderByCreatedAtAsc(candidaturaId).size();
    }

    private void registrarHistorico(Candidatura candidatura,
            EtapaProcesso etapaAnterior,
            EtapaProcesso etapaNova,
            StatusCandidatura statusAnterior,
            StatusCandidatura statusNovo,
            String observacao,
            UUID autorUserId,
            String autorNome) {

        var registro = new CandidaturaHistorico(
                candidatura,
                etapaAnterior,
                etapaNova,
                statusAnterior,
                statusNovo,
                observacao,
                autorUserId,
                autorNome);

        historicoRepository.save(registro);
    }

    private User buscarUsuario(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    /**
     * Mesma regra usada no CandidateProfileService: o perfil é criado sob demanda,
     * então o candidato consegue se candidatar mesmo sem ter aberto o perfil antes.
     */

    private CandidateProfile findOrCreateCandidateProfile(UUID userId) {
        return candidateProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = buscarUsuario(userId);
                    if (!"CANDIDATE".equals(user.getRole())) {
                        throw new BusinessException(HttpStatus.FORBIDDEN,
                                "Apenas candidatos podem se candidatar a vagas");
                    }
                    CandidateProfile profile = new CandidateProfile(user);
                    profile.setProfileImageUrl(user.getAvatarUrl());
                    return candidateProfileRepository.save(profile);
                });
    }
}
