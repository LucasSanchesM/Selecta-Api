package com.unisal.selectaapi.candidatura.service;

import java.util.List;
import java.util.UUID;

import com.unisal.selectaapi.vagas.entity.TipoDeEtapa;
import com.unisal.selectaapi.vagas.entity.VagaEtapa;
import com.unisal.selectaapi.vagas.repository.TipoDeEtapaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.candidatura.dto.CreateFeedbackDto;
import com.unisal.selectaapi.candidatura.dto.FeedbackResponse;
import com.unisal.selectaapi.candidatura.entity.Candidatura;
import com.unisal.selectaapi.candidatura.entity.CandidaturaFeedback;
import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.candidatura.repository.CandidaturaFeedbackRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.entity.User;

@Service
public class CandidaturaFeedbackService {

    private final CandidaturaFeedbackRepository feedbackRepository;
    private final CandidaturaService candidaturaService;
    private final UserRepository userRepository;
    private final CandidaturaMapper mapper;
    private final TipoDeEtapaRepository tipoDeEtapaRepository;

    public CandidaturaFeedbackService(CandidaturaFeedbackRepository feedbackRepository,
                                      CandidaturaService candidaturaService,
                                      UserRepository userRepository,
                                      CandidaturaMapper mapper, TipoDeEtapaRepository tipoDeEtapaRepository) {
        this.feedbackRepository = feedbackRepository;
        this.candidaturaService = candidaturaService;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.tipoDeEtapaRepository = tipoDeEtapaRepository;
    }

    @Transactional
    public FeedbackResponse criar(UUID empresaUserId, UUID candidaturaId, CreateFeedbackDto dto) {
        Candidatura candidatura = candidaturaService.buscarComoEmpresa(empresaUserId, candidaturaId);
        var etapaInformada = tipoDeEtapaRepository.findByIdDisponivelParaEmpresa(dto.etapaId(), empresaUserId)
                                                    .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "Etapa informada nao existe"));

        // Não faz sentido dar feedback de uma etapa que o processo ainda não alcançou
        validarEtapaDoFeedback(candidatura, etapaInformada);

        User autor = userRepository.findById(empresaUserId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        var feedback = new CandidaturaFeedback(
                candidatura,
                etapaInformada,
                dto.titulo(),
                dto.mensagem(),
                dto.nota(),
                autor.getId(),
                autor.getName());

        CandidaturaFeedback salvo = feedbackRepository.save(feedback);

        candidaturaService.notificarCandidato(
                candidatura,
                "Novo feedback na etapa " + etapaInformada.getNome() + ": " + dto.titulo());

        return mapper.toFeedbackResponse(salvo);
    }

    @Transactional
    public FeedbackResponse atualizar(UUID empresaUserId, UUID candidaturaId, UUID feedbackId, CreateFeedbackDto dto) {
        var candidatura = candidaturaService.buscarComoEmpresa(empresaUserId, candidaturaId);
        var etapaInformada = tipoDeEtapaRepository.findByIdDisponivelParaEmpresa(dto.etapaId(), empresaUserId)
                .orElseThrow(()-> new BusinessException(HttpStatus.NOT_FOUND, "Etapa informada nao existe"));

        validarEtapaDoFeedback(candidatura, etapaInformada);

        CandidaturaFeedback feedback = buscarFeedback(candidaturaId, feedbackId);

        feedback.setEtapa(etapaInformada);
        feedback.setTitulo(dto.titulo());
        feedback.setMensagem(dto.mensagem());
        feedback.setNota(dto.nota());

        return mapper.toFeedbackResponse(feedbackRepository.save(feedback));
    }

    @Transactional
    public void deletar(UUID empresaUserId, UUID candidaturaId, UUID feedbackId) {
        candidaturaService.buscarComoEmpresa(empresaUserId, candidaturaId);
        feedbackRepository.delete(buscarFeedback(candidaturaId, feedbackId));
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> listarComoEmpresa(UUID empresaUserId, UUID candidaturaId, UUID etapaId) {
        candidaturaService.buscarComoEmpresa(empresaUserId, candidaturaId);
        TipoDeEtapa etapaInformada = null;
        if(etapaId!=null) {
            etapaInformada = tipoDeEtapaRepository.findByIdDisponivelParaEmpresa(etapaId, empresaUserId)
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Etapa informada nao existe"));
        }
        return listar(candidaturaId, etapaInformada);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> listarComoCandidato(UUID userId, UUID candidaturaId, UUID etapaId) {
        var candidatura = candidaturaService.buscarComoCandidato(userId, candidaturaId);
        TipoDeEtapa etapaInformada = null;
        if(etapaId!=null) {
            etapaInformada = tipoDeEtapaRepository.findByIdDisponivelParaEmpresa(etapaId, candidatura.getVaga().getEmpresa().getId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Etapa informada nao existe"));
        }

        return listar(candidaturaId, etapaInformada);
    }

    private List<FeedbackResponse> listar(UUID candidaturaId, TipoDeEtapa etapa) {
        var feedbacks = etapa != null
                ? feedbackRepository.findByCandidaturaIdAndEtapaOrderByCreatedAtAsc(candidaturaId, etapa)
                : feedbackRepository.findByCandidaturaIdOrderByCreatedAtAsc(candidaturaId);

        return feedbacks.stream().map(mapper::toFeedbackResponse).toList();
    }

    private CandidaturaFeedback buscarFeedback(UUID candidaturaId, UUID feedbackId) {
        return feedbackRepository.findByIdAndCandidaturaId(feedbackId, candidaturaId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Feedback não encontrado para esta candidatura"));
    }

    //Em resumo aqui ele valida de a etapa informada é valida, ou seja, se não esta tentando acessar por exemplo uma etapa do futuro
    private void validarEtapaDoFeedback(
            Candidatura candidatura,
            TipoDeEtapa etapaInformada) {

        var etapas = candidatura.getVaga().getEtapas();

        int ordemAtual = etapas.stream()
                .filter(e -> e.getEtapa().getId()
                        .equals(candidatura.getEtapaAtual().getId()))
                .mapToInt(VagaEtapa::getOrdem)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "A etapa atual não está configurada nesta vaga"
                ));

        int ordemInformada = etapas.stream()
                .filter(e -> e.getEtapa().getId()
                        .equals(etapaInformada.getId()))
                .mapToInt(VagaEtapa::getOrdem)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.BAD_REQUEST,
                        "A etapa informada não pertence a esta vaga"
                ));

        if (ordemInformada > ordemAtual) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "Não é possível registrar feedback de uma etapa futura. Etapa atual: "
                            + candidatura.getEtapaAtual().getNome()
            );
        }
    }
}
