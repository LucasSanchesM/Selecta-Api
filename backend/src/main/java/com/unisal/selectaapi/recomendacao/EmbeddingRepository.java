package com.unisal.selectaapi.recomendacao;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    public EmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<String> findContentHash(EmbeddingTarget target, UUID id) {
        var table = Table.of(target);
        List<String> hashes = jdbcTemplate.queryForList(
                "SELECT content_hash FROM " + table.name + " WHERE " + table.idColumn + " = ?",
                String.class, id);
        return hashes.stream().findFirst().map(String::trim);
    }

    public void upsert(EmbeddingTarget target, UUID id, float[] embedding, String model,
            String contentHash, Instant sourceUpdatedAt) {
        var table = Table.of(target);
        jdbcTemplate.update("""
                INSERT INTO %1$s (%2$s, embedding, model, content_hash, source_updated_at, updated_at)
                VALUES (?, CAST(? AS vector), ?, ?, ?, now())
                ON CONFLICT (%2$s) DO UPDATE SET
                    embedding = EXCLUDED.embedding,
                    model = EXCLUDED.model,
                    content_hash = EXCLUDED.content_hash,
                    source_updated_at = EXCLUDED.source_updated_at,
                    updated_at = now()
                """.formatted(table.name, table.idColumn),
                id, toVectorLiteral(embedding), model, contentHash, toTimestamp(sourceUpdatedAt));
    }

    public void touch(EmbeddingTarget target, UUID id, Instant sourceUpdatedAt) {
        var table = Table.of(target);
        jdbcTemplate.update(
                "UPDATE " + table.name + " SET source_updated_at = ?, updated_at = now() WHERE " + table.idColumn + " = ?",
                toTimestamp(sourceUpdatedAt), id);
    }

    public void delete(EmbeddingTarget target, UUID id) {
        var table = Table.of(target);
        jdbcTemplate.update("DELETE FROM " + table.name + " WHERE " + table.idColumn + " = ?", id);
    }

    public List<UUID> findStaleVagaIds(String model, int limit) {
        return jdbcTemplate.queryForList("""
                SELECT j.id
                FROM jobs j
                LEFT JOIN job_embeddings e ON e.job_id = j.id
                WHERE j.ativa = true
                  AND (e.job_id IS NULL
                       OR e.model <> ?
                       OR e.source_updated_at IS NULL
                       OR e.source_updated_at < j.updated_at)
                ORDER BY j.updated_at DESC
                LIMIT ?
                """, UUID.class, model, limit);
    }

    public List<UUID> findStaleCandidatoIds(String model, int limit) {
        return jdbcTemplate.queryForList("""
                SELECT p.id
                FROM candidate_profiles p
                LEFT JOIN candidate_embeddings e ON e.candidate_profile_id = p.id
                WHERE (e.candidate_profile_id IS NULL
                       OR e.model <> ?
                       OR e.source_updated_at IS NULL
                       OR e.source_updated_at < p.updated_at)
                  AND (NULLIF(TRIM(p.headline), '') IS NOT NULL
                       OR NULLIF(TRIM(p.summary), '') IS NOT NULL
                       OR EXISTS (SELECT 1 FROM candidate_skills s WHERE s.candidate_profile_id = p.id)
                       OR EXISTS (SELECT 1 FROM candidate_experiences x WHERE x.candidate_profile_id = p.id)
                       OR EXISTS (SELECT 1 FROM candidate_projects pr WHERE pr.candidate_profile_id = p.id))
                ORDER BY p.updated_at DESC
                LIMIT ?
                """, UUID.class, model, limit);
    }

    static String toVectorLiteral(float[] embedding) {
        var builder = new StringBuilder(embedding.length * 10).append('[');
        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(embedding[index]);
        }
        return builder.append(']').toString();
    }

    private static Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private enum Table {
        CANDIDATO("candidate_embeddings", "candidate_profile_id"),
        VAGA("job_embeddings", "job_id");

        private final String name;
        private final String idColumn;

        Table(String name, String idColumn) {
            this.name = name;
            this.idColumn = idColumn;
        }

        static Table of(EmbeddingTarget target) {
            return target == EmbeddingTarget.CANDIDATO ? CANDIDATO : VAGA;
        }
    }
}
