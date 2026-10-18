package com.unisal.selectaapi.company;

import java.util.UUID;

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

import com.unisal.selectaapi.company.dto.CreateTeamMemberDto;
import com.unisal.selectaapi.company.dto.TeamMemberResponse;
import com.unisal.selectaapi.company.dto.TeamResponse;
import com.unisal.selectaapi.company.dto.UpdateTeamMemberDto;
import com.unisal.selectaapi.company.service.CompanyTeamService;

import jakarta.validation.Valid;

/**
 * Permissões são checadas no service:
 * proprietário, ADMIN ou RECRUTADOR.
 *
 * Caminho separado para não conflitar com CompanyMemberControler.
 */
@RestController
@RequestMapping("/empresa/equipe-settings")
public class CompanyTeamController {

    private final CompanyTeamService teamService;

    public CompanyTeamController(CompanyTeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public ResponseEntity<TeamResponse> listar(
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(
                teamService.listar(userId(jwt)));
    }

    @PostMapping
    public ResponseEntity<TeamMemberResponse> criar(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateTeamMemberDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.criar(userId(jwt), dto));
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<TeamMemberResponse> atualizar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("memberId") UUID memberId,
            @Valid @RequestBody UpdateTeamMemberDto dto) {

        return ResponseEntity.ok(
                teamService.atualizar(userId(jwt), memberId, dto));
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> remover(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("memberId") UUID memberId) {

        teamService.remover(userId(jwt), memberId);

        return ResponseEntity.noContent().build();
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}