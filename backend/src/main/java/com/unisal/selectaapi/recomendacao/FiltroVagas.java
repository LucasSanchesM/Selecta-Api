package com.unisal.selectaapi.recomendacao;

import java.math.BigDecimal;
import java.util.UUID;

import com.unisal.selectaapi.vagas.VagaRole;
import com.unisal.selectaapi.vagas.entity.ModalidadeVaga;

public record FiltroVagas(
        String titulo,
        VagaRole categoria,
        ModalidadeVaga modalidade,
        BigDecimal salarioMin,
        BigDecimal salarioMax,
        String cidade,
        String estado,
        Boolean ativa,
        UUID companyProfileId) {
}
