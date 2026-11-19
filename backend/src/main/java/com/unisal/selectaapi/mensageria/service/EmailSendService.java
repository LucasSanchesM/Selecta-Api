package com.unisal.selectaapi.mensageria.service;

import org.springframework.stereotype.Service;

import com.unisal.selectaapi.mensageria.EmailNotifier;
import com.unisal.selectaapi.mensageria.dto.EmailSendRequest;
import com.unisal.selectaapi.mensageria.template.VerificacaoCodigoEmailTemplate;

@Service
public class EmailSendService implements EmailNotifier {

    private final ResendEmailSender sender;

    public EmailSendService(ResendEmailSender sender) {
        this.sender = sender;
    }

    @Override
    public void enviarConfirmacaoEmail(EmailSendRequest request) {
        sender.enviar(request.email(), new VerificacaoCodigoEmailTemplate(request.code()).render());
    }
}
