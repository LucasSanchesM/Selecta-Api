package com.unisal.selectaapi.auth.dtos;

/** Resposta de toda rota que dispara um código por e-mail. */
public record CodeSentDto(String maskedEmail, int expiresInMinutes, int resendAvailableInSeconds) {
}
