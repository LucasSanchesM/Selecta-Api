
package com.unisal.selectaapi.vagas.dto;

import java.math.BigDecimal;

import com.unisal.selectaapi.vagas.VagaRole;
import com.unisal.selectaapi.vagas.entity.ModalidadeVaga;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

public record   UpdateVaga(

    String titulo,

    @DecimalMin(value = "0.0", message = "O salário não pode ser negativo")
    BigDecimal salario,

    @Min(value = 1, message = "A vaga deve ter pelo menos 1 posição disponível")
    Integer quantidadeVagas,

    String descricao,
    String beneficios,
    Double latitude,
    Double longitude,
    VagaRole categoria,
    ModalidadeVaga modalidade,
    Boolean ativa,
    Boolean salarioACombinar
) {
    public UpdateVaga(String titulo, BigDecimal salario, Integer quantidadeVagas, String descricao,
            String beneficios, Double latitude, Double longitude, VagaRole categoria,
            ModalidadeVaga modalidade, Boolean ativa) {
        this(titulo, salario, quantidadeVagas, descricao, beneficios, latitude, longitude,
                categoria, modalidade, ativa, null);
    }
}