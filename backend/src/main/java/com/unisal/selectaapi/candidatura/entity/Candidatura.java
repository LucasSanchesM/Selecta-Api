package com.unisal.selectaapi.candidatura.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.unisal.selectaapi.candidate.entity.CandidateProfile;
import com.unisal.selectaapi.vagas.entity.Vaga;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "applications", uniqueConstraints = @UniqueConstraint(name = "uk_application_vaga_candidato", columnNames = {
        "job_id", "candidate_profile_id" }))
public class Candidatura {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Vaga vaga;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidato;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EtapaProcesso etapaAtual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusCandidatura status;

    @Column(name = "carta_apresentacao", columnDefinition = "TEXT")
    private String cartaApresentacao;

    @Column(name = "curriculo_url", length = 2048)
    private String curriculoUrl;

    /** Preenchido quando a empresa aprova/reprova a candidatura. */
    @Column(name = "motivo_encerramento", columnDefinition = "TEXT")
    private String motivoEncerramento;

    @Column(name = "finalizada_em")
    private Instant finalizadaEm;

    @OneToMany(mappedBy = "candidatura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<CandidaturaHistorico> historico = new ArrayList<>();

    @OneToMany(mappedBy = "candidatura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<CandidaturaFeedback> feedbacks = new ArrayList<>();

    @Column(name = "chat_iniciado_em")
    private LocalDateTime chatIniciadoEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Candidatura() {
    }

    public Candidatura(Vaga vaga, CandidateProfile candidato, String cartaApresentacao, String curriculoUrl) {
        this.vaga = vaga;
        this.candidato = candidato;
        this.cartaApresentacao = cartaApresentacao;
        this.curriculoUrl = curriculoUrl;
        this.etapaAtual = EtapaProcesso.INSCRICAO;
        this.status = StatusCandidatura.EM_ANDAMENTO;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

        if (this.etapaAtual == null) {
            this.etapaAtual = EtapaProcesso.INSCRICAO;
        }
        if (this.status == null) {
            this.status = StatusCandidatura.EM_ANDAMENTO;
        }
    }

    public LocalDateTime getChatIniciadoEm() {
        return chatIniciadoEm;
    }

    public void setChatIniciadoEm(LocalDateTime chatIniciadoEm) {
        this.chatIniciadoEm = chatIniciadoEm;
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

    public Vaga getVaga() {
        return vaga;
    }

    public void setVaga(Vaga vaga) {
        this.vaga = vaga;
    }

    public CandidateProfile getCandidato() {
        return candidato;
    }

    public void setCandidato(CandidateProfile candidato) {
        this.candidato = candidato;
    }

    public EtapaProcesso getEtapaAtual() {
        return etapaAtual;
    }

    public void setEtapaAtual(EtapaProcesso etapaAtual) {
        this.etapaAtual = etapaAtual;
    }

    public StatusCandidatura getStatus() {
        return status;
    }

    public void setStatus(StatusCandidatura status) {
        this.status = status;
    }

    public String getCartaApresentacao() {
        return cartaApresentacao;
    }

    public void setCartaApresentacao(String cartaApresentacao) {
        this.cartaApresentacao = cartaApresentacao;
    }

    public String getCurriculoUrl() {
        return curriculoUrl;
    }

    public void setCurriculoUrl(String curriculoUrl) {
        this.curriculoUrl = curriculoUrl;
    }

    public String getMotivoEncerramento() {
        return motivoEncerramento;
    }

    public void setMotivoEncerramento(String motivoEncerramento) {
        this.motivoEncerramento = motivoEncerramento;
    }

    public Instant getFinalizadaEm() {
        return finalizadaEm;
    }

    public void setFinalizadaEm(Instant finalizadaEm) {
        this.finalizadaEm = finalizadaEm;
    }

    public List<CandidaturaHistorico> getHistorico() {
        return historico;
    }

    public void setHistorico(List<CandidaturaHistorico> historico) {
        this.historico = historico;
    }

    public List<CandidaturaFeedback> getFeedbacks() {
        return feedbacks;
    }

    public void setFeedbacks(List<CandidaturaFeedback> feedbacks) {
        this.feedbacks = feedbacks;
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
