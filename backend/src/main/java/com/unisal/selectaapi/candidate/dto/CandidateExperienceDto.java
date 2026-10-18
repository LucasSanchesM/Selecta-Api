package com.unisal.selectaapi.candidate.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CandidateExperienceDto(
    UUID id,
    String companyName,
    String role,
    String description,
    Instant startDate,
    Instant endDate,
    Boolean isCurrent,
    Instant createdAt
) {}
