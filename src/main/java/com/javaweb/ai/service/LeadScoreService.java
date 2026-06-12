package com.javaweb.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.dto.LeadScoreRequest;
import com.javaweb.ai.dto.LeadScoreResponse;
import com.javaweb.ai.entity.AiLeadScore;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.ai.repository.AiLeadScoreRepository;
import com.javaweb.appointment.entity.Appointment;
import com.javaweb.appointment.repository.AppointmentRepository;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.exception.BusinessException;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.entity.LeadActivity;
import com.javaweb.lead.enums.LeadPriority;
import com.javaweb.lead.repository.FollowUpTaskRepository;
import com.javaweb.lead.repository.LeadActivityRepository;
import com.javaweb.lead.repository.LeadRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class LeadScoreService {
    private final LeadRepository leadRepository;
    private final LeadActivityRepository activityRepository;
    private final FollowUpTaskRepository taskRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AiService aiService;
    private final LeadScoreScorer scorer;
    private final LeadScorePromptBuilder promptBuilder;
    private final AiLeadScoreRepository leadScoreRepository;
    private final ObjectMapper objectMapper;

    public LeadScoreService(
            LeadRepository leadRepository,
            LeadActivityRepository activityRepository,
            FollowUpTaskRepository taskRepository,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            AiService aiService,
            LeadScoreScorer scorer,
            LeadScorePromptBuilder promptBuilder,
            AiLeadScoreRepository leadScoreRepository,
            ObjectMapper objectMapper
    ) {
        this.leadRepository = leadRepository;
        this.activityRepository = activityRepository;
        this.taskRepository = taskRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.aiService = aiService;
        this.scorer = scorer;
        this.promptBuilder = promptBuilder;
        this.leadScoreRepository = leadScoreRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LeadScoreResponse score(
            Long leadId,
            LeadScoreRequest request,
            AuthUserPrincipal actor
    ) {
        Lead lead = requireAccessibleLead(leadId, actor);
        User generatedBy = userRepository.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        List<LeadActivity> activities = activityRepository.findAllByLeadIdOrderByOccurredAtDesc(leadId);
        List<FollowUpTask> tasks = taskRepository.findAllByLeadIdOrderByDueAtAsc(leadId);
        List<Appointment> appointments = appointmentRepository.findAllByLeadIdOrderByStartAtDesc(leadId);

        LeadScoreDraft fallback = scorer.score(lead, activities, tasks, appointments, Instant.now());
        LeadScorePrompt prompt = promptBuilder.build(
                lead,
                activities,
                tasks,
                appointments,
                fallback,
                request
        );
        AiCompletionResponse aiResponse = aiService.complete(new AiCompletionRequest(
                LeadScorePromptBuilder.OPERATION,
                prompt.systemPrompt(),
                prompt.userPrompt(),
                prompt.referenceType(),
                prompt.referenceId(),
                prompt.metadataJson()
        ));

        boolean fallbackUsed = true;
        String errorMessage = aiResponse.errorMessage();
        LeadScoreDraft result = fallback;
        if (aiResponse.status() == AiRequestStatus.SUCCESS && hasText(aiResponse.content())) {
            try {
                result = parse(aiResponse.content());
                fallbackUsed = false;
                errorMessage = null;
            } catch (IllegalArgumentException exception) {
                errorMessage = exception.getMessage();
            }
        }

        lead.setScore(result.score());
        lead.setPriority(result.priority());
        leadRepository.saveAndFlush(lead);
        leadScoreRepository.save(new AiLeadScore(
                lead,
                generatedBy,
                result.score(),
                result.priority(),
                limit(result.reason(), 1000),
                limit(result.suggestedFollowUp(), 1000),
                fallbackUsed,
                aiResponse.status(),
                aiResponse.provider(),
                aiResponse.model(),
                limit(errorMessage, 1000)
        ));

        return new LeadScoreResponse(
                lead.getId(),
                lead.getCode(),
                result.score(),
                result.priority(),
                result.reason(),
                result.suggestedFollowUp(),
                fallbackUsed,
                aiResponse.status(),
                aiResponse.provider(),
                aiResponse.model(),
                errorMessage
        );
    }

    private Lead requireAccessibleLead(Long leadId, AuthUserPrincipal actor) {
        Lead lead = leadRepository.findWithAiScoreDetailsById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        if (lead.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Lead not found");
        }
        if (!isManagerOrAdmin(actor)) {
            boolean createdByActor = lead.getCreatedBy() != null
                    && lead.getCreatedBy().getId().equals(actor.id());
            boolean assignedToActor = lead.getCurrentAssignee() != null
                    && lead.getCurrentAssignee().getId().equals(actor.id());
            if (!createdByActor && !assignedToActor) {
                throw new AccessDeniedException(
                        "Agents can only score leads they created or are assigned"
                );
            }
        }
        return lead;
    }

    private LeadScoreDraft parse(String content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            int score = Math.max(0, Math.min(100, root.path("score").asInt(-1)));
            if (score < 0) {
                throw new BusinessException("AI response is missing score");
            }
            return new LeadScoreDraft(
                    score,
                    LeadPriority.valueOf(requiredText(root, "priority", 20)),
                    requiredText(root, "reason", 1000),
                    requiredText(root, "suggestedFollowUp", 1000)
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("AI response is not valid lead-score JSON");
        }
    }

    private String requiredText(JsonNode root, String field, int maxLength) {
        String value = root.path(field).asText("").trim();
        if (!hasText(value)) {
            throw new BusinessException("AI response is missing " + field);
        }
        return limit(value, maxLength);
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
