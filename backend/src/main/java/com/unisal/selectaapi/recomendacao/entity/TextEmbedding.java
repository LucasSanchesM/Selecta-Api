package com.unisal.selectaapi.recomendacao.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Vetor de embedding de uma vaga ou de um perfil de candidato.
 * O {@code sourceId} é o id da vaga ou do perfil (UUIDs não colidem entre tabelas).
 * O {@code contentHash} permite reaproveitar o vetor enquanto o texto não muda.
 */
@Entity
@Table(name = "text_embeddings")
public class TextEmbedding {

    @Id
    @Column(name = "source_id")
    private UUID sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEmbedding tipo;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false, length = 120)
    private String modelo;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "vetor", columnDefinition = "real[]")
    private float[] vetor;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TextEmbedding() {
    }

    public TextEmbedding(UUID sourceId, TipoEmbedding tipo) {
        this.sourceId = sourceId;
        this.tipo = tipo;
    }

    public void atualizar(String contentHash, String modelo, float[] vetor) {
        this.contentHash = contentHash;
        this.modelo = modelo;
        this.vetor = vetor;
    }

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public TipoEmbedding getTipo() {
        return tipo;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getModelo() {
        return modelo;
    }

    public float[] getVetor() {
        return vetor;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
