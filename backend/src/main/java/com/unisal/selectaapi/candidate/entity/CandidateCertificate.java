package com.unisal.selectaapi.candidate.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "candidate_certificates")
public class CandidateCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String issuer;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "credential_url", length = 2048)
    private String credentialUrl;

    @Embedded
    private AttachedFile attachment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CandidateCertificate() {
    }

    public CandidateCertificate(CandidateProfile candidateProfile, String name, String issuer,
            LocalDate issueDate, String credentialUrl) {
        this.candidateProfile = candidateProfile;
        this.name = name;
        this.issuer = issuer;
        this.issueDate = issueDate;
        this.credentialUrl = credentialUrl;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public String getCredentialUrl() {
        return credentialUrl;
    }

    public void setCredentialUrl(String credentialUrl) {
        this.credentialUrl = credentialUrl;
    }

    /** Arquivo anexado, ou {@code null} quando não há anexo. */
    public AttachedFile getAttachment() {
        return attachment != null && attachment.getKey() != null ? attachment : null;
    }

    public void setAttachment(AttachedFile attachment) {
        this.attachment = attachment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
