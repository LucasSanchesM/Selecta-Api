package com.unisal.selectaapi.candidate.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CandidateProfileDto(
    UUID id,
    UUID userId,
    String userName,
    String userEmail,
    String profileImageUrl,
    String resumeUrl,
    String headline,
    String summary,
    String phone,
    String city,
    String state,
    String institution,
    String course,
    Integer currentSemester,
    Integer expectedGraduationYear,
    String linkedinUrl,
    String githubUrl,
    String portfolioUrl,
    List<String> skills,
    List<CandidateExperienceDto> experiences,
    List<CandidateProjectDto> projects,
    Instant createdAt,
    Instant updatedAt
) {}