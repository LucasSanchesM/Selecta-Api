package com.unisal.selectaapi.notificacoes.dtos;

import com.unisal.selectaapi.notificacoes.NotificacoesRole;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record PublishNotificacaoDto(String titulo, String descricao, NotificacoesRole type, String recursoId) {
}
