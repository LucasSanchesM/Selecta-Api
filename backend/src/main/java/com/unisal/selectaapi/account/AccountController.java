package com.unisal.selectaapi.account;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unisal.selectaapi.account.dto.AccountSettingsDto;
import com.unisal.selectaapi.account.dto.ChangePasswordDto;
import com.unisal.selectaapi.account.dto.NotificationPreferencesDto;
import com.unisal.selectaapi.account.dto.RequestEmailChangeDto;
import com.unisal.selectaapi.account.dto.UpdateAccountProfileDto;
import com.unisal.selectaapi.account.dto.UpdatePrivacyDto;
import com.unisal.selectaapi.account.service.AccountService;
import com.unisal.selectaapi.auth.dtos.CodeSentDto;
import com.unisal.selectaapi.auth.dtos.ConfirmCodeDto;
import com.unisal.selectaapi.auth.service.JwtPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/settings")
    public ResponseEntity<AccountSettingsDto> settings(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.getSettings(JwtPrincipal.userId(jwt)));
    }

    @PutMapping("/profile")
    public ResponseEntity<AccountSettingsDto> updateProfile(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateAccountProfileDto dto) {
        return ResponseEntity.ok(accountService.updateProfile(JwtPrincipal.userId(jwt), dto));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<AccountSettingsDto> removeAvatar(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.removeAvatar(JwtPrincipal.userId(jwt)));
    }

    @PostMapping("/email/request-code")
    public ResponseEntity<CodeSentDto> requestEmailChange(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RequestEmailChangeDto dto) {
        return ResponseEntity.ok(accountService.requestEmailChange(JwtPrincipal.userId(jwt), dto.newEmail()));
    }

    @PostMapping("/email/confirm")
    public ResponseEntity<AccountSettingsDto> confirmEmailChange(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConfirmCodeDto dto) {
        return ResponseEntity.ok(accountService.confirmEmailChange(JwtPrincipal.userId(jwt), dto.code()));
    }

    @PutMapping("/notifications")
    public ResponseEntity<AccountSettingsDto> updateNotifications(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NotificationPreferencesDto dto) {
        return ResponseEntity.ok(accountService.updateNotifications(JwtPrincipal.userId(jwt), dto));
    }

    @PutMapping("/privacy")
    public ResponseEntity<AccountSettingsDto> updatePrivacy(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePrivacyDto dto) {
        return ResponseEntity.ok(accountService.updatePrivacy(JwtPrincipal.userId(jwt), dto));
    }

    @PostMapping("/password/request-code")
    public ResponseEntity<CodeSentDto> requestPasswordChange(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.requestPasswordChange(JwtPrincipal.userId(jwt)));
    }

    @PostMapping("/password/change")
    public ResponseEntity<Map<String, Integer>> changePassword(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordDto dto) {
        int revoked = accountService.changePassword(JwtPrincipal.userId(jwt), JwtPrincipal.sessionId(jwt), dto);
        return ResponseEntity.ok(Map.of("revokedSessions", revoked));
    }

    @PostMapping("/export")
    public ResponseEntity<CodeSentDto> requestExport(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.requestDataExport(JwtPrincipal.userId(jwt)));
    }

    @PostMapping("/delete/request-code")
    public ResponseEntity<CodeSentDto> requestDeletion(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.requestAccountDeletion(JwtPrincipal.userId(jwt)));
    }

    @PostMapping("/delete")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ConfirmCodeDto dto) {
        accountService.deleteAccount(JwtPrincipal.userId(jwt), dto.code());
        return ResponseEntity.noContent().build();
    }
}
