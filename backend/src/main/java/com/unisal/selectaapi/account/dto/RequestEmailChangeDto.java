package com.unisal.selectaapi.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestEmailChangeDto(
        @NotBlank(message = "Informe o novo e-mail")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 254)
        String newEmail) {
}
