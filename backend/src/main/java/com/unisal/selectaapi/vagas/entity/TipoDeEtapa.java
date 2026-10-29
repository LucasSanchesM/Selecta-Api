    package com.unisal.selectaapi.vagas.entity;

    import com.unisal.selectaapi.company.entity.CompanyProfile;
    import com.unisal.selectaapi.vagas.PapelEtapa;
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

        @Column(name = "codigo")
        private String codigo;

        @Column(name = "ja_utilizada", nullable = false)
        private boolean jaUtilizada = false;

        @Column(name =  "disponibilidade", nullable = false)
        private boolean disponibilidade = true;

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
        public boolean isJaUtilizada() {
            return jaUtilizada;
        }

        public void setCodigo(String codigo) {
            if (!isSystem() && codigo != null) {
                throw new IllegalStateException(
                        "Etapas personalizadas não podem possuir código"
                );
            }
            this.codigo = codigo;
        }

        public String getCodigo() {
            return codigo;
        }

        public void marcarComoUtilizada() {
            this.jaUtilizada = true;
        }

        public boolean isDisponibilidade() {
            return disponibilidade;
        }

        public void setDisponibilidade(boolean disponibilidade) {
            this.disponibilidade = disponibilidade;
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
