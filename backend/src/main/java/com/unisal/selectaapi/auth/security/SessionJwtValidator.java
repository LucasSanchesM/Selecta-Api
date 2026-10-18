package com.unisal.selectaapi.auth.security;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import com.unisal.selectaapi.auth.repository.UserSessionRepository;

/**
 * Recusa JWTs de sessões encerradas. Sem isso, "Encerrar sessão" só teria efeito
 * quando o access token expirasse (até 15 minutos depois).
 */
public class SessionJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error SESSION_REVOKED = new OAuth2Error("invalid_token",
            "Sessão encerrada. Entre novamente.", null);

    private final UserSessionRepository repository;

    public SessionJwtValidator(UserSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String sid = jwt.getClaimAsString("sid");
        if (sid == null) {
            return OAuth2TokenValidatorResult.success();
        }

        UUID sessionId;
        try {
            sessionId = UUID.fromString(sid);
        } catch (IllegalArgumentException exception) {
            return OAuth2TokenValidatorResult.failure(SESSION_REVOKED);
        }

        boolean active = repository.existsByIdAndRevokedAtIsNullAndExpiresAtAfter(sessionId, LocalDateTime.now());
        return active ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(SESSION_REVOKED);
    }
}
