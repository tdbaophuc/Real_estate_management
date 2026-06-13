package com.javaweb.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.dto.PropertyRecommendationItemResponse;
import com.javaweb.ai.dto.PropertyRecommendationRequest;
import com.javaweb.ai.dto.PropertyRecommendationResponse;
import com.javaweb.ai.entity.AiRecommendation;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.ai.repository.AiRecommendationRepository;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.entity.CustomerRequirement;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.customer.repository.CustomerRequirementRepository;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.mapper.ListingMapper;
import com.javaweb.listing.repository.ListingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PropertyRecommendationService {
    private final CustomerRepository customerRepository;
    private final CustomerRequirementRepository requirementRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final PropertyRecommendationPromptBuilder promptBuilder;
    private final PropertyRecommendationScorer scorer;
    private final ListingMapper listingMapper;
    private final AiRecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;

    public PropertyRecommendationService(
            CustomerRepository customerRepository,
            CustomerRequirementRepository requirementRepository,
            ListingRepository listingRepository,
            UserRepository userRepository,
            AiService aiService,
            PropertyRecommendationPromptBuilder promptBuilder,
            PropertyRecommendationScorer scorer,
            ListingMapper listingMapper,
            AiRecommendationRepository recommendationRepository,
            ObjectMapper objectMapper
    ) {
        this.customerRepository = customerRepository;
        this.requirementRepository = requirementRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.promptBuilder = promptBuilder;
        this.scorer = scorer;
        this.listingMapper = listingMapper;
        this.recommendationRepository = recommendationRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PropertyRecommendationResponse recommend(
            Long customerId,
            PropertyRecommendationRequest request,
            AuthUserPrincipal actor
    ) {
        Customer customer = requireAccessibleCustomer(customerId, actor);
        User generatedBy = userRepository.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        CustomerRequirement requirement = latestActiveRequirement(customerId);
        List<Listing> candidates = findCandidates(requirement, request);
        if (candidates.isEmpty()) {
            return new PropertyRecommendationResponse(
                    customer.getId(),
                    customer.getCode(),
                    true,
                    AiRequestStatus.SKIPPED,
                    null,
                    null,
                    "No published listings match the customer's rule-based filters",
                    List.of()
            );
        }

        List<PropertyRecommendationDraft> fallbackScores = candidates.stream()
                .map(candidate -> scorer.score(candidate, customer, requirement, request))
                .sorted(Comparator.comparingInt(PropertyRecommendationDraft::matchScore).reversed())
                .limit(request.maxResults())
                .toList();

        PropertyRecommendationPrompt prompt = promptBuilder.build(
                customer,
                requirement,
                candidates,
                fallbackScores,
                request
        );
        AiCompletionResponse aiResponse = aiService.complete(new AiCompletionRequest(
                PropertyRecommendationPromptBuilder.OPERATION,
                prompt.systemPrompt(),
                prompt.userPrompt(),
                prompt.referenceType(),
                prompt.referenceId(),
                prompt.metadataJson()
        ));

        boolean fallbackUsed = true;
        String errorMessage = aiResponse.errorMessage();
        List<PropertyRecommendationDraft> drafts = fallbackScores;
        if (aiResponse.status() == AiRequestStatus.SUCCESS && hasText(aiResponse.content())) {
            try {
                drafts = parseAiRecommendations(aiResponse.content(), candidates, request.maxResults());
                fallbackUsed = false;
                errorMessage = null;
            } catch (IllegalArgumentException exception) {
                errorMessage = exception.getMessage();
            }
        }

        Map<Long, Listing> listingById = candidates.stream()
                .collect(LinkedHashMap::new, (map, listing) -> map.put(listing.getId(), listing), Map::putAll);
        List<PropertyRecommendationItemResponse> items = drafts.stream()
                .filter(draft -> listingById.containsKey(draft.listingId()))
                .map(draft -> toItemResponse(listingById.get(draft.listingId()), draft))
                .toList();
        saveRecommendations(customer, generatedBy, items, fallbackUsed, aiResponse, errorMessage);

        return new PropertyRecommendationResponse(
                customer.getId(),
                customer.getCode(),
                fallbackUsed,
                aiResponse.status(),
                aiResponse.provider(),
                aiResponse.model(),
                errorMessage,
                items
        );
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
                        "Agents can only recommend listings for customers they created or are assigned"
                );
            }
        }
        return customer;
    }

    private CustomerRequirement latestActiveRequirement(Long customerId) {
        return requirementRepository.findAllByCustomerIdAndActiveTrueOrderByCreatedAtDesc(customerId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private List<Listing> findCandidates(
            CustomerRequirement requirement,
            PropertyRecommendationRequest request
    ) {
        return listingRepository.findRecommendationCandidates(
                requirement == null ? null : requirement.getPurpose(),
                requirement == null || requirement.getPropertyType() == null
                        ? null
                        : requirement.getPropertyType().getId(),
                requirement == null || requirement.getProvince() == null
                        ? null
                        : requirement.getProvince().getId(),
                requirement == null || requirement.getDistrict() == null
                        ? null
                        : requirement.getDistrict().getId(),
                requirement == null || requirement.getWard() == null
                        ? null
                        : requirement.getWard().getId(),
                requirement == null ? null : requirement.getMinBudget(),
                requirement == null ? null : requirement.getMaxBudget(),
                requirement == null ? null : requirement.getMinArea(),
                requirement == null ? null : requirement.getMaxArea(),
                requirement == null ? null : requirement.getMinBedrooms(),
                requirement == null ? null : requirement.getMinBathrooms(),
                PageRequest.of(
                        0,
                        request.candidateLimit(),
                        Sort.by(Sort.Direction.DESC, "publishedAt")
                                .and(Sort.by(Sort.Direction.DESC, "id"))
                )
        ).getContent();
    }

    private List<PropertyRecommendationDraft> parseAiRecommendations(
            String content,
            List<Listing> candidates,
            int maxResults
    ) {
        try {
            JsonNode recommendations = objectMapper.readTree(content).get("recommendations");
            if (recommendations == null || !recommendations.isArray()) {
                throw new BusinessException("AI response is missing recommendations");
            }
            List<Long> candidateIds = candidates.stream().map(Listing::getId).toList();
            return toDrafts(recommendations, candidateIds).stream()
                    .limit(maxResults)
                    .toList();
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI response is not valid property-recommendation JSON");
        }
    }

    private List<PropertyRecommendationDraft> toDrafts(JsonNode recommendations, List<Long> candidateIds) {
        java.util.ArrayList<PropertyRecommendationDraft> drafts = new java.util.ArrayList<>();
        for (JsonNode node : recommendations) {
            Long listingId = node.path("listingId").isNumber() ? node.path("listingId").asLong() : null;
            if (listingId == null || !candidateIds.contains(listingId)) {
                continue;
            }
            drafts.add(new PropertyRecommendationDraft(
                    listingId,
                    Math.max(0, Math.min(100, node.path("matchScore").asInt(0))),
                    requiredText(node, "reason", 1000),
                    requiredText(node, "suggestedAction", 1000)
            ));
        }
        if (drafts.isEmpty()) {
            throw new BusinessException("AI response did not rank any candidate listing");
        }
        return drafts;
    }

    private String requiredText(JsonNode node, String field, int maxLength) {
        String value = node.path(field).asText("").trim();
        if (!hasText(value)) {
            throw new BusinessException("AI response is missing " + field);
        }
        return limit(value, maxLength);
    }

    private PropertyRecommendationItemResponse toItemResponse(
            Listing listing,
            PropertyRecommendationDraft draft
    ) {
        return new PropertyRecommendationItemResponse(
                listingMapper.toPublicResponse(listing),
                draft.matchScore(),
                draft.reason(),
                draft.suggestedAction()
        );
    }

    private void saveRecommendations(
            Customer customer,
            User generatedBy,
            List<PropertyRecommendationItemResponse> items,
            boolean fallbackUsed,
            AiCompletionResponse aiResponse,
            String errorMessage
    ) {
        List<AiRecommendation> entities = items.stream()
                .map(item -> new AiRecommendation(
                        customer,
                        listingRepository.getReferenceById(item.listing().id()),
                        generatedBy,
                        item.matchScore(),
                        limit(item.reason(), 1000),
                        limit(item.suggestedAction(), 1000),
                        fallbackUsed,
                        aiResponse.status(),
                        aiResponse.provider(),
                        aiResponse.model(),
                        limit(errorMessage, 1000)
                ))
                .toList();
        recommendationRepository.saveAll(entities);
    }

    private boolean isManagerOrAdmin(AuthUserPrincipal actor) {
        return actor.roles().contains(RoleCode.ADMIN.name())
                || actor.roles().contains(RoleCode.MANAGER.name());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
