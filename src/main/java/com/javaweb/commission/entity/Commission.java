package com.javaweb.commission.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.commission.enums.CommissionStatus;
import com.javaweb.property.entity.AuditableEntity;
import com.javaweb.transaction.entity.Transaction;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "commissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_commissions_transaction_beneficiary",
                columnNames = {"transaction_id", "beneficiary_user_id"}
        )
)
public class Commission extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_rule_id")
    private CommissionRule commissionRule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_user_id", nullable = false)
    private User beneficiaryUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommissionStatus status = CommissionStatus.PENDING;

    @Column(name = "base_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseAmount;

    @Column(precision = 7, scale = 4)
    private BigDecimal rate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by")
    private User paidBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(length = 1000)
    private String notes;

    protected Commission() {
    }

    public Commission(
            Transaction transaction,
            User beneficiaryUser,
            BigDecimal baseAmount,
            BigDecimal amount
    ) {
        this.transaction = transaction;
        this.beneficiaryUser = beneficiaryUser;
        this.baseAmount = baseAmount;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public CommissionRule getCommissionRule() { return commissionRule; }
    public void setCommissionRule(CommissionRule commissionRule) {
        this.commissionRule = commissionRule;
    }
    public User getBeneficiaryUser() { return beneficiaryUser; }
    public void setBeneficiaryUser(User beneficiaryUser) {
        this.beneficiaryUser = beneficiaryUser;
    }
    public CommissionStatus getStatus() { return status; }
    public void setStatus(CommissionStatus status) { this.status = status; }
    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    public User getPaidBy() { return paidBy; }
    public void setPaidBy(User paidBy) { this.paidBy = paidBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
