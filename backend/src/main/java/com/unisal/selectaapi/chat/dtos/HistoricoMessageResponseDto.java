package com.unisal.selectaapi.chat.dtos;

import java.time.Instant;
import java.time.LocalDateTime;

public record HistoricoMessageResponseDto(String remetente, String mensagem, boolean enviadoPorSiMesmo, Instant enviadoEm, boolean lida) {
}
