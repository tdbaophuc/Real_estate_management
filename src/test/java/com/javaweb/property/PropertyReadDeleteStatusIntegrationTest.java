package com.javaweb.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.enums.PropertyStatus;
import com.javaweb.property.repository.PropertyRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:property_read_delete_status_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropertyReadDeleteStatusIntegrationTest {
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
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User agent;
    private User secondAgent;
    private User manager;
    private User customer;
    private Province province;
    private PropertyType propertyType;
    private String agentToken;
    private String secondAgentToken;
    private String managerToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        propertyRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        agent = createUser("agent-day12@example.test", RoleCode.AGENT);
        secondAgent = createUser("second-agent-day12@example.test", RoleCode.AGENT);
        manager = createUser("manager-day12@example.test", RoleCode.MANAGER);
        customer = createUser("customer-day12@example.test", RoleCode.CUSTOMER);

        agentToken = login(agent.getEmail());
        secondAgentToken = login(secondAgent.getEmail());
        managerToken = login(manager.getEmail());
        customerToken = login(customer.getEmail());

        province = provinceRepository.saveAndFlush(new Province("P-D12", "Day 12 Province"));
        propertyType = propertyTypeRepository.findByCode("APARTMENT").orElseThrow();
    }

    @Test
    void managerShouldListWithPaginationAndGetDetails() throws Exception {
        createProperty("PROP-D12-C", manager, null);
        Property first = createProperty("PROP-D12-A", agent, agent);
        createProperty("PROP-D12-B", secondAgent, secondAgent);

        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("page", "0")
                        .queryParam("size", "2")
                        .queryParam("sortBy", "code")
                        .queryParam("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].code").value("PROP-D12-A"))
                .andExpect(jsonPath("$.data.content[1].code").value("PROP-D12-B"));

        mockMvc.perform(get("/api/v1/properties/{id}", first.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(first.getId()))
                .andExpect(jsonPath("$.data.propertyTypeCode").value("APARTMENT"))
                .andExpect(jsonPath("$.data.address.provinceId").value(province.getId()))
                .andExpect(jsonPath("$.data.amenities.length()").value(0));

        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("sortBy", "description"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void agentShouldOnlyReadCreatedOrAssignedProperties() throws Exception {
        Property created = createProperty("PROP-D12-CREATED", agent, agent);
        Property assigned = createProperty("PROP-D12-ASSIGNED", manager, agent);
        Property unrelated = createProperty("PROP-D12-OTHER", manager, secondAgent);

        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(get("/api/v1/properties/{id}", created.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/properties/{id}", assigned.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/properties/{id}", unrelated.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/properties"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteShouldSoftDeleteAndHideProperty() throws Exception {
        Property property = createProperty("PROP-D12-DELETE", agent, agent);

        mockMvc.perform(delete("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondAgentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Property deleted successfully"));

        Property deleted = propertyRepository.findById(property.getId()).orElseThrow();
        assertThat(deleted.getStatus()).isEqualTo(PropertyStatus.DELETED);
        assertThat(deleted.getDeletedAt()).isNotNull();

        mockMvc.perform(get("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(delete("/api/v1/properties/{id}", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void statusUpdateShouldEnforceTransitionsAndOwnership() throws Exception {
        Property property = createProperty("PROP-D12-STATUS", agent, agent);

        mockMvc.perform(patch("/api/v1/properties/{id}/status", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "AVAILABLE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Property status updated successfully"))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));

        mockMvc.perform(patch("/api/v1/properties/{id}/status", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DRAFT"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(patch("/api/v1/properties/{id}/status", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondAgentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RESERVED"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/properties/{id}/status", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RESERVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESERVED"));

        mockMvc.perform(patch("/api/v1/properties/{id}/status", property.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "DELETED"))))
                .andExpect(status().isUnprocessableEntity());
    }

    private Property createProperty(String code, User creator, User assignedAgent) {
        Address address = new Address(province, code + " Street");
        Property property = new Property(
                code,
                code + " Name",
                propertyType,
                address,
                creator,
                PropertyPurpose.SALE
        );
        property.setAssignedAgent(assignedAgent);
        return propertyRepository.saveAndFlush(property);
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Day 12 User");
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
