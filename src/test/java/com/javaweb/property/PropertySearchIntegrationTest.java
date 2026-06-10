package com.javaweb.property;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.enums.PropertyStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:property_search_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropertySearchIntegrationTest {
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
    private DistrictRepository districtRepository;

    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User agent;
    private User secondAgent;
    private User manager;
    private PropertyType apartment;
    private PropertyType house;
    private PropertyType villa;
    private Province cityProvince;
    private Province beachProvince;
    private District centralDistrict;
    private Ward riversideWard;
    private Ward downtownWard;
    private Ward beachWard;
    private String agentToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        propertyRepository.deleteAll();
        wardRepository.deleteAll();
        districtRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        agent = createUser("agent-search@example.test", RoleCode.AGENT);
        secondAgent = createUser("second-agent-search@example.test", RoleCode.AGENT);
        manager = createUser("manager-search@example.test", RoleCode.MANAGER);
        agentToken = login(agent.getEmail());
        managerToken = login(manager.getEmail());

        apartment = propertyTypeRepository.findByCode("APARTMENT").orElseThrow();
        house = propertyTypeRepository.findByCode("HOUSE").orElseThrow();
        villa = propertyTypeRepository.findByCode("VILLA").orElseThrow();

        cityProvince = new Province("P-D13-CITY", "Search City");
        centralDistrict = new District(cityProvince, "D-D13-CENTRAL", "Central District");
        District westDistrict = new District(cityProvince, "D-D13-WEST", "West District");
        riversideWard = new Ward(centralDistrict, "W-D13-RIVER", "Riverside Ward");
        downtownWard = new Ward(westDistrict, "W-D13-DOWN", "Downtown Ward");
        cityProvince.addDistrict(centralDistrict);
        cityProvince.addDistrict(westDistrict);
        centralDistrict.addWard(riversideWard);
        westDistrict.addWard(downtownWard);
        provinceRepository.saveAndFlush(cityProvince);

        beachProvince = new Province("P-D13-BEACH", "Beach Province");
        District coastDistrict = new District(
                beachProvince,
                "D-D13-COAST",
                "Coast District"
        );
        beachWard = new Ward(coastDistrict, "W-D13-BEACH", "Beach Ward");
        beachProvince.addDistrict(coastDistrict);
        coastDistrict.addWard(beachWard);
        provinceRepository.saveAndFlush(beachProvince);

        createProperty(
                "PROP-D13-RIVER",
                "Riverside Garden Apartment",
                "Quiet home near the river",
                apartment,
                PropertyPurpose.SALE,
                riversideWard,
                new BigDecimal("5000000000"),
                new BigDecimal("100"),
                3,
                2,
                PropertyStatus.AVAILABLE,
                agent,
                agent
        );
        createProperty(
                "PROP-D13-RENT",
                "Downtown Rental House",
                "City center rental",
                house,
                PropertyPurpose.RENT,
                downtownWard,
                new BigDecimal("25000000"),
                new BigDecimal("80"),
                2,
                1,
                PropertyStatus.DRAFT,
                manager,
                agent
        );
        createProperty(
                "PROP-D13-VILLA",
                "Beachfront Villa",
                "Luxury ocean property",
                villa,
                PropertyPurpose.SALE,
                beachWard,
                new BigDecimal("12000000000"),
                new BigDecimal("250"),
                4,
                4,
                PropertyStatus.SOLD,
                manager,
                secondAgent
        );
        Property deleted = createProperty(
                "PROP-D13-DELETED",
                "Deleted Riverside Property",
                "Should never appear",
                apartment,
                PropertyPurpose.SALE,
                riversideWard,
                new BigDecimal("4500000000"),
                new BigDecimal("95"),
                3,
                2,
                PropertyStatus.DELETED,
                manager,
                agent
        );
        deleted.setDeletedAt(Instant.now());
        propertyRepository.saveAndFlush(deleted);
    }

    @Test
    void shouldFilterByKeywordTypePurposeLocationAndStatus() throws Exception {
        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("keyword", "RIVER")
                        .queryParam("propertyTypeId", apartment.getId().toString())
                        .queryParam("purpose", "SALE")
                        .queryParam("provinceId", cityProvince.getId().toString())
                        .queryParam("districtId", centralDistrict.getId().toString())
                        .queryParam("wardId", riversideWard.getId().toString())
                        .queryParam("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].code").value("PROP-D13-RIVER"));

        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("keyword", "ocean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].code").value("PROP-D13-VILLA"));
    }

    @Test
    void shouldFilterRangesAndRoomsWithPaginationAndSorting() throws Exception {
        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("minPrice", "20000000")
                        .queryParam("maxPrice", "6000000000")
                        .queryParam("minArea", "75")
                        .queryParam("maxArea", "110")
                        .queryParam("bedrooms", "3")
                        .queryParam("bathrooms", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].code").value("PROP-D13-RIVER"));

        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("page", "0")
                        .queryParam("size", "2")
                        .queryParam("sortBy", "price")
                        .queryParam("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].code").value("PROP-D13-RENT"))
                .andExpect(jsonPath("$.data.content[1].code").value("PROP-D13-RIVER"));
    }

    @Test
    void shouldPreserveAgentVisibilityAndValidateSearchInput() throws Exception {
        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .queryParam("status", "SOLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("minPrice", "100")
                        .queryParam("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("minArea", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("purpose", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private Property createProperty(
            String code,
            String name,
            String description,
            PropertyType type,
            PropertyPurpose purpose,
            Ward ward,
            BigDecimal price,
            BigDecimal area,
            int bedrooms,
            int bathrooms,
            PropertyStatus status,
            User creator,
            User assignedAgent
    ) {
        Address address = new Address(ward.getDistrict().getProvince(), code + " Street");
        address.setDistrict(ward.getDistrict());
        address.setWard(ward);
        address.setFullAddress(code + " Street, " + ward.getName());
        Property property = new Property(code, name, type, address, creator, purpose);
        property.setDescription(description);
        property.setPrice(price);
        property.setLandArea(area);
        property.setBedrooms(bedrooms);
        property.setBathrooms(bathrooms);
        property.setStatus(status);
        property.setAssignedAgent(assignedAgent);
        return propertyRepository.saveAndFlush(property);
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Search User");
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
