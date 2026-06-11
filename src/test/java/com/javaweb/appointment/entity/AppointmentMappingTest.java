package com.javaweb.appointment.entity;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentMappingTest {
    private static final List<Class<?>> APPOINTMENT_ENTITIES = List.of(
            Appointment.class,
            AppointmentParticipant.class,
            ViewingFeedback.class
    );

    @Test
    void shouldDeclareAllEntityRelationshipsAsLazy() {
        for (Class<?> entityType : APPOINTMENT_ENTITIES) {
            for (Field field : entityType.getDeclaredFields()) {
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                if (manyToOne != null) {
                    assertThat(manyToOne.fetch())
                            .as("%s.%s should be lazy", entityType.getSimpleName(), field.getName())
                            .isEqualTo(FetchType.LAZY);
                }
                OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                if (oneToMany != null) {
                    assertThat(oneToMany.fetch())
                            .as("%s.%s should be lazy", entityType.getSimpleName(), field.getName())
                            .isEqualTo(FetchType.LAZY);
                }
            }
        }
    }

    @Test
    void shouldPersistAppointmentEnumsAsStrings() {
        assertStringEnum(Appointment.class, "status");
        assertStringEnum(AppointmentParticipant.class, "participantRole");
        assertStringEnum(AppointmentParticipant.class, "responseStatus");
        assertStringEnum(ViewingFeedback.class, "interestLevel");
    }

    @Test
    void shouldDeclareParticipantAndFeedbackUniqueness() {
        assertUniqueConstraint(
                AppointmentParticipant.class,
                "appointment_id",
                "user_id"
        );
        assertUniqueConstraint(
                ViewingFeedback.class,
                "appointment_id",
                "submitted_by"
        );
    }

    private void assertStringEnum(Class<?> entityType, String fieldName) {
        try {
            Enumerated enumerated =
                    entityType.getDeclaredField(fieldName).getAnnotation(Enumerated.class);
            assertThat(enumerated).isNotNull();
            assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
        } catch (NoSuchFieldException exception) {
            throw new AssertionError(
                    "Missing field " + entityType.getSimpleName() + "." + fieldName,
                    exception
            );
        }
    }

    private void assertUniqueConstraint(Class<?> entityType, String... columnNames) {
        Table table = entityType.getAnnotation(Table.class);
        assertThat(table).isNotNull();
        assertThat(Arrays.stream(table.uniqueConstraints()))
                .anySatisfy(constraint ->
                        assertThat(constraint.columnNames()).containsExactly(columnNames));
    }
}
