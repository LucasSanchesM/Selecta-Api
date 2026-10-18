package com.unisal.selectaapi.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.auth.entity.SecurityCode;
import com.unisal.selectaapi.auth.entity.SecurityCodePurpose;
import com.unisal.selectaapi.auth.repository.SecurityCodeRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.user.entity.User;

@Service
public class SecurityCodeService {

    public static final int CODE_DIGITS = 6;
    public static final int MAX_ATTEMPTS = 5;
    public static final int RESEND_COOLDOWN_SECONDS = 60;

    private final SecurityCodeRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;

    public SecurityCodeService(SecurityCodeRepository repository, BCryptPasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public record IssuedCode(String code, LocalDateTime expiresAt) {
    }

    @Transactional
    public IssuedCode issue(User user, SecurityCodePurpose purpose, String payload, Duration ttl, int cooldownSeconds) {
        var now = LocalDateTime.now();

        if (cooldownSeconds > 0) {
            repository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose)
                    .filter(latest -> latest.getConsumedAt() == null)
                    .filter(latest -> latest.getCreatedAt().isAfter(now.minusSeconds(cooldownSeconds)))
                    .ifPresent(latest -> {
                        throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                                "Aguarde um minuto antes de solicitar um novo código.");
                    });
        }

        repository.deleteByUserIdAndPurpose(user.getId(), purpose);
        repository.flush();

        String code = SecureTokens.numericCode(CODE_DIGITS);
        var expiresAt = now.plus(ttl);
        repository.save(new SecurityCode(user, purpose, passwordEncoder.encode(code), payload, now, expiresAt));
        return new IssuedCode(code, expiresAt);
    }

    /** Valida e consome o código; tentativas erradas ficam registradas mesmo com a exceção. */
    @Transactional(noRollbackFor = BusinessException.class)
    public SecurityCode consume(User user, SecurityCodePurpose purpose, String code) {
        var securityCode = repository
                .findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "Nenhum código pendente. Solicite um novo código."));

        var now = LocalDateTime.now();
        if (securityCode.getExpiresAt().isBefore(now)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "Código expirado. Solicite um novo código.");
        }
        if (securityCode.getAttempts() >= MAX_ATTEMPTS) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Muitas tentativas incorretas. Solicite um novo código.");
        }
        if (code == null || !passwordEncoder.matches(code, securityCode.getCodeHash())) {
            securityCode.registerFailedAttempt();
            repository.save(securityCode);
            int remaining = MAX_ATTEMPTS - securityCode.getAttempts();
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, remaining > 0
                    ? "Código inválido. Você ainda tem " + remaining + " tentativa(s)."
                    : "Muitas tentativas incorretas. Solicite um novo código.");
        }

        securityCode.consume(now);
        return repository.save(securityCode);
    }

    @Transactional
    public boolean tryConsume(User user, SecurityCodePurpose purpose, String code) {
        var found = repository.findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose);
        if (found.isEmpty()) {
            return false;
        }
        var securityCode = found.get();
        var now = LocalDateTime.now();
        if (securityCode.getExpiresAt().isBefore(now) || securityCode.getAttempts() >= MAX_ATTEMPTS) {
            return false;
        }
        if (code == null || !passwordEncoder.matches(code, securityCode.getCodeHash())) {
            securityCode.registerFailedAttempt();
            repository.save(securityCode);
            return false;
        }
        securityCode.consume(now);
        repository.save(securityCode);
        return true;
    }

    @Transactional
    public void invalidate(User user, SecurityCodePurpose purpose) {
        repository.deleteByUserIdAndPurpose(user.getId(), purpose);
    }

    @Transactional
    public void deleteAll(User user) {
        repository.deleteByUserId(user.getId());
    }
}
