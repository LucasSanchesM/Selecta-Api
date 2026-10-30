package com.unisal.selectaapi.candidatura.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.vagas.entity.TipoDeEtapa;

public record FeedbackResponse(

        UUID id,
        UUID candidaturaId,
        UUID etapaId,
        String etapaDescricao,
        String titulo,
        String mensagem,
        Integer nota,
        String autorNome,
        Instant createdAt,
        Instant updatedAt

) {
}
