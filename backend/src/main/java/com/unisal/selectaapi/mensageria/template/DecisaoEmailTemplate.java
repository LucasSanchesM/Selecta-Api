package com.unisal.selectaapi.mensageria.template;

import static com.unisal.selectaapi.mensageria.template.EmailLayout.escape;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.paragrafo;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.titulo;

import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;

public final class DecisaoEmailTemplate extends CandidaturaEmailTemplate {

    public DecisaoEmailTemplate(CandidaturaEmailRequest request) {
        super(request);
    }

    @Override
    protected String assunto() {
        return "Decisão sobre sua candidatura - " + request.vagaTitulo();
    }

    @Override
    protected String conteudo() {
        return titulo(saudacao("Há uma decisão sobre sua candidatura"))
                + paragrafoVaga("tomou uma decisão sobre o seu processo seletivo para")
                + painelEtapa()
                + paragrafo(escape(mensagemOu("Acesse a plataforma para conferir a decisão e os detalhes.")));
    }
}
