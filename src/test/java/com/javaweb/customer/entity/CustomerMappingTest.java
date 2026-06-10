package com.javaweb.customer.entity;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerMappingTest {
    private static final List<Class<?>> CUSTOMER_ENTITIES = List.of(
            Customer.class,
            CustomerRequirement.class,
            CustomerTag.class,
            CustomerNote.class,
            CustomerFavoriteListing.class
    );

    @Test
    void shouldDeclareAllEntityRelationshipsAsLazy() {
        for (Class<?> entityType : CUSTOMER_ENTITIES) {
            for (Field field : entityType.getDeclaredFields()) {
                ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
                if (manyToOne != null) {
                    assertThat(manyToOne.fetch())
                            .as("%s.%s should be lazy", entityType.getSimpleName(), field.getName())
                            .isEqualTo(FetchType.LAZY);
                }
                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                if (oneToOne != null) {
                    assertThat(oneToOne.fetch())
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
    void shouldPersistCustomerEnumsAsStrings() {
        assertStringEnum(Customer.class, "status");
        assertStringEnum(Customer.class, "source");
        assertStringEnum(Customer.class, "priority");
        assertStringEnum(CustomerRequirement.class, "purpose");
    }

    @Test
    void shouldDeclareCustomerTagAndFavoriteUniqueness() {
        assertUniqueConstraint(
                CustomerTag.class,
                "customer_id",
                "name"
        );
        assertUniqueConstraint(
                CustomerFavoriteListing.class,
                "customer_id",
                "listing_id"
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
