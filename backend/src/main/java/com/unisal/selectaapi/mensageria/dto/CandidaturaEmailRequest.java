package com.unisal.selectaapi.mensageria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Payload publicado na fila "candidatura.atualizacao" sempre que a etapa ou o
 * status de uma candidatura muda.
 */
public record CandidaturaEmailRequest(

        @Email(message = "email invalido") @NotBlank(message = "campo email precisa estar preenchido") String email,

        @NotBlank(message = "campo candidatoNome precisa estar preenchido") String candidatoNome,

        @NotBlank(message = "campo vagaTitulo precisa estar preenchido") String vagaTitulo,

        String empresaNome,

        String etapaDescricao,

        String statusDescricao,

        String mensagem,

        String agendamentoTitulo,

        Instant agendamentoInicio,

        Integer agendamentoDuracaoMinutos,

        String agendamentoLink) {
}
