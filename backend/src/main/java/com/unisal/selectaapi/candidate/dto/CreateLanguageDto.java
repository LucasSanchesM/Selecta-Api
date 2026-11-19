package com.unisal.selectaapi.candidate.dto;

import com.unisal.selectaapi.candidate.entity.LanguageProficiency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLanguageDto(
    @NotBlank(message = "Idioma é obrigatório")
    @Size(max = 100, message = "Idioma deve ter no máximo 100 caracteres")
    String language,

    @NotNull(message = "Nível de proficiência é obrigatório")
    LanguageProficiency proficiency
) {}
