package com.unisal.selectaapi.auth.dtos;

import java.time.LocalDateTime;

public record DeviceCodeDto(String code, LocalDateTime expiresAt, int expiresInSeconds) {
}
