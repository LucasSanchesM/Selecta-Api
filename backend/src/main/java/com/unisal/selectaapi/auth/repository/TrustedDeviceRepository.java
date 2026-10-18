package com.unisal.selectaapi.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.auth.entity.TrustedDevice;

public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, UUID> {

    Optional<TrustedDevice> findByUserIdAndDeviceIdHash(UUID userId, String deviceIdHash);

    long countByUserId(UUID userId);

    @Transactional
    void deleteByUserId(UUID userId);
}
