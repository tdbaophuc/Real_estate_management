package com.javaweb.ai.entity;

import com.javaweb.ai.enums.AiMessageRole;
import com.javaweb.ai.enums.AiRequestStatus;
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
@Table(name = "ai_messages")
public class AiMessage extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AiConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiMessageRole role;

    @Column(nullable = false, length = 8000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_status", length = 30)
    private AiRequestStatus aiStatus;

    @Column(length = 50)
    private String provider;

    @Column(length = 100)
    private String model;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    protected AiMessage() {
    }

    public AiMessage(AiMessageRole role, String content) {
        this.role = role;
        this.content = content;
    }

    void setConversation(AiConversation conversation) {
        this.conversation = conversation;
    }

    public Long getId() {
        return id;
    }

    public AiConversation getConversation() {
        return conversation;
    }

    public AiMessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public AiRequestStatus getAiStatus() {
        return aiStatus;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setAiResult(
            AiRequestStatus aiStatus,
            String provider,
            String model,
            String errorMessage
    ) {
        this.aiStatus = aiStatus;
        this.provider = provider;
        this.model = model;
        this.errorMessage = errorMessage;
    }
}
