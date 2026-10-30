package com.unisal.selectaapi.candidatura.dto;

import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFeedbackDto(

        @NotNull(message = "etapa é obrigatória") UUID etapaId,

        @NotBlank(message = "titulo é obrigatório") @Size(max = 150, message = "o título deve ter no máximo 150 caracteres") String titulo,

        @NotBlank(message = "mensagem é obrigatória") @Size(max = 5000, message = "a mensagem deve ter no máximo 5000 caracteres") String mensagem,

        @Min(value = 1, message = "a nota mínima é 1") @Max(value = 5, message = "a nota máxima é 5") Integer nota

) {
}
