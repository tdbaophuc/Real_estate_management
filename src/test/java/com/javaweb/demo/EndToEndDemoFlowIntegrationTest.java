package com.javaweb.demo;

import com.fasterxml.jackson.databind.JsonNode;
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
import com.javaweb.property.repository.AmenityRepository;
import com.javaweb.property.repository.DistrictRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import com.javaweb.property.repository.WardRepository;
import com.javaweb.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo_flow_day49_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.storage.local-root=target/test-storage/day49-demo-flow"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EndToEndDemoFlowIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";
    private static final Path STORAGE_ROOT = Path.of("target/test-storage/day49-demo-flow");
    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x00
    };

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
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User admin;
    private User owner;
    private User customerUser;
    private Province province;
    private District district;
    private Ward ward;

    @BeforeEach
    void setUpDemoSeeds() throws Exception {
        clearStorage();
        admin = createUser("day49-admin@example.test", RoleCode.ADMIN);
        owner = createUser("day49-owner@example.test", RoleCode.OWNER);
        customerUser = createUser("day49-customer@example.test", RoleCode.CUSTOMER);

        province = new Province("P-D49", "Day 49 Province");
        district = new District(province, "D-D49", "Day 49 District");
        ward = new Ward(district, "W-D49", "Day 49 Ward");
        province.addDistrict(district);
        district.addWard(ward);
        provinceRepository.saveAndFlush(province);
    }

    @Test
    void shouldRunEndToEndDemoFlowFromAdminLoginToTransaction() throws Exception {
        String adminToken = login(admin.getEmail());

        Long agentId = registerAgentCandidate();
        mockMvc.perform(patch("/api/v1/admin/users/{userId}/status", agentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "ACTIVE"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        mockMvc.perform(put("/api/v1/admin/users/{userId}/roles", agentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roles", Set.of("AGENT")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("AGENT"));

        User agent = userRepository.findWithRolesById(agentId).orElseThrow();
        String agentToken = login(agent.getEmail());
        String customerToken = login(customerUser.getEmail());
        Customer customer = seedCustomerForDemo(agent);

        Long propertyId = createProperty(agentToken, agentId);
        uploadPropertyImage(agentToken, propertyId);
        mockMvc.perform(patch("/api/v1/properties/{propertyId}/status", propertyId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "AVAILABLE"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));

        Long listingId = createAndPublishListing(agentToken, adminToken, propertyId);

        mockMvc.perform(get("/api/v1/search/listings")
                        .param("keyword", "Day 49"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].slug").value("day49-demo-listing"));

        mockMvc.perform(post("/api/v1/listings/{listingId}/favorite", listingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(listingId));
        mockMvc.perform(get("/api/v1/listings/favorites")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        Long leadId = createLead(agentToken, agentId, customer.getId(), listingId);
        Long appointmentId = createAppointment(
                agentToken,
                agentId,
                customer.getId(),
                propertyId,
                listingId,
                leadId
        );
        mockMvc.perform(get("/api/v1/appointments/{appointmentId}", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.leadId").value(leadId))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        Long transactionId = createTransaction(agentToken, agentId, customer.getId(), propertyId);
        mockMvc.perform(post("/api/v1/transactions/{transactionId}/deposits", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 1_000_000_000,
                                "currency", "VND",
                                "paymentMethod", "BANK_TRANSFER",
                                "referenceNumber", "DEP-D49-001",
                                "idempotencyKey", "DEP-D49-001"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"));
        mockMvc.perform(patch("/api/v1/transactions/{transactionId}/status", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "COMPLETED"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.confirmedAmount").value(1_000_000_000))
                .andExpect(jsonPath("$.data.remainingAmount").value(0));

        assertThat(transactionRepository.findById(transactionId)).isPresent();
    }

    private Long registerAgentCandidate() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "day49-agent@example.test",
                                "password", PASSWORD,
                                "fullName", "Day 49 Demo Agent",
                                "phone", "+84904900001"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"))
                .andReturn();
        return responseDataId(result);
    }

    private Long createProperty(String agentToken, Long agentId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/properties")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(propertyRequest(agentId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("PROP-D49-001"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        return responseDataId(result);
    }

    private void uploadPropertyImage(String agentToken, Long propertyId) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "day49-cover.png",
                MediaType.IMAGE_PNG_VALUE,
                PNG_BYTES
        );
        mockMvc.perform(multipart("/api/v1/properties/{propertyId}/images", propertyId)
                        .file(file)
                        .param("altText", "Day 49 demo cover")
                        .param("displayOrder", "0")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.coverImage").value(true))
                .andExpect(jsonPath("$.data.imageUrl").isNotEmpty());
    }

    private Long createAndPublishListing(
            String agentToken,
            String adminToken,
            Long propertyId
    ) throws Exception {
        MvcResult listingResult = mockMvc.perform(post("/api/v1/listings")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(listingRequest(propertyId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        Long listingId = responseDataId(listingResult);

        mockMvc.perform(patch("/api/v1/listings/{listingId}/submit", listingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));
        mockMvc.perform(patch("/api/v1/listings/{listingId}/approve", listingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
        mockMvc.perform(patch("/api/v1/listings/{listingId}/publish", listingId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        return listingId;
    }

    private Long createLead(
            String agentToken,
            Long agentId,
            Long customerId,
            Long listingId
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/leads")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "LEAD-D49-001",
                                "sourceCode", "WEBSITE",
                                "fullName", "Day 49 Customer",
                                "email", customerUser.getEmail(),
                                "priority", "HIGH",
                                "message", "Interested in the Day 49 demo listing",
                                "customerId", customerId,
                                "listingId", listingId,
                                "assignedAgentId", agentId
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.assignedAgentId").value(agentId))
                .andReturn();
        return responseDataId(result);
    }

    private Long createAppointment(
            String agentToken,
            Long agentId,
            Long customerId,
            Long propertyId,
            Long listingId,
            Long leadId
    ) throws Exception {
        Instant startAt = Instant.now().plus(3, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.SECONDS);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", "APT-D49-001");
        request.put("customerId", customerId);
        request.put("agentId", agentId);
        request.put("propertyId", propertyId);
        request.put("listingId", listingId);
        request.put("leadId", leadId);
        request.put("title", "Day 49 property viewing");
        request.put("startAt", startAt.toString());
        request.put("endAt", startAt.plus(1, ChronoUnit.HOURS).toString());
        request.put("timezone", "Asia/Ho_Chi_Minh");
        request.put("meetingLocation", "Property lobby");
        request.put("notes", "End-to-end demo appointment");

        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        return responseDataId(result);
    }

    private Long createTransaction(
            String agentToken,
            Long agentId,
            Long customerId,
            Long propertyId
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "TX-D49-001",
                                "propertyId", propertyId,
                                "customerId", customerId,
                                "agentId", agentId,
                                "transactionType", "SALE",
                                "agreedValue", 1_000_000_000,
                                "currency", "VND",
                                "transactionDate", LocalDate.now().toString(),
                                "expectedCompletionDate", LocalDate.now().plusDays(30).toString(),
                                "notes", "Day 49 demo transaction"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        return responseDataId(result);
    }

    private Map<String, Object> propertyRequest(Long agentId) {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("provinceId", province.getId());
        address.put("districtId", district.getId());
        address.put("wardId", ward.getId());
        address.put("streetAddress", "49 Demo Street");
        address.put("fullAddress", "49 Demo Street, Day 49 Ward");

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", "PROP-D49-001");
        request.put("name", "Day 49 Demo Property");
        request.put("description", "Property created for the Day 49 end-to-end demo flow");
        request.put("propertyTypeId", propertyTypeRepository.findByCode("APARTMENT")
                .orElseThrow()
                .getId());
        request.put("purpose", "SALE");
        request.put("price", "1000000000.00");
        request.put("currency", "VND");
        request.put("landArea", "80.00");
        request.put("floorArea", "72.00");
        request.put("bedrooms", 2);
        request.put("bathrooms", 2);
        request.put("floors", 1);
        request.put("legalStatus", "PINK_BOOK");
        request.put("ownerId", owner.getId());
        request.put("assignedAgentId", agentId);
        request.put("address", address);
        request.put("amenities", List.of(Map.of(
                "amenityId", amenityRepository.findByCode("PARKING").orElseThrow().getId(),
                "details", "One basement parking slot"
        )));
        return request;
    }

    private Map<String, Object> listingRequest(Long propertyId) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("propertyId", propertyId);
        request.put("code", "LIST-D49-001");
        request.put("title", "Day 49 Demo Listing");
        request.put("slug", "day49-demo-listing");
        request.put("description", "Published listing used by the Day 49 demo flow");
        request.put("purpose", "SALE");
        request.put("visibility", "PUBLIC");
        request.put("askingPrice", "1000000000.00");
        request.put("currency", "VND");
        request.put("seoTitle", "Day 49 Demo Listing");
        request.put("seoDescription", "End-to-end demo listing");
        request.put("seoKeywords", "day49,demo,real-estate");
        return request;
    }

    private Customer seedCustomerForDemo(User agent) {
        Customer customer = new Customer("CUS-D49-001", "Day 49 Customer", agent);
        customer.setUser(customerUser);
        customer.setAssignedAgent(agent);
        customer.setEmail(customerUser.getEmail());
        return customerRepository.saveAndFlush(customer);
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, passwordEncoder.encode(PASSWORD), roleCode + " Demo User");
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
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).at("/data/accessToken").asText();
    }

    private Long responseDataId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.at("/data/id").longValue();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void clearStorage() throws Exception {
        if (Files.exists(STORAGE_ROOT)) {
            try (var paths = Files.walk(STORAGE_ROOT)) {
                paths.sorted(Comparator.reverseOrder())
                        .filter(path -> !path.equals(STORAGE_ROOT))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception exception) {
                                throw new IllegalStateException(exception);
                            }
                        });
            }
        }
        Files.createDirectories(STORAGE_ROOT.resolve("public"));
        Files.createDirectories(STORAGE_ROOT.resolve("private"));
    }
}
