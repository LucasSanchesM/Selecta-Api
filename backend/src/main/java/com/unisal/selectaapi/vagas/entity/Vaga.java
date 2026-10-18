package com.unisal.selectaapi.vagas.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.unisal.selectaapi.company.entity.CompanyProfile;
import com.unisal.selectaapi.vagas.VagaRole;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
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
import jakarta.persistence.ElementCollection;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "jobs")
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal salario;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String beneficios;

    // Relacionamento real com a empresa
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_profile_id", nullable = false)
    private CompanyProfile empresa;

    // Localização da vaga
    @Column
    private String cidade;

    @Column(length = 2)
    private String estado;

    // Categoria e perfil da vaga
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VagaRole categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModalidadeVaga modalidade;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_meta_tags", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "tag", nullable = false, length = 80)
    private List<String> metaTags = new ArrayList<>();

    @Column(nullable = false)
    private Boolean ativa;

    @OneToMany(mappedBy = "vaga", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<VagaEtapa> etapas = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    public Vaga() {
    }

    public List<String> getMetaTags() {
        return metaTags;
    }

    public void setMetaTags(List<String> metaTags) {
        this.metaTags = metaTags == null ? new ArrayList<>() : new ArrayList<>(metaTags);
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();

        if (this.ativa == null) {
            this.ativa = true;
        }
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

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public BigDecimal getSalario() {
        return salario;
    }

    public void setSalario(BigDecimal salario) {
        this.salario = salario;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getBeneficios() {
        return beneficios;
    }

    public void setBeneficios(String beneficios) {
        this.beneficios = beneficios;
    }

    public CompanyProfile getEmpresa() {
        return empresa;
    }

    public void setEmpresa(CompanyProfile empresa) {
        this.empresa = empresa;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public VagaRole getCategoria() {
        return categoria;
    }

    public void setCategoria(VagaRole categoria) {
        this.categoria = categoria;
    }

    public ModalidadeVaga getModalidade() {
        return modalidade;
    }

    public void setModalidade(ModalidadeVaga modalidade) {
        this.modalidade = modalidade;
    }

    public Boolean getAtiva() {
        return ativa;
    }

    public void setAtiva(Boolean ativa) {
        this.ativa = ativa;
    }

    public List<VagaEtapa> getEtapas() {
        return etapas;
    }

    public void setEtapas(List<VagaEtapa> etapas) {
        this.etapas.clear();
        this.etapas.addAll(etapas);
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
