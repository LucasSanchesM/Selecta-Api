package com.unisal.selectaapi.notificacoes.listener;

import com.unisal.selectaapi.notificacoes.dtos.NotificacaoResponseDto;

import java.util.UUID;

public record CriarNotificacaoEvent(NotificacaoResponseDto response, UUID userId) {
}
