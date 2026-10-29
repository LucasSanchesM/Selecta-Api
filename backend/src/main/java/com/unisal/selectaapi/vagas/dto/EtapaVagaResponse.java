package com.unisal.selectaapi.vagas.dto;

import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;

import java.util.UUID;

public record EtapaVagaResponse(
        UUID etapaId,
        String nome,
        int ordem) {
}