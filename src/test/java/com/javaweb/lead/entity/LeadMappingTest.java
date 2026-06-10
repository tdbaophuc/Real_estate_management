package com.javaweb.lead.entity;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LeadMappingTest {
    private static final List<Class<?>> LEAD_ENTITIES = List.of(
            Lead.class,
            LeadSource.class,
            LeadAssignment.class,
            LeadNote.class,
            LeadActivity.class,
            FollowUpTask.class
    );

    @Test
    void shouldDeclareAllEntityRelationshipsAsLazy() {
        for (Class<?> entityType : LEAD_ENTITIES) {
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
    void shouldPersistLeadWorkflowEnumsAsStrings() {
        assertStringEnum(Lead.class, "status");
        assertStringEnum(Lead.class, "priority");
        assertStringEnum(LeadActivity.class, "activityType");
        assertStringEnum(FollowUpTask.class, "status");
        assertStringEnum(FollowUpTask.class, "priority");
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
}
