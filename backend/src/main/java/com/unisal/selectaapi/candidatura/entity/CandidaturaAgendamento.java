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
@Table(name = "application_appointments")
public class CandidaturaAgendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Candidatura candidatura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EtapaProcesso etapa;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String mensagem;

    @Column(nullable = false)
    private Instant inicio;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    @Column(nullable = false, length = 2048)
    private String link;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAgendamento status;

    @Column(name = "criado_por_user_id", nullable = false)
    private UUID criadoPorUserId;

    @Column(name = "criado_por_nome", nullable = false)
    private String criadoPorNome;

    @Column(name = "motivo_cancelamento", columnDefinition = "TEXT")
    private String motivoCancelamento;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CandidaturaAgendamento() {
    }

    public CandidaturaAgendamento(Candidatura candidatura, EtapaProcesso etapa, String titulo,
            String mensagem, Instant inicio, Integer duracaoMinutos, String link,
            UUID criadoPorUserId, String criadoPorNome) {
        this.candidatura = candidatura;
        this.etapa = etapa;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.inicio = inicio;
        this.duracaoMinutos = duracaoMinutos;
        this.link = link;
        this.status = StatusAgendamento.PENDENTE;
        this.criadoPorUserId = criadoPorUserId;
        this.criadoPorNome = criadoPorNome;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.status == null) {
            this.status = StatusAgendamento.PENDENTE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Candidatura getCandidatura() { return candidatura; }
    public EtapaProcesso getEtapa() { return etapa; }
    public String getTitulo() { return titulo; }
    public String getMensagem() { return mensagem; }
    public Instant getInicio() { return inicio; }
    public Integer getDuracaoMinutos() { return duracaoMinutos; }
    public String getLink() { return link; }
    public StatusAgendamento getStatus() { return status; }
    public UUID getCriadoPorUserId() { return criadoPorUserId; }
    public String getCriadoPorNome() { return criadoPorNome; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setEtapa(EtapaProcesso etapa) { this.etapa = etapa; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public void setInicio(Instant inicio) { this.inicio = inicio; }
    public void setDuracaoMinutos(Integer duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public void setLink(String link) { this.link = link; }
    public void setStatus(StatusAgendamento status) { this.status = status; }
    public void setMotivoCancelamento(String motivoCancelamento) { this.motivoCancelamento = motivoCancelamento; }
}