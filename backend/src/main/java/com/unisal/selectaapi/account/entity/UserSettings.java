package com.unisal.selectaapi.account.entity;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 10)
    private String language = "pt-BR";

    @Column(name = "application_updates_email", nullable = false)
    private boolean applicationUpdatesEmail = true;

    @Column(name = "application_updates_push", nullable = false)
    private boolean applicationUpdatesPush = true;

    @Column(name = "messages_email", nullable = false)
    private boolean messagesEmail = true;

    @Column(name = "messages_push", nullable = false)
    private boolean messagesPush = true;

    @Column(name = "recommended_jobs_email", nullable = false)
    private boolean recommendedJobsEmail = true;

    @Column(name = "recommended_jobs_push", nullable = false)
    private boolean recommendedJobsPush = false;

    @Column(name = "news_email", nullable = false)
    private boolean newsEmail = false;

    @Column(name = "news_push", nullable = false)
    private boolean newsPush = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", nullable = false, length = 20)
    private ProfileVisibility profileVisibility = ProfileVisibility.RECRUITERS;

    @Column(name = "show_salary", nullable = false)
    private boolean showSalary = false;

    @Column(name = "open_to_work", nullable = false)
    private boolean openToWork = true;

    @Column(name = "allow_direct_contact", nullable = false)
    private boolean allowDirectContact = true;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled = false;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    @Column(name = "last_export_requested_at")
    private LocalDateTime lastExportRequestedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserSettings() {
    }

    public UserSettings(User user) {
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isApplicationUpdatesEmail() {
        return applicationUpdatesEmail;
    }

    public void setApplicationUpdatesEmail(boolean applicationUpdatesEmail) {
        this.applicationUpdatesEmail = applicationUpdatesEmail;
    }

    public boolean isApplicationUpdatesPush() {
        return applicationUpdatesPush;
    }

    public void setApplicationUpdatesPush(boolean applicationUpdatesPush) {
        this.applicationUpdatesPush = applicationUpdatesPush;
    }

    public boolean isMessagesEmail() {
        return messagesEmail;
    }

    public void setMessagesEmail(boolean messagesEmail) {
        this.messagesEmail = messagesEmail;
    }

    public boolean isMessagesPush() {
        return messagesPush;
    }

    public void setMessagesPush(boolean messagesPush) {
        this.messagesPush = messagesPush;
    }

    public boolean isRecommendedJobsEmail() {
        return recommendedJobsEmail;
    }

    public void setRecommendedJobsEmail(boolean recommendedJobsEmail) {
        this.recommendedJobsEmail = recommendedJobsEmail;
    }

    public boolean isRecommendedJobsPush() {
        return recommendedJobsPush;
    }

    public void setRecommendedJobsPush(boolean recommendedJobsPush) {
        this.recommendedJobsPush = recommendedJobsPush;
    }

    public boolean isNewsEmail() {
        return newsEmail;
    }

    public void setNewsEmail(boolean newsEmail) {
        this.newsEmail = newsEmail;
    }

    public boolean isNewsPush() {
        return newsPush;
    }

    public void setNewsPush(boolean newsPush) {
        this.newsPush = newsPush;
    }

    public ProfileVisibility getProfileVisibility() {
        return profileVisibility;
    }

    public void setProfileVisibility(ProfileVisibility profileVisibility) {
        this.profileVisibility = profileVisibility;
    }

    public boolean isShowSalary() {
        return showSalary;
    }

    public void setShowSalary(boolean showSalary) {
        this.showSalary = showSalary;
    }

    public boolean isOpenToWork() {
        return openToWork;
    }

    public void setOpenToWork(boolean openToWork) {
        this.openToWork = openToWork;
    }

    public boolean isAllowDirectContact() {
        return allowDirectContact;
    }

    public void setAllowDirectContact(boolean allowDirectContact) {
        this.allowDirectContact = allowDirectContact;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    public LocalDateTime getLastExportRequestedAt() {
        return lastExportRequestedAt;
    }

    public void setLastExportRequestedAt(LocalDateTime lastExportRequestedAt) {
        this.lastExportRequestedAt = lastExportRequestedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
