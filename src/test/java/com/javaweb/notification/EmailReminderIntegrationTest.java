package com.javaweb.notification;

import com.javaweb.appointment.entity.Appointment;
import com.javaweb.appointment.enums.AppointmentStatus;
import com.javaweb.appointment.repository.AppointmentRepository;
import com.javaweb.auth.entity.Role;
import com.javaweb.auth.entity.User;
import com.javaweb.auth.enums.RoleCode;
import com.javaweb.auth.enums.UserStatus;
import com.javaweb.auth.repository.RoleRepository;
import com.javaweb.auth.repository.UserRepository;
import com.javaweb.customer.entity.Customer;
import com.javaweb.customer.repository.CustomerRepository;
import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.entity.Lead;
import com.javaweb.lead.entity.LeadSource;
import com.javaweb.lead.enums.FollowUpTaskStatus;
import com.javaweb.lead.repository.FollowUpTaskRepository;
import com.javaweb.lead.repository.LeadRepository;
import com.javaweb.lead.repository.LeadSourceRepository;
import com.javaweb.notification.email.EmailSender;
import com.javaweb.notification.email.LoggingEmailSender;
import com.javaweb.notification.entity.EmailLog;
import com.javaweb.notification.enums.EmailDeliveryStatus;
import com.javaweb.notification.job.ReminderScheduler;
import com.javaweb.notification.repository.EmailLogRepository;
import com.javaweb.notification.service.ReminderService;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:email_reminder_day28_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "app.reminders.enabled=false",
        "app.reminders.appointment-look-ahead=PT24H",
        "app.reminders.follow-up-look-ahead=PT24H",
        "app.reminders.batch-size=50"
})
@ActiveProfiles("test")
@Import(EmailReminderIntegrationTest.FixedClockConfig.class)
class EmailReminderIntegrationTest {
    private static final Instant NOW = Instant.parse("2030-01-15T01:00:00Z");

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private EmailLogRepository emailLogRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private FollowUpTaskRepository followUpTaskRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private LeadSourceRepository leadSourceRepository;

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
    private EmailSender emailSender;

    @Autowired
    private ApplicationContext applicationContext;

    private User agent;
    private User customerUser;
    private Customer customer;
    private Property property;

    @BeforeEach
    void setUp() {
        emailLogRepository.deleteAll();
        appointmentRepository.deleteAll();
        followUpTaskRepository.deleteAll();
        leadRepository.deleteAll();
        customerRepository.deleteAll();
        propertyRepository.deleteAll();
        provinceRepository.deleteAll();
        userRepository.deleteAll();

        agent = createUser("day28-agent@example.test", RoleCode.AGENT);
        customerUser = createUser("day28-customer@example.test", RoleCode.CUSTOMER);
        Province province = provinceRepository.saveAndFlush(
                new Province("P-D28", "Day 28 Province")
        );
        PropertyType type = propertyTypeRepository.findByCode("APARTMENT")
                .orElseThrow();
        property = new Property(
                "PROP-D28",
                "Day 28 Reminder Property",
                type,
                new Address(province, "28 Reminder Street"),
                agent,
                PropertyPurpose.SALE
        );
        property.setStatus(PropertyStatus.AVAILABLE);
        property = propertyRepository.saveAndFlush(property);

        customer = new Customer("CUS-D28", "Day 28 Customer", agent);
        customer.setUser(customerUser);
        customer.setEmail(customerUser.getEmail());
        customer.setAssignedAgent(agent);
        customer = customerRepository.saveAndFlush(customer);
    }

    @Test
    void shouldSendAndLogAppointmentAndFollowUpRemindersOnce() {
        Appointment appointment = createAppointment(
                "APT-D28-DUE",
                NOW.plus(2, ChronoUnit.HOURS),
                AppointmentStatus.CONFIRMED
        );
        FollowUpTask task = createFollowUpTask(
                "LEAD-D28-DUE",
                NOW.plus(3, ChronoUnit.HOURS),
                FollowUpTaskStatus.PENDING
        );

        assertThat(reminderService.processAppointmentReminders()).isEqualTo(1);
        assertThat(reminderService.processFollowUpReminders()).isEqualTo(1);

        List<EmailLog> logs = emailLogRepository.findAll();
        assertThat(logs).hasSize(3);
        assertThat(logs)
                .extracting(EmailLog::getStatus)
                .containsOnly(EmailDeliveryStatus.SENT);
        assertThat(logs)
                .extracting(EmailLog::getProviderMessageId)
                .allSatisfy(messageId -> assertThat(messageId).startsWith("log-"));
        assertThat(logs)
                .extracting(EmailLog::getBody)
                .allSatisfy(body -> assertThat(body).doesNotContain("{{"));
        assertThat(logs)
                .extracting(EmailLog::getReferenceType)
                .containsExactlyInAnyOrder(
                        "APPOINTMENT",
                        "APPOINTMENT",
                        "FOLLOW_UP_TASK"
                );

        Appointment updatedAppointment = appointmentRepository
                .findById(appointment.getId())
                .orElseThrow();
        FollowUpTask updatedTask = followUpTaskRepository
                .findById(task.getId())
                .orElseThrow();
        assertThat(updatedAppointment.getReminderSentAt()).isEqualTo(NOW);
        assertThat(updatedTask.getReminderSentAt()).isEqualTo(NOW);

        assertThat(reminderService.processAppointmentReminders()).isZero();
        assertThat(reminderService.processFollowUpReminders()).isZero();
        assertThat(emailLogRepository.count()).isEqualTo(3);
    }

    @Test
    void shouldIgnoreInactiveOrOutOfWindowReminderSources() {
        createAppointment(
                "APT-D28-CANCELLED",
                NOW.plus(1, ChronoUnit.HOURS),
                AppointmentStatus.CANCELLED
        );
        createAppointment(
                "APT-D28-LATER",
                NOW.plus(2, ChronoUnit.DAYS),
                AppointmentStatus.PENDING
        );
        createFollowUpTask(
                "LEAD-D28-COMPLETED",
                NOW.plus(1, ChronoUnit.HOURS),
                FollowUpTaskStatus.COMPLETED
        );
        createFollowUpTask(
                "LEAD-D28-LATER",
                NOW.plus(2, ChronoUnit.DAYS),
                FollowUpTaskStatus.PENDING
        );

        assertThat(reminderService.processAppointmentReminders()).isZero();
        assertThat(reminderService.processFollowUpReminders()).isZero();
        assertThat(emailLogRepository.count()).isZero();
    }

    @Test
    void testProfileShouldUseLoggingSenderAndKeepSchedulerDisabled() {
        assertThat(emailSender).isInstanceOf(LoggingEmailSender.class);
        assertThat(applicationContext.getBeansOfType(ReminderScheduler.class))
                .isEmpty();
    }

    private Appointment createAppointment(
            String code,
            Instant startAt,
            AppointmentStatus status
    ) {
        Appointment appointment = new Appointment(
                code,
                customer,
                agent,
                property,
                agent,
                "Property viewing",
                startAt,
                startAt.plus(1, ChronoUnit.HOURS)
        );
        appointment.setStatus(status);
        if (status == AppointmentStatus.CONFIRMED) {
            appointment.setConfirmedAt(NOW.minus(1, ChronoUnit.HOURS));
        }
        if (status == AppointmentStatus.CANCELLED) {
            appointment.setCancellationReason("Cancelled test appointment");
            appointment.setCancelledAt(NOW.minus(1, ChronoUnit.HOURS));
            appointment.setCancelledBy(agent);
        }
        appointment.setMeetingLocation("Property lobby");
        return appointmentRepository.saveAndFlush(appointment);
    }

    private FollowUpTask createFollowUpTask(
            String leadCode,
            Instant dueAt,
            FollowUpTaskStatus status
    ) {
        LeadSource source = leadSourceRepository.findByCodeAndActiveTrue("MANUAL")
                .orElseThrow();
        Lead lead = new Lead(leadCode, source, "Day 28 Prospect");
        lead.setPhone("0900000028");
        lead.setCustomer(customer);
        lead.setCurrentAssignee(agent);
        lead.setCreatedBy(agent);
        FollowUpTask task = new FollowUpTask(
                "Call prospect",
                agent,
                agent,
                dueAt
        );
        task.setStatus(status);
        if (status == FollowUpTaskStatus.COMPLETED) {
            task.setCompletedAt(NOW.minus(1, ChronoUnit.HOURS));
        }
        lead.addFollowUpTask(task);
        leadRepository.saveAndFlush(lead);
        return task;
    }

    private User createUser(String email, RoleCode roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = new User(email, "encoded-password", roleCode + " Reminder User");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedReminderClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
