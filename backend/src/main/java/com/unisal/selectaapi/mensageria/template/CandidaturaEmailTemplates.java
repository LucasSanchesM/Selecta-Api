package com.unisal.selectaapi.mensageria.template;

import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;

/**
 * Escolhe o template de acordo com a categoria ({@link com.unisal.selectaapi.mensageria.EmailUpdateType})
 * da mensagem recebida. Para criar um novo tipo: adicione o enum, uma classe de template
 * e um case aqui.
 */
public final class CandidaturaEmailTemplates {

    private CandidaturaEmailTemplates() {
    }

    public static EmailTemplate para(CandidaturaEmailRequest request) {
        if (request.type() == null) {
            return new GenericoEmailTemplate(request);
        }
        return switch (request.type()) {
            case STAGE -> new EtapaEmailTemplate(request);
            case DECISION -> new DecisaoEmailTemplate(request);
            case FEEDBACK -> new FeedbackEmailTemplate(request);
            case APPOINTMENT -> new ConviteEmailTemplate(request);
        };
    }
}
