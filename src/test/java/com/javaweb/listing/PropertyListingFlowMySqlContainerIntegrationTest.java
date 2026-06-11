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
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import com.javaweb.property.repository.AmenityRepository;
import com.javaweb.property.repository.DistrictRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import com.javaweb.property.repository.WardRepository;
import com.javaweb.support.AbstractMySqlContainerIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PropertyListingFlowMySqlContainerIntegrationTest extends AbstractMySqlContainerIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    private ListingRepository listingRepository;

    private User agent;
    private User manager;
    private User owner;
    private Province province;
    private District district;
    private Ward ward;
    private String agentToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        truncateMutableTables();

        agent = createUser("day44-agent@example.test", RoleCode.AGENT);
        manager = createUser("day44-manager@example.test", RoleCode.MANAGER);
        owner = createUser("day44-owner@example.test", RoleCode.OWNER);

        agentToken = login(agent.getEmail());
        managerToken = login(manager.getEmail());

        province = new Province("P-D44", "Day 44 Province");
        district = new District(province, "D-D44", "Day 44 District");
        ward = new Ward(district, "W-D44", "Day 44 Ward");
        province.addDistrict(district);
        district.addWard(ward);
        provinceRepository.saveAndFlush(province);
    }

    @Test
    void shouldCreatePropertyCreateListingAndPublishPublicSearchResultOnMySql() throws Exception {
        Long propertyId = createProperty();

        MvcResult listingResult = mockMvc.perform(post("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(listingRequest(propertyId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("LISTING-D44-001"))
                .andExpect(jsonPath("$.data.propertyId").value(propertyId))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        Long listingId = responseDataId(listingResult);

        mockMvc.perform(patch("/api/v1/listings/{listingId}/submit", listingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));

        mockMvc.perform(patch("/api/v1/listings/{listingId}/approve", listingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(patch("/api/v1/listings/{listingId}/publish", listingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/search/listings")
                        .param("keyword", "Day 44"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].slug").value("day44-container-listing"));

        Listing saved = listingRepository.findWithCoreDetailsById(listingId).orElseThrow();
        assertThat(saved.getSubmittedAt()).isNotNull();
        assertThat(saved.getReviewedAt()).isNotNull();
        assertThat(saved.getPublishedAt()).isNotNull();
    }

    private Long createProperty() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(propertyRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("PROP-D44-001"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.assignedAgentId").value(agent.getId()))
                .andReturn();
        return responseDataId(result);
    }

    private Map<String, Object> propertyRequest() {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("provinceId", province.getId());
        address.put("districtId", district.getId());
        address.put("wardId", ward.getId());
        address.put("streetAddress", "44 Integration Street");
        address.put("fullAddress", "44 Integration Street, Day 44 Ward");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", "PROP-D44-001");
        request.put("name", "Day 44 Container Property");
        request.put("description", "Property created by the Day 44 Testcontainers flow");
        request.put("propertyTypeId", propertyTypeRepository.findByCode("APARTMENT").orElseThrow().getId());
        request.put("purpose", "SALE");
        request.put("price", "7200000000.00");
        request.put("currency", "VND");
        request.put("landArea", "95.00");
        request.put("floorArea", "88.00");
        request.put("bedrooms", 3);
        request.put("bathrooms", 2);
        request.put("floors", 1);
        request.put("legalStatus", "PINK_BOOK");
        request.put("ownerId", owner.getId());
        request.put("address", address);
        request.put("amenities", List.of(Map.of(
                "amenityId",
                amenityRepository.findByCode("PARKING").orElseThrow().getId(),
                "details",
                "Basement parking"
        )));
        return request;
    }

    private Map<String, Object> listingRequest(Long propertyId) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("propertyId", propertyId);
        request.put("code", "LISTING-D44-001");
        request.put("title", "Day 44 Container Listing");
        request.put("slug", "day44-container-listing");
        request.put("description", "Published listing created from a property in the MySQL container flow");
        request.put("purpose", "SALE");
        request.put("visibility", "PUBLIC");
        request.put("askingPrice", "7300000000.00");
        request.put("currency", "VND");
        request.put("seoTitle", "Day 44 Integration Listing");
        request.put("seoDescription", "Day 44 integration test listing");
        request.put("seoKeywords", "day44,integration,testcontainers");
        return request;
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Day 44 User");
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

    private Long responseDataId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.at("/data/id").longValue();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void truncateMutableTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE listing_favorites");
        jdbcTemplate.execute("TRUNCATE TABLE listing_views");
        jdbcTemplate.execute("TRUNCATE TABLE listing_status_histories");
        jdbcTemplate.execute("TRUNCATE TABLE listings");
        jdbcTemplate.execute("TRUNCATE TABLE property_amenities");
        jdbcTemplate.execute("TRUNCATE TABLE property_images");
        jdbcTemplate.execute("TRUNCATE TABLE property_legal_documents");
        jdbcTemplate.execute("TRUNCATE TABLE properties");
        jdbcTemplate.execute("TRUNCATE TABLE addresses");
        jdbcTemplate.execute("TRUNCATE TABLE wards");
        jdbcTemplate.execute("TRUNCATE TABLE districts");
        jdbcTemplate.execute("TRUNCATE TABLE provinces");
        jdbcTemplate.execute("TRUNCATE TABLE refresh_tokens");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
