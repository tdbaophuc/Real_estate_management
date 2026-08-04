package com.javaweb.listing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.audit.AuditActions;
import com.javaweb.audit.repository.AuditLogRepository;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.entity.ListingStatusHistory;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.listing.repository.ListingStatusHistoryRepository;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:listing_review_workflow_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ListingReviewWorkflowIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

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
    private User secondAgent;
    private User manager;
    private User admin;
    private Property property;
    private String agentToken;
    private String secondAgentToken;
    private String managerToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        auditLogRepository.deleteAll();
        listingRepository.deleteAll();
        propertyRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        agent = createUser("listing-review-agent@example.test", RoleCode.AGENT);
        secondAgent = createUser("listing-review-second-agent@example.test", RoleCode.AGENT);
        manager = createUser("listing-review-manager@example.test", RoleCode.MANAGER);
        admin = createUser("listing-review-admin@example.test", RoleCode.ADMIN);

        agentToken = login(agent.getEmail());
        secondAgentToken = login(secondAgent.getEmail());
        managerToken = login(manager.getEmail());
        adminToken = login(admin.getEmail());

        Province province = provinceRepository.saveAndFlush(
                new Province("P-D19", "Day 19 Province")
        );
        PropertyType propertyType = propertyTypeRepository.findByCode("APARTMENT").orElseThrow();
        Address address = new Address(province, "19 Review Street");
        property = new Property(
                "PROP-D19",
                "Day 19 Property",
                propertyType,
                address,
                agent,
                PropertyPurpose.SALE
        );
        property.setAssignedAgent(agent);
        property = propertyRepository.saveAndFlush(property);
    }

    @Test
    void shouldCompleteApprovePublishAndUnpublishWorkflowWithHistory() throws Exception {
        Listing listing = createListing("LISTING-D19-APPROVE", agent);

        mockMvc.perform(patch("/api/v1/listings/{id}/submit", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Listing submitted for review successfully"))
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());

        mockMvc.perform(patch("/api/v1/listings/{id}/approve", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Listing approved successfully"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewedById").value(manager.getId()))
                .andExpect(jsonPath("$.data.reviewedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.rejectionReason").doesNotExist());

        assertThat(auditLogRepository
                .findAllByActionAndResourceTypeAndResourceIdOrderByCreatedAtDesc(
                        AuditActions.LISTING_APPROVED,
                        AuditActions.LISTING,
                        listing.getId()
                )).hasSize(1);

        mockMvc.perform(patch("/api/v1/listings/{id}/publish", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.publishedAt").isNotEmpty());

        mockMvc.perform(patch("/api/v1/listings/{id}/unpublish", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNPUBLISHED"))
                .andExpect(jsonPath("$.data.unpublishedAt").isNotEmpty());

        List<ListingStatusHistory> history =
                statusHistoryRepository.findAllByListingIdOrderByCreatedAtDesc(listing.getId());
        assertThat(history)
                .extracting(ListingStatusHistory::getToStatus)
                .containsExactly(
                        ListingStatus.UNPUBLISHED,
                        ListingStatus.PUBLISHED,
                        ListingStatus.APPROVED,
                        ListingStatus.PENDING_REVIEW
                );
        assertThat(history)
                .extracting(item -> item.getChangedBy().getId())
                .containsExactly(agent.getId(), agent.getId(), manager.getId(), agent.getId());
    }

    @Test
    void shouldPersistRejectionReasonAndAllowResubmission() throws Exception {
        Listing listing = createListing("LISTING-D19-REJECT", agent);
        submit(listing, agentToken);

        mockMvc.perform(patch("/api/v1/listings/{id}/reject", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason",
                                "Description requires legal clarification"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Listing rejected successfully"))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.reviewedById").value(admin.getId()))
                .andExpect(jsonPath("$.data.rejectionReason")
                        .value("Description requires legal clarification"));

        Listing rejected = listingRepository.findById(listing.getId()).orElseThrow();
        assertThat(rejected.getRejectionReason())
                .isEqualTo("Description requires legal clarification");

        List<ListingStatusHistory> rejectedHistory =
                statusHistoryRepository.findAllByListingIdOrderByCreatedAtDesc(listing.getId());
        assertThat(rejectedHistory.getFirst().getReason())
                .isEqualTo("Description requires legal clarification");
        assertThat(rejectedHistory.getFirst().getChangedBy().getId()).isEqualTo(admin.getId());
        assertThat(auditLogRepository
                .findAllByActionAndResourceTypeAndResourceIdOrderByCreatedAtDesc(
                        AuditActions.LISTING_REJECTED,
                        AuditActions.LISTING,
                        listing.getId()
                )).singleElement()
                .satisfies(log -> {
                    assertThat(log.getOldValueJson()).contains("PENDING_REVIEW");
                    assertThat(log.getNewValueJson())
                            .contains("REJECTED")
                            .contains("Description requires legal clarification");
                });

        mockMvc.perform(patch("/api/v1/listings/{id}/submit", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.reviewedById").doesNotExist())
                .andExpect(jsonPath("$.data.reviewedAt").doesNotExist())
                .andExpect(jsonPath("$.data.rejectionReason").doesNotExist());
    }

    @Test
    void shouldRestrictReviewActionsToManagerAndAdmin() throws Exception {
        Listing listing = createListing("LISTING-D19-REVIEW-AUTH", agent);
        submit(listing, agentToken);

        mockMvc.perform(patch("/api/v1/listings/{id}/approve", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/v1/listings/{id}/reject", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Not allowed"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/v1/listings/{id}/approve", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void shouldEnforceOwnershipReasonAndTransitionRules() throws Exception {
        Listing listing = createListing("LISTING-D19-RULES", agent);

        mockMvc.perform(patch("/api/v1/listings/{id}/submit", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondAgentToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/v1/listings/{id}/publish", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        submit(listing, agentToken);
        mockMvc.perform(patch("/api/v1/listings/{id}/submit", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/v1/listings/{id}/reject", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(patch("/api/v1/listings/{id}/unpublish", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/v1/listings/{id}/approve", Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void shouldSearchInternalListingsWithFiltersAndVisibility() throws Exception {
        Listing ownPending = createListing("LISTING-D19-SEARCH-OWN", agent);
        submit(ownPending, agentToken);
        Listing assignedListing = createListing("LISTING-D19-SEARCH-ASSIGNED", manager);
        Property secondAgentProperty = createProperty(
                "PROP-D19-SECOND",
                "Day 19 Second Property",
                secondAgent
        );
        Listing hiddenListing = createListing(
                "LISTING-D19-SEARCH-HIDDEN",
                secondAgent,
                secondAgentProperty
        );

        mockMvc.perform(get("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .queryParam("status", "PENDING_REVIEW")
                        .queryParam("purpose", "SALE")
                        .queryParam("createdBy", agent.getId().toString())
                        .queryParam("propertyId", property.getId().toString())
                        .queryParam("keyword", "SEARCH-OWN")
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("sortBy", "createdAt")
                        .queryParam("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(ownPending.getId()))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.content[0].property.id").value(property.getId()))
                .andExpect(jsonPath("$.data.content[0].creator.id").value(agent.getId()));

        mockMvc.perform(get("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .queryParam("keyword", "SEARCH")
                        .queryParam("sortBy", "code")
                        .queryParam("sortDirection", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(assignedListing.getId()))
                .andExpect(jsonPath("$.data.content[1].id").value(ownPending.getId()));

        mockMvc.perform(get("/api/v1/listings/{id}", hiddenListing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("keyword", "SEARCH-HIDDEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(hiddenListing.getId()));
    }

    @Test
    void shouldReturnInternalListingDetailWithSummariesHistoryAndCounts() throws Exception {
        Listing listing = createListing("LISTING-D19-DETAIL", agent);
        listing.setViewCount(3);
        listing.addFavorite(manager);
        listingRepository.saveAndFlush(listing);

        submit(listing, agentToken);
        mockMvc.perform(patch("/api/v1/listings/{id}/reject", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Needs clearer photos"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/listings/{id}", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(listing.getId()))
                .andExpect(jsonPath("$.data.code").value("LISTING-D19-DETAIL"))
                .andExpect(jsonPath("$.data.property.id").value(property.getId()))
                .andExpect(jsonPath("$.data.property.propertyTypeCode").value("APARTMENT"))
                .andExpect(jsonPath("$.data.property.assignedAgentId").value(agent.getId()))
                .andExpect(jsonPath("$.data.creator.id").value(agent.getId()))
                .andExpect(jsonPath("$.data.reviewer.id").value(manager.getId()))
                .andExpect(jsonPath("$.data.listingPackage").doesNotExist())
                .andExpect(jsonPath("$.data.viewCount").value(3))
                .andExpect(jsonPath("$.data.favoriteCount").value(1))
                .andExpect(jsonPath("$.data.statusHistory.length()").value(2))
                .andExpect(jsonPath("$.data.statusHistory[0].toStatus").value("REJECTED"))
                .andExpect(jsonPath("$.data.statusHistory[0].changedBy.id").value(manager.getId()))
                .andExpect(jsonPath("$.data.statusHistory[0].reason").value("Needs clearer photos"))
                .andExpect(jsonPath("$.data.statusHistory[1].toStatus").value("PENDING_REVIEW"));
    }

    private Listing createListing(String code, User creator) {
        return createListing(code, creator, property);
    }

    private Listing createListing(String code, User creator, Property targetProperty) {
        Listing listing = new Listing(
                code,
                targetProperty,
                creator,
                code + " Title",
                code.toLowerCase(),
                "Day 19 listing review workflow",
                ListingPurpose.SALE
        );
        return listingRepository.saveAndFlush(listing);
    }

    private Property createProperty(String code, String name, User assignedAgent) {
        Address address = new Address(property.getAddress().getProvince(), code + " Street");
        Property secondProperty = new Property(
                code,
                name,
                propertyTypeRepository.findByCode("APARTMENT").orElseThrow(),
                address,
                assignedAgent,
                PropertyPurpose.SALE
        );
        secondProperty.setAssignedAgent(assignedAgent);
        return propertyRepository.saveAndFlush(secondProperty);
    }

    private void submit(Listing listing, String token) throws Exception {
        mockMvc.perform(patch("/api/v1/listings/{id}/submit", listing.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Day 19 User");
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
