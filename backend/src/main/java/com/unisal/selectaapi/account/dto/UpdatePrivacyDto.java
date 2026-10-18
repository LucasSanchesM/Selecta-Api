package com.unisal.selectaapi.account.dto;

import com.unisal.selectaapi.account.entity.ProfileVisibility;

import jakarta.validation.constraints.NotNull;

public record UpdatePrivacyDto(
        @NotNull(message = "Escolha a visibilidade do perfil") ProfileVisibility profileVisibility,
        @NotNull Boolean showSalary,
        @NotNull Boolean openToWork,
        @NotNull Boolean allowDirectContact) {
}
