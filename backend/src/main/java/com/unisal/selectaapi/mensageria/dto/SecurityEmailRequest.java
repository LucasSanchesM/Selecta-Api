package com.unisal.selectaapi.mensageria.dto;

/**
 * E-mail transacional de segurança publicado na fila "email.seguranca".
 * Com `code` preenchido vira um e-mail de código; sem ele, um aviso.
 */
public record SecurityEmailRequest(
        String email,
        String subject,
        String title,
        String description,
        String code,
        Integer expiresInMinutes,
        String details) {
}
