package com.javaweb.ai.entity;

import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.auth.entity.User;
import com.javaweb.customer.entity.Customer;
import com.javaweb.listing.entity.Listing;
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
@Table(name = "ai_recommendations")
public class AiRecommendation extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "match_score", nullable = false)
    private int matchScore;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Column(name = "suggested_action", nullable = false, length = 1000)
    private String suggestedAction;

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

    protected AiRecommendation() {
    }

    public AiRecommendation(
            Customer customer,
            Listing listing,
            User generatedBy,
            int matchScore,
            String reason,
            String suggestedAction,
            boolean fallbackUsed,
            AiRequestStatus aiStatus,
            String provider,
            String model,
            String errorMessage
    ) {
        this.customer = customer;
        this.listing = listing;
        this.generatedBy = generatedBy;
        this.matchScore = matchScore;
        this.reason = reason;
        this.suggestedAction = suggestedAction;
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
