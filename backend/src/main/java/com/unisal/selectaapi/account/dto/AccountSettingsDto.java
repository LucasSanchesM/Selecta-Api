package com.unisal.selectaapi.account.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.unisal.selectaapi.account.entity.ProfileVisibility;

public record AccountSettingsDto(
        Account account,
        NotificationPreferencesDto notifications,
        PrivacySettingsDto privacy,
        Security security) {

    public record Account(
            UUID id,
            String name,
            String email,
            String phone,
            String language,
            String avatarUrl,
            String role) {
    }

    public record Security(
            boolean twoFactorEnabled,
            LocalDateTime passwordChangedAt,
            long trustedDevices,
            String maskedEmail) {
    }

    public record PrivacySettingsDto(
            ProfileVisibility profileVisibility,
            boolean showSalary,
            boolean openToWork,
            boolean allowDirectContact) {
    }
}
