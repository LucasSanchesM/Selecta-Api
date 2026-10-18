package com.unisal.selectaapi.candidatura.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_feedbacks")
public class CandidaturaFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Candidatura candidatura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EtapaProcesso etapa;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    /** Nota opcional de 1 a 5 dada pela empresa naquela etapa. */
    @Column
    private Integer nota;

    @Column(name = "autor_user_id", nullable = false)
    private UUID autorUserId;

    @Column(name = "autor_nome", nullable = false)
    private String autorNome;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CandidaturaFeedback() {
    }

    public CandidaturaFeedback(Candidatura candidatura,
            EtapaProcesso etapa,
            String titulo,
            String mensagem,
            Integer nota,
            UUID autorUserId,
            String autorNome) {
        this.candidatura = candidatura;
        this.etapa = etapa;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.nota = nota;
        this.autorUserId = autorUserId;
        this.autorNome = autorNome;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Candidatura getCandidatura() {
        return candidatura;
    }

    public void setCandidatura(Candidatura candidatura) {
        this.candidatura = candidatura;
    }

    public EtapaProcesso getEtapa() {
        return etapa;
    }

    public void setEtapa(EtapaProcesso etapa) {
        this.etapa = etapa;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public Integer getNota() {
        return nota;
    }

    public void setNota(Integer nota) {
        this.nota = nota;
    }

    public UUID getAutorUserId() {
        return autorUserId;
    }

    public void setAutorUserId(UUID autorUserId) {
        this.autorUserId = autorUserId;
    }

    public String getAutorNome() {
        return autorNome;
    }

    public void setAutorNome(String autorNome) {
        this.autorNome = autorNome;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
