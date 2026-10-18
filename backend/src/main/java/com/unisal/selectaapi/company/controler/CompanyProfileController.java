package com.unisal.selectaapi.company.controler;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.unisal.selectaapi.company.dto.CompanyProfileDto;
import com.unisal.selectaapi.company.dto.UpdateCompanyProfileDto;
import com.unisal.selectaapi.company.service.CompanyProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/company-profile")
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;

    public CompanyProfileController(CompanyProfileService companyProfileService) {
        this.companyProfileService = companyProfileService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('COMPANY')")
    public ResponseEntity<CompanyProfileDto> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
            companyProfileService.findCompanyOfUser(UUID.fromString(jwt.getSubject()))
        );
        }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('COMPANY')")
    public ResponseEntity<CompanyProfileDto> updateMyProfile(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCompanyProfileDto dto) {
        return ResponseEntity.ok(companyProfileService.updateProfile(UUID.fromString(jwt.getSubject()), dto));
    }

    @PostMapping(value = "/me/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('COMPANY')")
    public ResponseEntity<CompanyProfileDto> uploadMyLogo(@AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(companyProfileService.uploadLogo(UUID.fromString(jwt.getSubject()), file));
    }

    @DeleteMapping("/me/logo")
    @PreAuthorize("hasAuthority('COMPANY')")
    public ResponseEntity<CompanyProfileDto> deleteMyLogo(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(companyProfileService.deleteLogo(UUID.fromString(jwt.getSubject())));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('COMPANY')")
    public ResponseEntity<CompanyProfileDto> getCompanyProfileByUserId(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(companyProfileService.findProfileByUserId(userId));
    }
}