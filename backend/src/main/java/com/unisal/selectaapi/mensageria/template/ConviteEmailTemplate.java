package com.unisal.selectaapi.mensageria.template;

import static com.unisal.selectaapi.mensageria.template.EmailLayout.escape;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.paragrafo;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.titulo;

import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;

/** Convite para uma conversa/reunião agendada com o candidato. */
public final class ConviteEmailTemplate extends CandidaturaEmailTemplate {

    public ConviteEmailTemplate(CandidaturaEmailRequest request) {
        super(request);
    }

    @Override
    protected String assunto() {
        return assuntoConviteOuAtualizacao();
    }

    @Override
    protected String conteudo() {
        return titulo(saudacao("Você foi convidado para uma reunião"))
                + paragrafoVaga("marcou uma conversa com você para")
                + painelEtapa()
                + paragrafo(escape(mensagemOu("Confira os detalhes do encontro abaixo.")))
                + cardAgendamento();
    }
}
