package com.unisal.selectaapi.candidate.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@Table(name = "candidate_projects")
public class CandidateProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String projectUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "project_tools_skills",
        joinColumns = @JoinColumn(name = "project_id")
    )
    @Column(name = "tool_or_skill")
    private List<String> toolsAndSkills = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CandidateProject() {
    }

    public CandidateProject(
            CandidateProfile candidateProfile,
            String title,
            String description,
            String projectUrl,
            List<String> toolsAndSkills) {
        this.candidateProfile = candidateProfile;
        this.title = title;
        this.description = description;
        this.projectUrl = projectUrl;
        if (toolsAndSkills != null) {
            this.toolsAndSkills = toolsAndSkills;
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CandidateProfile getCandidateProfile() {
        return candidateProfile;
    }

    public void setCandidateProfile(CandidateProfile candidateProfile) {
        this.candidateProfile = candidateProfile;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProjectUrl() {
        return projectUrl;
    }

    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }

    public List<String> getToolsAndSkills() {
        return toolsAndSkills;
    }

    public void setToolsAndSkills(List<String> toolsAndSkills) {
        this.toolsAndSkills = toolsAndSkills;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}