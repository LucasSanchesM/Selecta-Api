package com.unisal.selectaapi.auth.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.auth.entity.VerificationCode;
import com.unisal.selectaapi.auth.repository.VerificationCodeRepository;
import com.unisal.selectaapi.user.entity.User;

@Service
public class VerificationCodeService {

    private static final int CODE_EXPIRATION_SECONDS = 10;

    private final VerificationCodeRepository verificationCodeRepository;

    public VerificationCodeService(VerificationCodeRepository verificationCodeRepository) {
        this.verificationCodeRepository = verificationCodeRepository;
    }

    @Transactional
    public void saveCode(User user, String code) {
        Instant now = Instant.now();
        verificationCodeRepository.deleteByUser(user);
        verificationCodeRepository.flush();
        verificationCodeRepository.save(
                new VerificationCode(user, code, now.plusSeconds(CODE_EXPIRATION_SECONDS*60), now));
    }

    public Optional<VerificationCode> getCode(User user) {
        return verificationCodeRepository.findByUser(user);
    }

    public boolean isRateLimited(User user) {
        return getCode(user)
                .map(data -> data.getCreatedAt().isAfter(Instant.now().minusSeconds(60)))
                .orElse(false);
    }

    @Transactional
    public void removeCode(User user) {
        verificationCodeRepository.deleteByUser(user);
    }
}
