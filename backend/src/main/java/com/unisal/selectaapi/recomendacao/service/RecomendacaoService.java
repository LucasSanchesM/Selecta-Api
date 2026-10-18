package com.unisal.selectaapi.recomendacao.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.candidate.entity.CandidateProfile;
import com.unisal.selectaapi.candidate.repository.CandidateProfileRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.recomendacao.entity.TipoEmbedding;
import com.unisal.selectaapi.vagas.entity.Vaga;

/**
 * Personaliza a ordem das vagas para o candidato logado. Nunca bloqueia a listagem:
 * sem perfil, com perfil vazio ou com a API de embeddings fora do ar, a ordem original é mantida.
 */
@Service
public class RecomendacaoService {

    private static final Logger log = LoggerFactory.getLogger(RecomendacaoService.class);

    private final CandidateProfileRepository candidateProfileRepository;
    private final EmbeddingService embeddingService;

    public RecomendacaoService(CandidateProfileRepository candidateProfileRepository,
            EmbeddingService embeddingService) {
        this.candidateProfileRepository = candidateProfileRepository;
        this.embeddingService = embeddingService;
    }

    /** Vetor do perfil + currículo do usuário, ou vazio quando ele não é um candidato com perfil preenchido. */
    @Transactional(readOnly = true)
    public Optional<float[]> vetorDoCandidato(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }

        Optional<CandidateProfile> perfil = candidateProfileRepository.findByUserId(userId);
        if (perfil.isEmpty()) {
            return Optional.empty();
        }

        String texto = TextoEmbedding.doCandidato(perfil.get());
        if (texto.isBlank()) {
            return Optional.empty();
        }

        UUID perfilId = perfil.get().getId();
        try {
            return Optional.ofNullable(embeddingService
                    .obterVetores(TipoEmbedding.CANDIDATO, Map.of(perfilId, texto))
                    .get(perfilId));
        } catch (BusinessException exception) {
            log.warn("Listando vagas sem personalização para o usuário {}: {}", userId, exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Ordena da vaga mais parecida para a menos parecida. A ordenação é estável,
     * então vagas empatadas mantêm a ordem em que chegaram.
     */
    public List<Vaga> ordenarPorRelevancia(float[] vetorCandidato, List<Vaga> vagas) {
        if (vetorCandidato == null || vagas.size() < 2) {
            return vagas;
        }

        Map<UUID, String> textosVagas = new LinkedHashMap<>();
        for (Vaga vaga : vagas) {
            String texto = TextoEmbedding.daVaga(vaga);
            if (!texto.isBlank()) {
                textosVagas.put(vaga.getId(), texto);
            }
        }

        Map<UUID, float[]> vetoresVagas;
        try {
            vetoresVagas = embeddingService.obterVetores(TipoEmbedding.VAGA, textosVagas);
        } catch (BusinessException exception) {
            log.warn("Mantendo a ordem padrão das vagas: {}", exception.getMessage());
            return vagas;
        }

        Map<UUID, Double> scores = new HashMap<>();
        vagas.forEach(vaga -> scores.put(vaga.getId(),
                Similaridade.cosseno(vetorCandidato, vetoresVagas.get(vaga.getId()))));

        return vagas.stream()
                .sorted(Comparator.comparingDouble((Vaga vaga) -> scores.get(vaga.getId())).reversed())
                .toList();
    }
}
