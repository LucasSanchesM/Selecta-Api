package com.unisal.selectaapi.mensageria;

import com.unisal.selectaapi.user.UserPreferences;

public enum EmailUpdateType {
    STAGE, DECISION, FEEDBACK, APPOINTMENT;

    public boolean enabled(UserPreferences preferences) {
        return switch (this) {
            case STAGE -> preferences.emailStageChanges();
            case DECISION -> preferences.emailDecisions();
            case FEEDBACK -> preferences.emailFeedback();
            case APPOINTMENT -> preferences.emailAppointments();
        };
    }
}
