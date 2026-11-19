package com.unisal.selectaapi.candidate.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCertificateDto(
    @NotBlank(message = "Nome do certificado é obrigatório")
    @Size(max = 255, message = "Nome do certificado deve ter no máximo 255 caracteres")
    String name,

    @NotBlank(message = "Instituição emissora é obrigatória")
    @Size(max = 255, message = "Instituição deve ter no máximo 255 caracteres")
    String issuer,

    @NotNull(message = "Data de emissão é obrigatória")
    @PastOrPresent(message = "Data de emissão não pode estar no futuro")
    LocalDate issueDate,

    @Size(max = 2048, message = "Link deve ter no máximo 2048 caracteres")
    @Pattern(regexp = "^$|^https?://\\S+$", message = "Link inválido. Use um endereço http:// ou https://")
    String credentialUrl
) {}
