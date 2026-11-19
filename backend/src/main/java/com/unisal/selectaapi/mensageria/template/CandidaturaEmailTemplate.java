package com.unisal.selectaapi.mensageria.template;

import static com.unisal.selectaapi.mensageria.template.EmailLayout.campo;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.escape;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.page;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.painel;
import static com.unisal.selectaapi.mensageria.template.EmailLayout.paragrafo;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;

public abstract class CandidaturaEmailTemplate implements EmailTemplate {

    private static final DateTimeFormatter AGENDAMENTO_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final ZoneId FUSO_AGENDAMENTO = ZoneId.of("America/Sao_Paulo");

    protected final CandidaturaEmailRequest request;

    protected CandidaturaEmailTemplate(CandidaturaEmailRequest request) {
        this.request = request;
    }

    @Override
    public EmailMessage render() {
        return new EmailMessage(assunto(), page(conteudo()));
    }

    protected abstract String assunto();

    protected abstract String conteudo();

    protected String empresa() {
        return request.empresaNome() != null ? request.empresaNome() : "A empresa";
    }

    protected String etapa() {
        return request.etapaDescricao() != null ? request.etapaDescricao() : "-";
    }

    protected String status() {
        return request.statusDescricao() != null ? request.statusDescricao() : "-";
    }

    /** Mensagem enviada pelo empresa, ou o texto padrão do tipo quando não há mensagem. */
    protected String mensagemOu(String padrao) {
        return request.mensagem() != null && !request.mensagem().isBlank() ? request.mensagem() : padrao;
    }

    protected String saudacao(String texto) {
        return "Olá, " + request.candidatoNome() + ". " + texto;
    }

    /** Parágrafo "{empresa} <acao> a vaga <vaga>.", com os valores escapados. */
    protected String paragrafoVaga(String acao) {
        return paragrafo(escape(empresa()) + " " + acao + " a vaga "
                + EmailLayout.destaque(request.vagaTitulo()) + ".");
    }

    /** Painel com a etapa atual e a situação da candidatura. */
    protected String painelEtapa() {
        return painel(campo("Etapa atual", etapa(), 20) + campo("Situação", status(), 16));
    }

    protected String assuntoPadraoAtualizacao() {
        return "Atualização da sua candidatura - " + request.vagaTitulo();
    }

    /** Assunto de convite quando há título de agendamento, senão o assunto de atualização. */
    protected String assuntoConviteOuAtualizacao() {
        return request.agendamentoTitulo() != null
                ? "Convite para reunião - " + request.agendamentoTitulo()
                : assuntoPadraoAtualizacao();
    }

    /** Cartão de convite da reunião. Vazio quando a candidatura não tem agendamento. */
    protected String cardAgendamento() {
        if (request.agendamentoLink() == null || request.agendamentoInicio() == null) {
            return "";
        }

        String data = request.agendamentoInicio().atZone(FUSO_AGENDAMENTO).format(AGENDAMENTO_FORMATTER);
        String duracao = String.valueOf(request.agendamentoDuracaoMinutos());

        return """
                <div style="
                    background-color: #F5F3FF;
                    border: 1px solid #DDD6FE;
                    border-top: 4px solid #7C3AED;
                    border-radius: 16px;
                    padding: 32px 24px;
                    margin: 8px 0 24px 0;
                    text-align: center;
                ">
                    <p style="margin:0 0 8px 0; color:#6D28D9; font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:.08em;">Convite de reunião</p>
                    <p style="margin:0 0 12px 0; color:#222222; font-size:18px; line-height:26px; font-weight:600;">%s</p>
                    <p style="margin:0 0 20px 0; color:#686868; font-size:14px; line-height:22px;">%s · %s minutos</p>
                    %s
                </div>
                """.formatted(
                escape(request.agendamentoTitulo()),
                escape(data),
                escape(duracao),
                EmailLayout.botao(request.agendamentoLink(), "Entrar na reunião"));
    }
}
