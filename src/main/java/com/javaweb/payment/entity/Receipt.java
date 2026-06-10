package com.javaweb.payment.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.property.entity.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "receipts")
public class Receipt extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by", nullable = false)
    private User issuedBy;

    @Column(name = "receipt_number", nullable = false, unique = true, length = 100)
    private String receiptNumber;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "payer_name", length = 200)
    private String payerName;

    @Column(length = 1000)
    private String notes;

    protected Receipt() {
    }

    public Receipt(
            Payment payment,
            User issuedBy,
            String receiptNumber,
            Instant issuedAt,
            BigDecimal amount
    ) {
        this.payment = payment;
        this.issuedBy = issuedBy;
        this.receiptNumber = receiptNumber;
        this.issuedAt = issuedAt;
        this.amount = amount;
    }

    public Long getId() { return id; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public User getIssuedBy() { return issuedBy; }
    public void setIssuedBy(User issuedBy) { this.issuedBy = issuedBy; }
    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getPayerName() { return payerName; }
    public void setPayerName(String payerName) { this.payerName = payerName; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
