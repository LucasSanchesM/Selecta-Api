package com.unisal.selectaapi.company.controler;

import java.util.UUID;

import com.unisal.selectaapi.company.dto.*;
import com.unisal.selectaapi.company.service.CompanyMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/empresa/equipe")
public class CompanyMemberControler {

    private final CompanyMemberService companyMemberService;

    public CompanyMemberControler(CompanyMemberService companyMemberService) {
        this.companyMemberService = companyMemberService;
    }


    @GetMapping
    public ResponseEntity<CompanyMembersDto> listar(@AuthenticationPrincipal Jwt jwt) {
        var lista = companyMemberService.listarMembros(userId(jwt));
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    public ResponseEntity<CompanyMemberDto> criar(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody CreateCompanyMemberDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyMemberService.criarMembro(userId(jwt), dto));
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<CompanyMemberDto> atualizar(@AuthenticationPrincipal Jwt jwt,
                                                        @PathVariable("memberId") UUID alvoUserId,
                                                        @Valid @RequestBody UpdateCompanyMemberDto dto) {
        return ResponseEntity.ok(companyMemberService.updateInfos(alvoUserId, dto, userId(jwt)));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable("memberId") UUID alvoUserId) {
        companyMemberService.deleteMemberCompany(alvoUserId, userId(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}