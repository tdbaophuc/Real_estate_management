package com.javaweb.ai.entity;

import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.auth.entity.User;
import com.javaweb.property.entity.CreatedEntity;
import com.javaweb.property.entity.PropertyImage;
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
@Table(name = "ai_image_analyses")
public class AiImageAnalysis extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_image_id", nullable = false)
    private PropertyImage propertyImage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(name = "result_json", nullable = false, length = 8000)
    private String resultJson;

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

    protected AiImageAnalysis() {
    }

    public AiImageAnalysis(
            PropertyImage propertyImage,
            User generatedBy,
            String resultJson,
            boolean fallbackUsed,
            AiRequestStatus aiStatus,
            String provider,
            String model,
            String errorMessage
    ) {
        this.propertyImage = propertyImage;
        this.generatedBy = generatedBy;
        this.resultJson = resultJson;
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
