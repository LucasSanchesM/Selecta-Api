package com.unisal.selectaapi.candidate;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;

import com.unisal.selectaapi.company.service.CompanyMemberService;
import com.unisal.selectaapi.candidatura.repository.CandidaturaRepository;
import com.unisal.selectaapi.exception.BusinessException;

import java.util.UUID;

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

import com.unisal.selectaapi.candidate.dto.CandidateProfileDto;
import com.unisal.selectaapi.candidate.dto.CreateExperienceDto;
import com.unisal.selectaapi.candidate.dto.CreateProfileDto;
import com.unisal.selectaapi.candidate.dto.CreateProjectDto;
import com.unisal.selectaapi.candidate.dto.UpdateCandidateProfileDto;
import com.unisal.selectaapi.candidate.service.CandidateProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/candidate-profile")
public class CandidateProfileController {

    private final CandidateProfileService candidateProfileService;

    private final CompanyMemberService companyMembers;
    private final CandidaturaRepository candidaturas;

    public CandidateProfileController(CandidateProfileService candidateProfileService,
            CompanyMemberService companyMembers, CandidaturaRepository candidaturas) {
        this.companyMembers = companyMembers;
        this.candidaturas = candidaturas;
        this.candidateProfileService = candidateProfileService;
    }

    @PostMapping("/me")
    public ResponseEntity<CandidateProfileDto> createMyProfile( @AuthenticationPrincipal Jwt jwt,
                                                                @Valid @RequestBody CreateProfileDto dto) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var response = candidateProfileService.createProfile(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CandidateProfileDto> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var response = candidateProfileService.findProfileByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me")
    public ResponseEntity<CandidateProfileDto> updateMyProfile( @AuthenticationPrincipal Jwt jwt,
                                                                @Valid @RequestBody UpdateCandidateProfileDto dto) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var response = candidateProfileService.updateProfile(userId, dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidateProfileDto> uploadMyAvatar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(candidateProfileService.uploadAvatar(userId, file));
    }

    @PutMapping(value = "/me/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CandidateProfileDto> uploadMyResume(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(candidateProfileService.uploadResume(userId, file));
    }

    @PostMapping("/me/experiences")
    public ResponseEntity<CandidateProfileDto> addExperience(@AuthenticationPrincipal Jwt jwt,
                                                            @Valid @RequestBody CreateExperienceDto dto) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var response = candidateProfileService.addExperience(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/me/experiences/{id}")
    public ResponseEntity<CandidateProfileDto> updateExperience(@AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable("id") UUID experienceId,
                                                                @Valid @RequestBody CreateExperienceDto dto) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var response = candidateProfileService.updateExperience(userId, experienceId, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/experiences/{id}")
    public ResponseEntity<CandidateProfileDto> deleteExperience(@AuthenticationPrincipal Jwt jwt,
                                                                @PathVariable("id") UUID experienceId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var response = candidateProfileService.deleteExperience(userId, experienceId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/projects")
    public ResponseEntity<CandidateProfileDto> addProject( @AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody CreateProjectDto dto) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var response = candidateProfileService.addProject(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/me/projects/{id}")
    public ResponseEntity<CandidateProfileDto> updateProject(@AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable("id") UUID projectId,
                                                            @Valid @RequestBody CreateProjectDto dto) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var response = candidateProfileService.updateProject(userId, projectId, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me/projects/{id}")
    public ResponseEntity<CandidateProfileDto> deleteProject(@AuthenticationPrincipal Jwt jwt,
                                                            @PathVariable("id") UUID projectId) {
        UUID userId = UUID.fromString(jwt.getSubject());
        var response = candidateProfileService.deleteProject(userId, projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CandidateProfileDto> getProfileByUserId(@AuthenticationPrincipal Jwt jwt, @PathVariable("userId") UUID userId) {
        UUID viewerId = UUID.fromString(jwt.getSubject());
        if (viewerId.equals(userId)) {
            return ResponseEntity.ok(candidateProfileService.findProfileByUserId(userId));
        }
        requireCompanyApplication(viewerId, userId);
        return ResponseEntity.ok(candidateProfileService.findProfileForCompany(userId));
    }

    @GetMapping(value = "/{userId}/resume/{fileId}.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadResume(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID userId, @PathVariable UUID fileId) {
        UUID viewerId = UUID.fromString(jwt.getSubject());
        boolean companyView = !viewerId.equals(userId);
        if (companyView) {
            requireCompanyApplication(viewerId, userId);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=curriculo.pdf")
                .body(candidateProfileService.downloadResume(userId, fileId, companyView));
    }

    private void requireCompanyApplication(UUID viewerId, UUID userId) {
        UUID companyId = companyMembers.pegarCompanyPeloUserId(viewerId).getId();
        if (!candidaturas.existsByCandidatoUserIdAndVagaEmpresaId(userId, companyId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "Perfil não encontrado para esta empresa");
        }
    }
}