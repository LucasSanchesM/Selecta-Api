package com.unisal.selectaapi.auth.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.auth.dtos.LoginResponseDto;
import com.unisal.selectaapi.auth.dtos.SessionResponseDto;
import com.unisal.selectaapi.auth.entity.UserSession;
import com.unisal.selectaapi.auth.repository.UserSessionRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.user.entity.User;

@Service
public class SessionService {

    public static final long ACCESS_TOKEN_SECONDS = 900L;
    public static final long REFRESH_TOKEN_DAYS = 7L;
    /** Requisições paralelas podem usar o mesmo refresh token logo após a rotação. */
    private static final long ROTATION_GRACE_SECONDS = 30L;

    private final UserSessionRepository repository;
    private final JwtEncoder jwtEncoder;

    public SessionService(UserSessionRepository repository, JwtEncoder jwtEncoder) {
        this.repository = repository;
        this.jwtEncoder = jwtEncoder;
    }

    @Transactional
    public LoginResponseDto startSession(User user, ClientInfo client) {
        var now = LocalDateTime.now();
        var session = new UserSession(
                user,
                client.deviceIdHash(),
                client.deviceName(),
                client.userAgent(),
                client.ipAddress(),
                client.location(),
                now,
                now.plusDays(REFRESH_TOKEN_DAYS));

        String secret = SecureTokens.randomToken(32);
        session.setRefreshTokenHash(SecureTokens.sha256(secret));
        repository.save(session);

        return issue(user, session.getId(), session.getId() + "." + secret);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponseDto refresh(String refreshToken) {
        int separator = refreshToken.indexOf('.');
        if (separator <= 0) {
            throw invalidSession();
        }

        UUID sessionId;
        try {
            sessionId = UUID.fromString(refreshToken.substring(0, separator));
        } catch (IllegalArgumentException exception) {
            throw invalidSession();
        }
        String presentedHash = SecureTokens.sha256(refreshToken.substring(separator + 1));

        var session = repository.findById(sessionId).orElseThrow(this::invalidSession);
        var now = LocalDateTime.now();
        if (!session.isActive(now)) {
            throw invalidSession();
        }

        boolean matchesCurrent = SecureTokens.constantTimeEquals(presentedHash, session.getRefreshTokenHash());
        boolean matchesPrevious = !matchesCurrent
                && session.getRotatedAt() != null
                && session.getRotatedAt().isAfter(now.minusSeconds(ROTATION_GRACE_SECONDS))
                && SecureTokens.constantTimeEquals(presentedHash, session.getPreviousRefreshTokenHash());

        if (!matchesCurrent && !matchesPrevious) {
            // Token antigo reutilizado fora da janela: possível roubo, derruba a sessão.
            session.revoke(now);
            repository.save(session);
            throw invalidSession();
        }

        String secret = SecureTokens.randomToken(32);
        session.rotate(SecureTokens.sha256(secret), now, now.plusDays(REFRESH_TOKEN_DAYS));
        repository.save(session);

        var user = session.getUser();
        return issue(user, session.getId(), session.getId() + "." + secret);
    }

    @Transactional(readOnly = true)
    public List<SessionResponseDto> listActive(UUID userId, UUID currentSessionId) {
        return repository
                .findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastActiveAtDesc(userId, LocalDateTime.now())
                .stream()
                .map(session -> new SessionResponseDto(
                        session.getId(),
                        session.getDeviceName(),
                        DeviceNameParser.isMobile(session.getUserAgent()),
                        session.getLocation(),
                        session.getIpAddress(),
                        session.getCreatedAt(),
                        session.getLastActiveAt(),
                        session.getId().equals(currentSessionId)))
                .sorted((a, b) -> Boolean.compare(b.current(), a.current()))
                .toList();
    }

    @Transactional
    public void revoke(UUID userId, UUID sessionId) {
        var session = repository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Sessão não encontrada"));
        session.revoke(LocalDateTime.now());
        repository.save(session);
    }

    @Transactional
    public int revokeOthers(UUID userId, UUID currentSessionId) {
        var now = LocalDateTime.now();
        var sessions = repository.findByUserIdAndRevokedAtIsNull(userId).stream()
                .filter(session -> !session.getId().equals(currentSessionId))
                .toList();
        sessions.forEach(session -> session.revoke(now));
        repository.saveAll(sessions);
        return sessions.size();
    }

    @Transactional
    public void revokeAll(UUID userId) {
        revokeOthers(userId, null);
    }

    @Transactional
    public void revokeCurrent(UUID sessionId) {
        if (sessionId == null) {
            return;
        }
        repository.findById(sessionId).ifPresent(session -> {
            session.revoke(LocalDateTime.now());
            repository.save(session);
        });
    }

    private LoginResponseDto issue(User user, UUID sessionId, String refreshToken) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .issuer("backend_selecta")
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TOKEN_SECONDS))
                .claim("scope", user.getRole())
                .claim("sid", sessionId.toString())
                .build();

        String jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return LoginResponseDto.tokens(jwt, ACCESS_TOKEN_SECONDS, user.getRole(), refreshToken);
    }

    private BusinessException invalidSession() {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "Sua sessão expirou. Entre novamente.");
    }
}
