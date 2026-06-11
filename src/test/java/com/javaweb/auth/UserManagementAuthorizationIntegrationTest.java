package com.javaweb.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user_management_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserManagementAuthorizationIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private String customerToken;

    @BeforeEach
    void setUpUsers() throws Exception {
        userRepository.deleteAll();
        admin = createUser("admin-users@example.test", RoleCode.ADMIN);
        manager = createUser("manager-users@example.test", RoleCode.MANAGER);
        customer = createUser("customer-users@example.test", RoleCode.CUSTOMER);
        adminToken = login(admin.getEmail());
        managerToken = login(manager.getEmail());
        customerToken = login(customer.getEmail());
    }

    @Test
    void managerShouldListAndReadUsersWithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "email")
                        .param("direction", "ASC")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false));

        mockMvc.perform(get("/api/v1/admin/users/{id}", customer.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(customer.getId()))
                .andExpect(jsonPath("$.data.email").value(customer.getEmail()))
                .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"));
    }

    @Test
    void managerShouldUpdateNonAdminStatusAndRoles() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", customer.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "LOCKED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));

        mockMvc.perform(put("/api/v1/admin/users/{id}/roles", customer.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roles": ["AGENT", "CUSTOMER"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles.length()").value(2))
                .andExpect(jsonPath("$.data.roles[0]").value("AGENT"))
                .andExpect(jsonPath("$.data.roles[1]").value("CUSTOMER"));

        User updated = userRepository.findWithRolesById(customer.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(updated.getRoles())
                .extracting(Role::getCode)
                .containsExactlyInAnyOrder(RoleCode.AGENT, RoleCode.CUSTOMER);
    }

    @Test
    void managerShouldNotModifyAdminOrAssignAdminRole() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", admin.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "LOCKED"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(put("/api/v1/admin/users/{id}/roles", customer.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roles": ["ADMIN"]}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void adminShouldAssignAdminRole() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/{id}/roles", customer.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roles": ["ADMIN", "CUSTOMER"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.data.roles[1]").value("CUSTOMER"));
    }

    @Test
    void customerShouldNotAccessUserManagementApis() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/status", manager.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "LOCKED"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldValidateRequestsAndReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("sortBy", "passwordHash")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("direction", "SIDEWAYS")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(put("/api/v1/admin/users/{id}/roles", customer.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roles": []}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/admin/users/{id}", Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " User");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/login")
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
