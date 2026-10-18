package com.unisal.selectaapi.auth.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record TwoFactorVerifyDto(
        @NotNull(message = "Desafio de login não informado") UUID challengeId,
        @NotNull(message = "Escolha como confirmar o acesso") TwoFactorMethod method,
        @NotBlank(message = "Informe o código")
        @Pattern(regexp = "\\d{6}", message = "O código tem 6 dígitos")
        String code,
        Boolean trustDevice) {
}
