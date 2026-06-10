package com.javaweb.property;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import com.javaweb.property.repository.AmenityRepository;
import com.javaweb.property.repository.DistrictRepository;
import com.javaweb.property.repository.PropertyRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:property_create_update_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropertyCreateUpdateIntegrationTest {
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
    private AmenityRepository amenityRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User agent;
    private User secondAgent;
    private User manager;
    private User admin;
    private User customer;
    private User owner;
    private Province province;
    private District district;
    private Ward ward;
    private String agentToken;
    private String secondAgentToken;
    private String managerToken;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        propertyRepository.deleteAll();
        wardRepository.deleteAll();
        districtRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        agent = createUser("agent-property@example.test", RoleCode.AGENT);
        secondAgent = createUser("second-agent-property@example.test", RoleCode.AGENT);
        manager = createUser("manager-property@example.test", RoleCode.MANAGER);
        admin = createUser("admin-property@example.test", RoleCode.ADMIN);
        customer = createUser("customer-property@example.test", RoleCode.CUSTOMER);
        owner = createUser("owner-property@example.test", RoleCode.OWNER);

        agentToken = login(agent.getEmail());
        secondAgentToken = login(secondAgent.getEmail());
        managerToken = login(manager.getEmail());
        adminToken = login(admin.getEmail());
        customerToken = login(customer.getEmail());

        province = new Province("P-D11", "Day 11 Province");
        district = new District(province, "D-D11", "Day 11 District");
        ward = new Ward(district, "W-D11", "Day 11 Ward");
        province.addDistrict(district);
        district.addWard(ward);
        provinceRepository.saveAndFlush(province);
    }

    @Test
    void agentShouldCreateDraftPropertyAssignedToSelf() throws Exception {
        Map<String, Object> request = validRequest("prop-day11-001");

        MvcResult result = mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Property created successfully"))
                .andExpect(jsonPath("$.data.code").value("PROP-DAY11-001"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.createdById").value(agent.getId()))
                .andExpect(jsonPath("$.data.assignedAgentId").value(agent.getId()))
                .andExpect(jsonPath("$.data.ownerId").value(owner.getId()))
                .andExpect(jsonPath("$.data.address.wardId").value(ward.getId()))
                .andExpect(jsonPath("$.data.amenities.length()").value(1))
                .andReturn();

        Long propertyId = responseDataId(result);
        Property saved = propertyRepository.findWithUpdateDetailsById(propertyId).orElseThrow();
        assertThat(saved.getAssignedAgent().getId()).isEqualTo(agent.getId());
        assertThat(saved.getAmenities()).hasSize(1);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void managerShouldCreateAndUpdatePropertyWithAssignmentAndAmenities() throws Exception {
        Map<String, Object> createRequest = validRequest("PROP-DAY11-002");
        createRequest.put("assignedAgentId", secondAgent.getId());
        Long propertyId = createProperty(createRequest, managerToken);

        Map<String, Object> updateRequest = validRequest("PROP-DAY11-002-UPDATED");
        updateRequest.put("name", "Updated house");
        updateRequest.put(
                "propertyTypeId",
                propertyTypeRepository.findByCode("HOUSE").orElseThrow().getId()
        );
        updateRequest.put("purpose", "RENT");
        updateRequest.put("price", "25000000.00");
        updateRequest.put("assignedAgentId", agent.getId());
        updateRequest.put("amenities", List.of(Map.of(
                "amenityId", amenityRepository.findByCode("GYM").orElseThrow().getId(),
                "details", "Private gym"
        )));

        mockMvc.perform(put("/api/v1/properties/{id}", propertyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Property updated successfully"))
                .andExpect(jsonPath("$.data.code").value("PROP-DAY11-002-UPDATED"))
                .andExpect(jsonPath("$.data.name").value("Updated house"))
                .andExpect(jsonPath("$.data.propertyTypeCode").value("HOUSE"))
                .andExpect(jsonPath("$.data.purpose").value("RENT"))
                .andExpect(jsonPath("$.data.assignedAgentId").value(agent.getId()))
                .andExpect(jsonPath("$.data.amenities.length()").value(1))
                .andExpect(jsonPath("$.data.amenities[0].code").value("GYM"))
                .andExpect(jsonPath("$.data.amenities[0].details").value("Private gym"));

        Property updated = propertyRepository.findWithUpdateDetailsById(propertyId).orElseThrow();
        assertThat(updated.getAmenities())
                .singleElement()
                .satisfies(link -> assertThat(link.getAmenity().getCode()).isEqualTo("GYM"));
    }

    @Test
    void adminShouldUpdatePropertyCreatedByAgent() throws Exception {
        Long propertyId = createProperty(validRequest("PROP-DAY11-ADMIN"), agentToken);
        Map<String, Object> updateRequest = validRequest("PROP-DAY11-ADMIN-UPDATED");
        updateRequest.put("assignedAgentId", secondAgent.getId());

        mockMvc.perform(put("/api/v1/properties/{id}", propertyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("PROP-DAY11-ADMIN-UPDATED"))
                .andExpect(jsonPath("$.data.assignedAgentId").value(secondAgent.getId()));
    }

    @Test
    void shouldEnforceEndpointAndOwnershipAuthorization() throws Exception {
        Long propertyId = createProperty(validRequest("PROP-DAY11-003"), agentToken);

        mockMvc.perform(put("/api/v1/properties/{id}", propertyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondAgentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("PROP-DAY11-003"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        Map<String, Object> assignOtherAgent = validRequest("PROP-DAY11-004");
        assignOtherAgent.put("assignedAgentId", secondAgent.getId());
        mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignOtherAgent)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("PROP-DAY11-005"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("PROP-DAY11-006"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldRejectInvalidRequestsAndBusinessReferences() throws Exception {
        Map<String, Object> invalid = validRequest("invalid code!");
        invalid.put("name", " ");
        invalid.put("price", "-1");
        mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        createProperty(validRequest("PROP-DAY11-DUP"), managerToken);
        mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("prop-day11-dup"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        Long secondPropertyId = createProperty(validRequest("PROP-DAY11-UPDATE-DUP"), managerToken);
        mockMvc.perform(put("/api/v1/properties/{id}", secondPropertyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("PROP-DAY11-DUP"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        mockMvc.perform(put("/api/v1/properties/{id}", Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("PROP-DAY11-MISSING"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        Map<String, Object> duplicateAmenities = validRequest("PROP-DAY11-007");
        Long parkingId = amenityRepository.findByCode("PARKING").orElseThrow().getId();
        duplicateAmenities.put("amenities", List.of(
                Map.of("amenityId", parkingId),
                Map.of("amenityId", parkingId)
        ));
        mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateAmenities)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        Province anotherProvince = provinceRepository.saveAndFlush(
                new Province("P-D11-OTHER", "Other Province")
        );
        Map<String, Object> invalidHierarchy = validRequest("PROP-DAY11-008");
        addressOf(invalidHierarchy).put("provinceId", anotherProvince.getId());
        mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidHierarchy)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        Map<String, Object> invalidOwner = validRequest("PROP-DAY11-009");
        invalidOwner.put("ownerId", customer.getId());
        mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidOwner)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    private Long createProperty(Map<String, Object> request, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return responseDataId(result);
    }

    private Long responseDataId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.at("/data/id").longValue();
    }

    private Map<String, Object> validRequest(String code) {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("provinceId", province.getId());
        address.put("districtId", district.getId());
        address.put("wardId", ward.getId());
        address.put("streetAddress", "12 Day 11 Street");
        address.put("fullAddress", "12 Day 11 Street, Day 11 Ward");
        address.put("latitude", "10.1234567");
        address.put("longitude", "106.1234567");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", code);
        request.put("name", "Day 11 Property");
        request.put("description", "Property create/update integration test");
        request.put(
                "propertyTypeId",
                propertyTypeRepository.findByCode("APARTMENT").orElseThrow().getId()
        );
        request.put("purpose", "SALE");
        request.put("price", "5000000000.00");
        request.put("currency", "vnd");
        request.put("landArea", "90.50");
        request.put("floorArea", "82.00");
        request.put("bedrooms", 3);
        request.put("bathrooms", 2);
        request.put("floors", 1);
        request.put("direction", "SOUTHEAST");
        request.put("legalStatus", "PINK_BOOK");
        request.put("furnitureStatus", "FULLY_FURNISHED");
        request.put("videoUrl", "https://example.test/video");
        request.put("virtualTourUrl", "https://example.test/tour");
        request.put("availableFrom", "2026-07-01");
        request.put("ownerId", owner.getId());
        request.put("assignedAgentId", null);
        request.put("address", address);
        request.put("amenities", List.of(Map.of(
                "amenityId",
                amenityRepository.findByCode("PARKING").orElseThrow().getId(),
                "details",
                "One parking slot"
        )));
        return request;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> addressOf(Map<String, Object> request) {
        return (Map<String, Object>) request.get("address");
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Property User");
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
