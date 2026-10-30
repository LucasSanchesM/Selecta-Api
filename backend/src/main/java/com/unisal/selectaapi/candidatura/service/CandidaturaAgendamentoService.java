package com.unisal.selectaapi.candidatura.service;

import java.util.List;
import java.util.UUID;

import com.unisal.selectaapi.notificacoes.service.NotificacaoService;
import com.unisal.selectaapi.vagas.entity.TipoDeEtapa;
import com.unisal.selectaapi.vagas.entity.VagaEtapa;
import com.unisal.selectaapi.vagas.repository.TipoDeEtapaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.candidatura.dto.AgendamentoResponse;
import com.unisal.selectaapi.candidatura.dto.CreateAgendamentoDto;
import com.unisal.selectaapi.candidatura.entity.Candidatura;
import com.unisal.selectaapi.candidatura.entity.CandidaturaAgendamento;
import com.unisal.selectaapi.candidatura.entity.StatusAgendamento;
import com.unisal.selectaapi.candidatura.repository.CandidaturaAgendamentoRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.entity.User;

@Service
public class CandidaturaAgendamentoService {

    private final CandidaturaAgendamentoRepository agendamentoRepository;
    private final CandidaturaService candidaturaService;
    private final UserRepository userRepository;
    private final CandidaturaMapper mapper;
    private final TipoDeEtapaRepository tipoDeEtapaRepository;

    public CandidaturaAgendamentoService(CandidaturaAgendamentoRepository agendamentoRepository, CandidaturaService candidaturaService, UserRepository userRepository, CandidaturaMapper mapper, TipoDeEtapaRepository tipoDeEtapaRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.candidaturaService = candidaturaService;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.tipoDeEtapaRepository = tipoDeEtapaRepository;
    }

    @Transactional
    public AgendamentoResponse criar(UUID empresaUserId, UUID candidaturaId, CreateAgendamentoDto dto) {
        Candidatura candidatura = candidaturaService.buscarComoEmpresa(empresaUserId, candidaturaId);
        validarCandidaturaAtiva(candidatura);

        User autor = buscarUsuario(empresaUserId);
        var etapa = buscarEtapaValida(candidatura, dto.etapaId());

        CandidaturaAgendamento agendamento = new CandidaturaAgendamento(
                candidatura, etapa, dto.titulo(), dto.mensagem(), dto.inicio(), dto.duracaoMinutos(),
                dto.link(), autor.getId(), autor.getName());

        CandidaturaAgendamento salvo = agendamentoRepository.save(agendamento);
        notificar(candidatura, "Você recebeu um novo convite para uma reunião.", salvo);
        return mapper.toAgendamentoResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listarComoEmpresa(UUID empresaUserId, UUID candidaturaId) {
        candidaturaService.buscarComoEmpresa(empresaUserId, candidaturaId);
        return listar(candidaturaId);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponse> listarComoCandidato(UUID userId, UUID candidaturaId) {
        candidaturaService.buscarComoCandidato(userId, candidaturaId);
        return listar(candidaturaId);
    }

    @Transactional
    public AgendamentoResponse atualizar(UUID empresaUserId, UUID candidaturaId, UUID agendamentoId,
            CreateAgendamentoDto dto) {
        Candidatura candidatura = candidaturaService.buscarComoEmpresa(empresaUserId, candidaturaId);
        validarCandidaturaAtiva(candidatura);

        CandidaturaAgendamento agendamento = buscar(candidaturaId, agendamentoId);
        if (agendamento.getStatus().isFinalizado()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Este agendamento já foi encerrado");
        }
        var etapa = buscarEtapaValida(candidatura, dto.etapaId());

        agendamento.setEtapa(etapa);
        agendamento.setTitulo(dto.titulo());
        agendamento.setMensagem(dto.mensagem());
        agendamento.setInicio(dto.inicio());
        agendamento.setDuracaoMinutos(dto.duracaoMinutos());
        agendamento.setLink(dto.link());
        agendamento.setStatus(StatusAgendamento.PENDENTE);

        CandidaturaAgendamento salvo = agendamentoRepository.save(agendamento);
        notificar(candidatura, "O convite da reunião foi atualizado.", salvo);
        return mapper.toAgendamentoResponse(salvo);
    }

    @Transactional
    public void cancelar(UUID empresaUserId, UUID candidaturaId, UUID agendamentoId, String motivo) {
        Candidatura candidatura = candidaturaService.buscarComoEmpresa(empresaUserId, candidaturaId);
        CandidaturaAgendamento agendamento = buscar(candidaturaId, agendamentoId);

        if (agendamento.getStatus().isFinalizado()) {
            throw new BusinessException(HttpStatus.CONFLICT, "Este agendamento já foi encerrado");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setMotivoCancelamento(motivo);
        agendamentoRepository.save(agendamento);
        notificar(candidatura, "O agendamento foi cancelado pela empresa.", agendamento);
    }

    @Transactional
    public AgendamentoResponse confirmar(UUID userId, UUID candidaturaId, UUID agendamentoId) {
        Candidatura candidatura = candidaturaService.buscarComoCandidato(userId, candidaturaId);
        validarCandidaturaAtiva(candidatura);
        CandidaturaAgendamento agendamento = buscar(candidaturaId, agendamentoId);
        validarPendente(agendamento);

        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        CandidaturaAgendamento salvo = agendamentoRepository.save(agendamento);
        return mapper.toAgendamentoResponse(salvo);
    }

    @Transactional
    public AgendamentoResponse recusar(UUID userId, UUID candidaturaId, UUID agendamentoId) {
        Candidatura candidatura = candidaturaService.buscarComoCandidato(userId, candidaturaId);
        validarCandidaturaAtiva(candidatura);
        CandidaturaAgendamento agendamento = buscar(candidaturaId, agendamentoId);
        validarPendente(agendamento);

        agendamento.setStatus(StatusAgendamento.RECUSADO);
        CandidaturaAgendamento salvo = agendamentoRepository.save(agendamento);
        return mapper.toAgendamentoResponse(salvo);
    }

    private List<AgendamentoResponse> listar(UUID candidaturaId) {
        return agendamentoRepository.findByCandidaturaIdOrderByInicioAsc(candidaturaId)
                .stream().map(mapper::toAgendamentoResponse).toList();
    }

    private CandidaturaAgendamento buscar(UUID candidaturaId, UUID agendamentoId) {
        return agendamentoRepository.findByIdAndCandidaturaId(agendamentoId, candidaturaId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Agendamento não encontrado para esta candidatura"));
    }

    private User buscarUsuario(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    private void validarCandidaturaAtiva(Candidatura candidatura) {
        if (candidatura.getStatus().isFinalizada()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Não é possível agendar uma candidatura finalizada");
        }
    }

    private TipoDeEtapa buscarEtapaValida(Candidatura candidatura, UUID etapaId) {
        var etapa = tipoDeEtapaRepository.findById(etapaId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "A etapa informada não existe"));

        var etapas = candidatura.getVaga().getEtapas();

        int ordemEtapa = etapas.stream()
                .filter(ve -> ve.getEtapa().getId().equals(etapa.getId()))
                .mapToInt(VagaEtapa::getOrdem)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST, "A etapa informada não pertence a esta vaga"));

        int ordemAtual = etapas.stream()
                .filter(ve -> ve.getEtapa().getId().equals(candidatura.getEtapaAtual().getId()))
                .mapToInt(VagaEtapa::getOrdem)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST, "A etapa atual não está configurada nesta vaga"));

        if (ordemEtapa > ordemAtual) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "Não é possível agendar uma etapa futura");
        }

        return etapa;
    }

    private void validarPendente(CandidaturaAgendamento agendamento) {
        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Somente agendamentos pendentes podem ser confirmados ou recusados");
        }
    }

    private void notificar(Candidatura candidatura, String mensagem, CandidaturaAgendamento agendamento) {
        candidaturaService.notificarCandidato(candidatura, mensagem, agendamento);
    }
}