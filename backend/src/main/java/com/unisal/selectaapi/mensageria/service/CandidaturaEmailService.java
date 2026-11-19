package com.unisal.selectaapi.mensageria.service;

import org.springframework.stereotype.Service;

import com.unisal.selectaapi.mensageria.CandidaturaNotifier;
import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;
import com.unisal.selectaapi.mensageria.template.CandidaturaEmailTemplates;

@Service
public class CandidaturaEmailService implements CandidaturaNotifier {

    private final ResendEmailSender sender;

    public CandidaturaEmailService(ResendEmailSender sender) {
        this.sender = sender;
    }

    @Override
    public void enviarAtualizacaoCandidatura(CandidaturaEmailRequest request) {
        sender.enviar(request.email(), CandidaturaEmailTemplates.para(request).render());
    }
}
