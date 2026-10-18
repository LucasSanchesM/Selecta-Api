package com.unisal.selectaapi.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.unisal.selectaapi.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Código de uso único para confirmar ações sensíveis (o valor fica salvo só como hash). */
@Entity
@Table(name = "security_codes", indexes = @Index(name = "idx_security_codes_user_purpose", columnList = "user_id, purpose"))
public class SecurityCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SecurityCodePurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(length = 320)
    private String payload;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    protected SecurityCode() {
    }

    public SecurityCode(User user, SecurityCodePurpose purpose, String codeHash, String payload,
            LocalDateTime now, LocalDateTime expiresAt) {
        this.user = user;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.payload = payload;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public void registerFailedAttempt() {
        this.attempts++;
    }

    public void consume(LocalDateTime now) {
        this.consumedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public SecurityCodePurpose getPurpose() {
        return purpose;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public String getPayload() {
        return payload;
    }

    public int getAttempts() {
        return attempts;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }
}
