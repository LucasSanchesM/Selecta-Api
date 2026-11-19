package com.unisal.selectaapi.mensageria.template;

import static com.unisal.selectaapi.mensageria.template.EmailLayout.codigo;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.destaque;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.observacao;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.page;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.paragrafo;

/** E-mail com o código de verificação do cadastro. */
public final class VerificacaoCodigoEmailTemplate implements EmailTemplate {

    private final String codigo;

    public VerificacaoCodigoEmailTemplate(String codigo) {
        this.codigo = codigo;
    }

    @Override
    public EmailMessage render() {
        String conteudo = paragrafo("Para concluir seu cadastro, utilize o código de verificação abaixo.")
                + codigo(codigo)
                + paragrafo("Este código é válido por " + destaque("10 minutos") + ".")
                + observacao("Se você não solicitou esta verificação, nenhuma ação é necessária.");

        return new EmailMessage("Código de Verificação - Processo Seletivo",
                page("Obrigado por se cadastrar!", "Confirme seu e-mail", conteudo));
    }
}
