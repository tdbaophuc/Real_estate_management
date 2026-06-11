package com.javaweb.lead;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:lead_management_day24_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LeadManagementIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User agent;
    private User secondAgent;
    private User manager;
    private User admin;
    private User customer;
    private String agentToken;
    private String secondAgentToken;
    private String managerToken;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        leadRepository.deleteAll();
        userRepository.deleteAll();

        agent = createUser("day24-agent@example.test", RoleCode.AGENT);
        secondAgent = createUser("day24-second-agent@example.test", RoleCode.AGENT);
        manager = createUser("day24-manager@example.test", RoleCode.MANAGER);
        admin = createUser("day24-admin@example.test", RoleCode.ADMIN);
        customer = createUser("day24-customer@example.test", RoleCode.CUSTOMER);
        agentToken = login(agent.getEmail());
        secondAgentToken = login(secondAgent.getEmail());
        managerToken = login(manager.getEmail());
        adminToken = login(admin.getEmail());
        customerToken = login(customer.getEmail());
    }

    @Test
    void managerShouldCreateListDetailAndAssignLead() throws Exception {
        Map<String, Object> request = validRequest("LEAD-D24-MANAGER");
        request.put("assignedAgentId", agent.getId());
        Long leadId = createLead(request, managerToken);

        mockMvc.perform(get("/api/v1/leads")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("status", "ASSIGNED")
                        .queryParam("assignedAgentId", agent.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(leadId))
                .andExpect(jsonPath("$.data.content[0].sourceCode").value("WEBSITE"));

        mockMvc.perform(get("/api/v1/leads/{id}", leadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lead.code").value("LEAD-D24-MANAGER"))
                .andExpect(jsonPath("$.data.assignments.length()").value(1))
                .andExpect(jsonPath("$.data.activities.length()").value(1))
                .andExpect(jsonPath("$.data.followUpTasks.length()").value(0));

        mockMvc.perform(patch("/api/v1/leads/{id}/assign", leadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "agentId", secondAgent.getId(),
                                "notes", "Rebalanced by admin"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedToId").value(secondAgent.getId()))
                .andExpect(jsonPath("$.data.active").value(true));

        mockMvc.perform(get("/api/v1/leads/{id}", leadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lead.assignedAgentId")
                        .value(secondAgent.getId()))
                .andExpect(jsonPath("$.data.assignments.length()").value(2))
                .andExpect(jsonPath("$.data.assignments[0].active").value(true))
                .andExpect(jsonPath("$.data.assignments[1].active").value(false))
                .andExpect(jsonPath("$.data.activities.length()").value(2));
    }

    @Test
    void shouldEnforceAgentVisibilityAndRoleAuthorization() throws Exception {
        Long ownLeadId = createLead(validRequest("LEAD-D24-OWN"), agentToken);
        Lead ownLead = leadRepository.findById(ownLeadId).orElseThrow();
        assertThat(ownLead.getCurrentAssignee().getId()).isEqualTo(agent.getId());
        assertThat(ownLead.getStatus().name()).isEqualTo("ASSIGNED");

        Map<String, Object> otherRequest = validRequest("LEAD-D24-OTHER");
        otherRequest.put("assignedAgentId", secondAgent.getId());
        Long otherLeadId = createLead(otherRequest, managerToken);

        mockMvc.perform(get("/api/v1/leads")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(ownLeadId));

        mockMvc.perform(get("/api/v1/leads/{id}", otherLeadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/leads/{id}/assign", ownLeadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "agentId", secondAgent.getId()
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/leads")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                validRequest("LEAD-D24-FORBIDDEN")
                        )))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/leads"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void agentShouldProgressLeadAndAddCareRecords() throws Exception {
        Long leadId = createLead(validRequest("LEAD-D24-CARE"), agentToken);

        updateStatus(leadId, "CONTACTED", null, agentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONTACTED"))
                .andExpect(jsonPath("$.data.lastContactedAt").isNotEmpty());

        mockMvc.perform(post("/api/v1/leads/{id}/notes", leadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "Prospect prefers a weekend viewing",
                                "pinned", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.authorId").value(agent.getId()))
                .andExpect(jsonPath("$.data.pinned").value(true));

        mockMvc.perform(post("/api/v1/leads/{id}/activities", leadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "activityType", "CALL",
                                "subject", "Qualification call",
                                "details", "Confirmed budget and preferred district"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.activityType").value("CALL"))
                .andExpect(jsonPath("$.data.actorId").value(agent.getId()));

        mockMvc.perform(post("/api/v1/leads/{id}/follow-up-tasks", leadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Send matching listings",
                                "description", "Prepare three suitable listings",
                                "priority", "HIGH",
                                "dueAt", Instant.now().plus(2, ChronoUnit.DAYS).toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.assignedToId").value(agent.getId()));

        mockMvc.perform(get("/api/v1/leads/{id}", leadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notes.length()").value(1))
                .andExpect(jsonPath("$.data.followUpTasks.length()").value(1))
                .andExpect(jsonPath("$.data.activities.length()").value(3));
    }

    @Test
    void shouldValidateLeadWorkflowBusinessRules() throws Exception {
        Long leadId = createLead(validRequest("LEAD-D24-RULES"), agentToken);
        Long unassignedLeadId = createLead(
                validRequest("LEAD-D24-UNASSIGNED"),
                managerToken
        );

        mockMvc.perform(post("/api/v1/leads")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                validRequest("LEAD-D24-RULES")
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        updateStatus(leadId, "CLOSED_WON", null, agentToken)
                .andExpect(status().isUnprocessableEntity());

        updateStatus(unassignedLeadId, "ASSIGNED", null, managerToken)
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/v1/leads/{id}/assign", leadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "agentId", customer.getId()
                        ))))
                .andExpect(status().isUnprocessableEntity());

        updateStatus(
                unassignedLeadId,
                "INVALID",
                "Duplicate website submission",
                managerToken
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INVALID"));

        updateStatus(leadId, "CONTACTED", null, agentToken)
                .andExpect(status().isOk());

        updateStatus(leadId, "CLOSED_LOST", null, agentToken)
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/leads/{id}/activities", leadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "activityType", "STATUS_CHANGE",
                                "subject", "Forged audit event"
                        ))))
                .andExpect(status().isUnprocessableEntity());

        updateStatus(leadId, "CLOSED_LOST", "Budget no longer available", agentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED_LOST"))
                .andExpect(jsonPath("$.data.lostReason")
                        .value("Budget no longer available"));

        mockMvc.perform(post("/api/v1/leads/{id}/follow-up-tasks", leadId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Invalid terminal task",
                                "priority", "MEDIUM",
                                "dueAt", Instant.now().plus(1, ChronoUnit.DAYS).toString()
                        ))))
                .andExpect(status().isUnprocessableEntity());

        Map<String, Object> invalidContact = validRequest("LEAD-D24-CONTACT");
        invalidContact.remove("email");
        invalidContact.remove("phone");
        mockMvc.perform(post("/api/v1/leads")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidContact)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/leads")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("sortBy", "deletedAt"))
                .andExpect(status().isUnprocessableEntity());
    }

    private org.springframework.test.web.servlet.ResultActions updateStatus(
            Long leadId,
            String status,
            String reason,
            String token
    ) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("status", status);
        if (reason != null) {
            request.put("reason", reason);
        }
        return mockMvc.perform(patch("/api/v1/leads/{id}/status", leadId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private Long createLead(Map<String, Object> request, String token) throws Exception {
        String response = mockMvc.perform(post("/api/v1/leads")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private Map<String, Object> validRequest(String code) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", code);
        request.put("sourceCode", "WEBSITE");
        request.put("fullName", code + " Prospect");
        request.put("email", code.toLowerCase() + "@example.test");
        request.put("phone", "0900000024");
        request.put("priority", "MEDIUM");
        request.put("message", "Day 24 lead");
        return request;
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Day 24 User");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }

    private String login(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
