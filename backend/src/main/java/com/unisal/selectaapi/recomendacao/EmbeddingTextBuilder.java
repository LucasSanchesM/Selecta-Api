package com.unisal.selectaapi.recomendacao;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.unisal.selectaapi.candidate.entity.CandidateExperience;
import com.unisal.selectaapi.candidate.entity.CandidateProfile;
import com.unisal.selectaapi.candidate.entity.CandidateProject;
import com.unisal.selectaapi.vagas.entity.Vaga;


@Component
public class EmbeddingTextBuilder {

    private static final int MAX_TOTAL_CHARS = 24_000;
    private static final int MAX_FIELD_CHARS = 2_500;
    private static final int MAX_ITEMS = 15;

    public String buildCandidato(CandidateProfile profile) {
        var text = new StringBuilder();

        appendLine(text, "Cargo/objetivo profissional", profile.getHeadline());
        appendLine(text, "Resumo profissional", profile.getSummary());

        String formacao = joinNonBlank(" - ",
                profile.getCourse(),
                profile.getInstitution(),
                profile.getCurrentSemester() != null ? profile.getCurrentSemester() + "º semestre" : null,
                profile.getExpectedGraduationYear() != null ? "conclusão prevista em " + profile.getExpectedGraduationYear() : null);
        appendLine(text, "Formação", formacao);

        appendLine(text, "Habilidades", joinList(profile.getSkills()));

        List<CandidateExperience> experiencias = profile.getExperiences().stream()
                .sorted(Comparator.comparing(
                        (CandidateExperience exp) -> Boolean.TRUE.equals(exp.getIsCurrent()) ? Instant.MAX : nullSafe(exp.getEndDate()))
                        .reversed())
                .limit(MAX_ITEMS)
                .toList();
        if (!experiencias.isEmpty()) {
            text.append("Experiências profissionais:\n");
            for (CandidateExperience exp : experiencias) {
                text.append("- ")
                        .append(joinNonBlank(" na ", exp.getRole(), exp.getCompanyName()));
                String periodo = periodo(exp.getStartDate(), exp.getEndDate(), exp.getIsCurrent());
                if (!periodo.isBlank()) {
                    text.append(" (").append(periodo).append(')');
                }
                if (hasText(exp.getDescription())) {
                    text.append(": ").append(truncate(clean(exp.getDescription()), MAX_FIELD_CHARS));
                }
                text.append('\n');
            }
        }

        List<CandidateProject> projetos = profile.getProjects().stream().limit(MAX_ITEMS).toList();
        if (!projetos.isEmpty()) {
            text.append("Projetos:\n");
            for (CandidateProject projeto : projetos) {
                text.append("- ").append(clean(projeto.getTitle()));
                if (hasText(projeto.getDescription())) {
                    text.append(": ").append(truncate(clean(projeto.getDescription()), MAX_FIELD_CHARS));
                }
                String ferramentas = joinList(projeto.getToolsAndSkills());
                if (!ferramentas.isBlank()) {
                    text.append(" Tecnologias: ").append(ferramentas).append('.');
                }
                text.append('\n');
            }
        }

        appendLine(text, "Localização", joinNonBlank(" - ", profile.getCity(), profile.getState()));

        return truncate(text.toString().trim(), MAX_TOTAL_CHARS);
    }

    public String buildVaga(Vaga vaga) {
        var text = new StringBuilder();

        appendLine(text, "Vaga", vaga.getTitulo());
        if (vaga.getCategoria() != null) {
            appendLine(text, "Área", humanize(vaga.getCategoria().name()));
        }
        if (vaga.getModalidade() != null) {
            appendLine(text, "Modalidade", humanize(vaga.getModalidade().name()));
        }
        appendLine(text, "Descrição e requisitos", truncate(clean(vaga.getDescricao()), MAX_TOTAL_CHARS / 2));
        appendLine(text, "Palavras-chave", joinList(vaga.getMetaTags()));

        var empresa = vaga.getEmpresa();
        if (empresa != null) {
            appendLine(text, "Setor da empresa", empresa.getIndustry());
            appendLine(text, "Sobre a empresa", truncate(clean(empresa.getDescription()), MAX_FIELD_CHARS));
        }

        appendLine(text, "Localização", joinNonBlank(" - ", vaga.getCidade(), vaga.getEstado()));

        return truncate(text.toString().trim(), MAX_TOTAL_CHARS);
    }

    private static Instant nullSafe(Instant instant) {
        return instant == null ? Instant.EPOCH : instant;
    }

    private static String periodo(Instant inicio, Instant fim, Boolean atual) {
        if (inicio == null) {
            return Boolean.TRUE.equals(atual) ? "atual" : "";
        }
        YearMonth start = YearMonth.from(inicio.atZone(ZoneOffset.UTC));
        boolean isAtual = Boolean.TRUE.equals(atual) || fim == null;
        YearMonth end = isAtual ? YearMonth.now(ZoneOffset.UTC) : YearMonth.from(fim.atZone(ZoneOffset.UTC));

        long meses = Math.max(ChronoUnit.MONTHS.between(start, end), 0);
        String duracao = meses >= 12
                ? (meses / 12) + (meses / 12 == 1 ? " ano" : " anos") + (meses % 12 > 0 ? " e " + (meses % 12) + " meses" : "")
                : meses + (meses == 1 ? " mês" : " meses");

        return start + " a " + (isAtual ? "atual" : end.toString()) + ", " + duracao;
    }

    private static void appendLine(StringBuilder text, String label, String value) {
        if (hasText(value)) {
            text.append(label).append(": ").append(clean(value)).append('\n');
        }
    }

    private static String joinList(Collection<String> values) {
        if (values == null) {
            return "";
        }
        return values.stream()
                .filter(EmbeddingTextBuilder::hasText)
                .map(EmbeddingTextBuilder::clean)
                .distinct()
                .limit(60)
                .collect(Collectors.joining(", "));
    }

    private static String joinNonBlank(String separator, String... values) {
        return java.util.Arrays.stream(values)
                .filter(Objects::nonNull)
                .filter(EmbeddingTextBuilder::hasText)
                .map(EmbeddingTextBuilder::clean)
                .collect(Collectors.joining(separator));
    }

    private static String humanize(String enumName) {
        return enumName.replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
