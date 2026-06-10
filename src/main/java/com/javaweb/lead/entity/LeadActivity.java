package com.javaweb.lead.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.lead.enums.LeadActivityType;
import com.javaweb.property.entity.CreatedEntity;
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
@Table(name = "lead_activities")
public class LeadActivity extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private LeadActivityType activityType;

    @Column(length = 250)
    private String subject;

    @Column(length = 4000)
    private String details;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    protected LeadActivity() {
    }

    public LeadActivity(LeadActivityType activityType, User actor) {
        this.activityType = activityType;
        this.actor = actor;
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

    public User getActor() {
        return actor;
    }

    public LeadActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(LeadActivityType activityType) {
        this.activityType = activityType;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
