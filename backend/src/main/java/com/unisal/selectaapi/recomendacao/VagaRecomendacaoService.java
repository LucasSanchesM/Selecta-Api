package com.unisal.selectaapi.recomendacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import com.unisal.selectaapi.recomendacao.config.EmbeddingProperties;

@Service
public class VagaRecomendacaoService {

    private static final double BOOST_MESMA_CIDADE = 0.03;
    private static final double BOOST_MESMO_ESTADO = 0.015;

    private final NamedParameterJdbcTemplate jdbc;
    private final EmbeddingProperties properties;
    private final EmbeddingSchemaInitializer schema;

    public VagaRecomendacaoService(NamedParameterJdbcTemplate jdbc,
            EmbeddingProperties properties,
            EmbeddingSchemaInitializer schema) {
        this.jdbc = jdbc;
        this.properties = properties;
        this.schema = schema;
    }

    public boolean isAtiva() {
        return properties.isEnabled() && schema.isAvailable();
    }

    /**
     * @return ordered job ids for the requested page, or empty when the user has
     *         no candidate embedding (company users, anonymous, profile not indexed yet).
     */
    public Optional<List<UUID>> rankearVagas(UUID userId, FiltroVagas filtro, int page, int size) {
        if (!isAtiva() || userId == null) {
            return Optional.empty();
        }

        Optional<PerfilVetorial> perfil = buscarPerfil(userId);
        if (perfil.isEmpty()) {
            return Optional.empty();
        }

        var params = new MapSqlParameterSource()
                .addValue("vetor", perfil.get().vetor())
                .addValue("model", properties.getModel())
                .addValue("limit", size)
                .addValue("offset", (long) page * size);

        String boost = montarBoostLocalizacao(perfil.get(), params);
        String where = montarFiltros(filtro, params);

        String sql = """
                SELECT j.id
                FROM jobs j
                LEFT JOIN job_embeddings e ON e.job_id = j.id AND e.model = :model
                WHERE %s
                ORDER BY
                    CASE WHEN e.embedding IS NULL THEN NULL
                         ELSE (1 - (e.embedding <=> CAST(:vetor AS vector))) + %s
                    END DESC NULLS LAST,
                    j.created_at DESC,
                    j.id
                LIMIT :limit OFFSET :offset
                """.formatted(where, boost);

        return Optional.of(jdbc.queryForList(sql, params, UUID.class));
    }

    private Optional<PerfilVetorial> buscarPerfil(UUID userId) {
        var params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("model", properties.getModel());

        List<PerfilVetorial> result = jdbc.query("""
                SELECT ce.embedding::text AS vetor, p.city, p.state
                FROM candidate_embeddings ce
                JOIN candidate_profiles p ON p.id = ce.candidate_profile_id
                WHERE p.user_id = :userId AND ce.model = :model
                """, params, (rs, rowNum) -> new PerfilVetorial(
                rs.getString("vetor"), rs.getString("city"), rs.getString("state")));

        return result.stream().findFirst();
    }

    private static String montarBoostLocalizacao(PerfilVetorial perfil, MapSqlParameterSource params) {
        List<String> casos = new ArrayList<>();
        if (hasText(perfil.cidade())) {
            params.addValue("perfilCidade", perfil.cidade().trim().toLowerCase());
            casos.add("WHEN j.modalidade <> 'REMOTO' AND lower(j.cidade) = :perfilCidade THEN " + BOOST_MESMA_CIDADE);
        }
        if (hasText(perfil.estado())) {
            params.addValue("perfilEstado", perfil.estado().trim().toLowerCase());
            casos.add("WHEN j.modalidade <> 'REMOTO' AND lower(j.estado) = :perfilEstado THEN " + BOOST_MESMO_ESTADO);
        }
        if (casos.isEmpty()) {
            return "0";
        }
        return "(CASE " + String.join(" ", casos) + " ELSE 0 END)";
    }

    private static String montarFiltros(FiltroVagas filtro, MapSqlParameterSource params) {
        List<String> condicoes = new ArrayList<>();
        condicoes.add("1 = 1");

        if (hasText(filtro.titulo())) {
            condicoes.add("lower(j.titulo) LIKE :titulo");
            params.addValue("titulo", "%" + filtro.titulo().toLowerCase() + "%");
        }
        if (filtro.categoria() != null) {
            condicoes.add("j.categoria = :categoria");
            params.addValue("categoria", filtro.categoria().name());
        }
        if (filtro.modalidade() != null) {
            condicoes.add("j.modalidade = :modalidade");
            params.addValue("modalidade", filtro.modalidade().name());
        }
        if (filtro.salarioMin() != null) {
            condicoes.add("j.salario >= :salarioMin");
            params.addValue("salarioMin", filtro.salarioMin());
        }
        if (filtro.salarioMax() != null) {
            condicoes.add("j.salario <= :salarioMax");
            params.addValue("salarioMax", filtro.salarioMax());
        }
        if (hasText(filtro.cidade())) {
            condicoes.add("lower(j.cidade) LIKE :cidade");
            params.addValue("cidade", "%" + filtro.cidade().toLowerCase() + "%");
        }
        if (hasText(filtro.estado())) {
            condicoes.add("lower(j.estado) = :estado");
            params.addValue("estado", filtro.estado().toLowerCase());
        }
        if (filtro.ativa() != null) {
            condicoes.add("j.ativa = :ativa");
            params.addValue("ativa", filtro.ativa());
        }
        if (filtro.companyProfileId() != null) {
            condicoes.add("j.company_profile_id = :companyProfileId");
            params.addValue("companyProfileId", filtro.companyProfileId());
        }

        return String.join(" AND ", condicoes);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record PerfilVetorial(String vetor, String cidade, String estado) {
    }
}
