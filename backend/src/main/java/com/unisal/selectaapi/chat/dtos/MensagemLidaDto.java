package com.unisal.selectaapi.chat.dtos;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record MensagemLidaDto(UUID candidaturaId, Instant lidaEm) {
}
