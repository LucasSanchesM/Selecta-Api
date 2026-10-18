package com.unisal.selectaapi.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.unisal.selectaapi.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tentativa de login aguardando a segunda etapa. Criada quando a senha confere,
 * mas o dispositivo ainda não é confiável para uma conta com 2FA ativa.
 */
@Entity
@Table(name = "login_challenges", indexes = @Index(name = "idx_login_challenges_user", columnList = "user_id"))
public class LoginChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_id_hash", length = 64)
    private String deviceIdHash;

    @Column(name = "device_name", nullable = false, length = 120)
    private String deviceName;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(length = 120)
    private String location;

    @Column(name = "email_code_hash", length = 100)
    private String emailCodeHash;

    @Column(name = "email_code_sent_at")
    private LocalDateTime emailCodeSentAt;

    @Column(name = "email_code_expires_at")
    private LocalDateTime emailCodeExpiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "denied_at")
    private LocalDateTime deniedAt;

    protected LoginChallenge() {
    }

    public LoginChallenge(User user, String deviceIdHash, String deviceName, String userAgent, String ipAddress,
            String location, LocalDateTime now, LocalDateTime expiresAt) {
        this.user = user;
        this.deviceIdHash = deviceIdHash;
        this.deviceName = deviceName;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.location = location;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    public boolean isPending(LocalDateTime now) {
        return completedAt == null && deniedAt == null && expiresAt.isAfter(now);
    }

    public void attachEmailCode(String codeHash, LocalDateTime now, LocalDateTime codeExpiresAt) {
        this.emailCodeHash = codeHash;
        this.emailCodeSentAt = now;
        this.emailCodeExpiresAt = codeExpiresAt;
    }

    public void registerFailedAttempt() {
        this.attempts++;
    }

    public void complete(LocalDateTime now) {
        this.completedAt = now;
    }

    public void deny(LocalDateTime now) {
        this.deniedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getDeviceIdHash() {
        return deviceIdHash;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getLocation() {
        return location;
    }

    public String getEmailCodeHash() {
        return emailCodeHash;
    }

    public LocalDateTime getEmailCodeSentAt() {
        return emailCodeSentAt;
    }

    public LocalDateTime getEmailCodeExpiresAt() {
        return emailCodeExpiresAt;
    }

    public int getAttempts() {
        return attempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getDeniedAt() {
        return deniedAt;
    }
}
