package com.unisal.selectaapi.vagas.entity;

import com.unisal.selectaapi.company.entity.CompanyProfile;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tipos_de_etapas")
public class TipoDeEtapa {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_profile_id")
    private CompanyProfile companyProfile;

    @Column(name = "created_at", nullable = false)
    private Instant created_at;

    public TipoDeEtapa(CompanyProfile companyProfile, String nome) {
        this.companyProfile = companyProfile;
        this.nome = nome;
    }

    public TipoDeEtapa() {
    }

    @PrePersist
    void onCreate(){
        this.created_at = Instant.now();
    }

    public boolean isSystem(){
        return companyProfile == null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public CompanyProfile getCompanyProfile() {
        return companyProfile;
    }

    public void setCompanyProfile(CompanyProfile companyProfile) {
        this.companyProfile = companyProfile;
    }

    public Instant getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Instant created_at) {
        this.created_at = created_at;
    }
}
