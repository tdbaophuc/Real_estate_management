package com.javaweb.notification.service;

import com.javaweb.appointment.entity.Appointment;
import com.javaweb.appointment.enums.AppointmentStatus;
import com.javaweb.appointment.repository.AppointmentRepository;
import com.javaweb.auth.entity.User;
import com.javaweb.common.exception.ResourceNotFoundException;
import com.javaweb.lead.entity.FollowUpTask;
import com.javaweb.lead.enums.FollowUpTaskStatus;
import com.javaweb.lead.repository.FollowUpTaskRepository;
import com.javaweb.notification.config.ReminderProperties;
import com.javaweb.notification.email.EmailDeliveryRequest;
import com.javaweb.notification.email.EmailDeliveryService;
import com.javaweb.notification.entity.NotificationTemplate;
import com.javaweb.notification.repository.NotificationTemplateRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ReminderService {
    public static final String APPOINTMENT_TEMPLATE_CODE =
            "APPOINTMENT_REMINDER_EMAIL";
    public static final String FOLLOW_UP_TEMPLATE_CODE =
            "FOLLOW_UP_REMINDER_EMAIL";
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    private final AppointmentRepository appointmentRepository;
    private final FollowUpTaskRepository followUpTaskRepository;
    private final NotificationTemplateRepository templateRepository;
    private final EmailDeliveryService emailDeliveryService;
    private final ReminderProperties properties;
    private final Clock clock;

    public ReminderService(
            AppointmentRepository appointmentRepository,
            FollowUpTaskRepository followUpTaskRepository,
            NotificationTemplateRepository templateRepository,
            EmailDeliveryService emailDeliveryService,
            ReminderProperties properties,
            Clock clock
    ) {
        this.appointmentRepository = appointmentRepository;
        this.followUpTaskRepository = followUpTaskRepository;
        this.templateRepository = templateRepository;
        this.emailDeliveryService = emailDeliveryService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public int processAppointmentReminders() {
        Instant now = clock.instant();
        NotificationTemplate template = requireTemplate(
                APPOINTMENT_TEMPLATE_CODE
        );
        var appointments = appointmentRepository
                .findAllByReminderSentAtIsNullAndStatusInAndStartAtBetweenOrderByStartAtAsc(
                        java.util.Set.of(
                                AppointmentStatus.PENDING,
                                AppointmentStatus.CONFIRMED
                        ),
                        now,
                        now.plus(properties.appointmentLookAhead()),
                        PageRequest.of(0, properties.batchSize())
                );
        for (Appointment appointment : appointments) {
            sendAppointmentReminder(template, appointment, now);
            appointment.setReminderSentAt(now);
        }
        appointmentRepository.saveAll(appointments);
        return appointments.size();
    }

    @Transactional
    public int processFollowUpReminders() {
        Instant now = clock.instant();
        NotificationTemplate template = requireTemplate(FOLLOW_UP_TEMPLATE_CODE);
        var tasks = followUpTaskRepository
                .findAllByReminderSentAtIsNullAndStatusInAndDueAtBetweenOrderByDueAtAsc(
                        java.util.Set.of(
                                FollowUpTaskStatus.PENDING,
                                FollowUpTaskStatus.IN_PROGRESS
                        ),
                        now,
                        now.plus(properties.followUpLookAhead()),
                        PageRequest.of(0, properties.batchSize())
                );
        for (FollowUpTask task : tasks) {
            User recipient = task.getAssignedTo();
            emailDeliveryService.deliver(new EmailDeliveryRequest(
                    template,
                    recipient,
                    recipient.getEmail(),
                    "FOLLOW_UP_TASK",
                    task.getId(),
                    task.getDueAt(),
                    Map.of(
                            "recipientName", recipient.getFullName(),
                            "taskTitle", task.getTitle(),
                            "leadName", task.getLead().getFullName(),
                            "dueTime", format(task.getDueAt(), ZoneId.of("UTC"))
                    )
            ));
            task.setReminderSentAt(now);
        }
        followUpTaskRepository.saveAll(tasks);
        return tasks.size();
    }

    private void sendAppointmentReminder(
            NotificationTemplate template,
            Appointment appointment,
            Instant now
    ) {
        Map<String, User> recipients = new LinkedHashMap<>();
        User customerUser = appointment.getCustomer().getUser();
        String customerEmail = appointment.getCustomer().getEmail();
        if ((customerEmail == null || customerEmail.isBlank())
                && customerUser != null) {
            customerEmail = customerUser.getEmail();
        }
        if (customerEmail != null && !customerEmail.isBlank()) {
            recipients.put(customerEmail.toLowerCase(), customerUser);
        }
        recipients.putIfAbsent(
                appointment.getAgent().getEmail().toLowerCase(),
                appointment.getAgent()
        );

        ZoneId zone = resolveZone(appointment.getTimezone());
        for (Map.Entry<String, User> recipient : recipients.entrySet()) {
            User user = recipient.getValue();
            String recipientName = user == null
                    ? appointment.getCustomer().getFullName()
                    : user.getFullName();
            emailDeliveryService.deliver(new EmailDeliveryRequest(
                    template,
                    user,
                    recipient.getKey(),
                    "APPOINTMENT",
                    appointment.getId(),
                    now,
                    Map.of(
                            "recipientName", recipientName,
                            "appointmentTitle", appointment.getTitle(),
                            "startTime", format(appointment.getStartAt(), zone),
                            "meetingLocation", valueOrDefault(
                                    appointment.getMeetingLocation(),
                                    "To be confirmed"
                            )
                    )
            ));
        }
    }

    private NotificationTemplate requireTemplate(String code) {
        return templateRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active email template not found: " + code
                ));
    }

    private ZoneId resolveZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (RuntimeException exception) {
            return ZoneId.of("UTC");
        }
    }

    private String format(Instant instant, ZoneId zone) {
        return DATE_TIME_FORMAT.format(instant.atZone(zone));
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
