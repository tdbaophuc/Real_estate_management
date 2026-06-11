package com.javaweb.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.appointment.entity.Appointment;
import com.javaweb.appointment.enums.AppointmentStatus;
import com.javaweb.appointment.repository.AppointmentRepository;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.commission.entity.Commission;
import com.javaweb.commission.enums.CommissionStatus;
import com.javaweb.commission.repository.CommissionRepository;
import com.javaweb.contract.enums.ContractType;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.entity.LeadSource;
import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.enums.FollowUpTaskStatus;
import com.javaweb.lead.enums.LeadPipelineStatus;
import com.javaweb.lead.repository.FollowUpTaskRepository;
import com.javaweb.lead.repository.LeadRepository;
import com.javaweb.lead.repository.LeadSourceRepository;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.payment.entity.Payment;
import com.javaweb.payment.enums.PaymentMethod;
import com.javaweb.payment.enums.PaymentStatus;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.enums.PropertyStatus;
import com.javaweb.property.repository.PropertyRepository;
import com.javaweb.property.repository.PropertyTypeRepository;
import com.javaweb.property.repository.ProvinceRepository;
import com.javaweb.transaction.entity.Deposit;
import com.javaweb.transaction.entity.Transaction;
import com.javaweb.transaction.enums.DepositStatus;
import com.javaweb.transaction.enums.TransactionStatus;
import com.javaweb.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dashboard_day40_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommissionRepository commissionRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private FollowUpTaskRepository followUpTaskRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private LeadSourceRepository leadSourceRepository;

    @Autowired
    private ListingRepository listingRepository;

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

    private User admin;
    private User manager;
    private User agentOne;
    private User agentTwo;
    private String adminToken;
    private String managerToken;
    private String agentToken;

    @BeforeEach
    void setUp() throws Exception {
        commissionRepository.deleteAll();
        transactionRepository.deleteAll();
        appointmentRepository.deleteAll();
        followUpTaskRepository.deleteAll();
        leadRepository.deleteAll();
        leadSourceRepository.deleteAll();
        listingRepository.deleteAll();
        customerRepository.deleteAll();
        propertyRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        User owner = createUser("day40-owner@example.test", RoleCode.OWNER);
        agentOne = createUser("day40-agent-one@example.test", RoleCode.AGENT);
        agentTwo = createUser("day40-agent-two@example.test", RoleCode.AGENT);
        manager = createUser("day40-manager@example.test", RoleCode.MANAGER);
        admin = createUser("day40-admin@example.test", RoleCode.ADMIN);
        adminToken = login(admin.getEmail());
        managerToken = login(manager.getEmail());
        agentToken = login(agentOne.getEmail());

        Province province = provinceRepository.saveAndFlush(
                new Province("P-D40", "Day 40 Province")
        );
        PropertyType propertyType = propertyTypeRepository.findByCode("APARTMENT")
                .orElseThrow();
        Property property = new Property(
                "PROP-D40",
                "Day 40 Dashboard Property",
                propertyType,
                new Address(province, "40 Dashboard Street"),
                agentOne,
                PropertyPurpose.SALE
        );
        property.setStatus(PropertyStatus.AVAILABLE);
        property.setOwner(owner);
        property.setAssignedAgent(agentOne);
        property = propertyRepository.saveAndFlush(property);

        createListings(property);
        LeadSource source = leadSourceRepository.saveAndFlush(
                new LeadSource("D40-WEB", "Dashboard Web")
        );
        createLeads(source);

        Customer customer = new Customer("CUS-D40", "Day 40 Buyer", agentOne);
        customer.setAssignedAgent(agentOne);
        customer.setEmail("day40-buyer@example.test");
        customer = customerRepository.saveAndFlush(customer);
        createTransactions(property, customer, owner);
        createAgentWorkload(property, customer);
    }

    @Test
    void shouldReturnAdminDashboardMetrics() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/admin")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(5))
                .andExpect(jsonPath("$.data.totalProperties").value(1))
                .andExpect(jsonPath("$.data.totalListings").value(2))
                .andExpect(jsonPath("$.data.pendingListings").value(1))
                .andExpect(jsonPath("$.data.totalLeads").value(3))
                .andExpect(jsonPath("$.data.leadsByStatus[0].status").value("NEW"))
                .andExpect(jsonPath("$.data.leadsByStatus[0].count").value(1))
                .andExpect(jsonPath("$.data.totalTransactions").value(3))
                .andExpect(jsonPath("$.data.transactionsByStatus[4].status")
                        .value("COMPLETED"))
                .andExpect(jsonPath("$.data.transactionsByStatus[4].count").value(2))
                .andExpect(jsonPath("$.data.revenueSummary[0].currency").value("VND"))
                .andExpect(jsonPath("$.data.revenueSummary[0].completedTransactions")
                        .value(2))
                .andExpect(jsonPath(
                        "$.data.revenueSummary[0].completedTransactionValue"
                ).value(3000000))
                .andExpect(jsonPath("$.data.revenueSummary[0].completedPayments")
                        .value(1000000))
                .andExpect(jsonPath("$.data.revenueSummary[0].verifiedDeposits")
                        .value(2000000))
                .andExpect(jsonPath("$.data.revenueSummary[0].paidCommissions")
                        .value(25000))
                .andExpect(jsonPath("$.data.topAgents[0].agentId")
                        .value(agentOne.getId()))
                .andExpect(jsonPath("$.data.topAgents[0].totalTransactions").value(2))
                .andExpect(jsonPath("$.data.topAgents[0].completedTransactions")
                        .value(1));
    }

    @Test
    void shouldReturnManagerPerformanceAndEnforceRoles() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/manager")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAgents").value(2))
                .andExpect(jsonPath("$.data.totalLeads").value(3))
                .andExpect(jsonPath("$.data.leadCloseRate").value(50.0))
                .andExpect(jsonPath("$.data.totalTransactions").value(3))
                .andExpect(jsonPath("$.data.pendingCommissions").value(1))
                .andExpect(jsonPath("$.data.paidCommissions").value(1))
                .andExpect(jsonPath("$.data.topAgents.length()").value(2));

        mockMvc.perform(get("/api/v1/dashboard/admin")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/dashboard/manager")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/dashboard/manager"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnAgentDashboardForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/agent")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myLeads").value(2))
                .andExpect(jsonPath("$.data.todayAppointments").value(1))
                .andExpect(jsonPath("$.data.followUpTasks").value(2))
                .andExpect(jsonPath("$.data.overdueFollowUpTasks").value(1))
                .andExpect(jsonPath("$.data.activeTransactions").value(1))
                .andExpect(jsonPath("$.data.myCommissions").value(2))
                .andExpect(jsonPath("$.data.myCommissionAmounts[0].currency")
                        .value("VND"))
                .andExpect(jsonPath("$.data.myCommissionAmounts[0].count")
                        .value(2))
                .andExpect(jsonPath("$.data.myCommissionAmounts[0].amount")
                        .value(35000));

        mockMvc.perform(get("/api/v1/dashboard/agent"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnDateRangeReportsAndEnforceManagementRoles() throws Exception {
        String today = LocalDate.now(BUSINESS_ZONE).toString();

        mockMvc.perform(get("/api/v1/reports/revenue")
                        .param("from", today)
                        .param("to", today)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value(today))
                .andExpect(jsonPath("$.data.to").value(today))
                .andExpect(jsonPath("$.data.revenueSummary[0].currency").value("VND"))
                .andExpect(jsonPath("$.data.revenueSummary[0].completedTransactions")
                        .value(2))
                .andExpect(jsonPath("$.data.revenueSummary[0].completedPayments")
                        .value(1000000))
                .andExpect(jsonPath("$.data.revenueSummary[0].verifiedDeposits")
                        .value(2000000))
                .andExpect(jsonPath("$.data.revenueSummary[0].paidCommissions")
                        .value(25000));

        mockMvc.perform(get("/api/v1/reports/leads")
                        .param("from", today)
                        .param("to", today)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalLeads").value(3))
                .andExpect(jsonPath("$.data.leadsByStatus[0].status").value("NEW"));

        mockMvc.perform(get("/api/v1/reports/transactions")
                        .param("from", today)
                        .param("to", today)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalTransactions").value(3))
                .andExpect(jsonPath(
                        "$.data.completedTransactionValues[0].amount"
                ).value(3000000));

        mockMvc.perform(get("/api/v1/reports/commissions")
                        .param("from", today)
                        .param("to", today)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCommissions").value(2))
                .andExpect(jsonPath("$.data.commissionAmounts[0].amount")
                        .value(35000));

        mockMvc.perform(get("/api/v1/reports/leads")
                        .param("from", today)
                        .param("to", today)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidReportDateRange() throws Exception {
        mockMvc.perform(get("/api/v1/reports/revenue")
                        .param("from", "2026-06-12")
                        .param("to", "2026-06-11")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    private void createListings(Property property) {
        Listing pending = new Listing(
                "LIST-D40-PENDING",
                property,
                agentOne,
                "Pending dashboard listing",
                "pending-dashboard-listing",
                "Pending listing",
                ListingPurpose.SALE
        );
        pending.setStatus(ListingStatus.PENDING_REVIEW);
        pending.setSubmittedAt(Instant.now());
        listingRepository.save(pending);

        Listing published = new Listing(
                "LIST-D40-PUBLISHED",
                property,
                agentOne,
                "Published dashboard listing",
                "published-dashboard-listing",
                "Published listing",
                ListingPurpose.SALE
        );
        published.setStatus(ListingStatus.PUBLISHED);
        published.setPublishedAt(Instant.now());
        listingRepository.saveAndFlush(published);
    }

    private void createLeads(LeadSource source) {
        Lead newLead = lead(
                "LEAD-D40-NEW",
                source,
                LeadPipelineStatus.NEW,
                agentOne
        );
        FollowUpTask overdue = new FollowUpTask(
                "Call overdue lead",
                agentOne,
                manager,
                Instant.now().minusSeconds(3600)
        );
        overdue.setStatus(FollowUpTaskStatus.PENDING);
        newLead.addFollowUpTask(overdue);
        FollowUpTask upcoming = new FollowUpTask(
                "Prepare viewing",
                agentOne,
                manager,
                Instant.now().plusSeconds(3600)
        );
        upcoming.setStatus(FollowUpTaskStatus.IN_PROGRESS);
        newLead.addFollowUpTask(upcoming);
        leadRepository.save(newLead);
        leadRepository.save(lead(
                "LEAD-D40-WON",
                source,
                LeadPipelineStatus.CLOSED_WON,
                agentOne
        ));
        leadRepository.saveAndFlush(lead(
                "LEAD-D40-LOST",
                source,
                LeadPipelineStatus.CLOSED_LOST,
                agentTwo
        ));
    }

    private Lead lead(
            String code,
            LeadSource source,
            LeadPipelineStatus status,
            User assignee
    ) {
        Lead lead = new Lead(code, source, code);
        lead.setEmail(code.toLowerCase() + "@example.test");
        lead.setStatus(status);
        lead.setCurrentAssignee(assignee);
        return lead;
    }

    private void createTransactions(
            Property property,
            Customer customer,
            User owner
    ) {
        Transaction first = transaction(
                "TRAN-D40-ONE",
                new BigDecimal("1000000"),
                property,
                customer,
                owner,
                agentOne,
                TransactionStatus.COMPLETED
        );
        Payment payment = new Payment(
                agentOne,
                new BigDecimal("1000000"),
                PaymentMethod.BANK_TRANSFER,
                "PAY-D40-ONE"
        );
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(Instant.now());
        payment.setConfirmedAt(Instant.now());
        first.addPayment(payment);
        first = transactionRepository.saveAndFlush(first);

        Transaction inProgress = transaction(
                "TRAN-D40-TWO",
                new BigDecimal("500000"),
                property,
                customer,
                owner,
                agentOne,
                TransactionStatus.PAYMENT_IN_PROGRESS
        );
        inProgress = transactionRepository.saveAndFlush(inProgress);

        Transaction secondCompleted = transaction(
                "TRAN-D40-THREE",
                new BigDecimal("2000000"),
                property,
                customer,
                owner,
                agentTwo,
                TransactionStatus.COMPLETED
        );
        Deposit deposit = new Deposit(
                agentTwo,
                new BigDecimal("2000000"),
                PaymentMethod.BANK_TRANSFER,
                "DEP-D40-THREE"
        );
        deposit.setStatus(DepositStatus.VERIFIED);
        deposit.setReceivedAt(Instant.now());
        deposit.setVerifiedAt(Instant.now());
        secondCompleted.addDeposit(deposit);
        secondCompleted = transactionRepository.saveAndFlush(secondCompleted);

        Commission paid = new Commission(
                first,
                agentOne,
                first.getAgreedValue(),
                new BigDecimal("25000")
        );
        paid.setStatus(CommissionStatus.PAID);
        paid.setApprovedBy(manager);
        paid.setApprovedAt(Instant.now());
        paid.setPaidBy(manager);
        paid.setPaidAt(Instant.now());
        commissionRepository.save(paid);

        Commission pending = new Commission(
                inProgress,
                agentOne,
                inProgress.getAgreedValue(),
                new BigDecimal("10000")
        );
        commissionRepository.saveAndFlush(pending);
    }

    private void createAgentWorkload(Property property, Customer customer) {
        Instant startAt = LocalDate.now(BUSINESS_ZONE)
                .atTime(18, 0)
                .atZone(BUSINESS_ZONE)
                .toInstant();
        Appointment appointment = new Appointment(
                "APT-D40-TODAY",
                customer,
                agentOne,
                property,
                manager,
                "Today property viewing",
                startAt,
                startAt.plusSeconds(3600)
        );
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setConfirmedAt(Instant.now());
        appointmentRepository.saveAndFlush(appointment);
    }

    private Transaction transaction(
            String code,
            BigDecimal value,
            Property property,
            Customer customer,
            User owner,
            User agent,
            TransactionStatus status
    ) {
        Transaction transaction = new Transaction(
                code,
                ContractType.SALE,
                value,
                property,
                customer,
                owner,
                agent,
                agent
        );
        transaction.setStatus(status);
        if (status == TransactionStatus.COMPLETED) {
            transaction.setCompletedAt(Instant.now());
        }
        return transaction;
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(
                email,
                passwordEncoder.encode(PASSWORD),
                roleCode + " Dashboard User"
        );
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
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
