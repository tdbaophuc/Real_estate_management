package com.javaweb.contract.entity;

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

class ContractMappingTest {
    private static final List<Class<?>> CONTRACT_ENTITIES = List.of(
            Contract.class,
            ContractParty.class,
            ContractDocument.class,
            ContractTemplate.class,
            ContractSignature.class
    );

    @Test
    void shouldDeclareAllEntityRelationshipsAsLazy() {
        for (Class<?> entityType : CONTRACT_ENTITIES) {
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
    void shouldPersistContractEnumsAsStrings() {
        assertStringEnum(Contract.class, "contractType");
        assertStringEnum(Contract.class, "status");
        assertStringEnum(ContractTemplate.class, "contractType");
        assertStringEnum(ContractParty.class, "partyRole");
        assertStringEnum(ContractDocument.class, "documentType");
        assertStringEnum(ContractSignature.class, "signatureMethod");
        assertStringEnum(ContractSignature.class, "status");
    }

    @Test
    void shouldDeclareTemplateDocumentAndSignatureUniqueness() {
        assertUniqueConstraint(ContractTemplate.class, "code", "version");
        assertUniqueConstraint(
                ContractDocument.class,
                "contract_id",
                "document_type",
                "version"
        );
        assertUniqueConstraint(ContractDocument.class, "file_resource_id");
        assertUniqueConstraint(
                ContractSignature.class,
                "contract_party_id",
                "contract_document_id"
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
