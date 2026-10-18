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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Dispositivo que já passou pela verificação em duas etapas e não precisa repeti-la. */
@Entity
@Table(name = "trusted_devices", uniqueConstraints = @UniqueConstraint(name = "uk_trusted_devices_user_device", columnNames = {
        "user_id", "device_id_hash" }))
public class TrustedDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_id_hash", nullable = false, length = 64)
    private String deviceIdHash;

    @Column(name = "device_name", nullable = false, length = 120)
    private String deviceName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt;

    protected TrustedDevice() {
    }

    public TrustedDevice(User user, String deviceIdHash, String deviceName, LocalDateTime now) {
        this.user = user;
        this.deviceIdHash = deviceIdHash;
        this.deviceName = deviceName;
        this.createdAt = now;
        this.lastUsedAt = now;
    }

    public void touch(LocalDateTime now) {
        this.lastUsedAt = now;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }
}
