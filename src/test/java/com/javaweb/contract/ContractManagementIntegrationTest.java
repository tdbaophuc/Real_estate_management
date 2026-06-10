package com.javaweb.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.contract.entity.Contract;
import com.javaweb.contract.enums.ContractStatus;
import com.javaweb.contract.repository.ContractDocumentRepository;
import com.javaweb.contract.repository.ContractRepository;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.enums.PropertyStatus;
import com.javaweb.property.repository.PropertyRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import com.javaweb.storage.repository.FileResourceRepository;
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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:contract_management_day30_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.storage.local-root=./target/contract-management-storage"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContractManagementIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";
    private static final byte[] PDF_BYTES =
            "%PDF-1.4\nDay 30 contract".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ContractDocumentRepository documentRepository;

    @Autowired
    private FileResourceRepository fileResourceRepository;

    @Autowired
    private CustomerRepository customerRepository;

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

    private User owner;
    private User agent;
    private User secondAgent;
    private User manager;
    private Customer customer;
    private Property property;
    private String agentToken;
    private String secondAgentToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        contractRepository.deleteAll();
        fileResourceRepository.deleteAll();
        customerRepository.deleteAll();
        propertyRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        owner = createUser("day30-owner@example.test", RoleCode.CUSTOMER);
        agent = createUser("day30-agent@example.test", RoleCode.AGENT);
        secondAgent = createUser("day30-second-agent@example.test", RoleCode.AGENT);
        manager = createUser("day30-manager@example.test", RoleCode.MANAGER);
        agentToken = login(agent.getEmail());
        secondAgentToken = login(secondAgent.getEmail());
        managerToken = login(manager.getEmail());

        Province province = provinceRepository.saveAndFlush(
                new Province("P-D30", "Day 30 Province")
        );
        PropertyType propertyType = propertyTypeRepository.findByCode("APARTMENT")
                .orElseThrow();
        property = new Property(
                "PROP-D30",
                "Day 30 Contract Property",
                propertyType,
                new Address(province, "30 Contract Street"),
                agent,
                PropertyPurpose.SALE
        );
        property.setStatus(PropertyStatus.AVAILABLE);
        property.setOwner(owner);
        property.setAssignedAgent(agent);
        property = propertyRepository.saveAndFlush(property);

        customer = new Customer("CUS-D30", "Day 30 Buyer", agent);
        customer.setAssignedAgent(agent);
        customer.setEmail("day30-buyer@example.test");
        customer.setPhone("0900000030");
        customer = customerRepository.saveAndFlush(customer);
    }

    @Test
    void shouldCompleteContractLifecycle() throws Exception {
        Long contractId = createContract("CONTRACT-D30-FLOW", agentToken);

        mockMvc.perform(put("/api/v1/contracts/{id}", contractId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated sale contract"))
                .andExpect(jsonPath("$.data.totalValue").value(2600000000L));

        uploadDocument(
                contractId,
                "contract-draft.pdf",
                "DRAFT",
                agentToken,
                status().isCreated()
        ).andExpect(jsonPath("$.data.version").value(1));
        uploadDocument(
                contractId,
                "contract-draft-v2.pdf",
                "DRAFT",
                agentToken,
                status().isCreated()
        ).andExpect(jsonPath("$.data.version").value(2));

        mockMvc.perform(patch("/api/v1/contracts/{id}/submit-review", contractId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());

        mockMvc.perform(patch("/api/v1/contracts/{id}/approve", contractId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/contracts/{id}/approve", contractId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_SIGNATURE"))
                .andExpect(jsonPath("$.data.approvedAt").isNotEmpty());

        mockMvc.perform(patch("/api/v1/contracts/{id}/mark-signed", contractId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isUnprocessableEntity());

        uploadDocument(
                contractId,
                "contract-signed.pdf",
                "SIGNED",
                agentToken,
                status().isCreated()
        ).andExpect(jsonPath("$.data.documentType").value("SIGNED"));

        mockMvc.perform(patch("/api/v1/contracts/{id}/mark-signed", contractId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SIGNED"))
                .andExpect(jsonPath("$.data.signedAt").isNotEmpty());

        Contract contract = contractRepository.findById(contractId).orElseThrow();
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.SIGNED);
        assertThat(documentRepository.findAllByContractIdOrderByCreatedAtDesc(contractId))
                .hasSize(3);
    }

    @Test
    void shouldEnforceVisibilityAndStatusWorkflow() throws Exception {
        Long contractId = createContract("CONTRACT-D30-AUTH", agentToken);

        mockMvc.perform(get("/api/v1/contracts/{id}", contractId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondAgentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/contracts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondAgentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/contracts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(contractId));

        mockMvc.perform(patch("/api/v1/contracts/{id}/submit-review", contractId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/v1/contracts/{id}/cancel", contractId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Buyer withdrew"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason")
                        .value("Buyer withdrew"));

        mockMvc.perform(put("/api/v1/contracts/{id}", contractId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest())))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(get("/api/v1/contracts/{id}", contractId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAgentCreatingContractForAnotherAgent() throws Exception {
        Map<String, Object> request = createRequest("CONTRACT-D30-OTHER");
        request.put("agentId", secondAgent.getId());

        mockMvc.perform(post("/api/v1/contracts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private Long createContract(String code, String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/contracts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(code))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.parties.length()").value(3))
                .andExpect(jsonPath("$.data.parties[0].partyRole").value("BUYER"))
                .andExpect(jsonPath("$.data.parties[1].partyRole").value("SELLER"))
                .andExpect(jsonPath("$.data.parties[2].partyRole").value("AGENT"))
                .andReturn();
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.at("/data/id").longValue();
    }

    private org.springframework.test.web.servlet.ResultActions uploadDocument(
            Long contractId,
            String fileName,
            String documentType,
            String token,
            org.springframework.test.web.servlet.ResultMatcher expectedStatus
    ) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                fileName,
                MediaType.APPLICATION_PDF_VALUE,
                PDF_BYTES
        );
        return mockMvc.perform(multipart(
                                "/api/v1/contracts/{id}/documents",
                                contractId
                        )
                        .file(file)
                        .param("documentType", documentType)
                        .param("primaryDocument", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(expectedStatus);
    }

    private Map<String, Object> createRequest(String code) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", code);
        request.put("contractType", "SALE");
        request.put("propertyId", property.getId());
        request.put("customerId", customer.getId());
        request.put("agentId", agent.getId());
        request.put("title", "Sale contract");
        request.put("totalValue", 2500000000L);
        request.put("currency", "VND");
        request.put("effectiveDate", LocalDate.now().plusDays(7).toString());
        request.put("expirationDate", LocalDate.now().plusDays(30).toString());
        request.put("terms", "Standard contract terms");
        request.put("notes", "Day 30 contract");
        return request;
    }

    private Map<String, Object> updateRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", "Updated sale contract");
        request.put("totalValue", 2600000000L);
        request.put("currency", "VND");
        request.put("effectiveDate", LocalDate.now().plusDays(10).toString());
        request.put("expirationDate", LocalDate.now().plusDays(40).toString());
        request.put("terms", "Updated contract terms");
        request.put("notes", "Updated Day 30 contract");
        return request;
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(
                email,
                passwordEncoder.encode(PASSWORD),
                roleCode + " Contract User"
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
