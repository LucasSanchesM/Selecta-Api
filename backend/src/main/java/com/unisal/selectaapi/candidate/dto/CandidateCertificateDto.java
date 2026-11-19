package com.unisal.selectaapi.candidate.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** {@code fileUrl} é o caminho autenticado para baixar o anexo; {@code null} quando não há anexo. */
public record CandidateCertificateDto(
    UUID id,
    String name,
    String issuer,
    LocalDate issueDate,
    String credentialUrl,
    String fileUrl,
    String fileName,
    String fileContentType,
    Instant createdAt
) {}
