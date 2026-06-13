package com.javaweb.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.javaweb.ai.dto.AiCompletionRequest;
import com.javaweb.ai.dto.AiCompletionResponse;
import com.javaweb.ai.dto.LeadScoreRequest;
import com.javaweb.ai.dto.LeadScoreResponse;
import com.javaweb.ai.enums.AiRequestStatus;
import com.javaweb.ai.repository.AiLeadScoreRepository;
import com.javaweb.appointment.entity.Appointment;
import com.javaweb.appointment.enums.AppointmentStatus;
import com.javaweb.appointment.repository.AppointmentRepository;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.entity.CustomerRequirement;
import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.entity.LeadActivity;
import com.javaweb.lead.entity.LeadSource;
import com.javaweb.lead.enums.FollowUpTaskStatus;
import com.javaweb.lead.enums.LeadActivityType;
import com.javaweb.lead.enums.LeadPipelineStatus;
import com.javaweb.lead.enums.LeadPriority;
import com.javaweb.lead.repository.FollowUpTaskRepository;
import com.javaweb.lead.repository.LeadActivityRepository;
import com.javaweb.lead.repository.LeadRepository;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.enums.PropertyPurpose;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeadScoreServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final LeadRepository leadRepository = mock(LeadRepository.class);
    private final LeadActivityRepository activityRepository = mock(LeadActivityRepository.class);
    private final FollowUpTaskRepository taskRepository = mock(FollowUpTaskRepository.class);
    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final AiService aiService = mock(AiService.class);
    private final AiLeadScoreRepository leadScoreRepository = mock(AiLeadScoreRepository.class);
    private final LeadScoreService service = new LeadScoreService(
            leadRepository,
            activityRepository,
            taskRepository,
            appointmentRepository,
            userRepository,
            aiService,
            new LeadScoreScorer(),
            new LeadScorePromptBuilder(objectMapper),
            leadScoreRepository,
            objectMapper
    );

    @Test
    void shouldUseRuleBasedFallbackWhenAiIsSkipped() {
        User agent = user(10L);
        Lead lead = lead(agent);
        when(leadRepository.findWithAiScoreDetailsById(7L)).thenReturn(Optional.of(lead));
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(activityRepository.findAllByLeadIdOrderByOccurredAtDesc(7L))
                .thenReturn(List.of(activity(agent, 1), activity(agent, 2), activity(agent, 3)));
        when(taskRepository.findAllByLeadIdOrderByDueAtAsc(7L))
                .thenReturn(List.of(upcomingTask(agent)));
        when(appointmentRepository.findAllByLeadIdOrderByStartAtDesc(7L))
                .thenReturn(List.of(confirmedAppointment(lead, agent)));
        when(aiService.complete(any())).thenReturn(AiCompletionResponse.skipped(
                "noop",
                "not-configured",
                "AI provider is disabled or API key is not configured"
        ));

        LeadScoreResponse response = service.score(7L, new LeadScoreRequest("vi"), manager());

        assertThat(response.fallbackUsed()).isTrue();
        assertThat(response.aiStatus()).isEqualTo(AiRequestStatus.SKIPPED);
        assertThat(response.score()).isBetween(75, 100);
        assertThat(response.priority()).isEqualTo(LeadPriority.HIGH);
        assertThat(response.reason()).contains("confirmed appointment", "interaction");
        assertThat(lead.getScore()).isEqualTo(response.score());
        assertThat(lead.getPriority()).isEqualTo(response.priority());
        verify(leadRepository).saveAndFlush(lead);
        verify(leadScoreRepository).save(any());
    }

    @Test
    void shouldUseAiScoreWhenResponseIsValid() {
        User agent = user(10L);
        Lead lead = lead(agent);
        when(leadRepository.findWithAiScoreDetailsById(7L)).thenReturn(Optional.of(lead));
        when(userRepository.findById(10L)).thenReturn(Optional.of(agent));
        when(activityRepository.findAllByLeadIdOrderByOccurredAtDesc(7L)).thenReturn(List.of(activity(agent, 1)));
        when(taskRepository.findAllByLeadIdOrderByDueAtAsc(7L)).thenReturn(List.of());
        when(appointmentRepository.findAllByLeadIdOrderByStartAtDesc(7L)).thenReturn(List.of());
        when(aiService.complete(any())).thenReturn(new AiCompletionResponse(
                AiRequestStatus.SUCCESS,
                "test-provider",
                "test-model",
                """
                        {
                          "score": 64,
                          "priority": "MEDIUM",
                          "reason": "Lead has useful context but needs more qualification.",
                          "suggestedFollowUp": "Ask for budget timing and preferred district."
                        }
                        """,
                "STOP",
                80,
                60,
                140,
                null
        ));

        LeadScoreResponse response = service.score(7L, new LeadScoreRequest("vi"), manager());

        assertThat(response.fallbackUsed()).isFalse();
        assertThat(response.score()).isEqualTo(64);
        assertThat(response.priority()).isEqualTo(LeadPriority.MEDIUM);
        assertThat(response.reason()).contains("useful context");
        assertThat(lead.getScore()).isEqualTo(64);
        assertThat(lead.getPriority()).isEqualTo(LeadPriority.MEDIUM);

        ArgumentCaptor<AiCompletionRequest> captor = ArgumentCaptor.forClass(AiCompletionRequest.class);
        verify(aiService).complete(captor.capture());
        assertThat(captor.getValue().operation()).isEqualTo("LEAD_SCORING");
        assertThat(captor.getValue().metadataJson()).contains("\"code\":\"LEAD-AI-001\"");
    }

    private Lead lead(User agent) {
        LeadSource source = new LeadSource("LISTING_INQUIRY", "Listing inquiry");
        Customer customer = new Customer("CUS-AI-SCORE", "Nguyen Van B", agent);
        customer.setAssignedAgent(agent);
        customer.addRequirement(requirement());
        Lead lead = new Lead("LEAD-AI-001", source, "Nguyen Van B");
        ReflectionTestUtils.setField(lead, "id", 7L);
        lead.setCreatedBy(agent);
        lead.setCurrentAssignee(agent);
        lead.setCustomer(customer);
        lead.setStatus(LeadPipelineStatus.INTERESTED);
        lead.setEmail("buyer@example.test");
        lead.setPhone("0900000000");
        lead.setMessage("Interested in a central apartment");
        lead.setLastContactedAt(Instant.now().minus(12, ChronoUnit.HOURS));
        return lead;
    }

    private CustomerRequirement requirement() {
        CustomerRequirement requirement = new CustomerRequirement(ListingPurpose.SALE);
        requirement.setMinBudget(new BigDecimal("4000000000"));
        requirement.setMaxBudget(new BigDecimal("7000000000"));
        requirement.setDescription("Can ho trung tam");
        return requirement;
    }

    private LeadActivity activity(User agent, int daysAgo) {
        LeadActivity activity = new LeadActivity(LeadActivityType.CALL, agent);
        activity.setSubject("Customer call");
        activity.setDetails("Discussed property needs");
        activity.setOccurredAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        return activity;
    }

    private FollowUpTask upcomingTask(User agent) {
        FollowUpTask task = new FollowUpTask(
                "Send matching listings",
                agent,
                agent,
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
        task.setStatus(FollowUpTaskStatus.PENDING);
        return task;
    }

    private Appointment confirmedAppointment(Lead lead, User agent) {
        Province province = new Province("HCM", "TP Ho Chi Minh");
        Address address = new Address(province, "1 Dong Khoi");
        Property property = new Property(
                "PROP-SCORE",
                "Can ho Quan 1",
                new PropertyType("APT", "Can ho"),
                address,
                agent,
                PropertyPurpose.SALE
        );
        Appointment appointment = new Appointment(
                "APT-SCORE",
                lead.getCustomer(),
                agent,
                property,
                agent,
                "Viewing",
                Instant.now().plus(2, ChronoUnit.DAYS),
                Instant.now().plus(2, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS)
        );
        appointment.setLead(lead);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        return appointment;
    }

    private User user(Long id) {
        User user = new User("agent@example.test", "password", "Agent");
        user.setStatus(UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private AuthUserPrincipal manager() {
        return new AuthUserPrincipal(
                10L,
                "manager@example.test",
                "password",
                "Manager",
                UserStatus.ACTIVE,
                null,
                List.of("MANAGER"),
                List.of(),
                List.of()
        );
    }
}
