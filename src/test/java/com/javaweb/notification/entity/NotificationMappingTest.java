package com.javaweb.notification.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMappingTest {

    @Test
    void shouldMapNotificationEntitiesToExpectedTables() {
        assertTable(Notification.class, "notifications");
        assertTable(NotificationTemplate.class, "notification_templates");
        assertTable(EmailLog.class, "email_logs");
    }

    private void assertTable(Class<?> type, String tableName) {
        assertThat(type).hasAnnotation(Entity.class);
        assertThat(type.getAnnotation(Table.class).name()).isEqualTo(tableName);
    }
}
