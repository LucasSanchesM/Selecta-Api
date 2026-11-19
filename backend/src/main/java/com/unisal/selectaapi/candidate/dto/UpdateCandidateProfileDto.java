package com.unisal.selectaapi.candidate.dto;

import org.hibernate.validator.constraints.br.CPF;

import java.util.List;

public record UpdateCandidateProfileDto(
    String headline,
    String summary,
    @CPF
    String cpf,
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
    List<String> skills
) {}
