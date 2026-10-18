package com.unisal.selectaapi.candidate.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CandidateProjectDto(
    UUID id,
    String title,
    String description,
    String projectUrl,
    List<String> toolsAndSkills,
    Instant createdAt
) {}
