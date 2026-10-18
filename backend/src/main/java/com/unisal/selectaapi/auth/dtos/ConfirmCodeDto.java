package com.unisal.selectaapi.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfirmCodeDto(
        @NotBlank(message = "Informe o código")
        @Pattern(regexp = "\\d{6}", message = "O código tem 6 dígitos")
        String code) {
}
