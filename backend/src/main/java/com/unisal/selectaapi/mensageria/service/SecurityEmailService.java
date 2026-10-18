package com.unisal.selectaapi.mensageria.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import com.unisal.selectaapi.mensageria.dto.SecurityEmailRequest;

@Service
public class SecurityEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityEmailService.class);
    private static final String FROM = "Selecta <noreply@gabrieldias.tech>";

    @Value("${resend.api.key}")
    private String apiKey;

    public void send(SecurityEmailRequest request) {
        var options = CreateEmailOptions.builder()
                .from(FROM)
                .to(request.email())
                .subject(request.subject())
                .html(render(request.title(), request.description(), request.code(), request.expiresInMinutes(),
                        request.details()))
                .build();
        deliver(options, request.email());
    }

    @Async
    public void sendDataExport(String email, String name, String json) {
        var attachment = Attachment.builder()
                .fileName("selecta-meus-dados-" + LocalDate.now() + ".json")
                .content(Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)))
                .build();

        var options = CreateEmailOptions.builder()
                .from(FROM)
                .to(email)
                .subject("Sua cópia dos dados da Selecta")
                .html(render(
                        "Seus dados estão prontos",
                        "Olá, " + name + ". Em anexo está uma cópia do seu perfil, das suas preferências e do histórico de candidaturas na Selecta, em formato JSON.",
                        null,
                        null,
                        "Se você não fez esta solicitação, altere sua senha e encerre as sessões desconhecidas nas configurações da conta."))
                .attachments(attachment)
                .build();
        deliver(options, email);
    }

    private void deliver(CreateEmailOptions options, String email) {
        try {
            new Resend(apiKey).emails().send(options);
        } catch (ResendException exception) {
            LOGGER.error("Falha ao enviar e-mail de segurança para {}", email, exception);
        }
    }

    private String render(String title, String description, String code, Integer expiresInMinutes, String details) {
        String codeBlock = code == null ? "" : """
                <div style="background-color:#F7F7F7;border:1px solid #E6DECC;border-radius:12px;padding:24px;text-align:center;margin-bottom:24px;">
                    <p style="margin:0 0 8px 0;color:#919191;font-size:13px;line-height:20px;">Seu código</p>
                    <p style="margin:0;color:#222222;font-size:32px;line-height:40px;font-weight:600;letter-spacing:6px;">%s</p>
                </div>
                <p style="margin:0 0 8px 0;color:#686868;font-size:14px;line-height:22px;">
                    Este código é válido por <strong style="color:#494949;">%d minutos</strong> e só pode ser usado uma vez.
                </p>
                """.formatted(HtmlUtils.htmlEscape(code), expiresInMinutes != null ? expiresInMinutes : 10);

        String detailsBlock = details == null ? "" : """
                <p style="margin:0;color:#919191;font-size:14px;line-height:22px;">%s</p>
                """.formatted(HtmlUtils.htmlEscape(details));

        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"></head>
                <body style="margin:0;padding:0;background-color:#f7f7f7;font-family:Inter,Arial,sans-serif;">
                    <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="padding:40px 16px;">
                        <tr><td align="center">
                            <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="max-width:520px;background-color:#FEFEFE;border:1px solid #E6DECC;border-radius:14px;">
                                <tr><td style="padding:40px 40px 32px 40px;">
                                    <p style="margin:0 0 32px 0;color:#222222;font-size:15px;font-weight:600;">Selecta</p>
                                    <h1 style="margin:0 0 12px 0;color:#4A4A4A;font-size:19px;line-height:28px;font-weight:500;">%s</h1>
                                    <p style="margin:0 0 28px 0;color:#686868;font-size:15px;line-height:24px;">%s</p>
                                    %s
                                    %s
                                </td></tr>
                                <tr><td style="padding:24px 40px;border-top:1px solid #E6DECC;">
                                    <p style="margin:0;color:#919191;font-size:12px;line-height:18px;">
                                        Este é um e-mail automático de segurança. Nunca compartilhe seus códigos com ninguém — a equipe da Selecta jamais vai pedi-los.
                                    </p>
                                </td></tr>
                            </table>
                        </td></tr>
                    </table>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(title), HtmlUtils.htmlEscape(description), codeBlock, detailsBlock);
    }
}
