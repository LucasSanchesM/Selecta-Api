package com.unisal.selectaapi.company.dto;

import java.time.Instant;
import java.util.UUID;

public record CompanyMemberDto(
        UUID memberId,
        UUID userId,
        String name,
        String email,
        String role,
        Boolean active,
        boolean voce,
        Instant createdAt) {}
