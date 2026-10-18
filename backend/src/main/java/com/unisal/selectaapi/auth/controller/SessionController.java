package com.unisal.selectaapi.auth.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unisal.selectaapi.auth.dtos.SessionResponseDto;
import com.unisal.selectaapi.auth.service.JwtPrincipal;
import com.unisal.selectaapi.auth.service.SessionService;

@RestController
@RequestMapping("/auth/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public ResponseEntity<List<SessionResponseDto>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(sessionService.listActive(JwtPrincipal.userId(jwt), JwtPrincipal.sessionId(jwt)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        sessionService.revoke(JwtPrincipal.userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/revoke-others")
    public ResponseEntity<Map<String, Integer>> revokeOthers(@AuthenticationPrincipal Jwt jwt) {
        int revoked = sessionService.revokeOthers(JwtPrincipal.userId(jwt), JwtPrincipal.sessionId(jwt));
        return ResponseEntity.ok(Map.of("revoked", revoked));
    }
}
