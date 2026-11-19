package com.unisal.selectaapi.candidate;

import java.util.List;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.unisal.selectaapi.candidate.dto.CandidateCertificateDto;
import com.unisal.selectaapi.candidate.dto.CandidateLanguageDto;
import com.unisal.selectaapi.candidate.dto.CreateCertificateDto;
import com.unisal.selectaapi.candidate.dto.CreateLanguageDto;
import com.unisal.selectaapi.candidate.service.CandidateCredentialService;
import com.unisal.selectaapi.candidate.service.CandidateCredentialService.DownloadedFile;
import com.unisal.selectaapi.candidatura.repository.CandidaturaRepository;
import com.unisal.selectaapi.company.service.CompanyMemberService;
import com.unisal.selectaapi.exception.BusinessException;

import jakarta.validation.Valid;

/**
 * Certificados e idiomas do perfil. O dono gerencia em {@code /me/...}; o download dos anexos
 * também é liberado para empresas que receberam uma candidatura desse candidato, ja ta integrado tbm
 */

@RestController
@RequestMapping("/candidate-profile")
public class CandidateCredentialController {

    private final CandidateCredentialService service;
    private final CompanyMemberService companyMembers;
    private final CandidaturaRepository candidaturas;

    public CandidateCredentialController(CandidateCredentialService service,
            CompanyMemberService companyMembers, CandidaturaRepository candidaturas) {
        this.service = service;
        this.companyMembers = companyMembers;
        this.candidaturas = candidaturas;
    }

    // certificados

    @GetMapping("/me/certificates")
    public List<CandidateCertificateDto> listCertificates(@AuthenticationPrincipal Jwt jwt) {
        return service.listCertificates(userId(jwt));
    }

    @PostMapping("/me/certificates")
    public ResponseEntity<CandidateCertificateDto> addCertificate(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCertificateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addCertificate(userId(jwt), dto));
    }

    @PutMapping("/me/certificates/{id}")
    public CandidateCertificateDto updateCertificate(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @Valid @RequestBody CreateCertificateDto dto) {
        return service.updateCertificate(userId(jwt), id, dto);
    }

    @DeleteMapping("/me/certificates/{id}")
    public ResponseEntity<Void> deleteCertificate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.deleteCertificate(userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/me/certificates/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CandidateCertificateDto attachCertificateFile(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return service.attachCertificateFile(userId(jwt), id, file);
    }

    @DeleteMapping("/me/certificates/{id}/file")
    public CandidateCertificateDto removeCertificateFile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.removeCertificateFile(userId(jwt), id);
    }

    @GetMapping("/{userId}/certificates/{id}/file")
    public ResponseEntity<byte[]> downloadCertificateFile(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId, @PathVariable UUID id) {
        requireAccess(jwt, userId);
        return file(service.downloadCertificateFile(userId, id));
    }

    //idiomas

    @GetMapping("/me/languages")
    public List<CandidateLanguageDto> listLanguages(@AuthenticationPrincipal Jwt jwt) {
        return service.listLanguages(userId(jwt));
    }

    @PostMapping("/me/languages")
    public ResponseEntity<CandidateLanguageDto> addLanguage(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateLanguageDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addLanguage(userId(jwt), dto));
    }

    @PutMapping("/me/languages/{id}")
    public CandidateLanguageDto updateLanguage(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @Valid @RequestBody CreateLanguageDto dto) {
        return service.updateLanguage(userId(jwt), id, dto);
    }

    @DeleteMapping("/me/languages/{id}")
    public ResponseEntity<Void> deleteLanguage(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.deleteLanguage(userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/me/languages/{id}/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CandidateLanguageDto attachLanguageCertificate(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return service.attachLanguageCertificate(userId(jwt), id, file);
    }

    @DeleteMapping("/me/languages/{id}/certificate")
    public CandidateLanguageDto removeLanguageCertificate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.removeLanguageCertificate(userId(jwt), id);
    }

    @GetMapping("/{userId}/languages/{id}/certificate")
    public ResponseEntity<byte[]> downloadLanguageCertificate(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId, @PathVariable UUID id) {
        requireAccess(jwt, userId);
        return file(service.downloadLanguageCertificate(userId, id));
    }

    // internos

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private void requireAccess(Jwt jwt, UUID ownerId) {
        UUID viewerId = userId(jwt);
        if (viewerId.equals(ownerId)) {
            return;
        }
        UUID companyId = companyMembers.pegarCompanyPeloUserId(viewerId).getId();
        if (!candidaturas.existsByCandidatoUserIdAndVagaEmpresaId(ownerId, companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Arquivo não encontrado para esta empresa");
        }
    }

    private static ResponseEntity<byte[]> file(DownloadedFile file) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(file.fileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(file.content());
    }
}
