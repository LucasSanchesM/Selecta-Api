package com.unisal.selectaapi.auth.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unisal.selectaapi.auth.dtos.CompanyRegistrationDto;
import com.unisal.selectaapi.auth.dtos.LoginRequestDto;
import com.unisal.selectaapi.auth.dtos.LoginResponseDto;
import com.unisal.selectaapi.auth.dtos.RefreshTokenRequestDto;
import com.unisal.selectaapi.auth.dtos.ResendCodeDto;
import com.unisal.selectaapi.auth.dtos.VerifyEmailDto;
import com.unisal.selectaapi.auth.service.AuthService;
import com.unisal.selectaapi.auth.service.ClientInfo;
import com.unisal.selectaapi.auth.service.CompanyRegistrationService;
import com.unisal.selectaapi.auth.service.SessionService;
import com.unisal.selectaapi.user.UserService;
import com.unisal.selectaapi.user.dto.UserResponseDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final CompanyRegistrationService companyRegistrationService;
    private final SessionService sessionService;

    public AuthController(
            AuthService authService,
            UserService userService,
            CompanyRegistrationService companyRegistrationService,
            SessionService sessionService) {
        this.authService = authService;
        this.userService = userService;
        this.companyRegistrationService = companyRegistrationService;
        this.sessionService = sessionService;
    }

    @PostMapping("/register-company")
    public ResponseEntity<UserResponseDto> registerCompany(
            @Valid @RequestBody CompanyRegistrationDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyRegistrationService.register(dto));
    }

    /**
     * Login com e-mail e senha.
     * Quando necessário, retorna o desafio de autenticação em duas etapas.
     */
    @PostMapping
    public ResponseEntity<LoginResponseDto> logarUser(
            @Valid @RequestBody LoginRequestDto dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                authService.loginUser(dto, ClientInfo.from(request)));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponseDto> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDto dto) {
        return ResponseEntity.ok(
                sessionService.refresh(dto.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt) {
        String sid = jwt.getClaimAsString("sid");

        sessionService.revokeCurrent(
                sid != null ? UUID.fromString(sid) : null);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Valid @RequestBody VerifyEmailDto dto) {
        userService.verifyEmail(dto.email(), dto.code());

        return ResponseEntity.ok(
                Map.of("message", "Conta ativada com sucesso!"));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<Map<String, String>> resendCode(
            @Valid @RequestBody ResendCodeDto dto) {
        userService.resendVerificationCode(dto.email());

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Novo código de verificação enviado para o email"));
    }
}