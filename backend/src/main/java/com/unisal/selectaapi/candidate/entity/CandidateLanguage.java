package com.unisal.selectaapi.candidate.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

@Entity
@Table(name = "candidate_languages")
public class CandidateLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(name = "language_name", nullable = false, length = 100)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LanguageProficiency proficiency;

    /** Certificado de conclusão/proficiência do idioma (opcional). */
    @Embedded
    private AttachedFile certificate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CandidateLanguage() {
    }

    public CandidateLanguage(CandidateProfile candidateProfile, String language, LanguageProficiency proficiency) {
        this.candidateProfile = candidateProfile;
        this.language = language;
        this.proficiency = proficiency;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public CandidateProfile getCandidateProfile() {
        return candidateProfile;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public LanguageProficiency getProficiency() {
        return proficiency;
    }

    public void setProficiency(LanguageProficiency proficiency) {
        this.proficiency = proficiency;
    }

    /** Arquivo do certificado, ou {@code null} quando não há. */
    public AttachedFile getCertificate() {
        return certificate != null && certificate.getKey() != null ? certificate : null;
    }

    public void setCertificate(AttachedFile certificate) {
        this.certificate = certificate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
