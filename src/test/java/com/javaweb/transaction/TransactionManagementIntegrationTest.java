package com.javaweb.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.audit.AuditActions;
import com.javaweb.audit.repository.AuditLogRepository;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.contract.entity.Contract;
import com.javaweb.contract.enums.ContractStatus;
import com.javaweb.contract.enums.ContractType;
import com.javaweb.contract.repository.ContractRepository;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.payment.repository.PaymentRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:transaction_management_day32_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionManagementIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

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

    private User owner;
    private User agent;
    private User secondAgent;
    private User manager;
    private Customer customer;
    private Property property;
    private Contract signedContract;
    private String agentToken;
    private String secondAgentToken;
    private String managerToken;

    @BeforeEach
    void setUp() throws Exception {
        auditLogRepository.deleteAll();
        transactionRepository.deleteAll();
        contractRepository.deleteAll();
        customerRepository.deleteAll();
        propertyRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        owner = createUser("day32-owner@example.test", RoleCode.OWNER);
        agent = createUser("day32-agent@example.test", RoleCode.AGENT);
        secondAgent = createUser("day32-second-agent@example.test", RoleCode.AGENT);
        manager = createUser("day32-manager@example.test", RoleCode.MANAGER);
        agentToken = login(agent.getEmail());
        secondAgentToken = login(secondAgent.getEmail());
        managerToken = login(manager.getEmail());

        Province province = provinceRepository.saveAndFlush(
                new Province("P-D32", "Day 32 Province")
        );
        PropertyType propertyType = propertyTypeRepository.findByCode("APARTMENT")
                .orElseThrow();
        property = new Property(
                "PROP-D32",
                "Day 32 Transaction Property",
                propertyType,
                new Address(province, "32 Transaction Street"),
                agent,
                PropertyPurpose.SALE
        );
        property.setStatus(PropertyStatus.AVAILABLE);
        property.setOwner(owner);
        property.setAssignedAgent(agent);
        property = propertyRepository.saveAndFlush(property);

        customer = new Customer("CUS-D32", "Day 32 Buyer", agent);
        customer.setAssignedAgent(agent);
        customer.setEmail("day32-buyer@example.test");
        customer = customerRepository.saveAndFlush(customer);

        signedContract = new Contract(
                "CONTRACT-D32",
                ContractType.SALE,
                "Day 32 signed contract",
                property,
                customer,
                owner,
                agent,
                agent
        );
        signedContract.setTotalValue(new java.math.BigDecimal("1000000"));
        signedContract.setStatus(ContractStatus.SIGNED);
        signedContract.setSignedAt(Instant.now());
        signedContract = contractRepository.saveAndFlush(signedContract);
    }

    @Test
    void shouldCompleteExternalPaymentTrackingWorkflow() throws Exception {
        Long transactionId = createTransaction("TRANSACTION-D32-FLOW", null, agentToken);

        MvcResult depositResult = mockMvc.perform(post(
                                "/api/v1/transactions/{id}/deposits",
                                transactionId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 300000,
                                "currency", "VND",
                                "paymentMethod", "BANK_TRANSFER",
                                "referenceNumber", "DEP-D32-001",
                                "idempotencyKey", "DEP-D32-IDEMPOTENCY"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andReturn();
        Long depositId = responseDataId(depositResult);

        mockMvc.perform(post("/api/v1/transactions/{id}/deposits", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 300000,
                                "currency", "VND",
                                "paymentMethod", "BANK_TRANSFER",
                                "referenceNumber", "DEP-D32-001",
                                "idempotencyKey", "DEP-D32-IDEMPOTENCY"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(depositId));

        MvcResult scheduleResult = mockMvc.perform(post(
                                "/api/v1/transactions/{id}/payment-schedules",
                                transactionId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "installmentNumber", 1,
                                "label", "Final settlement",
                                "dueDate", LocalDate.now().plusDays(10).toString(),
                                "amount", 700000,
                                "currency", "VND"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();
        Long scheduleId = responseDataId(scheduleResult);

        MvcResult paymentResult = mockMvc.perform(post(
                                "/api/v1/transactions/{id}/payments",
                                transactionId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "paymentScheduleId", scheduleId,
                                "amount", 700000,
                                "currency", "VND",
                                "paymentMethod", "BANK_TRANSFER",
                                "referenceNumber", "PAY-D32-001",
                                "idempotencyKey", "PAY-D32-IDEMPOTENCY"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andReturn();
        Long paymentId = responseDataId(paymentResult);

        mockMvc.perform(post("/api/v1/transactions/{id}/invoices", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "invoiceNumber", "INV-D32-001",
                                "issueDate", LocalDate.now().toString(),
                                "dueDate", LocalDate.now().plusDays(10).toString(),
                                "subtotal", 700000,
                                "taxAmount", 0,
                                "currency", "VND",
                                "billedToName", customer.getFullName()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ISSUED"))
                .andExpect(jsonPath("$.data.totalAmount").value(700000));

        mockMvc.perform(post(
                                "/api/v1/transactions/{id}/payments/{paymentId}/receipt",
                                transactionId,
                                paymentId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "receiptNumber", "REC-D32-001",
                                "payerName", customer.getFullName()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(700000));

        mockMvc.perform(patch("/api/v1/transactions/{id}/status", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "COMPLETED"
                        ))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/transactions/{id}/status", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "COMPLETED"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.confirmedAmount").value(1000000))
                .andExpect(jsonPath("$.data.remainingAmount").value(0))
                .andExpect(jsonPath("$.data.paymentSchedules[0].status").value("PAID"))
                .andExpect(jsonPath("$.data.payments[0].receipt.receiptNumber")
                        .value("REC-D32-001"));

        assertThat(paymentRepository.findById(paymentId)).isPresent();
        assertThat(auditLogRepository
                .findAllByActionAndResourceTypeAndResourceIdOrderByCreatedAtDesc(
                        AuditActions.TRANSACTION_STATUS_CHANGED,
                        AuditActions.TRANSACTION,
                        transactionId
                )).singleElement()
                .satisfies(log -> {
                    assertThat(log.getOldValueJson()).contains("PAYMENT_IN_PROGRESS");
                    assertThat(log.getNewValueJson()).contains("COMPLETED");
                });
    }

    @Test
    void shouldCreateFromSignedContractAndEnforceVisibility() throws Exception {
        Long transactionId = createTransaction(
                "TRANSACTION-D32-CONTRACT",
                signedContract.getId(),
                agentToken
        );

        mockMvc.perform(get("/api/v1/transactions/{id}", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONTRACT_SIGNED"))
                .andExpect(jsonPath("$.data.contractId").value(signedContract.getId()));

        mockMvc.perform(get("/api/v1/transactions/{id}", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondAgentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondAgentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("status", "CONTRACT_SIGNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void shouldRejectInvalidFinancialOperations() throws Exception {
        Long transactionId = createTransaction("TRANSACTION-D32-RULES", null, agentToken);

        mockMvc.perform(patch("/api/v1/transactions/{id}/status", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "COMPLETED"
                        ))))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post(
                                "/api/v1/transactions/{id}/payment-schedules",
                                transactionId
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "installmentNumber", 1,
                                "label", "Too large",
                                "dueDate", LocalDate.now().plusDays(5).toString(),
                                "amount", 1100000,
                                "currency", "VND"
                        ))))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/transactions/{id}/payments", transactionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "amount", 1100000,
                                "currency", "VND",
                                "paymentMethod", "BANK_TRANSFER",
                                "idempotencyKey", "PAY-D32-OVERPAY"
                        ))))
                .andExpect(status().isUnprocessableEntity());

        Map<String, Object> otherAgentRequest = transactionRequest(
                "TRANSACTION-D32-OTHER",
                null
        );
        otherAgentRequest.put("agentId", secondAgent.getId());
        mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otherAgentRequest)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/transactions/{id}", transactionId))
                .andExpect(status().isUnauthorized());
    }

    private Long createTransaction(
            String code,
            Long contractId,
            String token
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/transactions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                transactionRequest(code, contractId)
                        )))
                .andExpect(status().isCreated())
                .andReturn();
        return responseDataId(result);
    }

    private Map<String, Object> transactionRequest(String code, Long contractId) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", code);
        if (contractId != null) {
            request.put("contractId", contractId);
        }
        request.put("propertyId", property.getId());
        request.put("customerId", customer.getId());
        request.put("agentId", agent.getId());
        request.put("transactionType", "SALE");
        request.put("agreedValue", 1000000);
        request.put("currency", "VND");
        request.put("transactionDate", LocalDate.now().toString());
        request.put(
                "expectedCompletionDate",
                LocalDate.now().plusDays(30).toString()
        );
        request.put("notes", "Day 32 transaction");
        return request;
    }

    private Long responseDataId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.at("/data/id").longValue();
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(
                email,
                passwordEncoder.encode(PASSWORD),
                roleCode + " Transaction User"
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
