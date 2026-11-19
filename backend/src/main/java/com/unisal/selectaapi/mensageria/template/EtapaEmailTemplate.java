package com.unisal.selectaapi.mensageria.template;

import static com.unisal.selectaapi.mensageria.template.EmailLayout.escape;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.paragrafo;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.titulo;

import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;

/** Candidato mudou de etapa no processo seletivo. */
public final class EtapaEmailTemplate extends CandidaturaEmailTemplate {

    public EtapaEmailTemplate(CandidaturaEmailRequest request) {
        super(request);
    }

    @Override
    protected String assunto() {
        return assuntoPadraoAtualizacao();
    }

    @Override
    protected String conteudo() {
        return titulo(saudacao("Sua candidatura mudou de etapa"))
                + paragrafoVaga("atualizou o seu processo seletivo para")
                + painelEtapa()
                + paragrafo(escape(mensagemOu("Acesse a plataforma para ver os detalhes e os feedbacks desta etapa.")))
                + cardAgendamento();
    }
}
