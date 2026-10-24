
package com.unisal.selectaapi.vagas.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;
import com.unisal.selectaapi.vagas.VagaRole;
import com.unisal.selectaapi.vagas.entity.ModalidadeVaga;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateVagaDto(

    @NotBlank(message = "O campo 'titulo' é obrigatório")
    String titulo,

    @NotNull(message = "O campo 'salario' é obrigatório")
    @DecimalMin(value = "0.0", inclusive = true, message = "O salário não pode ser negativo")
    BigDecimal salario,

    @Min(value = 1, message = "A vaga deve ter pelo menos 1 posição disponível")
    Integer quantidadeVagas,

    @NotBlank(message = "O campo 'descricao' é obrigatório")
    String descricao,

    @NotBlank(message = "O campo 'beneficios' é obrigatório")
    String beneficios,

    @NotNull(message = "O campo 'companyProfileId' é obrigatório")
    UUID companyProfileId,

    @NotNull(message = "O campo 'categoria' é obrigatório")
    VagaRole categoria,

    @NotNull(message = "O campo 'modalidade' é obrigatório")
    ModalidadeVaga modalidade,

    @NotNull(message = "O campo 'latitude' é obrigatorio")
    Double latitude,

    @NotNull(message = "O campo 'longitude' é obrigatorio")
    Double longitude,

    @Size(max = 5, message = "A vaga pode ter no máximo 5 etapas configuráveis")
    List<EtapaProcesso> etapas

) {}