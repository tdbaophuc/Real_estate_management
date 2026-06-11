package com.javaweb.lead.repository;

import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.entity.LeadActivity;
import com.javaweb.lead.entity.LeadAssignment;
import com.javaweb.lead.entity.LeadNote;
import com.javaweb.lead.entity.LeadSource;
import com.javaweb.lead.enums.FollowUpTaskStatus;
import com.javaweb.lead.enums.LeadActivityType;
import com.javaweb.lead.enums.LeadPipelineStatus;
import com.javaweb.lead.enums.LeadPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class LeadRepositoryIntegrationTest {
    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private LeadSourceRepository sourceRepository;

    @Autowired
    private LeadAssignmentRepository assignmentRepository;

    @Autowired
    private LeadNoteRepository noteRepository;

    @Autowired
    private LeadActivityRepository activityRepository;

    @Autowired
    private FollowUpTaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private User agent;
    private LeadSource websiteSource;

    @BeforeEach
    void setUp() {
        agent = createAgent("lead-schema-agent@example.test");
        websiteSource = sourceRepository.findByCodeAndActiveTrue("WEBSITE").orElseThrow();
    }

    @Test
    void shouldPersistLeadAndAllPipelineRecords() {
        Lead lead = new Lead("LEAD-001", websiteSource, "Website Prospect");
        lead.setEmail("prospect@example.test");
        lead.setCreatedBy(agent);
        lead.setCurrentAssignee(agent);
        lead.setStatus(LeadPipelineStatus.ASSIGNED);
        lead.setPriority(LeadPriority.HIGH);
        lead.setScore(85);

        lead.addAssignment(new LeadAssignment(agent, agent));
        lead.addNote(new LeadNote(agent, "Prospect asked for a call after work"));

        LeadActivity activity = new LeadActivity(LeadActivityType.CALL, agent);
        activity.setSubject("Qualification call");
        lead.addActivity(activity);

        FollowUpTask task = new FollowUpTask(
                "Send matching listings",
                agent,
                agent,
                Instant.now().plus(1, ChronoUnit.DAYS)
        );
        task.setPriority(LeadPriority.HIGH);
        lead.addFollowUpTask(task);

        Lead saved = leadRepository.saveAndFlush(lead);

        assertThat(leadRepository.findByCodeAndDeletedAtIsNull("LEAD-001")).contains(saved);
        assertThat(assignmentRepository
                .findFirstByLeadIdAndActiveTrueOrderByAssignedAtDesc(saved.getId()))
                .isPresent();
        assertThat(noteRepository.findAllByLeadIdOrderByPinnedDescCreatedAtDesc(
                saved.getId(),
                PageRequest.of(0, 10)
        )).hasSize(1);
        assertThat(activityRepository.findAllByLeadIdOrderByOccurredAtDesc(
                saved.getId(),
                PageRequest.of(0, 10)
        )).hasSize(1);
        assertThat(taskRepository.findAllByLeadIdOrderByDueAtAsc(
                saved.getId(),
                PageRequest.of(0, 10)
        )).hasSize(1);
        assertThat(taskRepository
                .findAllByAssignedToIdAndStatusInAndDueAtBeforeOrderByDueAtAsc(
                        agent.getId(),
                        List.of(FollowUpTaskStatus.PENDING, FollowUpTaskStatus.IN_PROGRESS),
                        Instant.now().plus(2, ChronoUnit.DAYS),
                        PageRequest.of(0, 10)
                ))
                .extracting(FollowUpTask::getTitle)
                .containsExactly("Send matching listings");
    }

    @Test
    void shouldEnforceUniqueLeadCode() {
        Lead first = new Lead("LEAD-UNIQUE", websiteSource, "First Prospect");
        first.setPhone("0900000023");
        leadRepository.saveAndFlush(first);

        Lead duplicate = new Lead("LEAD-UNIQUE", websiteSource, "Duplicate Prospect");
        duplicate.setPhone("0900000024");
        assertThatThrownBy(() -> leadRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceLeadScoreRange() {
        Lead invalidScore = new Lead("LEAD-SCORE", websiteSource, "Scored Prospect");
        invalidScore.setPhone("0900000025");
        invalidScore.setScore(101);
        assertThatThrownBy(() -> leadRepository.saveAndFlush(invalidScore))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldCascadeDeleteLeadOwnedPipelineRecords() {
        Lead lead = new Lead("LEAD-CASCADE", websiteSource, "Cascade Prospect");
        lead.setPhone("0900000026");
        lead.addAssignment(new LeadAssignment(agent, agent));
        lead.addNote(new LeadNote(agent, "Cascade note"));
        lead.addActivity(new LeadActivity(LeadActivityType.EMAIL, agent));
        lead.addFollowUpTask(new FollowUpTask(
                "Cascade task",
                agent,
                agent,
                Instant.now().plus(1, ChronoUnit.DAYS)
        ));
        lead = leadRepository.saveAndFlush(lead);
        Long leadId = lead.getId();

        leadRepository.delete(lead);
        leadRepository.flush();

        assertThat(assignmentRepository.findAllByLeadIdOrderByAssignedAtDesc(leadId)).isEmpty();
        assertThat(noteRepository.findAllByLeadIdOrderByPinnedDescCreatedAtDesc(
                leadId,
                PageRequest.of(0, 10)
        )).isEmpty();
        assertThat(activityRepository.findAllByLeadIdOrderByOccurredAtDesc(
                leadId,
                PageRequest.of(0, 10)
        )).isEmpty();
        assertThat(taskRepository.findAllByLeadIdOrderByDueAtAsc(
                leadId,
                PageRequest.of(0, 10)
        )).isEmpty();
    }

    private User createAgent(String email) {
        Role role = roleRepository.findByCode(RoleCode.AGENT).orElseThrow();
        User user = new User(email, "encoded-password", "Lead Schema Agent");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }
}
