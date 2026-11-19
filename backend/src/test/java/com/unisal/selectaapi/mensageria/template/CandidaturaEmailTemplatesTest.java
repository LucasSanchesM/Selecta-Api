package com.unisal.selectaapi.mensageria.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.unisal.selectaapi.mensageria.EmailUpdateType;
import com.unisal.selectaapi.mensageria.dto.CandidaturaEmailRequest;

class CandidaturaEmailTemplatesTest {

    @Test
    void escolheTemplatePeloTipo() {
        assertThat(CandidaturaEmailTemplates.para(request(EmailUpdateType.STAGE, null)))
                .isInstanceOf(EtapaEmailTemplate.class);
        assertThat(CandidaturaEmailTemplates.para(request(EmailUpdateType.DECISION, null)))
                .isInstanceOf(DecisaoEmailTemplate.class);
        assertThat(CandidaturaEmailTemplates.para(request(EmailUpdateType.FEEDBACK, null)))
                .isInstanceOf(FeedbackEmailTemplate.class);
        assertThat(CandidaturaEmailTemplates.para(request(EmailUpdateType.APPOINTMENT, null)))
                .isInstanceOf(ConviteEmailTemplate.class);
        assertThat(CandidaturaEmailTemplates.para(request(null, null)))
                .isInstanceOf(GenericoEmailTemplate.class);
    }

    @Test
    void assuntoDoConviteUsaTituloDoAgendamento() {
        var msg = CandidaturaEmailTemplates.para(
                request(EmailUpdateType.APPOINTMENT, "Entrevista técnica")).render();

        assertThat(msg.subject()).isEqualTo("Convite para reunião - Entrevista técnica");
        assertThat(msg.html()).contains("https://meet.example.com/abc");
    }

    @Test
    void assuntoDeDecisaoIncluiVaga() {
        var msg = CandidaturaEmailTemplates.para(request(EmailUpdateType.DECISION, null)).render();

        assertThat(msg.subject()).isEqualTo("Decisão sobre sua candidatura - Dev Java");
    }

    @Test
    void escapaConteudoDinamicoNoHtml() {
        var base = request(EmailUpdateType.FEEDBACK, null);
        var comHtml = new CandidaturaEmailRequest(base.email(), base.candidatoNome(), base.vagaTitulo(),
                base.empresaNome(), base.etapaDescricao(), base.statusDescricao(),
                "<script>alert('x')</script>", null, null, null, null, base.userId(), base.type());

        var html = CandidaturaEmailTemplates.para(comHtml).render().html();

        assertThat(html).doesNotContain("<script>");
        assertThat(html).contains("&lt;script&gt;");
    }

    private CandidaturaEmailRequest request(EmailUpdateType type, String agendamentoTitulo) {
        boolean convite = agendamentoTitulo != null;
        return new CandidaturaEmailRequest(
                "pessoa@example.com",
                "Maria",
                "Dev Java",
                "Acme",
                "Entrevista",
                "Em andamento",
                null,
                agendamentoTitulo,
                convite ? Instant.parse("2026-12-01T13:00:00Z") : null,
                convite ? 45 : null,
                convite ? "https://meet.example.com/abc" : null,
                UUID.randomUUID(),
                type);
    }
}
