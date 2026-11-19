package com.unisal.selectaapi.candidate.service;

import java.util.UUID;

import com.unisal.selectaapi.candidate.dto.CandidateCertificateDto;
import com.unisal.selectaapi.candidate.dto.CandidateLanguageDto;
import com.unisal.selectaapi.candidate.entity.AttachedFile;
import com.unisal.selectaapi.candidate.entity.CandidateCertificate;
import com.unisal.selectaapi.candidate.entity.CandidateLanguage;

public final class CredentialMapper {

    private CredentialMapper() {
    }

    public static CandidateCertificateDto toDto(UUID userId, CandidateCertificate certificate) {
        AttachedFile file = certificate.getAttachment();
        return new CandidateCertificateDto(
                certificate.getId(),
                certificate.getName(),
                certificate.getIssuer(),
                certificate.getIssueDate(),
                certificate.getCredentialUrl(),
                file == null ? null : "/candidate-profile/" + userId + "/certificates/" + certificate.getId() + "/file",
                file == null ? null : file.getName(),
                file == null ? null : file.getContentType(),
                certificate.getCreatedAt());
    }

    public static CandidateLanguageDto toDto(UUID userId, CandidateLanguage language) {
        AttachedFile file = language.getCertificate();
        return new CandidateLanguageDto(
                language.getId(),
                language.getLanguage(),
                language.getProficiency(),
                file == null ? null : "/candidate-profile/" + userId + "/languages/" + language.getId() + "/certificate",
                file == null ? null : file.getName(),
                file == null ? null : file.getContentType(),
                language.getCreatedAt());
    }
}
