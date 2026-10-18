package com.unisal.selectaapi.candidatura.dto;

import java.time.Instant;
import java.time.LocalDateTime;

import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAgendamentoDto(

        @NotNull(message = "etapa é obrigatória") EtapaProcesso etapa,

        @NotBlank(message = "titulo é obrigatório")
        @Size(max = 150, message = "o título deve ter no máximo 150 caracteres") String titulo,

        @Size(max = 5000, message = "a mensagem deve ter no máximo 5000 caracteres") String mensagem,

        @NotNull(message = "inicio é obrigatório") @Future(message = "o agendamento deve ser futuro") Instant inicio,

        @NotNull(message = "duracaoMinutos é obrigatória")
        @Min(value = 15, message = "a duração mínima é de 15 minutos")
        @Max(value = 480, message = "a duração máxima é de 480 minutos") Integer duracaoMinutos,

        @NotBlank(message = "link é obrigatório")
        @Size(max = 2048, message = "o link deve ter no máximo 2048 caracteres") String link
) {
}