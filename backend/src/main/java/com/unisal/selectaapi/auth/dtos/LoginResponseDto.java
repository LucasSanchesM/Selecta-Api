package com.unisal.selectaapi.auth.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Resposta do login. Quando a conta tem verificação em duas etapas e o dispositivo
 * é novo, os tokens vêm nulos e o cliente deve concluir o desafio `challengeId`.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponseDto(
        String tokenJwt,
        Long expiresInJwt,
        String role,
        String refreshToken,
        Boolean twoFactorRequired,
        String challengeId,
        String maskedEmail) {

    public static LoginResponseDto tokens(String tokenJwt, long expiresInJwt, String role, String refreshToken) {
        return new LoginResponseDto(tokenJwt, expiresInJwt, role, refreshToken, false, null, null);
    }

    public static LoginResponseDto challenge(String challengeId, String role, String maskedEmail) {
        return new LoginResponseDto(null, null, role, null, true, challengeId, maskedEmail);
    }
}
