package com.unisal.selectaapi.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.auth.entity.SecurityCode;
import com.unisal.selectaapi.auth.entity.SecurityCodePurpose;

public interface SecurityCodeRepository extends JpaRepository<SecurityCode, UUID> {

    Optional<SecurityCode> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(UUID userId, SecurityCodePurpose purpose);

    Optional<SecurityCode> findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(UUID userId,
            SecurityCodePurpose purpose);

    @Transactional
    void deleteByUserIdAndPurpose(UUID userId, SecurityCodePurpose purpose);

    @Transactional
    void deleteByUserId(UUID userId);
}
