package com.javaweb.commission;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.commission.repository.CommissionRepository;
import com.javaweb.commission.repository.CommissionRuleRepository;
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
import com.javaweb.transaction.repository.TransactionRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:commission_management_day33_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommissionManagementIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommissionRepository commissionRepository;

    @Autowired
    private CommissionRuleRepository ruleRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ContractRepository contractRepository;

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
        commissionRepository.deleteAll();
        ruleRepository.deleteAll();
        transactionRepository.deleteAll();
        contractRepository.deleteAll();
        customerRepository.deleteAll();
        propertyRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        User owner = createUser("day33-owner@example.test", RoleCode.OWNER);
        agent = createUser("day33-agent@example.test", RoleCode.AGENT);
        secondAgent = createUser("day33-second-agent@example.test", RoleCode.AGENT);
        manager = createUser("day33-manager@example.test", RoleCode.MANAGER);
        agentToken = login(agent.getEmail());
        secondAgentToken = login(secondAgent.getEmail());
        managerToken = login(manager.getEmail());

        Province province = provinceRepository.saveAndFlush(
                new Province("P-D33", "Day 33 Province")
        );
        PropertyType propertyType = propertyTypeRepository.findByCode("APARTMENT")
                .orElseThrow();
        property = new Property(
                "PROP-D33",
                "Day 33 Commission Property",
                propertyType,
                new Address(province, "33 Commission Street"),
                agent,
                PropertyPurpose.SALE
        );
        property.setStatus(PropertyStatus.AVAILABLE);
        property.setOwner(owner);
        property.setAssignedAgent(agent);
        property = propertyRepository.saveAndFlush(property);

        customer = new Customer("CUS-D33", "Day 33 Buyer", agent);
        customer.setAssignedAgent(agent);
        customer.setEmail("day33-buyer@example.test");
        customer = customerRepository.saveAndFlush(customer);
    }

    @Test
    void shouldCalculateListAndPayAgentCommission() throws Exception {
        createPercentageRule("SALE-2-5", 10, "2.5000");

        Long transactionId = createTransaction();
        recordFullPayment(transactionId);

        mockMvc.perform(patch("/api/v1/transactions/{id}/status", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "COMPLETED"
                        ))))
                .andExpect(status().isOk());

        MvcResult mineResult = mockMvc.perform(get("/api/v1/commissions/my")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.content[0].baseAmount").value(1000000))
                .andExpect(jsonPath("$.data.content[0].rate").value(2.5))
                .andExpect(jsonPath("$.data.content[0].amount").value(25000))
                .andReturn();
        Long commissionId = responseContentId(mineResult);

        mockMvc.perform(get("/api/v1/commissions/my")
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondAgentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/commissions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/commissions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(patch(
                                "/api/v1/commissions/{id}/mark-paid",
                                commissionId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "paymentReference", "COM-D33-PAY"
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(
                                "/api/v1/commissions/{id}/mark-paid",
                                commissionId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "paymentReference", "COM-D33-PAY",
                                "notes", "Paid outside the system"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.paymentReference").value("COM-D33-PAY"))
                .andExpect(jsonPath("$.data.approvedById").value(manager.getId()))
                .andExpect(jsonPath("$.data.paidById").value(manager.getId()));

        assertThat(commissionRepository.findById(commissionId))
                .get()
                .extracting(item -> item.getStatus().name())
                .isEqualTo("PAID");
    }

    @Test
    void shouldManageRulesAndValidateRuleShape() throws Exception {
        Long ruleId = createPercentageRule("SALE-1-5", 5, "1.5000");

        Map<String, Object> update = ruleRequest("SALE-1-5", 7, "2.0000");
        update.put("active", false);
        mockMvc.perform(put("/api/v1/commission-rules/{id}", ruleId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.priority").value(7))
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.rate").value(2.0));

        mockMvc.perform(get("/api/v1/commission-rules")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));

        mockMvc.perform(post("/api/v1/commission-rules")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ruleRequest("FORBIDDEN", 1, "1.0000")
                        )))
                .andExpect(status().isForbidden());

        Map<String, Object> invalid = ruleRequest("INVALID", 1, "1.0000");
        invalid.put("fixedAmount", 10000);
        mockMvc.perform(post("/api/v1/commission-rules")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    private Long createPercentageRule(
            String code,
            int priority,
            String rate
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/commission-rules")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                ruleRequest(code, priority, rate)
                        )))
                .andExpect(status().isCreated())
                .andReturn();
        return responseDataId(result);
    }

    private Map<String, Object> ruleRequest(
            String code,
            int priority,
            String rate
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", code);
        request.put("name", code + " commission");
        request.put("transactionType", "SALE");
        request.put("calculationType", "PERCENTAGE");
        request.put("rate", rate);
        request.put("currency", "VND");
        request.put("minTransactionValue", 0);
        request.put("maxTransactionValue", 2000000);
        request.put("priority", priority);
        request.put("active", true);
        return request;
    }

    private Long createTransaction() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", "TRANSACTION-D33",
                                "propertyId", property.getId(),
                                "customerId", customer.getId(),
                                "agentId", agent.getId(),
                                "transactionType", "SALE",
                                "agreedValue", 1000000,
                                "currency", "VND"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return responseDataId(result);
    }

    private void recordFullPayment(Long transactionId) throws Exception {
        mockMvc.perform(post("/api/v1/transactions/{id}/payments", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 1000000,
                                "currency", "VND",
                                "paymentMethod", "BANK_TRANSFER",
                                "idempotencyKey", "PAY-D33-FULL"
                        ))))
                .andExpect(status().isCreated());
    }

    private Long responseDataId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.at("/data/id").longValue();
    }

    private Long responseContentId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.at("/data/content/0/id").longValue();
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(
                email,
                passwordEncoder.encode(PASSWORD),
                roleCode + " Commission User"
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
