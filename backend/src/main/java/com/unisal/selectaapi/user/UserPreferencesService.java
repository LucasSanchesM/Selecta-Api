package com.unisal.selectaapi.user;

import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.unisal.selectaapi.candidate.repository.CandidateProfileRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.recomendacao.*;

@Service
public class UserPreferencesService {
    private final UserRepository users;
    private final CandidateProfileRepository profiles;
    private final EmbeddingRepository embeddings;
    private final EmbeddingSchemaInitializer schema;
    private final ApplicationEventPublisher events;

    public UserPreferencesService(UserRepository users, CandidateProfileRepository profiles,
            EmbeddingRepository embeddings, EmbeddingSchemaInitializer schema, ApplicationEventPublisher events) {
        this.users = users;
        this.profiles = profiles;
        this.embeddings = embeddings;
        this.schema = schema;
        this.events = events;
    }

    public UserPreferences get(UUID userId) {
        return users.findById(userId).orElseThrow(UserPreferencesService::missingUser).getPreferences();
    }

    @Transactional
    public UserPreferences update(UUID userId, UserPreferences preferences) {
        var user = users.lockById(userId).orElseThrow(UserPreferencesService::missingUser);
        boolean personalizationChanged = !preferences.personalizedJobs().equals(user.getPreferences().personalizedJobs());
        user.setPreferences(preferences);
        if (personalizationChanged) {
            profiles.findByUserId(userId).ifPresent(profile -> {
                if (preferences.personalizedJobs()) {
                    events.publishEvent(EmbeddingRequestedEvent.candidato(profile.getId()));
                } else if (schema.isAvailable()) {
                    embeddings.delete(EmbeddingTarget.CANDIDATO, profile.getId());
                }
            });
        }
        return preferences;
    }

    private static BusinessException missingUser() {
        return new BusinessException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
    }
}
