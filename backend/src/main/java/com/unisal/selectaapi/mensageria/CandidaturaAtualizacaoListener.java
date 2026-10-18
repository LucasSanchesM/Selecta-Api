package com.unisal.selectaapi.mensageria;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.unisal.selectaapi.account.service.UserSettingsService;
import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;

@Component
public class CandidaturaAtualizacaoListener {

    private final CandidaturaNotifier candidaturaNotifier;
    private final UserSettingsService userSettingsService;

    public CandidaturaAtualizacaoListener(CandidaturaNotifier candidaturaNotifier,
            UserSettingsService userSettingsService) {
        this.candidaturaNotifier = candidaturaNotifier;
        this.userSettingsService = userSettingsService;
    }

    @RabbitListener(queues = "candidatura.atualizacao")
    public void messageListener(CandidaturaEmailRequest parametros) {
        if (!userSettingsService.wantsApplicationUpdateEmails(parametros.email())) {
            return;
        }
        System.out.println("Atualização de candidatura enviada para: " + parametros.email());
        candidaturaNotifier.enviarAtualizacaoCandidatura(parametros);
    }
}
