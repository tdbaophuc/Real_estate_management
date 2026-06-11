package com.javaweb.ai.entity;

import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.property.entity.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_request_logs")
public class AiRequestLog extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(length = 100)
    private String model;

    @Column(nullable = false, length = 100)
    private String operation;

    @Column(name = "system_prompt", length = 4000)
    private String systemPrompt;

    @Column(name = "user_prompt", nullable = false, length = 8000)
    private String userPrompt;

    @Column(name = "metadata_json", length = 8000)
    private String metadataJson;

    @Column(name = "response_content", length = 8000)
    private String responseContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiRequestStatus status;

    @Column(name = "finish_reason", length = 100)
    private String finishReason;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "reference_type", length = 100)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    protected AiRequestLog() {
    }

    public AiRequestLog(
            String provider,
            String model,
            String operation,
            String systemPrompt,
            String userPrompt,
            String metadataJson,
            String referenceType,
            Long referenceId
    ) {
        this.provider = provider;
        this.model = model;
        this.operation = operation;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.metadataJson = metadataJson;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getModel() {
        return model;
    }

    public String getOperation() {
        return operation;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public String getResponseContent() {
        return responseContent;
    }

    public AiRequestStatus getStatus() {
        return status;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void complete(AiCompletionResponseSnapshot response, long latencyMs) {
        this.status = response.status();
        this.responseContent = response.content();
        this.finishReason = response.finishReason();
        this.promptTokens = response.promptTokens();
        this.completionTokens = response.completionTokens();
        this.totalTokens = response.totalTokens();
        this.errorMessage = response.errorMessage();
        this.latencyMs = latencyMs;
    }

    public record AiCompletionResponseSnapshot(
            AiRequestStatus status,
            String content,
            String finishReason,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            String errorMessage
    ) {
    }
}
