package com.javaweb.transaction.entity;

import com.javaweb.commission.entity.Commission;
import com.javaweb.commission.entity.CommissionRule;
import com.javaweb.payment.entity.Invoice;
import com.javaweb.payment.entity.Payment;
import com.javaweb.payment.entity.Receipt;
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

class TransactionPaymentMappingTest {
    private static final List<Class<?>> ENTITIES = List.of(
            Transaction.class,
            Deposit.class,
            PaymentSchedule.class,
            Payment.class,
            Invoice.class,
            Receipt.class,
            Commission.class,
            CommissionRule.class
    );

    @Test
    void shouldDeclareAllEntityRelationshipsAsLazy() {
        for (Class<?> entityType : ENTITIES) {
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
    void shouldPersistFinancialEnumsAsStrings() {
        assertStringEnum(Transaction.class, "transactionType");
        assertStringEnum(Transaction.class, "status");
        assertStringEnum(Deposit.class, "paymentMethod");
        assertStringEnum(Deposit.class, "status");
        assertStringEnum(PaymentSchedule.class, "status");
        assertStringEnum(Payment.class, "paymentMethod");
        assertStringEnum(Payment.class, "status");
        assertStringEnum(Invoice.class, "status");
        assertStringEnum(CommissionRule.class, "transactionType");
        assertStringEnum(CommissionRule.class, "calculationType");
        assertStringEnum(Commission.class, "status");
    }

    @Test
    void shouldDeclareInstallmentAndCommissionUniqueness() {
        assertUniqueConstraint(
                PaymentSchedule.class,
                "transaction_id",
                "installment_number"
        );
        assertUniqueConstraint(
                Commission.class,
                "transaction_id",
                "beneficiary_user_id"
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
