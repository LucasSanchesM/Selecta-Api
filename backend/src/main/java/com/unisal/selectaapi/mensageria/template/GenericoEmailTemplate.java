package com.unisal.selectaapi.mensageria.template;

import static com.unisal.selectaapi.mensageria.template.EmailLayout.escape;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.paragrafo;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.titulo;

import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;

/**
 * Template de fallback para mensagens antigas da fila que chegam sem categoria
 * ({@code type == null}). Mantém o texto genérico de atualização.
 */
public final class GenericoEmailTemplate extends CandidaturaEmailTemplate {

    public GenericoEmailTemplate(CandidaturaEmailRequest request) {
        super(request);
    }

    @Override
    protected String assunto() {
        return assuntoConviteOuAtualizacao();
    }

    @Override
    protected String conteudo() {
        return titulo(saudacao("Sua candidatura foi atualizada"))
                + paragrafoVaga("atualizou o seu processo seletivo para")
                + painelEtapa()
                + paragrafo(escape(mensagemOu("Acesse a plataforma para ver os detalhes e os feedbacks desta etapa.")))
                + cardAgendamento();
    }
}
