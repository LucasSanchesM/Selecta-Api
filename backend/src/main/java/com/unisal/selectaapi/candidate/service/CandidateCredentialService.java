package com.unisal.selectaapi.candidate.service;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.unisal.selectaapi.candidate.dto.CandidateCertificateDto;
import com.unisal.selectaapi.candidate.dto.CandidateLanguageDto;
import com.unisal.selectaapi.candidate.dto.CreateCertificateDto;
import com.unisal.selectaapi.candidate.dto.CreateLanguageDto;
import com.unisal.selectaapi.candidate.entity.AttachedFile;
import com.unisal.selectaapi.candidate.entity.CandidateCertificate;
import com.unisal.selectaapi.candidate.entity.CandidateLanguage;
import com.unisal.selectaapi.candidate.entity.CandidateProfile;
import com.unisal.selectaapi.candidate.repository.CandidateCertificateRepository;
import com.unisal.selectaapi.candidate.repository.CandidateLanguageRepository;
import com.unisal.selectaapi.candidate.repository.CandidateProfileRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.recomendacao.EmbeddingRequestedEvent;
import com.unisal.selectaapi.storage.DocumentUploadValidator;
import com.unisal.selectaapi.storage.FileStorage;
import com.unisal.selectaapi.storage.StoredFile;

@Service
public class CandidateCredentialService {

    public record DownloadedFile(byte[] content, String contentType, String fileName) {}

    private final CandidateProfileRepository profiles;
    private final CandidateCertificateRepository certificates;
    private final CandidateLanguageRepository languages;
    private final FileStorage fileStorage;
    private final DocumentUploadValidator validator;
    private final ApplicationEventPublisher eventPublisher;

    public CandidateCredentialService(CandidateProfileRepository profiles,
            CandidateCertificateRepository certificates,
            CandidateLanguageRepository languages,
            FileStorage fileStorage,
            DocumentUploadValidator validator,
            ApplicationEventPublisher eventPublisher) {
        this.profiles = profiles;
        this.certificates = certificates;
        this.languages = languages;
        this.fileStorage = fileStorage;
        this.validator = validator;
        this.eventPublisher = eventPublisher;
    }

    // certificados

    @Transactional(readOnly = true)
    public List<CandidateCertificateDto> listCertificates(UUID userId) {
        return certificates.findByCandidateProfileUserIdOrderByIssueDateDesc(userId).stream()
                .map(c -> CredentialMapper.toDto(userId, c)).toList();
    }

    @Transactional
    public CandidateCertificateDto addCertificate(UUID userId, CreateCertificateDto dto) {
        var profile = getProfile(userId);
        var certificate = new CandidateCertificate(profile, dto.name().trim(), dto.issuer().trim(),
                dto.issueDate(), blankToNull(dto.credentialUrl()));
        var saved = certificates.save(certificate);
        requestEmbedding(profile);
        return CredentialMapper.toDto(userId, saved);
    }

    @Transactional
    public CandidateCertificateDto updateCertificate(UUID userId, UUID id, CreateCertificateDto dto) {
        var certificate = getCertificate(userId, id);
        certificate.setName(dto.name().trim());
        certificate.setIssuer(dto.issuer().trim());
        certificate.setIssueDate(dto.issueDate());
        certificate.setCredentialUrl(blankToNull(dto.credentialUrl()));
        var saved = certificates.save(certificate);
        requestEmbedding(certificate.getCandidateProfile());
        return CredentialMapper.toDto(userId, saved);
    }

    @Transactional
    public void deleteCertificate(UUID userId, UUID id) {
        var certificate = getCertificate(userId, id);
        String key = keyOf(certificate.getAttachment());
        var profile = certificate.getCandidateProfile();
        profile.getCertificates().remove(certificate);
        certificates.delete(certificate);
        certificates.flush();
        fileStorage.deleteQuietly(key);
        requestEmbedding(profile);
    }

    @Transactional
    public CandidateCertificateDto attachCertificateFile(UUID userId, UUID id, MultipartFile file) {
        var certificate = getCertificate(userId, id);
        replaceFile(userId, "certificates", file, certificate::getAttachment, certificate::setAttachment,
                () -> certificates.saveAndFlush(certificate));
        return CredentialMapper.toDto(userId, certificate);
    }

    @Transactional
    public CandidateCertificateDto removeCertificateFile(UUID userId, UUID id) {
        var certificate = getCertificate(userId, id);
        removeFile(certificate::getAttachment, certificate::setAttachment,
                () -> certificates.saveAndFlush(certificate));
        return CredentialMapper.toDto(userId, certificate);
    }

    @Transactional(readOnly = true)
    public DownloadedFile downloadCertificateFile(UUID userId, UUID id) {
        return download(getCertificate(userId, id).getAttachment());
    }

    //  idiomas

    @Transactional(readOnly = true)
    public List<CandidateLanguageDto> listLanguages(UUID userId) {
        return languages.findByCandidateProfileUserIdOrderByLanguageAsc(userId).stream()
                .map(l -> CredentialMapper.toDto(userId, l)).toList();
    }

    @Transactional
    public CandidateLanguageDto addLanguage(UUID userId, CreateLanguageDto dto) {
        var profile = getProfile(userId);
        String name = dto.language().trim();
        if (languages.existsByCandidateProfileUserIdAndLanguageIgnoreCase(userId, name)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Este idioma já foi adicionado ao seu perfil");
        }
        var saved = languages.save(new CandidateLanguage(profile, name, dto.proficiency()));
        requestEmbedding(profile);
        return CredentialMapper.toDto(userId, saved);
    }

    @Transactional
    public CandidateLanguageDto updateLanguage(UUID userId, UUID id, CreateLanguageDto dto) {
        var language = getLanguage(userId, id);
        String name = dto.language().trim();
        if (languages.existsByCandidateProfileUserIdAndLanguageIgnoreCaseAndIdNot(userId, name, id)) {
            throw new BusinessException(HttpStatus.CONFLICT, "Este idioma já foi adicionado ao seu perfil");
        }
        language.setLanguage(name);
        language.setProficiency(dto.proficiency());
        var saved = languages.save(language);
        requestEmbedding(language.getCandidateProfile());
        return CredentialMapper.toDto(userId, saved);
    }

    @Transactional
    public void deleteLanguage(UUID userId, UUID id) {
        var language = getLanguage(userId, id);
        String key = keyOf(language.getCertificate());
        var profile = language.getCandidateProfile();
        profile.getLanguages().remove(language);
        languages.delete(language);
        languages.flush();
        fileStorage.deleteQuietly(key);
        requestEmbedding(profile);
    }

    @Transactional
    public CandidateLanguageDto attachLanguageCertificate(UUID userId, UUID id, MultipartFile file) {
        var language = getLanguage(userId, id);
        replaceFile(userId, "languages", file, language::getCertificate, language::setCertificate,
                () -> languages.saveAndFlush(language));
        return CredentialMapper.toDto(userId, language);
    }

    @Transactional
    public CandidateLanguageDto removeLanguageCertificate(UUID userId, UUID id) {
        var language = getLanguage(userId, id);
        removeFile(language::getCertificate, language::setCertificate, () -> languages.saveAndFlush(language));
        return CredentialMapper.toDto(userId, language);
    }

    @Transactional(readOnly = true)
    public DownloadedFile downloadLanguageCertificate(UUID userId, UUID id) {
        return download(getLanguage(userId, id).getCertificate());
    }

    // internos

    /** Envia o novo arquivo, grava a referência e só então apaga o antigo; desfaz o envio se o banco falhar. */
    private void replaceFile(UUID userId, String folder, MultipartFile file,
            Supplier<AttachedFile> current, Consumer<AttachedFile> setter, Runnable persist) {
        var document = validator.validate(file);
        StoredFile uploaded = fileStorage.storePrivate(userId + "/" + folder,
                UUID.randomUUID() + "." + document.extension(), document.content(), document.contentType());

        String previousKey = keyOf(current.get());
        setter.accept(new AttachedFile(uploaded.key(), document.originalName(), document.contentType()));
        try {
            persist.run();
        } catch (RuntimeException exception) {
            fileStorage.deleteQuietly(uploaded.key());
            throw exception;
        }
        fileStorage.deleteQuietly(previousKey);
    }

    private void removeFile(Supplier<AttachedFile> current, Consumer<AttachedFile> setter, Runnable persist) {
        String previousKey = keyOf(current.get());
        if (previousKey == null) {
            return;
        }
        setter.accept(null);
        persist.run();
        fileStorage.deleteQuietly(previousKey);
    }

    private DownloadedFile download(AttachedFile file) {
        if (file == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Nenhum arquivo anexado");
        }
        return new DownloadedFile(fileStorage.readPrivate(file.getKey()), file.getContentType(), file.getName());
    }

    private CandidateProfile getProfile(UUID userId) {
        return profiles.findByUserId(userId).orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                "Não foi encontrado um perfil associado a este usuario"));
    }

    private CandidateCertificate getCertificate(UUID userId, UUID id) {
        return certificates.findByIdAndCandidateProfileUserId(id, userId).orElseThrow(
                () -> new BusinessException(HttpStatus.NOT_FOUND, "Certificado não encontrado para este usuário"));
    }

    private CandidateLanguage getLanguage(UUID userId, UUID id) {
        return languages.findByIdAndCandidateProfileUserId(id, userId).orElseThrow(
                () -> new BusinessException(HttpStatus.NOT_FOUND, "Idioma não encontrado para este usuário"));
    }

    private static String keyOf(AttachedFile file) {
        return file == null ? null : file.getKey();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void requestEmbedding(CandidateProfile profile) {
        if (eventPublisher != null && profile != null && profile.getId() != null
                && profile.getUser().getPreferences().personalizedJobs()) {
            eventPublisher.publishEvent(EmbeddingRequestedEvent.candidato(profile.getId()));
        }
    }
}
