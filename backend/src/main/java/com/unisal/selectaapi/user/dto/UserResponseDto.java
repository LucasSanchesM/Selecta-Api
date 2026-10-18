package com.unisal.selectaapi.user.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponseDto(
    UUID id,
    String name,
    String email,
    String role,
    Boolean active,
    String avatarUrl,
    Instant createdAt,
    Instant updatedAt
) {}
