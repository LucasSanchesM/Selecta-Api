package com.unisal.selectaapi.mensageria.template;

import static com.unisal.selectaapi.mensageria.template.EmailLayout.escape;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.paragrafo;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.titulo;

import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;

public final class FeedbackEmailTemplate extends CandidaturaEmailTemplate {

    public FeedbackEmailTemplate(CandidaturaEmailRequest request) {
        super(request);
    }

    @Override
    protected String assunto() {
        return "Novo feedback - " + request.vagaTitulo();
    }

    @Override
    protected String conteudo() {
        return titulo(saudacao("Você recebeu um novo feedback"))
                + paragrafoVaga("registrou um feedback na sua candidatura para")
                + painelEtapa()
                + paragrafo(escape(mensagemOu("Acesse a plataforma para ler o feedback completo.")));
    }
}
