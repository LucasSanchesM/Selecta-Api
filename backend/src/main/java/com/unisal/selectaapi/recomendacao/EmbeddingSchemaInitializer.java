package com.unisal.selectaapi.recomendacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.unisal.selectaapi.recomendacao.config.EmbeddingProperties;

import jakarta.annotation.PostConstruct;

@Component
@DependsOn("entityManagerFactory")
public class EmbeddingSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private volatile boolean available;

    public EmbeddingSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    void initialize() {
        int dims = EmbeddingProperties.DIMENSIONS;
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS job_embeddings (
                        job_id UUID PRIMARY KEY REFERENCES jobs(id) ON DELETE CASCADE,
                        embedding vector(%d) NOT NULL,
                        model VARCHAR(100) NOT NULL,
                        content_hash CHAR(64) NOT NULL,
                        source_updated_at TIMESTAMPTZ,
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
                    )
                    """.formatted(dims));

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS candidate_embeddings (
                        candidate_profile_id UUID PRIMARY KEY REFERENCES candidate_profiles(id) ON DELETE CASCADE,
                        embedding vector(%d) NOT NULL,
                        model VARCHAR(100) NOT NULL,
                        content_hash CHAR(64) NOT NULL,
                        source_updated_at TIMESTAMPTZ,
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
                    )
                    """.formatted(dims));

            available = true;
        } catch (DataAccessException exception) {
            available = false;
            log.error("Recomendação por embeddings desativada: não foi possível preparar o pgvector. "
                    + "Use uma imagem Postgres com pgvector (ex.: pgvector/pgvector:pg16). Causa: {}",
                    exception.getMostSpecificCause().getMessage());
            return;
        }

        try {
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS job_embeddings_hnsw_idx
                    ON job_embeddings USING hnsw (embedding vector_cosine_ops)
                    """);
        } catch (DataAccessException exception) {
            log.warn("Índice HNSW não criado (pgvector < 0.5?). A busca continua funcionando sem ele: {}",
                    exception.getMostSpecificCause().getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }
}
