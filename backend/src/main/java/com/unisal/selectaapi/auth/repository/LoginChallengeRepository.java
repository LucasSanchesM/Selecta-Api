package com.unisal.selectaapi.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.auth.entity.LoginChallenge;

public interface LoginChallengeRepository extends JpaRepository<LoginChallenge, UUID> {

    List<LoginChallenge> findByUserIdAndCompletedAtIsNullAndDeniedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId, LocalDateTime now);

    Optional<LoginChallenge> findByIdAndUserId(UUID id, UUID userId);

    @Transactional
    void deleteByUserId(UUID userId);
}
