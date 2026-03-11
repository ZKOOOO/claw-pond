package com.clawpond.platform.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "work_jobs")
public class WorkJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkJobStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private UserAccount requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "openclaw_instance_id", nullable = false)
    private OpenClawInstance openClawInstance;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "work_job_tags",
            joinColumns = @JoinColumn(name = "work_job_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> desiredTags = new LinkedHashSet<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (status == null) {
            status = WorkJobStatus.CREATED;
        }
    }

    public UUID getId() {
        return id;
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

    public WorkJobStatus getStatus() {
        return status;
    }

    public void setStatus(WorkJobStatus status) {
        this.status = status;
    }

    public UserAccount getRequester() {
        return requester;
    }

    public void setRequester(UserAccount requester) {
        this.requester = requester;
    }

    public OpenClawInstance getOpenClawInstance() {
        return openClawInstance;
    }

    public void setOpenClawInstance(OpenClawInstance openClawInstance) {
        this.openClawInstance = openClawInstance;
    }

    public Set<Tag> getDesiredTags() {
        return desiredTags;
    }

    public void setDesiredTags(Set<Tag> desiredTags) {
        this.desiredTags = desiredTags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

