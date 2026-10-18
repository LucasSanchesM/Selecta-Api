package com.unisal.selectaapi.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordDto(
        @NotBlank(message = "Informe o código enviado ao seu e-mail")
        @Pattern(regexp = "\\d{6}", message = "O código tem 6 dígitos")
        String code,

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres")
        String newPassword,

        Boolean signOutOtherSessions) {
}
