package com.unisal.selectaapi.auth.dtos;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(@NotBlank(message = "O refresh token é obrigatório") String refreshToken) {
}
