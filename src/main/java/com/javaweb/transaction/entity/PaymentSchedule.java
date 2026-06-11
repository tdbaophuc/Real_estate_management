package com.javaweb.transaction.entity;

import com.javaweb.payment.entity.Payment;
import com.javaweb.property.entity.AuditableEntity;
import com.javaweb.transaction.enums.PaymentScheduleStatus;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "payment_schedules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_schedules_transaction_installment",
                columnNames = {"transaction_id", "installment_number"}
        )
)
public class PaymentSchedule extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "installment_number", nullable = false)
    private int installmentNumber;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentScheduleStatus status = PaymentScheduleStatus.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "paymentSchedule", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL)
    private List<Payment> payments = new ArrayList<>();

    protected PaymentSchedule() {
    }

    public PaymentSchedule(
            int installmentNumber,
            String label,
            LocalDate dueDate,
            BigDecimal amount
    ) {
        this.installmentNumber = installmentNumber;
        this.label = label;
        this.dueDate = dueDate;
        this.amount = amount;
    }

    void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public Long getId() { return id; }
    public Transaction getTransaction() { return transaction; }
    public int getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(int installmentNumber) {
        this.installmentNumber = installmentNumber;
    }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public PaymentScheduleStatus getStatus() { return status; }
    public void setStatus(PaymentScheduleStatus status) { this.status = status; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public List<Payment> getPayments() { return payments; }
}
