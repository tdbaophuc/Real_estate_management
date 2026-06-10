package com.javaweb.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import com.javaweb.property.repository.DistrictRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import com.javaweb.property.repository.WardRepository;
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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:customer_management_day22_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerManagementIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User agent;
    private User secondAgent;
    private User manager;
    private User admin;
    private User customerUser;
    private String agentToken;
    private String secondAgentToken;
    private String managerToken;
    private String adminToken;
    private String customerToken;
    private Province province;
    private District district;
    private Ward ward;

    @BeforeEach
    void setUp() throws Exception {
        customerRepository.deleteAll();
        wardRepository.deleteAll();
        districtRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        agent = createUser("day22-agent@example.test", RoleCode.AGENT);
        secondAgent = createUser("day22-second-agent@example.test", RoleCode.AGENT);
        manager = createUser("day22-manager@example.test", RoleCode.MANAGER);
        admin = createUser("day22-admin@example.test", RoleCode.ADMIN);
        customerUser = createUser("day22-customer@example.test", RoleCode.CUSTOMER);
        agentToken = login(agent.getEmail());
        secondAgentToken = login(secondAgent.getEmail());
        managerToken = login(manager.getEmail());
        adminToken = login(admin.getEmail());
        customerToken = login(customerUser.getEmail());

        province = new Province("P-D22", "Day 22 Province");
        district = new District(province, "D-D22", "Day 22 District");
        ward = new Ward(district, "W-D22", "Day 22 Ward");
        province.addDistrict(district);
        district.addWard(ward);
        provinceRepository.saveAndFlush(province);
    }

    @Test
    void managerShouldCreateReadUpdateAndSoftDeleteCustomer() throws Exception {
        Map<String, Object> request = validRequest("CUS-D22-MANAGER");
        request.put("userId", customerUser.getId());
        request.put("assignedAgentId", agent.getId());

        Long customerId = createCustomer(request, managerToken);

        mockMvc.perform(get("/api/v1/customers/{id}", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customer.code").value("CUS-D22-MANAGER"))
                .andExpect(jsonPath("$.data.customer.userId").value(customerUser.getId()))
                .andExpect(jsonPath("$.data.customer.assignedAgentId").value(agent.getId()))
                .andExpect(jsonPath("$.data.requirements.length()").value(0))
                .andExpect(jsonPath("$.data.notes.length()").value(0));

        request.put("fullName", "Updated Customer Name");
        request.put("priority", "HIGH");
        request.put("assignedAgentId", secondAgent.getId());
        mockMvc.perform(put("/api/v1/customers/{id}", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated Customer Name"))
                .andExpect(jsonPath("$.data.priority").value("HIGH"))
                .andExpect(jsonPath("$.data.assignedAgentId").value(secondAgent.getId()));

        mockMvc.perform(delete("/api/v1/customers/{id}", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk());

        Customer deleted = customerRepository.findById(customerId).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.getStatus().name()).isEqualTo("ARCHIVED");

        mockMvc.perform(get("/api/v1/customers/{id}", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldEnforceAgentVisibilityAndRoleAuthorization() throws Exception {
        Long ownCustomerId = createCustomer(validRequest("CUS-D22-AGENT"), agentToken);
        Customer ownCustomer = customerRepository.findById(ownCustomerId).orElseThrow();
        assertThat(ownCustomer.getAssignedAgent().getId()).isEqualTo(agent.getId());

        Map<String, Object> managedRequest = validRequest("CUS-D22-OTHER");
        managedRequest.put("assignedAgentId", secondAgent.getId());
        Long otherCustomerId = createCustomer(managedRequest, managerToken);

        mockMvc.perform(get("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(ownCustomerId));

        mockMvc.perform(get("/api/v1/customers/{id}", otherCustomerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                validRequest("CUS-D22-FORBIDDEN")
                        )))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAddNotesRequirementsAndBuildTimeline() throws Exception {
        Long customerId = createCustomer(validRequest("CUS-D22-TIMELINE"), agentToken);

        mockMvc.perform(post("/api/v1/customers/{id}/notes", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", "Customer requested a weekend call",
                                "pinned", true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.authorId").value(agent.getId()))
                .andExpect(jsonPath("$.data.pinned").value(true));

        Map<String, Object> requirement = new LinkedHashMap<>();
        requirement.put("purpose", "SALE");
        requirement.put(
                "propertyTypeId",
                propertyTypeRepository.findByCode("APARTMENT").orElseThrow().getId()
        );
        requirement.put("provinceId", province.getId());
        requirement.put("districtId", district.getId());
        requirement.put("wardId", ward.getId());
        requirement.put("minBudget", 2_000_000_000L);
        requirement.put("maxBudget", 5_000_000_000L);
        requirement.put("currency", "vnd");
        requirement.put("minArea", 70);
        requirement.put("maxArea", 120);
        requirement.put("minBedrooms", 2);
        requirement.put("minBathrooms", 2);
        requirement.put("description", "Near the city center");

        mockMvc.perform(post("/api/v1/customers/{id}/requirements", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requirement)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.purpose").value("SALE"))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.wardName").value("Day 22 Ward"));

        mockMvc.perform(get("/api/v1/customers/{id}", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notes.length()").value(1))
                .andExpect(jsonPath("$.data.requirements.length()").value(1));

        mockMvc.perform(get("/api/v1/customers/{id}/timeline", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.type == 'CUSTOMER_CREATED')]").exists())
                .andExpect(jsonPath("$.data[?(@.type == 'NOTE_ADDED')]").exists())
                .andExpect(jsonPath("$.data[?(@.type == 'REQUIREMENT_ADDED')]").exists());
    }

    @Test
    void shouldValidateCustomerAndRequirementBusinessRules() throws Exception {
        Map<String, Object> linked = validRequest("CUS-D22-LINKED");
        linked.put("userId", customerUser.getId());
        createCustomer(linked, managerToken);

        Map<String, Object> duplicateLink = validRequest("CUS-D22-DUP-LINK");
        duplicateLink.put("userId", customerUser.getId());
        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateLink)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        Map<String, Object> agentLink = validRequest("CUS-D22-AGENT-LINK");
        agentLink.put("userId", customerUser.getId());
        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(agentLink)))
                .andExpect(status().isForbidden());

        Long customerId = createCustomer(validRequest("CUS-D22-VALIDATE"), managerToken);
        mockMvc.perform(post("/api/v1/customers/{id}/notes", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", " ",
                                "pinned", false
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/customers/{id}/requirements", customerId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "purpose": "SALE",
                                  "minBudget": 500,
                                  "maxBudget": 100,
                                  "currency": "VND"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("sortBy", "deletedAt"))
                .andExpect(status().isUnprocessableEntity());
    }

    private Long createCustomer(Map<String, Object> request, String token) throws Exception {
        String response = mockMvc.perform(post("/api/v1/customers")
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
        request.put("fullName", code + " Customer");
        request.put("email", code.toLowerCase() + "@example.test");
        request.put("phone", "0900000022");
        request.put("status", "ACTIVE");
        request.put("source", "MANUAL");
        request.put("priority", "MEDIUM");
        request.put("preferredContactMethod", "EMAIL");
        request.put("notes", "Day 22 customer");
        return request;
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Day 22 User");
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
