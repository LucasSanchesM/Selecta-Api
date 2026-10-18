package com.unisal.selectaapi.auth.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unisal.selectaapi.auth.dtos.ChallengeIdDto;
import com.unisal.selectaapi.auth.dtos.CodeSentDto;
import com.unisal.selectaapi.auth.dtos.ConfirmCodeDto;
import com.unisal.selectaapi.auth.dtos.DeviceCodeDto;
import com.unisal.selectaapi.auth.dtos.LoginResponseDto;
import com.unisal.selectaapi.auth.dtos.PendingLoginDto;
import com.unisal.selectaapi.auth.dtos.TwoFactorVerifyDto;
import com.unisal.selectaapi.auth.service.ClientInfo;
import com.unisal.selectaapi.auth.service.JwtPrincipal;
import com.unisal.selectaapi.auth.service.TwoFactorService;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.entity.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth/two-factor")
public class TwoFactorController {

    private final TwoFactorService twoFactorService;
    private final UserRepository userRepository;

    public TwoFactorController(TwoFactorService twoFactorService, UserRepository userRepository) {
        this.twoFactorService = twoFactorService;
        this.userRepository = userRepository;
    }

    // ---- Etapas públicas: o dispositivo novo ainda não tem sessão ----

    @PostMapping("/email")
    public ResponseEntity<CodeSentDto> sendEmailCode(@Valid @RequestBody ChallengeIdDto dto, HttpServletRequest request) {
        return ResponseEntity.ok(twoFactorService.sendEmailCode(dto.challengeId(), ClientInfo.from(request)));
    }

    @PostMapping("/verify")
    public ResponseEntity<LoginResponseDto> verify(@Valid @RequestBody TwoFactorVerifyDto dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(twoFactorService.verify(dto, ClientInfo.from(request)));
    }

    // ---- Etapas autenticadas: feitas num dispositivo já conectado ----

    @GetMapping("/pending")
    public ResponseEntity<List<PendingLoginDto>> pending(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(twoFactorService.listPending(JwtPrincipal.userId(jwt)));
    }

    @PostMapping("/pending/{id}/deny")
    public ResponseEntity<Void> deny(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        twoFactorService.deny(JwtPrincipal.userId(jwt), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/device-code")
    public ResponseEntity<DeviceCodeDto> generateDeviceCode(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(twoFactorService.generateDeviceCode(currentUser(jwt)));
    }

    @PostMapping("/enable")
    public ResponseEntity<Void> enable(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        twoFactorService.enable(currentUser(jwt), ClientInfo.from(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/disable/request-code")
    public ResponseEntity<CodeSentDto> requestDisable(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(twoFactorService.requestDisable(currentUser(jwt)));
    }

    @PostMapping("/disable")
    public ResponseEntity<Void> disable(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ConfirmCodeDto dto) {
        twoFactorService.disable(currentUser(jwt), dto.code());
        return ResponseEntity.noContent().build();
    }

    private User currentUser(Jwt jwt) {
        return userRepository.findById(JwtPrincipal.userId(jwt))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }
}
