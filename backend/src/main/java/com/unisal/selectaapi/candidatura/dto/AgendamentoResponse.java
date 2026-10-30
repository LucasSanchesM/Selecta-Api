package com.unisal.selectaapi.candidatura.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.candidatura.entity.StatusAgendamento;

public record AgendamentoResponse(
        UUID id,
        UUID candidaturaId,
        UUID etapaId,
        String etapaDescricao,
        String titulo,
        String mensagem,
        Instant inicio,
        Integer duracaoMinutos,
        String link,
        StatusAgendamento status,
        String statusDescricao,
        UUID criadoPorUserId,
        String criadoPorNome,
        String motivoCancelamento,
        Instant createdAt,
        Instant updatedAt
) {
}