package com.javaweb.property.entity;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyMappingTest {

    private static final List<Class<?>> PROPERTY_ENTITIES = List.of(
            Province.class,
            District.class,
            Ward.class,
            Address.class,
            PropertyType.class,
            Amenity.class,
            Property.class,
            PropertyAmenity.class,
            PropertyImage.class,
            PropertyLegalDocument.class
    );

    @Test
    void shouldDeclareAllEntityRelationshipsAsLazy() {
        for (Class<?> entityType : PROPERTY_ENTITIES) {
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

                OneToOne oneToOne = field.getAnnotation(OneToOne.class);
                if (oneToOne != null) {
                    assertThat(oneToOne.fetch())
                            .as("%s.%s should be lazy", entityType.getSimpleName(), field.getName())
                            .isEqualTo(FetchType.LAZY);
                }
            }
        }
    }

    @Test
    void shouldPersistPropertyEnumsAsStrings() {
        assertStringEnum(Property.class, "purpose");
        assertStringEnum(Property.class, "status");
        assertStringEnum(Property.class, "direction");
        assertStringEnum(Property.class, "legalStatus");
        assertStringEnum(Property.class, "furnitureStatus");
        assertStringEnum(Amenity.class, "category");
        assertStringEnum(PropertyLegalDocument.class, "documentType");
        assertStringEnum(PropertyLegalDocument.class, "verificationStatus");
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
