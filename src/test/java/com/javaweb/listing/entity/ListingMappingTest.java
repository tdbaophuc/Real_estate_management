package com.javaweb.listing.entity;

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

class ListingMappingTest {

    private static final List<Class<?>> LISTING_ENTITIES = List.of(
            Listing.class,
            ListingPackage.class,
            ListingStatusHistory.class,
            ListingView.class,
            ListingFavorite.class
    );

    @Test
    void shouldDeclareAllEntityRelationshipsAsLazy() {
        for (Class<?> entityType : LISTING_ENTITIES) {
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
    void shouldPersistListingEnumsAsStrings() {
        assertStringEnum(Listing.class, "purpose");
        assertStringEnum(Listing.class, "status");
        assertStringEnum(Listing.class, "visibility");
        assertStringEnum(ListingStatusHistory.class, "fromStatus");
        assertStringEnum(ListingStatusHistory.class, "toStatus");
    }

    @Test
    void shouldDeclareUniqueFavoritePerListingAndUser() {
        Table table = ListingFavorite.class.getAnnotation(Table.class);

        assertThat(table).isNotNull();
        assertThat(Arrays.stream(table.uniqueConstraints()))
                .anySatisfy(constraint -> assertThat(constraint.columnNames())
                        .containsExactly("listing_id", "user_id"));
    }

    private void assertStringEnum(Class<?> entityType, String fieldName) {
        try {
            Enumerated enumerated = entityType.getDeclaredField(fieldName).getAnnotation(Enumerated.class);
            assertThat(enumerated)
                    .as("%s.%s should declare @Enumerated", entityType.getSimpleName(), fieldName)
                    .isNotNull();
            assertThat(enumerated.value())
                    .as("%s.%s should use EnumType.STRING", entityType.getSimpleName(), fieldName)
                    .isEqualTo(EnumType.STRING);
        } catch (NoSuchFieldException exception) {
            throw new AssertionError("Missing field " + entityType.getSimpleName() + "." + fieldName, exception);
        }
    }
}
