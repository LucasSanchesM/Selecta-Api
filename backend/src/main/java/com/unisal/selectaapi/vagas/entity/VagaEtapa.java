package com.unisal.selectaapi.vagas.entity;

import java.util.UUID;

import com.unisal.selectaapi.candidatura.entity.EtapaProcesso;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "job_stages", uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_stage_type", columnNames = { "job_id", "etapa_id" }),
        @UniqueConstraint(name = "uk_job_stage_order", columnNames = { "job_id", "stage_order" })
})
public class VagaEtapa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Vaga vaga;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa", nullable = false)
    private TipoDeEtapa etapa;

    @Column(name = "stage_order", nullable = false)
    private Integer ordem;

    public VagaEtapa() {
    }

    public VagaEtapa(Vaga vaga, TipoDeEtapa etapa, int ordem) {
        this.vaga = vaga;
        this.etapa = etapa;
        this.ordem = ordem;
    }

    public UUID getId() {
        return id;
    }

    public Vaga getVaga() {
        return vaga;
    }

    public TipoDeEtapa getEtapa() {
        return etapa;
    }

    public Integer getOrdem() {
        return ordem;
    }
}