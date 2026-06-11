package com.javaweb.listing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.entity.ListingPackage;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.repository.ListingPackageRepository;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.enums.PropertyPurpose;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:listing_create_update_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ListingCreateUpdateIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingPackageRepository listingPackageRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User agent;
    private User assignedAgent;
    private User manager;
    private User admin;
    private User customer;
    private Property ownedProperty;
    private Property assignedProperty;
    private Property unrelatedProperty;
    private ListingPackage premiumPackage;
    private String agentToken;
    private String assignedAgentToken;
    private String managerToken;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() throws Exception {
        listingRepository.deleteAll();
        listingPackageRepository.deleteAll();
        propertyRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        agent = createUser("listing-create-agent@example.test", RoleCode.AGENT);
        assignedAgent = createUser("listing-assigned-agent@example.test", RoleCode.AGENT);
        manager = createUser("listing-create-manager@example.test", RoleCode.MANAGER);
        admin = createUser("listing-create-admin@example.test", RoleCode.ADMIN);
        customer = createUser("listing-create-customer@example.test", RoleCode.CUSTOMER);

        agentToken = login(agent.getEmail());
        assignedAgentToken = login(assignedAgent.getEmail());
        managerToken = login(manager.getEmail());
        adminToken = login(admin.getEmail());
        customerToken = login(customer.getEmail());

        Province province = provinceRepository.saveAndFlush(
                new Province("P-D18", "Day 18 Province")
        );
        PropertyType propertyType = propertyTypeRepository.findByCode("APARTMENT").orElseThrow();
        ownedProperty = createProperty(
                "PROP-D18-OWNED",
                propertyType,
                province,
                agent,
                agent,
                PropertyPurpose.SALE
        );
        assignedProperty = createProperty(
                "PROP-D18-ASSIGNED",
                propertyType,
                province,
                manager,
                assignedAgent,
                PropertyPurpose.RENT
        );
        unrelatedProperty = createProperty(
                "PROP-D18-UNRELATED",
                propertyType,
                province,
                manager,
                agent,
                PropertyPurpose.SALE
        );

        premiumPackage = new ListingPackage("PREMIUM-D18", "Premium Day 18", 30);
        premiumPackage.setFeatured(true);
        premiumPackage.setPriorityLevel(100);
        premiumPackage = listingPackageRepository.saveAndFlush(premiumPackage);
    }

    @Test
    void agentShouldCreateDraftListingFromOwnedProperty() throws Exception {
        Map<String, Object> request = validCreateRequest(ownedProperty, "listing-d18-001");
        request.put("status", "PUBLISHED");

        MvcResult result = mockMvc.perform(post("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Listing created successfully"))
                .andExpect(jsonPath("$.data.code").value("LISTING-D18-001"))
                .andExpect(jsonPath("$.data.propertyId").value(ownedProperty.getId()))
                .andExpect(jsonPath("$.data.createdById").value(agent.getId()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.data.currency").value("VND"))
                .andExpect(jsonPath("$.data.listingPackageId").value(premiumPackage.getId()))
                .andReturn();

        Listing saved = listingRepository.findWithCoreDetailsById(responseDataId(result)).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ListingStatus.DRAFT);
        assertThat(saved.getProperty().getId()).isEqualTo(ownedProperty.getId());
        assertThat(saved.getCreatedBy().getId()).isEqualTo(agent.getId());
        assertThat(saved.getSubmittedAt()).isNull();
        assertThat(saved.getReviewedAt()).isNull();
        assertThat(saved.getPublishedAt()).isNull();
    }

    @Test
    void assignedAgentShouldCreateAndManagerShouldEditPublicFields() throws Exception {
        Map<String, Object> createRequest = validCreateRequest(
                assignedProperty,
                "LISTING-D18-ASSIGNED"
        );
        createRequest.put("purpose", "RENT");
        Long listingId = createListing(createRequest, assignedAgentToken);

        Map<String, Object> updateRequest = validUpdateRequest(
                "updated-rental-listing-d18",
                "RENT"
        );
        updateRequest.put("title", "Updated rental listing");
        updateRequest.put("description", "Updated public description");
        updateRequest.put("visibility", "INTERNAL");
        updateRequest.put("askingPrice", "28000000.00");
        updateRequest.put("listingPackageId", null);
        updateRequest.put("seoTitle", "Updated SEO title");
        updateRequest.put("propertyId", unrelatedProperty.getId());
        updateRequest.put("code", "LISTING-D18-CHANGED");
        updateRequest.put("status", "PUBLISHED");

        mockMvc.perform(put("/api/v1/listings/{id}", listingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Listing updated successfully"))
                .andExpect(jsonPath("$.data.code").value("LISTING-D18-ASSIGNED"))
                .andExpect(jsonPath("$.data.propertyId").value(assignedProperty.getId()))
                .andExpect(jsonPath("$.data.createdById").value(assignedAgent.getId()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.title").value("Updated rental listing"))
                .andExpect(jsonPath("$.data.slug").value("updated-rental-listing-d18"))
                .andExpect(jsonPath("$.data.visibility").value("INTERNAL"))
                .andExpect(jsonPath("$.data.askingPrice").value(28000000.00))
                .andExpect(jsonPath("$.data.listingPackageId").doesNotExist())
                .andExpect(jsonPath("$.data.seoTitle").value("Updated SEO title"));
    }

    @Test
    void shouldEnforceEndpointAndOwnershipAuthorization() throws Exception {
        mockMvc.perform(post("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedAgentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                validCreateRequest(ownedProperty, "LISTING-D18-FORBIDDEN")
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        Long listingId = createListing(
                validCreateRequest(ownedProperty, "LISTING-D18-OWNER"),
                agentToken
        );
        mockMvc.perform(put("/api/v1/listings/{id}", listingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(assignedAgentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                validUpdateRequest("listing-d18-owner", "SALE")
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                validCreateRequest(ownedProperty, "LISTING-D18-CUSTOMER")
                        )))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                validCreateRequest(ownedProperty, "LISTING-D18-ANONYMOUS")
                        )))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                validCreateRequest(unrelatedProperty, "LISTING-D18-ADMIN")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.createdById").value(admin.getId()));
    }

    @Test
    void shouldRejectInvalidReferencesDuplicatesAndPurposeMismatch() throws Exception {
        Map<String, Object> invalid = validCreateRequest(ownedProperty, "invalid code!");
        invalid.put("title", " ");
        invalid.put("askingPrice", "-1");
        mockMvc.perform(post("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        Map<String, Object> missingProperty = validCreateRequest(
                ownedProperty,
                "LISTING-D18-MISSING"
        );
        missingProperty.put("propertyId", Long.MAX_VALUE);
        mockMvc.perform(post("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(missingProperty)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        Map<String, Object> purposeMismatch = validCreateRequest(
                ownedProperty,
                "LISTING-D18-PURPOSE"
        );
        purposeMismatch.put("purpose", "RENT");
        mockMvc.perform(post("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(purposeMismatch)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        createListing(validCreateRequest(ownedProperty, "LISTING-D18-DUP"), agentToken);
        Map<String, Object> duplicateCode = validCreateRequest(
                unrelatedProperty,
                "listing-d18-dup"
        );
        duplicateCode.put("slug", "another-listing-d18-slug");
        mockMvc.perform(post("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateCode)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        Map<String, Object> duplicateSlug = validCreateRequest(
                unrelatedProperty,
                "LISTING-D18-OTHER"
        );
        duplicateSlug.put("slug", "listing-d18-dup");
        mockMvc.perform(post("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateSlug)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        Long secondListingId = createListing(
                validCreateRequest(unrelatedProperty, "LISTING-D18-UPDATE-DUP"),
                managerToken
        );
        Map<String, Object> duplicateSlugUpdate = validUpdateRequest(
                "listing-d18-dup",
                "SALE"
        );
        mockMvc.perform(put("/api/v1/listings/{id}", secondListingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateSlugUpdate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        premiumPackage.setActive(false);
        listingPackageRepository.saveAndFlush(premiumPackage);
        mockMvc.perform(post("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                validCreateRequest(unrelatedProperty, "LISTING-D18-INACTIVE-PACKAGE")
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldOnlyEditDraftOrRejectedListings() throws Exception {
        Long listingId = createListing(
                validCreateRequest(ownedProperty, "LISTING-D18-PUBLISHED"),
                agentToken
        );
        Listing listing = listingRepository.findById(listingId).orElseThrow();
        listing.setStatus(ListingStatus.PUBLISHED);
        listingRepository.saveAndFlush(listing);

        mockMvc.perform(put("/api/v1/listings/{id}", listingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                validUpdateRequest("listing-d18-published", "SALE")
                        )))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(put("/api/v1/listings/{id}", Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                validUpdateRequest("missing-listing-d18", "SALE")
                        )))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    private Property createProperty(
            String code,
            PropertyType propertyType,
            Province province,
            User creator,
            User assignedAgent,
            PropertyPurpose purpose
    ) {
        Address address = new Address(province, code + " Street");
        Property property = new Property(
                code,
                code + " Name",
                propertyType,
                address,
                creator,
                purpose
        );
        property.setAssignedAgent(assignedAgent);
        return propertyRepository.saveAndFlush(property);
    }

    private Long createListing(Map<String, Object> request, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/listings")
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

    private Map<String, Object> validCreateRequest(Property property, String code) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("propertyId", property.getId());
        request.put("code", code);
        request.put("title", "Day 18 Listing");
        request.put("slug", code.toLowerCase());
        request.put("description", "Listing create/update integration test");
        request.put("purpose", property.getPurpose().name());
        request.put("visibility", "PUBLIC");
        request.put("askingPrice", "5200000000.00");
        request.put("currency", "vnd");
        request.put("listingPackageId", premiumPackage.getId());
        request.put("seoTitle", "Day 18 SEO title");
        request.put("seoDescription", "Day 18 SEO description");
        request.put("seoKeywords", "day18,listing");
        return request;
    }

    private Map<String, Object> validUpdateRequest(String slug, String purpose) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", "Updated Day 18 Listing");
        request.put("slug", slug);
        request.put("description", "Updated listing description");
        request.put("purpose", purpose);
        request.put("visibility", "PUBLIC");
        request.put("askingPrice", "5300000000.00");
        request.put("currency", "vnd");
        request.put("listingPackageId", premiumPackage.getId());
        request.put("seoTitle", "Updated SEO title");
        request.put("seoDescription", "Updated SEO description");
        request.put("seoKeywords", "updated,listing");
        return request;
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Day 18 User");
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
