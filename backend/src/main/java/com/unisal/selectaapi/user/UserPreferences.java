package com.unisal.selectaapi.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;

@Embeddable
public record UserPreferences(
        @NotNull @Column(nullable = false, columnDefinition = "boolean default true") Boolean showEmail,
        @NotNull @Column(nullable = false, columnDefinition = "boolean default true") Boolean showPhone,
        @NotNull @Column(nullable = false, columnDefinition = "boolean default true") Boolean showResume,
        @NotNull @Column(nullable = false, columnDefinition = "boolean default true") Boolean showPhoto,
        @NotNull @Column(nullable = false, columnDefinition = "boolean default true") Boolean emailStageChanges,
        @NotNull @Column(nullable = false, columnDefinition = "boolean default true") Boolean emailDecisions,
        @NotNull @Column(nullable = false, columnDefinition = "boolean default true") Boolean emailFeedback,
        @NotNull @Column(nullable = false, columnDefinition = "boolean default true") Boolean emailAppointments,
        @NotNull @Column(nullable = false, columnDefinition = "boolean default true") Boolean personalizedJobs) {

    public static UserPreferences defaults() {
        return new UserPreferences(true, true, true, true, true, true, true, true, true);
    }
}
