package com.unisal.selectaapi.services;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import com.unisal.selectaapi.candidate.entity.CandidateProfile;
import com.unisal.selectaapi.candidate.repository.CandidateProfileRepository;
import com.unisal.selectaapi.mensageria.*;
import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;
import com.unisal.selectaapi.recomendacao.*;
import com.unisal.selectaapi.user.*;
import com.unisal.selectaapi.user.entity.User;
import static org.mockito.Mockito.*;

class UserPreferencesServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final User user = new User();
    private final UUID userId = UUID.randomUUID();

    @Test
    void respectsFeedbackOptOutForAnEmailAlreadyInTheQueue() {
        var notifier = mock(CandidaturaNotifier.class);
        var listener = new CandidaturaAtualizacaoListener(notifier, users);
        user.setEmail("pessoa@example.org");
        user.setPreferences(new UserPreferences(true, true, true, true, true, true, false, true, true));
        when(users.findById(userId)).thenReturn(Optional.of(user));

        var feedback = message(EmailUpdateType.FEEDBACK);
        var appointment = message(EmailUpdateType.APPOINTMENT);
        listener.messageListener(feedback);
        listener.messageListener(appointment);
        verify(notifier, never()).enviarAtualizacaoCandidatura(feedback);
        verify(notifier).enviarAtualizacaoCandidatura(appointment);
    }

    @Test
    void removesVectorOnOptOutAndRequestsIndexingOnOptIn() {
        var profiles = mock(CandidateProfileRepository.class);
        var schema = mock(EmbeddingSchemaInitializer.class);
        var embeddings = mock(EmbeddingRepository.class);
        var events = mock(ApplicationEventPublisher.class);
        var service = new UserPreferencesService(users, profiles, embeddings, schema, events);
        var profile = new CandidateProfile(user);
        profile.setId(UUID.randomUUID());
        when(users.lockById(userId)).thenReturn(Optional.of(user));
        when(profiles.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(schema.isAvailable()).thenReturn(true);

        service.update(userId, new UserPreferences(true, true, true, true, true, true, true, true, false));
        verify(embeddings).delete(EmbeddingTarget.CANDIDATO, profile.getId());
        service.update(userId, UserPreferences.defaults());
        verify(events).publishEvent(EmbeddingRequestedEvent.candidato(profile.getId()));
    }

    private CandidaturaEmailRequest message(EmailUpdateType type) {
        return new CandidaturaEmailRequest(user.getEmail(), "Pessoa", "Vaga", "Empresa", "Entrevista",
                "Em andamento", null, null, null, null, null, userId, type);
    }
}
