package com.javaweb.lead.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.lead.enums.FollowUpTaskStatus;
import com.javaweb.lead.enums.LeadPriority;
import com.javaweb.property.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "follow_up_tasks")
public class FollowUpTask extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_to", nullable = false)
    private User assignedTo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FollowUpTaskStatus status = FollowUpTaskStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadPriority priority = LeadPriority.MEDIUM;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected FollowUpTask() {
    }

    public FollowUpTask(String title, User assignedTo, User createdBy, Instant dueAt) {
        this.title = title;
        this.assignedTo = assignedTo;
        this.createdBy = createdBy;
        this.dueAt = dueAt;
    }

    void setLead(Lead lead) {
        this.lead = lead;
    }

    public Long getId() {
        return id;
    }

    public Lead getLead() {
        return lead;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }

    public User getCreatedBy() {
        return createdBy;
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

    public FollowUpTaskStatus getStatus() {
        return status;
    }

    public void setStatus(FollowUpTaskStatus status) {
        this.status = status;
    }

    public LeadPriority getPriority() {
        return priority;
    }

    public void setPriority(LeadPriority priority) {
        this.priority = priority;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public void setDueAt(Instant dueAt) {
        this.dueAt = dueAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
