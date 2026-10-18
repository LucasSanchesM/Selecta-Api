package com.unisal.selectaapi.company.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyProfileDto(
        UUID id,
        UUID userId,
        String companyName,
        String cnpj,
        String description,
        String industry,
        String website,
        String city,
        String state,
        String logoUrl,
        Instant createdAt,
        Instant updatedAt) {
}
