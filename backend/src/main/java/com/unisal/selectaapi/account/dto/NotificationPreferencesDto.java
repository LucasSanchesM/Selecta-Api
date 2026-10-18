package com.unisal.selectaapi.account.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferencesDto(
        @NotNull Boolean applicationUpdatesEmail,
        @NotNull Boolean applicationUpdatesPush,
        @NotNull Boolean messagesEmail,
        @NotNull Boolean messagesPush,
        @NotNull Boolean recommendedJobsEmail,
        @NotNull Boolean recommendedJobsPush,
        @NotNull Boolean newsEmail,
        @NotNull Boolean newsPush) {
}
