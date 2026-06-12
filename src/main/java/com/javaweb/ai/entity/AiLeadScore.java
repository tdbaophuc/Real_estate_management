package com.javaweb.ai.entity;

import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.auth.entity.User;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.enums.LeadPriority;
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

@Entity
@Table(name = "ai_lead_scores")
public class AiLeadScore extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeadPriority priority;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "suggested_follow_up", nullable = false, length = 1000)
    private String suggestedFollowUp;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status", nullable = false, length = 30)
    private AiRequestStatus aiStatus;

    @Column(length = 50)
    private String provider;

    @Column(length = 100)
    private String model;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    protected AiLeadScore() {
    }

    public AiLeadScore(
            Lead lead,
            User generatedBy,
            int score,
            LeadPriority priority,
            String reason,
            String suggestedFollowUp,
            boolean fallbackUsed,
            AiRequestStatus aiStatus,
            String provider,
            String model,
            String errorMessage
    ) {
        this.lead = lead;
        this.generatedBy = generatedBy;
        this.score = score;
        this.priority = priority;
        this.reason = reason;
        this.suggestedFollowUp = suggestedFollowUp;
        this.fallbackUsed = fallbackUsed;
        this.aiStatus = aiStatus;
        this.provider = provider;
        this.model = model;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }
}
