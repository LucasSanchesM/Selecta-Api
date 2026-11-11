package com.unisal.selectaapi.mensageria;

import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.UserPreferences;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;

@Component
public class CandidaturaAtualizacaoListener {

    private final CandidaturaNotifier candidaturaNotifier;

    private final UserRepository users;

    public CandidaturaAtualizacaoListener(CandidaturaNotifier candidaturaNotifier, UserRepository users) {
        this.users = users;
        this.candidaturaNotifier = candidaturaNotifier;
    }

    @RabbitListener(queues = "candidatura.atualizacao")
    public void messageListener(CandidaturaEmailRequest parametros) {
        var recipient = parametros.userId() == null
                ? users.findByEmail(parametros.email()) : users.findById(parametros.userId());
        recipient.filter(user -> user.getEmail().equals(parametros.email()))
                .filter(user -> enabled(parametros, user.getPreferences()))
                .ifPresent(user -> candidaturaNotifier.enviarAtualizacaoCandidatura(parametros));
    }

    private boolean enabled(CandidaturaEmailRequest request, UserPreferences preferences) {
        if (request.type() != null) {
            return request.type().enabled(preferences);
        }
        // Mensagens antigas sem categoria só podem ser entregues quando a preferência é inequívoca.
        if (request.agendamentoInicio() != null) {
            return preferences.emailAppointments();
        }
        return preferences.emailStageChanges() && preferences.emailDecisions() && preferences.emailFeedback();
    }
}
