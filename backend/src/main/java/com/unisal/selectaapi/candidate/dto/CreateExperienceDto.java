package com.unisal.selectaapi.candidate.dto;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;

public record CreateExperienceDto(
    @NotBlank(message = "Nome da organização ou empresa é obrigatório")
    String companyName,

    @NotBlank(message = "Cargo ou função é obrigatório")
    String role,

    String description,
    Instant startDate,
    Instant endDate,
    Boolean isCurrent
) {}
