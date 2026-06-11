package com.javaweb.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.audit.entity.AuditLog;
import com.javaweb.audit.repository.AuditLogRepository;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:audit_log_day42_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User manager;
    private User customer;
    private String adminToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        admin = createUser("audit-admin@example.test", RoleCode.ADMIN);
        manager = createUser("audit-manager@example.test", RoleCode.MANAGER);
        customer = createUser("audit-customer@example.test", RoleCode.CUSTOMER);
        adminToken = login(admin.getEmail());
        managerToken = login(manager.getEmail());
    }

    @Test
    void shouldRecordUserChangesAndAllowAdminSearchAndDetail() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", customer.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOCKED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/admin/users/{id}/roles", customer.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roles\":[\"AGENT\",\"CUSTOMER\"]}"))
                .andExpect(status().isOk());

        List<AuditLog> statusLogs =
                auditLogRepository
                        .findAllByActionAndResourceTypeAndResourceIdOrderByCreatedAtDesc(
                                AuditActions.USER_STATUS_CHANGED,
                                AuditActions.USER,
                                customer.getId()
                        );
        assertThat(statusLogs).hasSize(1);
        assertThat(statusLogs.getFirst().getActor().getId())
                .isEqualTo(manager.getId());
        assertThat(statusLogs.getFirst().getOldValueJson())
                .contains("\"status\":\"ACTIVE\"");
        assertThat(statusLogs.getFirst().getNewValueJson())
                .contains("\"status\":\"LOCKED\"");

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("action", "user_roles_changed")
                        .param("resourceType", "user")
                        .param("resourceId", customer.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].actorId")
                        .value(manager.getId()))
                .andExpect(jsonPath("$.data.content[0].action")
                        .value(AuditActions.USER_ROLES_CHANGED))
                .andExpect(jsonPath("$.data.content[0].oldValue.roles[0]")
                        .value("CUSTOMER"))
                .andExpect(jsonPath("$.data.content[0].newValue.roles[0]")
                        .value("AGENT"))
                .andExpect(jsonPath("$.data.content[0].newValue.roles[1]")
                        .value("CUSTOMER"));

        Long auditLogId = auditLogRepository.findAll().stream()
                .filter(log -> log.getAction().equals(
                        AuditActions.USER_STATUS_CHANGED
                ))
                .findFirst()
                .orElseThrow()
                .getId();
        mockMvc.perform(get("/api/v1/audit-logs/{id}", auditLogId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(auditLogId))
                .andExpect(jsonPath("$.data.resourceId").value(customer.getId()))
                .andExpect(jsonPath("$.data.oldValue.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.newValue.status").value("LOCKED"));
    }

    @Test
    void shouldRestrictAuditApiToAdminAndValidateSearch() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("from", "2026-06-12T00:00:00Z")
                        .param("to", "2026-06-11T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code")
                        .value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(get("/api/v1/audit-logs/{id}", Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(
                email,
                passwordEncoder.encode(PASSWORD),
                roleCode + " Audit User"
        );
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).at("/data/accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
