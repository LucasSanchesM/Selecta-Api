package com.unisal.selectaapi.candidate.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CPF;

import java.util.List;

public record CreateProfileDto(
    String headline,
    String summary,
    @NotBlank(message = "Cpf eh obrigatorio")
    @CPF(message = "Cpf invalido")
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
    List<String> skills,
    List<CreateExperienceDto> experiences,
    List<CreateProjectDto> projects
) {}