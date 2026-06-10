package com.javaweb.transaction.repository;

import com.javaweb.commission.enums.CommissionCalculationType;
import com.javaweb.commission.enums.CommissionStatus;
import com.javaweb.commission.repository.CommissionRepository;
import com.javaweb.commission.repository.CommissionRuleRepository;
import com.javaweb.payment.enums.InvoiceStatus;
import com.javaweb.payment.enums.PaymentStatus;
import com.javaweb.payment.repository.InvoiceRepository;
import com.javaweb.payment.repository.PaymentRepository;
import com.javaweb.payment.repository.ReceiptRepository;
import com.javaweb.transaction.entity.Transaction;
import com.javaweb.transaction.enums.DepositStatus;
import com.javaweb.transaction.enums.PaymentScheduleStatus;
import com.javaweb.transaction.enums.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionPaymentRepositoryIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DepositRepository depositRepository;

    @Autowired
    private PaymentScheduleRepository scheduleRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private CommissionRuleRepository commissionRuleRepository;

    @Autowired
    private CommissionRepository commissionRepository;

    @BeforeEach
    void setUpFinancialGraph() {
        jdbcTemplate.update(
                """
                INSERT INTO users (email, password_hash, full_name, status, email_verified)
                VALUES
                    ('transaction-owner@example.com', 'hash', 'Transaction Owner', 'ACTIVE', TRUE),
                    ('transaction-agent@example.com', 'hash', 'Transaction Agent', 'ACTIVE', TRUE),
                    ('transaction-customer@example.com', 'hash', 'Transaction Customer', 'ACTIVE', TRUE),
                    ('transaction-manager@example.com', 'hash', 'Transaction Manager', 'ACTIVE', TRUE)
                """
        );
        jdbcTemplate.update(
                "INSERT INTO provinces (code, name) VALUES ('TRANSACTION-PROVINCE', 'Transaction Province')"
        );
        jdbcTemplate.update(
                """
                INSERT INTO addresses (province_id, street_address, full_address)
                SELECT id, '31 Transaction Street', '31 Transaction Street'
                FROM provinces
                WHERE code = 'TRANSACTION-PROVINCE'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO properties (
                    code, property_type_id, address_id, owner_id, created_by,
                    assigned_agent_id, name, purpose, status, price
                )
                SELECT
                    'TRANSACTION-PROPERTY-001',
                    property_type.id,
                    address.id,
                    owner.id,
                    agent.id,
                    agent.id,
                    'Transaction test property',
                    'SALE',
                    'AVAILABLE',
                    3000000000
                FROM property_types property_type
                CROSS JOIN addresses address
                CROSS JOIN users owner
                CROSS JOIN users agent
                WHERE property_type.code = 'APARTMENT'
                  AND address.full_address = '31 Transaction Street'
                  AND owner.email = 'transaction-owner@example.com'
                  AND agent.email = 'transaction-agent@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO customers (
                    code, user_id, assigned_agent_id, created_by, full_name, email
                )
                SELECT
                    'TRANSACTION-CUSTOMER-001',
                    customer.id,
                    agent.id,
                    agent.id,
                    customer.full_name,
                    customer.email
                FROM users customer
                CROSS JOIN users agent
                WHERE customer.email = 'transaction-customer@example.com'
                  AND agent.email = 'transaction-agent@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO contracts (
                    code, property_id, customer_id, owner_id, agent_id, created_by,
                    contract_type, title, total_value, status, signed_at
                )
                SELECT
                    'TRANSACTION-CONTRACT-001',
                    property.id,
                    customer.id,
                    owner.id,
                    agent.id,
                    agent.id,
                    'SALE',
                    'Signed transaction contract',
                    3000000000,
                    'SIGNED',
                    CURRENT_TIMESTAMP
                FROM properties property
                CROSS JOIN customers customer
                CROSS JOIN users owner
                CROSS JOIN users agent
                WHERE property.code = 'TRANSACTION-PROPERTY-001'
                  AND customer.code = 'TRANSACTION-CUSTOMER-001'
                  AND owner.email = 'transaction-owner@example.com'
                  AND agent.email = 'transaction-agent@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO transactions (
                    code, contract_id, property_id, customer_id, owner_id, agent_id,
                    created_by, transaction_type, status, agreed_value,
                    transaction_date, expected_completion_date
                )
                SELECT
                    'TRANSACTION-001',
                    contract.id,
                    property.id,
                    customer.id,
                    owner.id,
                    agent.id,
                    agent.id,
                    'SALE',
                    'PAYMENT_IN_PROGRESS',
                    3000000000,
                    DATE '2030-01-15',
                    DATE '2030-03-15'
                FROM contracts contract
                CROSS JOIN properties property
                CROSS JOIN customers customer
                CROSS JOIN users owner
                CROSS JOIN users agent
                WHERE contract.code = 'TRANSACTION-CONTRACT-001'
                  AND property.code = 'TRANSACTION-PROPERTY-001'
                  AND customer.code = 'TRANSACTION-CUSTOMER-001'
                  AND owner.email = 'transaction-owner@example.com'
                  AND agent.email = 'transaction-agent@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO payment_schedules (
                    transaction_id, installment_number, label, due_date,
                    amount, paid_amount, status, paid_at
                )
                SELECT id, 1, 'First installment', DATE '2030-02-01',
                       1000000000, 1000000000, 'PAID', CURRENT_TIMESTAMP
                FROM transactions
                WHERE code = 'TRANSACTION-001'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO deposits (
                    transaction_id, received_by, amount, payment_method, status,
                    reference_number, idempotency_key, received_at, verified_at
                )
                SELECT transaction.id, agent.id, 300000000, 'BANK_TRANSFER', 'VERIFIED',
                       'DEP-REF-001', 'DEP-IDEMPOTENCY-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM transactions transaction
                CROSS JOIN users agent
                WHERE transaction.code = 'TRANSACTION-001'
                  AND agent.email = 'transaction-agent@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO payments (
                    transaction_id, payment_schedule_id, received_by, amount,
                    payment_method, status, reference_number, idempotency_key,
                    paid_at, confirmed_at
                )
                SELECT transaction.id, schedule.id, agent.id, 1000000000,
                       'BANK_TRANSFER', 'COMPLETED', 'PAY-REF-001',
                       'PAY-IDEMPOTENCY-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM transactions transaction
                CROSS JOIN payment_schedules schedule
                CROSS JOIN users agent
                WHERE transaction.code = 'TRANSACTION-001'
                  AND schedule.transaction_id = transaction.id
                  AND schedule.installment_number = 1
                  AND agent.email = 'transaction-agent@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO invoices (
                    transaction_id, issued_by, invoice_number, status, issue_date,
                    due_date, subtotal, tax_amount, total_amount, billed_to_name
                )
                SELECT transaction.id, manager.id, 'INV-TRANSACTION-001', 'ISSUED',
                       DATE '2030-01-15', DATE '2030-02-01',
                       1000000000, 0, 1000000000, customer.full_name
                FROM transactions transaction
                CROSS JOIN users manager
                CROSS JOIN customers customer
                WHERE transaction.code = 'TRANSACTION-001'
                  AND manager.email = 'transaction-manager@example.com'
                  AND customer.code = 'TRANSACTION-CUSTOMER-001'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO receipts (
                    payment_id, issued_by, receipt_number, issued_at, amount, payer_name
                )
                SELECT payment.id, manager.id, 'REC-TRANSACTION-001',
                       CURRENT_TIMESTAMP, payment.amount, customer.full_name
                FROM payments payment
                CROSS JOIN users manager
                CROSS JOIN customers customer
                WHERE payment.idempotency_key = 'PAY-IDEMPOTENCY-001'
                  AND manager.email = 'transaction-manager@example.com'
                  AND customer.code = 'TRANSACTION-CUSTOMER-001'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO commission_rules (
                    code, name, transaction_type, calculation_type, rate,
                    priority, effective_from, created_by
                )
                SELECT 'SALE-STANDARD-2PCT', 'Standard sale commission', 'SALE',
                       'PERCENTAGE', 2.0000, 10, DATE '2029-01-01', manager.id
                FROM users manager
                WHERE manager.email = 'transaction-manager@example.com'
                """
        );
        jdbcTemplate.update(
                """
                INSERT INTO commissions (
                    transaction_id, commission_rule_id, beneficiary_user_id,
                    status, base_amount, rate, amount
                )
                SELECT transaction.id, rule.id, agent.id, 'PENDING',
                       transaction.agreed_value, rule.rate, 60000000
                FROM transactions transaction
                CROSS JOIN commission_rules rule
                CROSS JOIN users agent
                WHERE transaction.code = 'TRANSACTION-001'
                  AND rule.code = 'SALE-STANDARD-2PCT'
                  AND agent.email = 'transaction-agent@example.com'
                """
        );
    }

    @Test
    void shouldMapAndQueryCompleteFinancialGraph() {
        Transaction transaction = transactionRepository.findByCode("TRANSACTION-001")
                .orElseThrow();

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PAYMENT_IN_PROGRESS);
        assertThat(transaction.getContract().getCode()).isEqualTo("TRANSACTION-CONTRACT-001");
        assertThat(transaction.getProperty().getCode()).isEqualTo("TRANSACTION-PROPERTY-001");
        assertThat(transaction.getCustomer().getCode()).isEqualTo("TRANSACTION-CUSTOMER-001");
        assertThat(transactionRepository.findAllByAgentId(
                transaction.getAgent().getId(),
                PageRequest.of(0, 10)
        )).hasSize(1);

        assertThat(depositRepository.findAllByTransactionIdAndStatus(
                transaction.getId(),
                DepositStatus.VERIFIED
        )).singleElement().satisfies(deposit ->
                assertThat(deposit.getIdempotencyKey()).isEqualTo("DEP-IDEMPOTENCY-001"));
        assertThat(scheduleRepository.findAllByTransactionIdOrderByInstallmentNumberAsc(
                transaction.getId()
        )).singleElement().satisfies(schedule ->
                assertThat(schedule.getStatus()).isEqualTo(PaymentScheduleStatus.PAID));
        assertThat(paymentRepository.findAllByTransactionIdAndStatus(
                transaction.getId(),
                PaymentStatus.COMPLETED
        )).singleElement().satisfies(payment ->
                assertThat(receiptRepository.findByPaymentId(payment.getId()))
                        .get()
                        .extracting("receiptNumber")
                        .isEqualTo("REC-TRANSACTION-001"));
        assertThat(invoiceRepository.findAllByTransactionIdOrderByIssueDateDesc(
                transaction.getId()
        )).singleElement().satisfies(invoice ->
                assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED));
        assertThat(commissionRuleRepository
                .findAllByActiveTrueAndTransactionTypeOrderByPriorityDesc(
                        transaction.getTransactionType()
                )).singleElement().satisfies(rule ->
                assertThat(rule.getCalculationType())
                        .isEqualTo(CommissionCalculationType.PERCENTAGE));
        assertThat(commissionRepository.findAllByTransactionId(transaction.getId()))
                .singleElement()
                .satisfies(commission ->
                        assertThat(commission.getStatus()).isEqualTo(CommissionStatus.PENDING));
    }

    @Test
    void shouldEnforceFinancialLifecycleAndAmountConstraints() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                UPDATE transactions
                SET status = 'COMPLETED'
                WHERE code = 'TRANSACTION-001'
                """
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                UPDATE invoices
                SET total_amount = subtotal + tax_amount + 1
                WHERE invoice_number = 'INV-TRANSACTION-001'
                """
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO payment_schedules (
                    transaction_id, installment_number, label, due_date, amount
                )
                SELECT id, 1, 'Duplicate installment', DATE '2030-03-01', 500000000
                FROM transactions
                WHERE code = 'TRANSACTION-001'
                """
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO commission_rules (
                    code, name, calculation_type, rate, fixed_amount, created_by
                )
                SELECT 'INVALID-COMMISSION-RULE', 'Invalid rule',
                       'PERCENTAGE', 2.0000, 1000000, id
                FROM users
                WHERE email = 'transaction-manager@example.com'
                """
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
