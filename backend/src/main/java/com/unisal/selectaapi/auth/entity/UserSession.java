package com.unisal.selectaapi.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.unisal.selectaapi.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Uma sessão por login. O access token (JWT) carrega o id da sessão na claim
 * "sid"; encerrar a sessão invalida o JWT e o refresh token imediatamente.
 */
@Entity
@Table(name = "user_sessions", indexes = @Index(name = "idx_user_sessions_user", columnList = "user_id"))
public class UserSession {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "previous_refresh_token_hash", length = 64)
    private String previousRefreshTokenHash;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_active_at", nullable = false)
    private LocalDateTime lastActiveAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    protected UserSession() {
    }

    public UserSession(User user, String deviceIdHash, String deviceName, String userAgent, String ipAddress,
            String location, LocalDateTime now, LocalDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.deviceIdHash = deviceIdHash;
        this.deviceName = deviceName;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.location = location;
        this.createdAt = now;
        this.lastActiveAt = now;
        this.expiresAt = expiresAt;
    }

    public boolean isActive(LocalDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void rotate(String newHash, LocalDateTime now, LocalDateTime newExpiresAt) {
        this.previousRefreshTokenHash = this.refreshTokenHash;
        this.refreshTokenHash = newHash;
        this.rotatedAt = now;
        this.lastActiveAt = now;
        this.expiresAt = newExpiresAt;
    }

    public void revoke(LocalDateTime now) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public String getPreviousRefreshTokenHash() {
        return previousRefreshTokenHash;
    }

    public LocalDateTime getRotatedAt() {
        return rotatedAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }
}
