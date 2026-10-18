package com.unisal.selectaapi.candidatura.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;

public record FeedbackResponse(

        UUID id,
        UUID candidaturaId,
        EtapaProcesso etapa,
        String etapaDescricao,
        String titulo,
        String mensagem,
        Integer nota,
        String autorNome,
        Instant createdAt,
        Instant updatedAt

) {
}
