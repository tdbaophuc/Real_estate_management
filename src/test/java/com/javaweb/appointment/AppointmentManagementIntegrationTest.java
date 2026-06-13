package com.javaweb.appointment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.appointment.entity.Appointment;
import com.javaweb.appointment.enums.AppointmentStatus;
import com.javaweb.appointment.repository.AppointmentParticipantRepository;
import com.javaweb.appointment.repository.AppointmentRepository;
import com.javaweb.appointment.repository.ViewingFeedbackRepository;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.repository.ListingRepository;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.enums.PropertyStatus;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:appointment_management_day26_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentManagementIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentParticipantRepository participantRepository;

    @Autowired
    private ViewingFeedbackRepository feedbackRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ListingRepository listingRepository;

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
    private User customerUser;
    private Customer customer;
    private Property property;
    private Property secondProperty;
    private Listing listing;
    private String agentToken;
    private String secondAgentToken;
    private String managerToken;
    private String customerToken;
    private Instant startAt;

    @BeforeEach
    void setUp() throws Exception {
        appointmentRepository.deleteAll();
        listingRepository.deleteAll();
        customerRepository.deleteAll();
        propertyRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        agent = createUser("day26-agent@example.test", RoleCode.AGENT);
        secondAgent = createUser("day26-second-agent@example.test", RoleCode.AGENT);
        manager = createUser("day26-manager@example.test", RoleCode.MANAGER);
        customerUser = createUser("day26-customer@example.test", RoleCode.CUSTOMER);
        agentToken = login(agent.getEmail());
        secondAgentToken = login(secondAgent.getEmail());
        managerToken = login(manager.getEmail());
        customerToken = login(customerUser.getEmail());

        Province province = provinceRepository.saveAndFlush(
                new Province("P-D26", "Day 26 Province")
        );
        PropertyType propertyType = propertyTypeRepository.findByCode("APARTMENT")
                .orElseThrow();
        property = createProperty(
                "PROP-D26-1",
                "Day 26 Property One",
                "26 Appointment Street",
                province,
                propertyType
        );
        secondProperty = createProperty(
                "PROP-D26-2",
                "Day 26 Property Two",
                "28 Appointment Street",
                province,
                propertyType
        );
        listing = new Listing(
                "LISTING-D26",
                property,
                agent,
                "Day 26 Published Listing",
                "day-26-published-listing",
                "Listing for appointment API integration tests",
                ListingPurpose.SALE
        );
        listing.setStatus(ListingStatus.PUBLISHED);
        listing = listingRepository.saveAndFlush(listing);

        customer = new Customer("CUS-D26", "Day 26 Customer", agent);
        customer.setUser(customerUser);
        customer.setAssignedAgent(agent);
        customer.setEmail(customerUser.getEmail());
        customer = customerRepository.saveAndFlush(customer);
        startAt = Instant.now().plus(3, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    void shouldCompleteCustomerAndAgentViewingWorkflow() throws Exception {
        Long appointmentId = createAppointment(
                validRequest("APT-D26-FLOW", agent.getId(), property.getId(), startAt),
                agentToken
        );

        mockMvc.perform(get("/api/v1/appointments/my")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(appointmentId))
                .andExpect(jsonPath("$.data.content[0].participants.length()").value(2));

        mockMvc.perform(patch("/api/v1/appointments/{id}/confirm", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.confirmedAt").isNotEmpty())
                .andExpect(jsonPath(
                        "$.data.participants[?(@.userId == "
                                + customerUser.getId()
                                + ")].responseStatus"
                ).value("ACCEPTED"));

        mockMvc.perform(patch("/api/v1/appointments/{id}/complete", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").isNotEmpty());

        mockMvc.perform(post("/api/v1/appointments/{id}/feedback", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "rating", 5,
                                "interestLevel", "HIGH",
                                "comments", "Customer wants to negotiate",
                                "positivePoints", "Good location",
                                "concerns", "Price",
                                "nextAction", "Prepare an offer"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.appointmentId").value(appointmentId))
                .andExpect(jsonPath("$.data.submittedById").value(agent.getId()))
                .andExpect(jsonPath("$.data.rating").value(5));

        mockMvc.perform(post("/api/v1/appointments/{id}/feedback", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "interestLevel", "MEDIUM"
                        ))))
                .andExpect(status().isConflict());

        assertThat(feedbackRepository
                .findAllByAppointmentIdOrderByCreatedAtDesc(appointmentId))
                .hasSize(1);
    }

    @Test
    void shouldRejectAgentAndPropertyScheduleConflicts() throws Exception {
        createAppointment(
                validRequest("APT-D26-BASE", agent.getId(), property.getId(), startAt),
                managerToken
        );
        Instant overlap = startAt.plus(30, ChronoUnit.MINUTES);

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(
                                "APT-D26-AGENT-CONFLICT",
                                agent.getId(),
                                secondProperty.getId(),
                                overlap
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Agent already has an overlapping appointment"));

        mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(
                                "APT-D26-PROPERTY-CONFLICT",
                                secondAgent.getId(),
                                property.getId(),
                                overlap
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Property already has an overlapping appointment"));
    }

    @Test
    void shouldRescheduleAsNewAppointmentAndReleaseOldSlot() throws Exception {
        Long originalId = createAppointment(
                validRequest("APT-D26-RESCHEDULE", agent.getId(), property.getId(), startAt),
                agentToken
        );
        Instant newStart = startAt.plus(1, ChronoUnit.DAYS);
        MvcResult result = mockMvc.perform(
                        patch("/api/v1/appointments/{id}/reschedule", originalId)
                                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of(
                                        "startAt", newStart.toString(),
                                        "endAt", newStart.plus(1, ChronoUnit.HOURS).toString(),
                                        "meetingLocation", "Updated lobby"
                                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.rescheduledFromId").value(originalId))
                .andExpect(jsonPath("$.data.meetingLocation").value("Updated lobby"))
                .andReturn();
        Long replacementId = responseDataId(result);

        Appointment original = appointmentRepository.findById(originalId).orElseThrow();
        Appointment replacement = appointmentRepository.findById(replacementId).orElseThrow();
        assertThat(original.getStatus()).isEqualTo(AppointmentStatus.RESCHEDULED);
        assertThat(replacement.getRescheduledFrom().getId()).isEqualTo(originalId);
        assertThat(participantRepository
                .findAllByAppointmentIdOrderByCreatedAtAsc(replacementId))
                .hasSize(2);

        createAppointment(
                validRequest(
                        "APT-D26-REUSE-SLOT",
                        secondAgent.getId(),
                        property.getId(),
                        startAt
                ),
                managerToken
        );
    }

    @Test
    void customerShouldCancelAndCancelledSlotShouldNotConflict() throws Exception {
        Long appointmentId = createAppointment(
                validRequest("APT-D26-CANCEL", agent.getId(), property.getId(), startAt),
                agentToken
        );

        mockMvc.perform(patch("/api/v1/appointments/{id}/cancel", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Customer is unavailable"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason")
                        .value("Customer is unavailable"))
                .andExpect(jsonPath("$.data.cancelledById").value(customerUser.getId()));

        createAppointment(
                validRequest(
                        "APT-D26-AFTER-CANCEL",
                        secondAgent.getId(),
                        property.getId(),
                        startAt
                ),
                managerToken
        );
    }

    @Test
    void shouldEnforceAppointmentVisibilityAndOperationRoles() throws Exception {
        Long appointmentId = createAppointment(
                validRequest("APT-D26-AUTH", agent.getId(), property.getId(), startAt),
                agentToken
        );

        mockMvc.perform(get("/api/v1/appointments/{id}", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(secondAgentToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/appointments/{id}/complete", appointmentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .queryParam("agentId", agent.getId().toString())
                        .queryParam("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(appointmentId));

        mockMvc.perform(get("/api/v1/appointments/{id}", appointmentId))
                .andExpect(status().isUnauthorized());
    }

    private Property createProperty(
            String code,
            String name,
            String street,
            Province province,
            PropertyType propertyType
    ) {
        Property created = new Property(
                code,
                name,
                propertyType,
                new Address(province, street),
                agent,
                PropertyPurpose.SALE
        );
        created.setStatus(PropertyStatus.AVAILABLE);
        created.setAssignedAgent(agent);
        return propertyRepository.saveAndFlush(created);
    }

    private Long createAppointment(
            Map<String, Object> request,
            String token
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/appointments")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return responseDataId(result);
    }

    private Long responseDataId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );
        return response.at("/data/id").longValue();
    }

    private Map<String, Object> validRequest(
            String code,
            Long agentId,
            Long propertyId,
            Instant requestedStart
    ) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", code);
        request.put("customerId", customer.getId());
        request.put("agentId", agentId);
        request.put("propertyId", propertyId);
        if (propertyId.equals(property.getId())) {
            request.put("listingId", listing.getId());
        }
        request.put("title", "Property viewing");
        request.put("startAt", requestedStart.toString());
        request.put(
                "endAt",
                requestedStart.plus(1, ChronoUnit.HOURS).toString()
        );
        request.put("timezone", "Asia/Ho_Chi_Minh");
        request.put("meetingLocation", "Property lobby");
        request.put("notes", "Day 26 appointment");
        return request;
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(
                email,
                passwordEncoder.encode(PASSWORD),
                roleCode + " Appointment User"
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
