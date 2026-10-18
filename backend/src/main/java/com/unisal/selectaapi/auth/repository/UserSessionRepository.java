package com.unisal.selectaapi.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.unisal.selectaapi.auth.entity.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    boolean existsByIdAndRevokedAtIsNullAndExpiresAtAfter(UUID id, LocalDateTime now);

    List<UserSession> findByUserIdAndRevokedAtIsNullAndExpiresAtAfterOrderByLastActiveAtDesc(UUID userId,
            LocalDateTime now);

    List<UserSession> findByUserIdAndRevokedAtIsNull(UUID userId);

    Optional<UserSession> findByIdAndUserId(UUID id, UUID userId);
}
