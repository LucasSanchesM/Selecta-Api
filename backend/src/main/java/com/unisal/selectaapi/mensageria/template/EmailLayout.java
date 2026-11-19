package com.unisal.selectaapi.mensageria.template;

/**
 * Blocos de HTML compartilhados pelos e-mails. Tudo é baseado em tabelas e estilos inline
 * para funcionar em Gmail, Outlook e clientes móveis.
 */
public final class EmailLayout {

    private static final String FONTE = "'Segoe UI', Inter, Helvetica, Arial, sans-serif";

    private EmailLayout() {
    }

    /** Página com o banner padrão da plataforma. */
    public static String page(String conteudo) {
        return page("Processos Seletivos", "Mais transparentes e humanizados", conteudo);
    }

    /** Página com banner personalizado (linha pequena em caixa-alta + título grande). */
    public static String page(String eyebrow, String tituloBanner, String conteudo) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="color-scheme" content="light">
                </head>

                <body style="margin: 0; padding: 0; background-color: #F3F0FA; font-family: %s; -webkit-font-smoothing: antialiased;">
                    <table width="100%%" cellpadding="0" cellspacing="0" border="0" role="presentation" style="background-color: #F3F0FA;">
                        <tr>
                            <td align="center" style="padding: 32px 12px 40px 12px;">
                                <table width="100%%" cellpadding="0" cellspacing="0" border="0" role="presentation" style="max-width: 600px;">
                                    <tr>
                                        <td align="center" style="padding: 0 0 20px 0; color: #4C1D95; font-size: 15px; font-weight: 700; letter-spacing: .02em;">
                                            <span style="display: inline-block; width: 10px; height: 10px; border-radius: 50%%; background-color: #7C3AED; margin-right: 8px;"></span>Seleta
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="background-color: #FFFFFF; border-radius: 20px; overflow: hidden; box-shadow: 0 8px 32px rgba(76, 29, 149, 0.12);">
                                            <table width="100%%" cellpadding="0" cellspacing="0" border="0" role="presentation">
                                                <tr>
                                                    <td align="center" style="
                                                        background-color: #5B21B6;
                                                        background-image: linear-gradient(135deg, #8B5CF6 0%%, #5B21B6 55%%, #3B0F8C 100%%);
                                                        padding: 44px 32px 40px 32px;
                                                    ">
                                                        <table cellpadding="0" cellspacing="0" border="0" role="presentation" align="center">
                                                            <tr>
                                                                <td style="width: 40px; border-top: 1px solid rgba(255,255,255,.45); font-size: 0; line-height: 0;">&nbsp;</td>
                                                                <td align="center" style="padding: 0 14px;">
                                                                    <div style="width: 48px; height: 48px; line-height: 48px; border-radius: 50%%; background-color: rgba(255,255,255,.18); border: 1px solid rgba(255,255,255,.35); color: #FFFFFF; font-size: 22px; text-align: center;">&#9993;</div>
                                                                </td>
                                                                <td style="width: 40px; border-top: 1px solid rgba(255,255,255,.45); font-size: 0; line-height: 0;">&nbsp;</td>
                                                            </tr>
                                                        </table>
                                                        <p style="margin: 20px 0 8px 0; color: #DDD6FE; font-size: 12px; font-weight: 700; letter-spacing: .16em; text-transform: uppercase;">
                                                            %s
                                                        </p>
                                                        <p style="margin: 0; color: #FFFFFF; font-size: 26px; line-height: 34px; font-weight: 800;">
                                                            %s
                                                        </p>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 40px 40px 16px 40px;">
                                                        %s
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td style="padding: 0 40px 32px 40px;">
                                                        <div style="border-top: 1px solid #EDE9FE; padding-top: 24px;">
                                                            <p style="margin: 0 0 6px 0; color: #6B7280; font-size: 12px; line-height: 18px;">
                                                                Este é um e-mail automático. Por favor, não responda esta mensagem.
                                                            </p>
                                                            <p style="margin: 0; color: #9CA3AF; font-size: 12px; line-height: 18px;">
                                                                Feito com cuidado para tornar sua jornada mais clara e justa.
                                                            </p>
                                                        </div>
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td align="center" style="background-color: #4C1D95; padding: 16px 24px; color: #DDD6FE; font-size: 12px; line-height: 18px;">
                                                        Processos Seletivos &middot; Mais transparentes e humanizados
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(FONTE, escape(eyebrow), escape(tituloBanner), conteudo);
    }

    public static String titulo(String texto) {
        return "<h1 style=\"margin: 0 0 16px 0; color: #1F1235; font-size: 24px; line-height: 32px; font-weight: 800;\">"
                + escape(texto) + "</h1>";
    }

    /** Parágrafo de corpo. Recebe HTML: escape os valores dinâmicos antes de passar. */
    public static String paragrafo(String html) {
        return "<p style=\"margin: 0 0 24px 0; color: #4B5563; font-size: 16px; line-height: 26px;\">"
                + html + "</p>";
    }

    /** Pequeno parágrafo secundário (cor mais suave, fonte menor). */
    public static String observacao(String html) {
        return "<p style=\"margin: 0 0 8px 0; color: #6B7280; font-size: 14px; line-height: 22px;\">" + html + "</p>";
    }

    /** Cartão com faixa lateral colorida, usado para destacar informações. */
    public static String painel(String html) {
        return "<div style=\"background-color: #F5F3FF; border: 1px solid #EDE9FE; border-left: 4px solid #7C3AED;"
                + " border-radius: 12px; padding: 24px 24px 4px 24px; margin-bottom: 24px;\">" + html + "</div>";
    }

    /** Par rótulo/valor dentro de um painel. */
    public static String campo(String rotulo, String valor, int tamanhoValor) {
        return "<p style=\"margin: 0 0 4px 0; color: #7C3AED; font-size: 12px; line-height: 18px; font-weight: 700;"
                + " letter-spacing: .08em; text-transform: uppercase;\">"
                + escape(rotulo) + "</p>"
                + "<p style=\"margin: 0 0 20px 0; color: #1F1235; font-size: " + tamanhoValor
                + "px; line-height: " + (tamanhoValor + 8) + "px; font-weight: 700;\">"
                + escape(valor) + "</p>";
    }

    /**
     * Código de verificação em uma caixa por caractere. Cada caixa é uma célula de tabela
     * (flexbox não é suportado por Outlook/Gmail).
     */
    public static String codigo(String codigo) {
        String valor = codigo == null ? "" : codigo.trim();
        StringBuilder celulas = new StringBuilder();
        for (int i = 0; i < valor.length(); i++) {
            celulas.append("<td align=\"center\" style=\"width: 46px; height: 58px; background-color: #FFFFFF;"
                    + " border: 2px solid #7C3AED; border-radius: 12px; color: #4C1D95; font-size: 28px;"
                    + " line-height: 58px; font-weight: 800; font-family: 'Courier New', monospace;\">")
                    .append(escape(String.valueOf(valor.charAt(i))))
                    .append("</td>");
            if (i < valor.length() - 1) {
                celulas.append("<td style=\"width: 8px; font-size: 0; line-height: 0;\">&nbsp;</td>");
            }
        }
        return "<div style=\"background-color: #F5F3FF; border: 1px solid #EDE9FE; border-radius: 16px;"
                + " padding: 28px 16px; margin-bottom: 24px; text-align: center;\">"
                + "<p style=\"margin: 0 0 16px 0; color: #7C3AED; font-size: 12px; line-height: 18px; font-weight: 700;"
                + " letter-spacing: .12em; text-transform: uppercase;\">Seu código de verificação</p>"
                + "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" role=\"presentation\" align=\"center\"><tr>"
                + celulas + "</tr></table></div>";
    }

    /** Trecho em destaque (negrito colorido) para um valor dinâmico. */
    public static String destaque(String valor) {
        return "<strong style=\"color: #6D28D9;\">" + escape(valor) + "</strong>";
    }

    public static String botao(String url, String texto) {
        return "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" role=\"presentation\" align=\"center\"><tr>"
                + "<td align=\"center\" style=\"border-radius: 12px; background-color: #7C3AED;"
                + " background-image: linear-gradient(135deg, #8B5CF6 0%, #6D28D9 100%);"
                + " box-shadow: 0 6px 16px rgba(109, 40, 217, 0.35);\">"
                + "<a href=\"" + escape(url) + "\" style=\"display: inline-block; color: #FFFFFF;"
                + " text-decoration: none; padding: 15px 32px; font-size: 15px; font-weight: 700;"
                + " letter-spacing: .01em;\">" + escape(texto) + "</a></td></tr></table>";
    }

    public static String escape(String valor) {
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
