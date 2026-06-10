package com.javaweb.notification.job;

import com.javaweb.notification.service.ReminderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.reminders",
        name = "enabled",
        havingValue = "true"
)
public class ReminderScheduler {
    private final ReminderService reminderService;

    public ReminderScheduler(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @Scheduled(
            cron = "${app.reminders.cron:0 */5 * * * *}",
            zone = "${app.reminders.zone:UTC}"
    )
    public void sendReminders() {
        reminderService.processAppointmentReminders();
        reminderService.processFollowUpReminders();
    }
}
