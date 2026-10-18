package com.unisal.selectaapi.auth.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record PendingLoginDto(
        UUID id,
        String deviceName,
        boolean mobile,
        String location,
        String ipAddress,
        LocalDateTime createdAt,
        LocalDateTime expiresAt) {
}
