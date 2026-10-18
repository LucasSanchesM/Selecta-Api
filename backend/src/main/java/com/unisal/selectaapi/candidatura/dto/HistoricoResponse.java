package com.unisal.selectaapi.candidatura.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.candidatura.entity.StatusCandidatura;

public record HistoricoResponse(

        UUID id,
        EtapaProcesso etapaAnterior,
        EtapaProcesso etapaNova,
        String etapaNovaDescricao,
        StatusCandidatura statusAnterior,
        StatusCandidatura statusNovo,
        String observacao,
        String autorNome,
        Instant createdAt

) {
}
