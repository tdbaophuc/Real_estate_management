package com.javaweb.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.reminders")
public record ReminderProperties(
        Duration appointmentLookAhead,
        Duration followUpLookAhead,
        Integer batchSize
) {
    public ReminderProperties {
        appointmentLookAhead = appointmentLookAhead == null
                ? Duration.ofHours(24)
                : appointmentLookAhead;
        followUpLookAhead = followUpLookAhead == null
                ? Duration.ofHours(24)
                : followUpLookAhead;
        batchSize = batchSize == null ? 100 : batchSize;
        if (appointmentLookAhead.isNegative() || appointmentLookAhead.isZero()) {
            throw new IllegalArgumentException(
                    "app.reminders.appointment-look-ahead must be positive"
            );
        }
        if (followUpLookAhead.isNegative() || followUpLookAhead.isZero()) {
            throw new IllegalArgumentException(
                    "app.reminders.follow-up-look-ahead must be positive"
            );
        }
        if (batchSize < 1 || batchSize > 1000) {
            throw new IllegalArgumentException(
                    "app.reminders.batch-size must be between 1 and 1000"
            );
        }
    }
}
