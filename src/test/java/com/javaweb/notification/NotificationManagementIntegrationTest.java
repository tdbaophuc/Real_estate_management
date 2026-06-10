package com.javaweb.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.notification.entity.Notification;
import com.javaweb.notification.repository.EmailLogRepository;
import com.javaweb.notification.repository.NotificationRepository;
import com.javaweb.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification_management_day27_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationManagementIntegrationTest {
    private static final String PASSWORD = "StrongPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private EmailLogRepository emailLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User customer;
    private User agent;
    private String customerToken;
    private String agentToken;
    private Notification customerUnread;
    private Notification customerRead;
    private Notification agentUnread;

    @BeforeEach
    void setUp() throws Exception {
        emailLogRepository.deleteAll();
        notificationRepository.deleteAll();
        templateRepository.deleteAll();
        userRepository.deleteAll();

        customer = createUser("day27-customer@example.test", RoleCode.CUSTOMER);
        agent = createUser("day27-agent@example.test", RoleCode.AGENT);
        customerToken = login(customer.getEmail());
        agentToken = login(agent.getEmail());

        customerUnread = notificationRepository.saveAndFlush(notification(
                customer,
                "APPOINTMENT_REMINDER",
                "Upcoming viewing",
                "Your viewing starts tomorrow"
        ));
        customerRead = notification(
                customer,
                "LISTING_MATCH",
                "New listing match",
                "A new listing matches your saved search"
        );
        customerRead.setReadAt(Instant.now());
        customerRead = notificationRepository.saveAndFlush(customerRead);
        agentUnread = notificationRepository.saveAndFlush(notification(
                agent,
                "FOLLOW_UP_DUE",
                "Follow-up due",
                "A lead follow-up is due"
        ));
    }

    @Test
    void shouldListOnlyCurrentUsersNotificationsWithUnreadFilter() throws Exception {
        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content.length()").value(2));

        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                        .queryParam("unread", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(customerUnread.getId()))
                .andExpect(jsonPath("$.data.content[0].read").value(false));

        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, bearer(agentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(agentUnread.getId()));
    }

    @Test
    void shouldCountAndMarkSingleNotificationReadIdempotently() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        mockMvc.perform(patch(
                                "/api/v1/notifications/{id}/read",
                                customerUnread.getId()
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        mockMvc.perform(patch(
                                "/api/v1/notifications/{id}/read",
                                customerUnread.getId()
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void shouldMarkAllCurrentUsersNotificationsReadWithoutChangingOthers() throws Exception {
        notificationRepository.saveAndFlush(notification(
                customer,
                "SYSTEM",
                "Second unread notification",
                "Another notification"
        ));

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(2));

        assertThat(notificationRepository
                .countByRecipientIdAndReadAtIsNull(customer.getId()))
                .isZero();
        assertThat(notificationRepository
                .countByRecipientIdAndReadAtIsNull(agent.getId()))
                .isEqualTo(1);
    }

    @Test
    void shouldHideOtherUsersNotificationsAndRequireAuthentication() throws Exception {
        mockMvc.perform(patch(
                                "/api/v1/notifications/{id}/read",
                                agentUnread.getId()
                        )
                        .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    private Notification notification(
            User recipient,
            String type,
            String title,
            String message
    ) {
        Notification notification = new Notification(recipient, type, title, message);
        notification.setActionUrl("/app/notifications");
        notification.setReferenceType("TEST");
        notification.setReferenceId(27L);
        notification.setMetadataJson("{\"day\":27}");
        return notification;
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(
                email,
                passwordEncoder.encode(PASSWORD),
                roleCode + " Notification User"
        );
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }

    private String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
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
