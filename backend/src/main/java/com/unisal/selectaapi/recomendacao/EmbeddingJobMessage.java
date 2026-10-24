package com.unisal.selectaapi.recomendacao;

import java.util.UUID;

public record EmbeddingJobMessage(EmbeddingTarget target, UUID id) {
}
