package com.unisal.selectaapi.mensageria.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.unisal.selectaapi.mensageria.CandidaturaNotifier;
import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;

@Service
public class CandidaturaEmailService implements CandidaturaNotifier {

    private static final DateTimeFormatter AGENDAMENTO_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    @Value("${resend.api.key}")
    private String apiKey;

    @Override
    public void enviarAtualizacaoCandidatura(CandidaturaEmailRequest request) {
        Resend resend = new Resend(apiKey);

        String empresa = request.empresaNome() != null ? request.empresaNome() : "A empresa";
        String etapa = request.etapaDescricao() != null ? request.etapaDescricao() : "-";
        String status = request.statusDescricao() != null ? request.statusDescricao() : "-";
        String mensagem = request.mensagem() != null && !request.mensagem().isBlank()
                ? request.mensagem()
                : "Acesse a plataforma para ver os detalhes e os feedbacks desta etapa.";
        String agendamentoHtml = montarAgendamentoHtml(request);

        String htmlContent = """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                </head>

                <body style="
                    margin: 0;
                    padding: 0;
                    background-color: #f7f7f7;
                    font-family: Inter, Arial, sans-serif;
                ">

                    <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="padding: 40px 16px;">
                        <tr>
                            <td align="center">

                                <table
                                    width="100%%"
                                    cellpadding="0"
                                    cellspacing="0"
                                    border="0"
                                    style="
                                        max-width: 520px;
                                        background-color: #FEFEFE;
                                        border: 1px solid #E6DECC;
                                        border-radius: 14px;
                                    "
                                >

                                    <tr>
                                        <td style="padding: 40px 40px 32px 40px;">

                                            <p style="
                                                margin: 0 0 32px 0;
                                                color: #222222;
                                                font-size: 15px;
                                                font-weight: 500;
                                            ">
                                                Plataforma
                                            </p>

                                            <h1 style="
                                                margin: 0 0 12px 0;
                                                color: #4A4A4A;
                                                font-size: 19px;
                                                line-height: 28px;
                                                font-weight: 500;
                                            ">
                                                Olá, %s. Sua candidatura foi atualizada
                                            </h1>

                                            <p style="
                                                margin: 0 0 32px 0;
                                                color: #686868;
                                                font-size: 15px;
                                                line-height: 24px;
                                            ">
                                                %s atualizou o seu processo seletivo para a vaga
                                                <strong style="color:#494949;">%s</strong>.
                                            </p>

                                            <div style="
                                                background-color: #F7F7F7;
                                                border: 1px solid #E6DECC;
                                                border-radius: 12px;
                                                padding: 24px;
                                                margin-bottom: 32px;
                                            ">

                                                <p style="
                                                    margin: 0 0 8px 0;
                                                    color: #919191;
                                                    font-size: 13px;
                                                    line-height: 20px;
                                                ">
                                                    Etapa atual
                                                </p>

                                                <p style="
                                                    margin: 0 0 20px 0;
                                                    color: #222222;
                                                    font-size: 20px;
                                                    line-height: 28px;
                                                    font-weight: 500;
                                                ">
                                                    %s
                                                </p>

                                                <p style="
                                                    margin: 0 0 8px 0;
                                                    color: #919191;
                                                    font-size: 13px;
                                                    line-height: 20px;
                                                ">
                                                    Situação
                                                </p>

                                                <p style="
                                                    margin: 0;
                                                    color: #222222;
                                                    font-size: 16px;
                                                    line-height: 24px;
                                                    font-weight: 500;
                                                ">
                                                    %s
                                                </p>

                                            </div>

                                            <p style="
                                                margin: 0;
                                                color: #686868;
                                                font-size: 14px;
                                                line-height: 22px;
                                            ">
                                                %s
                                            </p>

                                            %s

                                        </td>
                                    </tr>

                                    <tr>
                                        <td style="
                                            padding: 24px 40px;
                                            border-top: 1px solid #E6DECC;
                                        ">
                                            <p style="
                                                margin: 0;
                                                color: #919191;
                                                font-size: 12px;
                                                line-height: 18px;
                                            ">
                                                Este é um e-mail automático.
                                                Por favor, não responda esta mensagem.
                                            </p>
                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>
                    </table>

                </body>
                </html>
                """
                .formatted(
                        request.candidatoNome(),
                        empresa,
                        request.vagaTitulo(),
                        etapa,
                        status,
                        mensagem,
                        agendamentoHtml);

        CreateEmailOptions sendEmailRequest = CreateEmailOptions.builder()
                .from("Plataforma <noreply@gabrieldias.tech>")
                .to(request.email())
                    .subject(request.agendamentoTitulo() != null
                        ? "Convite para reunião - " + request.agendamentoTitulo()
                        : "Atualização da sua candidatura - " + request.vagaTitulo())
                .html(htmlContent)
                .build();

        try {
            resend.emails().send(sendEmailRequest);
        } catch (ResendException e) {
            e.printStackTrace();
        }
    }

    private String montarAgendamentoHtml(CandidaturaEmailRequest request) {
        if (request.agendamentoLink() == null || request.agendamentoInicio() == null) {
            return "";
        }

        String titulo = escapar(request.agendamentoTitulo());
        String data = request.agendamentoInicio().atZone(ZoneId.of("America/Sao_Paulo"))
                .format(AGENDAMENTO_FORMATTER);
        String duracao = String.valueOf(request.agendamentoDuracaoMinutos());
        String link = escapar(request.agendamentoLink());

        return """
                <div style="
                    background-color: #F5F3FF;
                    border: 1px solid #DDD6FE;
                    border-radius: 12px;
                    padding: 24px;
                    margin-top: 24px;
                ">
                    <p style="margin:0 0 8px 0; color:#6D28D9; font-size:12px; font-weight:700; text-transform:uppercase; letter-spacing:.08em;">Convite de reunião</p>
                    <p style="margin:0 0 12px 0; color:#222222; font-size:18px; line-height:26px; font-weight:600;">%s</p>
                    <p style="margin:0 0 20px 0; color:#686868; font-size:14px; line-height:22px;">%s · %s minutos</p>
                    <a href="%s" style="display:inline-block; background:#7C3AED; color:#FFFFFF; text-decoration:none; border-radius:8px; padding:12px 18px; font-size:14px; font-weight:600;">Entrar na reunião</a>
                </div>
                """.formatted(titulo, data, duracao, link);
    }

    private String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
