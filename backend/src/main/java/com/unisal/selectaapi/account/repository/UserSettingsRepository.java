package com.unisal.selectaapi.account.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.account.entity.UserSettings;

public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {

    Optional<UserSettings> findByUserId(UUID userId);

    Optional<UserSettings> findByUserEmail(String email);

    @Transactional
    void deleteByUserId(UUID userId);
}
