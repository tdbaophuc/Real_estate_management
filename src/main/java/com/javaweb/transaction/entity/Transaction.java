package com.javaweb.transaction.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.contract.entity.Contract;
import com.javaweb.contract.enums.ContractType;
import com.javaweb.customer.entity.Customer;
import com.javaweb.payment.entity.Invoice;
import com.javaweb.payment.entity.Payment;
import com.javaweb.property.entity.AuditableEntity;
import com.javaweb.property.entity.Property;
import com.javaweb.transaction.enums.TransactionStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transactions")
public class Transaction extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", unique = true)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private ContractType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "agreed_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal agreedValue;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "transaction_date")
    private LocalDate transactionDate;

    @Column(name = "expected_completion_date")
    private LocalDate expectedCompletionDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    @Column(length = 2000)
    private String notes;

    @OneToMany(mappedBy = "transaction", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Deposit> deposits = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentSchedule> paymentSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Invoice> invoices = new ArrayList<>();

    protected Transaction() {
    }

    public Transaction(
            String code,
            ContractType transactionType,
            BigDecimal agreedValue,
            Property property,
            Customer customer,
            User owner,
            User agent,
            User createdBy
    ) {
        this.code = code;
        this.transactionType = transactionType;
        this.agreedValue = agreedValue;
        this.property = property;
        this.customer = customer;
        this.owner = owner;
        this.agent = agent;
        this.createdBy = createdBy;
    }

    public Deposit addDeposit(Deposit deposit) {
        deposits.add(deposit);
        deposit.setTransaction(this);
        return deposit;
    }

    public PaymentSchedule addPaymentSchedule(PaymentSchedule schedule) {
        paymentSchedules.add(schedule);
        schedule.setTransaction(this);
        return schedule;
    }

    public Payment addPayment(Payment payment) {
        payments.add(payment);
        payment.setTransaction(this);
        return payment;
    }

    public Invoice addInvoice(Invoice invoice) {
        invoices.add(invoice);
        invoice.setTransaction(this);
        return invoice;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }
    public Property getProperty() { return property; }
    public void setProperty(Property property) { this.property = property; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public User getAgent() { return agent; }
    public void setAgent(User agent) { this.agent = agent; }
    public User getCreatedBy() { return createdBy; }
    public ContractType getTransactionType() { return transactionType; }
    public void setTransactionType(ContractType transactionType) {
        this.transactionType = transactionType;
    }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public BigDecimal getAgreedValue() { return agreedValue; }
    public void setAgreedValue(BigDecimal agreedValue) { this.agreedValue = agreedValue; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }
    public LocalDate getExpectedCompletionDate() { return expectedCompletionDate; }
    public void setExpectedCompletionDate(LocalDate expectedCompletionDate) {
        this.expectedCompletionDate = expectedCompletionDate;
    }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public Instant getRefundedAt() { return refundedAt; }
    public void setRefundedAt(Instant refundedAt) { this.refundedAt = refundedAt; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<Deposit> getDeposits() { return deposits; }
    public List<PaymentSchedule> getPaymentSchedules() { return paymentSchedules; }
    public List<Payment> getPayments() { return payments; }
    public List<Invoice> getInvoices() { return invoices; }
}
