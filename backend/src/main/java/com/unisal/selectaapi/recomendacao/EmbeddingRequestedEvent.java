package com.unisal.selectaapi.recomendacao;

import java.util.UUID;

public record EmbeddingRequestedEvent(EmbeddingTarget target, UUID id) {

    public static EmbeddingRequestedEvent candidato(UUID candidateProfileId) {
        return new EmbeddingRequestedEvent(EmbeddingTarget.CANDIDATO, candidateProfileId);
    }

    public static EmbeddingRequestedEvent vaga(UUID vagaId) {
        return new EmbeddingRequestedEvent(EmbeddingTarget.VAGA, vagaId);
    }
}
