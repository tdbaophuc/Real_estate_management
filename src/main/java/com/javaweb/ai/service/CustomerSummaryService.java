package com.javaweb.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.dto.CustomerSummaryResponse;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.entity.CustomerFavoriteListing;
import com.javaweb.customer.entity.CustomerNote;
import com.javaweb.customer.entity.CustomerRequirement;
import com.javaweb.customer.repository.CustomerFavoriteListingRepository;
import com.javaweb.customer.repository.CustomerNoteRepository;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.customer.repository.CustomerRequirementRepository;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.repository.LeadRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerSummaryService {
    private static final String OPERATION = "CUSTOMER_SUMMARY";
    private static final String SYSTEM_PROMPT = """
            You summarize real-estate customer profiles for agents. Use only provided facts.
            Return only valid JSON with keys: needsSummary, interactionSummary,
            interestedProperties, potentialLevel, nextBestAction.
            interestedProperties must be an array of strings. potentialLevel should be LOW, MEDIUM, or HIGH.
            """;

    private final CustomerRepository customerRepository;
    private final CustomerRequirementRepository requirementRepository;
    private final CustomerNoteRepository noteRepository;
    private final CustomerFavoriteListingRepository favoriteRepository;
    private final LeadRepository leadRepository;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public CustomerSummaryService(
            CustomerRepository customerRepository,
            CustomerRequirementRepository requirementRepository,
            CustomerNoteRepository noteRepository,
            CustomerFavoriteListingRepository favoriteRepository,
            LeadRepository leadRepository,
            AiService aiService,
            ObjectMapper objectMapper
    ) {
        this.customerRepository = customerRepository;
        this.requirementRepository = requirementRepository;
        this.noteRepository = noteRepository;
        this.favoriteRepository = favoriteRepository;
        this.leadRepository = leadRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public CustomerSummaryResponse summarize(Long customerId, AuthUserPrincipal actor) {
        Customer customer = requireAccessibleCustomer(customerId, actor);
        List<CustomerRequirement> requirements =
                requirementRepository.findAllByCustomerIdAndActiveTrueOrderByCreatedAtDesc(customerId);
        List<CustomerNote> notes = noteRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
        List<CustomerFavoriteListing> favorites = favoriteRepository
                .findAllByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(0, 10))
                .getContent();
        List<Lead> leads = leadRepository.findAllByCustomerIdAndDeletedAtIsNullOrderByCreatedAtDesc(customerId);

        CustomerSummaryResponse fallback = fallback(customer, requirements, notes, favorites, leads, null, null);
        String metadataJson = metadata(customer, requirements, notes, favorites, leads);
        AiCompletionResponse aiResponse = aiService.complete(new AiCompletionRequest(
                OPERATION,
                SYSTEM_PROMPT,
                "Summarize this customer profile:\n" + metadataJson,
                "customer",
                customer.getId(),
                metadataJson
        ));
        if (aiResponse.status() == AiRequestStatus.SUCCESS && hasText(aiResponse.content())) {
            try {
                return parse(customer, aiResponse);
            } catch (IllegalArgumentException exception) {
                return fallback(customer, requirements, notes, favorites, leads, aiResponse, exception.getMessage());
            }
        }
        return fallback(customer, requirements, notes, favorites, leads, aiResponse, aiResponse.errorMessage());
    }

    private Customer requireAccessibleCustomer(Long customerId, AuthUserPrincipal actor) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        if (customer.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Customer not found");
        }
        if (!isManagerOrAdmin(actor)) {
            boolean createdByActor = customer.getCreatedBy().getId().equals(actor.id());
            boolean assignedToActor = customer.getAssignedAgent() != null
                    && customer.getAssignedAgent().getId().equals(actor.id());
            if (!createdByActor && !assignedToActor) {
                throw new AccessDeniedException(
                        "Agents can only summarize customers they created or are assigned"
                );
            }
        }
        return customer;
    }

    private CustomerSummaryResponse parse(Customer customer, AiCompletionResponse aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse.content());
            return new CustomerSummaryResponse(
                    customer.getId(),
                    customer.getCode(),
                    requiredText(root, "needsSummary", 1000),
                    requiredText(root, "interactionSummary", 1000),
                    stringArray(root.get("interestedProperties")),
                    requiredText(root, "potentialLevel", 20),
                    requiredText(root, "nextBestAction", 1000),
                    false,
                    aiResponse.status(),
                    aiResponse.provider(),
                    aiResponse.model(),
                    null
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI response is not valid customer-summary JSON");
        }
    }

    private CustomerSummaryResponse fallback(
            Customer customer,
            List<CustomerRequirement> requirements,
            List<CustomerNote> notes,
            List<CustomerFavoriteListing> favorites,
            List<Lead> leads,
            AiCompletionResponse aiResponse,
            String errorMessage
    ) {
        return new CustomerSummaryResponse(
                customer.getId(),
                customer.getCode(),
                requirements.isEmpty()
                        ? "No active requirement has been recorded"
                        : requirements.getFirst().getPurpose() + " need: " + nullToDefault(requirements.getFirst().getDescription(), "details pending"),
                notes.isEmpty()
                        ? "No customer notes recorded"
                        : "Latest note: " + notes.getFirst().getContent(),
                favorites.stream()
                        .map(favorite -> favorite.getListing().getTitle())
                        .limit(5)
                        .toList(),
                leads.stream().anyMatch(lead -> lead.getScore() != null && lead.getScore() >= 75)
                        ? "HIGH"
                        : favorites.isEmpty() && leads.isEmpty() ? "LOW" : "MEDIUM",
                leads.isEmpty()
                        ? "Create or qualify a lead before the next outreach"
                        : "Contact the customer and update the latest lead status after the conversation",
                true,
                aiResponse == null ? AiRequestStatus.SKIPPED : aiResponse.status(),
                aiResponse == null ? null : aiResponse.provider(),
                aiResponse == null ? null : aiResponse.model(),
                errorMessage
        );
    }

    private String metadata(
            Customer customer,
            List<CustomerRequirement> requirements,
            List<CustomerNote> notes,
            List<CustomerFavoriteListing> favorites,
            List<Lead> leads
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerCode", customer.getCode());
        payload.put("fullName", customer.getFullName());
        payload.put("priority", customer.getPriority().name());
        payload.put("notes", customer.getNotes());
        payload.put("requirements", requirements.stream().map(requirement -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("purpose", requirement.getPurpose().name());
            item.put("minBudget", requirement.getMinBudget());
            item.put("maxBudget", requirement.getMaxBudget());
            item.put("description", requirement.getDescription());
            return item;
        }).toList());
        payload.put("latestNotes", notes.stream().limit(5).map(CustomerNote::getContent).toList());
        payload.put("favorites", favorites.stream().map(favorite -> favorite.getListing().getTitle()).toList());
        payload.put("leads", leads.stream().limit(5).map(lead -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", lead.getCode());
            item.put("status", lead.getStatus().name());
            item.put("priority", lead.getPriority().name());
            item.put("score", lead.getScore());
            return item;
        }).toList());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to build customer summary metadata", exception);
        }
    }

    private String requiredText(JsonNode root, String field, int maxLength) {
        String value = root.path(field).asText("").trim();
        if (!hasText(value)) {
            throw new BusinessException("AI response is missing " + field);
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    private List<String> stringArray(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        for (JsonNode item : node) {
            String value = item.asText("").trim();
            if (hasText(value)) {
                values.add(value);
            }
        }
        return values.stream().limit(10).toList();
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
