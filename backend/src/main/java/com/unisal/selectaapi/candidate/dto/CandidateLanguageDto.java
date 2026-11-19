package com.unisal.selectaapi.candidate.dto;

import java.time.Instant;
import java.util.UUID;

import com.unisal.selectaapi.candidate.entity.LanguageProficiency;

/** {@code certificateUrl} é o caminho autenticado para baixar o certificado; {@code null} quando não há. */
public record CandidateLanguageDto(
    UUID id,
    String language,
    LanguageProficiency proficiency,
    String certificateUrl,
    String certificateFileName,
    String certificateContentType,
    Instant createdAt
) {}
