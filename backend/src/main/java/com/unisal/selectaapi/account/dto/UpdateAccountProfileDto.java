package com.unisal.selectaapi.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAccountProfileDto(
        @NotBlank(message = "Informe seu nome completo")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String name,

        @Pattern(regexp = "^$|^[0-9()+\\-\\s]{8,20}$", message = "Informe um telefone válido")
        String phone,

        @NotBlank(message = "Escolha um idioma")
        @Pattern(regexp = "pt-BR|en|es", message = "Idioma não suportado")
        String language) {
}
