package com.unisal.selectaapi.mensageria.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.unisal.selectaapi.mensageria.template.EmailMessage;

/**
 * Único ponto de envio de e-mails via Resend. Os serviços montam o conteúdo com
 * um template e delegam o envio para cá.
 */
@Component
public class ResendEmailSender {

    private static final String REMETENTE = "Plataforma <noreply@gabrieldias.tech>";

    @Value("${resend.api.key}")
    private String apiKey;

    public void enviar(String destinatario, EmailMessage mensagem) {
        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(REMETENTE)
                .to(destinatario)
                .subject(mensagem.subject())
                .html(mensagem.html())
                .build();

        try {
            new Resend(apiKey).emails().send(options);
        } catch (ResendException e) {
            e.printStackTrace();
        }
    }
}
