package com.javaweb.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.LeadScoreRequest;
import com.javaweb.appointment.entity.Appointment;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.entity.CustomerRequirement;
import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.entity.LeadActivity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
class LeadScorePromptBuilder {
    static final String OPERATION = "LEAD_SCORING";

    private static final String SYSTEM_PROMPT = """
            You are a real-estate CRM lead scoring assistant. Score only from the provided facts.
            Return only valid JSON with keys: score, priority, reason, suggestedFollowUp.
            score must be an integer from 0 to 100. priority must be LOW, MEDIUM, or HIGH.
            """;

    private final ObjectMapper objectMapper;

    LeadScorePromptBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    LeadScorePrompt build(
            Lead lead,
            List<LeadActivity> activities,
            List<FollowUpTask> tasks,
            List<Appointment> appointments,
            LeadScoreDraft fallbackScore,
            LeadScoreRequest request
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("language", request.language() == null ? "vi" : request.language());
        payload.put("lead", leadPayload(lead));
        payload.put("customer", customerPayload(lead.getCustomer()));
        payload.put("recentActivities", activities.stream().limit(10).map(this::activityPayload).toList());
        payload.put("followUpTasks", tasks.stream().limit(10).map(this::taskPayload).toList());
        payload.put("appointments", appointments.stream().limit(10).map(this::appointmentPayload).toList());
        payload.put("fallbackScore", fallbackPayload(fallbackScore));

        String sourceJson = toJson(payload);
        return new LeadScorePrompt(
                SYSTEM_PROMPT,
                """
                        Score this lead for sales priority. Consider source, budget, interactions,
                        recent activity, appointment status, favorite listings and time since last contact.
                        Source data:
                        %s
                        """.formatted(sourceJson),
                sourceJson,
                "lead",
                lead.getId()
        );
    }

    private Map<String, Object> leadPayload(Lead lead) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", lead.getId());
        payload.put("code", lead.getCode());
        payload.put("source", lead.getSource() == null ? null : lead.getSource().getCode());
        payload.put("status", lead.getStatus().name());
        payload.put("currentPriority", lead.getPriority().name());
        payload.put("currentScore", lead.getScore());
        payload.put("message", lead.getMessage());
        payload.put("hasEmail", lead.getEmail() != null);
        payload.put("hasPhone", lead.getPhone() != null);
        payload.put("lastContactedAt", lead.getLastContactedAt());
        payload.put("createdAt", lead.getCreatedAt());
        payload.put("listingId", lead.getListing() == null ? null : lead.getListing().getId());
        payload.put("listingTitle", lead.getListing() == null ? null : lead.getListing().getTitle());
        return payload;
    }

    private Map<String, Object> customerPayload(Customer customer) {
        if (customer == null) {
            return Map.of();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", customer.getId());
        payload.put("code", customer.getCode());
        payload.put("priority", customer.getPriority().name());
        payload.put("notes", customer.getNotes());
        payload.put("favoriteListingCount", customer.getFavoriteListings().size());
        payload.put("activeRequirements", customer.getRequirements().stream()
                .filter(CustomerRequirement::isActive)
                .map(this::requirementPayload)
                .toList());
        return payload;
    }

    private Map<String, Object> requirementPayload(CustomerRequirement requirement) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("purpose", requirement.getPurpose().name());
        payload.put("propertyType", requirement.getPropertyType() == null
                ? null
                : requirement.getPropertyType().getName());
        payload.put("province", requirement.getProvince() == null ? null : requirement.getProvince().getName());
        payload.put("district", requirement.getDistrict() == null ? null : requirement.getDistrict().getName());
        payload.put("minBudget", requirement.getMinBudget());
        payload.put("maxBudget", requirement.getMaxBudget());
        payload.put("currency", requirement.getCurrency());
        payload.put("description", requirement.getDescription());
        return payload;
    }

    private Map<String, Object> activityPayload(LeadActivity activity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", activity.getActivityType().name());
        payload.put("subject", activity.getSubject());
        payload.put("details", activity.getDetails());
        payload.put("occurredAt", activity.getOccurredAt());
        return payload;
    }

    private Map<String, Object> taskPayload(FollowUpTask task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", task.getTitle());
        payload.put("status", task.getStatus().name());
        payload.put("priority", task.getPriority().name());
        payload.put("dueAt", task.getDueAt());
        payload.put("completedAt", task.getCompletedAt());
        return payload;
    }

    private Map<String, Object> appointmentPayload(Appointment appointment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", appointment.getCode());
        payload.put("status", appointment.getStatus().name());
        payload.put("startAt", appointment.getStartAt());
        payload.put("completedAt", appointment.getCompletedAt());
        payload.put("cancelledAt", appointment.getCancelledAt());
        return payload;
    }

    private Map<String, Object> fallbackPayload(LeadScoreDraft fallbackScore) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("score", fallbackScore.score());
        payload.put("priority", fallbackScore.priority().name());
        payload.put("reason", fallbackScore.reason());
        payload.put("suggestedFollowUp", fallbackScore.suggestedFollowUp());
        return payload;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to build AI lead score prompt", exception);
        }
    }
}
