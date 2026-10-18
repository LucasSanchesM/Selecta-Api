package com.unisal.selectaapi.recomendacao;

import java.util.UUID;

/**
 * Published inside the business transaction. It is only forwarded to RabbitMQ
 * after the commit, so the worker always reads the persisted data.
 */
public record EmbeddingRequestedEvent(EmbeddingTarget target, UUID id) {

    public static EmbeddingRequestedEvent candidato(UUID candidateProfileId) {
        return new EmbeddingRequestedEvent(EmbeddingTarget.CANDIDATO, candidateProfileId);
    }

    public static EmbeddingRequestedEvent vaga(UUID vagaId) {
        return new EmbeddingRequestedEvent(EmbeddingTarget.VAGA, vagaId);
    }
}
