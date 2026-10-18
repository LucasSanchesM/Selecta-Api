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
import jakarta.persistence.Table;

/**
 * Timeline imutável da candidatura. Cada mudança de etapa ou status gera uma
 * linha aqui, e é isso que o candidato enxerga como "acompanhamento".
 */
@Entity
@Table(name = "application_status_history")
public class CandidaturaHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Candidatura candidatura;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_anterior", length = 40)
    private EtapaProcesso etapaAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_nova", nullable = false, length = 40)
    private EtapaProcesso etapaNova;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", length = 30)
    private StatusCandidatura statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 30)
    private StatusCandidatura statusNovo;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    /** Quem provocou a mudança (empresa ou o próprio candidato ao cancelar). */
    @Column(name = "autor_user_id")
    private UUID autorUserId;

    @Column(name = "autor_nome")
    private String autorNome;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CandidaturaHistorico() {
    }

    public CandidaturaHistorico(Candidatura candidatura,
            EtapaProcesso etapaAnterior,
            EtapaProcesso etapaNova,
            StatusCandidatura statusAnterior,
            StatusCandidatura statusNovo,
            String observacao,
            UUID autorUserId,
            String autorNome) {
        this.candidatura = candidatura;
        this.etapaAnterior = etapaAnterior;
        this.etapaNova = etapaNova;
        this.statusAnterior = statusAnterior;
        this.statusNovo = statusNovo;
        this.observacao = observacao;
        this.autorUserId = autorUserId;
        this.autorNome = autorNome;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
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

    public EtapaProcesso getEtapaAnterior() {
        return etapaAnterior;
    }

    public void setEtapaAnterior(EtapaProcesso etapaAnterior) {
        this.etapaAnterior = etapaAnterior;
    }

    public EtapaProcesso getEtapaNova() {
        return etapaNova;
    }

    public void setEtapaNova(EtapaProcesso etapaNova) {
        this.etapaNova = etapaNova;
    }

    public StatusCandidatura getStatusAnterior() {
        return statusAnterior;
    }

    public void setStatusAnterior(StatusCandidatura statusAnterior) {
        this.statusAnterior = statusAnterior;
    }

    public StatusCandidatura getStatusNovo() {
        return statusNovo;
    }

    public void setStatusNovo(StatusCandidatura statusNovo) {
        this.statusNovo = statusNovo;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
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
}
