package com.unisal.selectaapi.auth.dtos;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ChallengeIdDto(@NotNull(message = "Desafio de login não informado") UUID challengeId) {
}
