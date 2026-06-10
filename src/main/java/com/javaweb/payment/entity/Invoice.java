package com.javaweb.payment.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.payment.enums.InvoiceStatus;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
public class Invoice extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by", nullable = false)
    private User issuedBy;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 100)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "billed_to_name", nullable = false, length = 200)
    private String billedToName;

    @Column(name = "billed_to_email", length = 255)
    private String billedToEmail;

    @Column(name = "billed_to_address", length = 500)
    private String billedToAddress;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(length = 2000)
    private String notes;

    protected Invoice() {
    }

    public Invoice(
            User issuedBy,
            String invoiceNumber,
            LocalDate issueDate,
            BigDecimal subtotal,
            BigDecimal totalAmount,
            String billedToName
    ) {
        this.issuedBy = issuedBy;
        this.invoiceNumber = invoiceNumber;
        this.issueDate = issueDate;
        this.subtotal = subtotal;
        this.totalAmount = totalAmount;
        this.billedToName = billedToName;
    }

    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public Long getId() { return id; }
    public Transaction getTransaction() { return transaction; }
    public User getIssuedBy() { return issuedBy; }
    public void setIssuedBy(User issuedBy) { this.issuedBy = issuedBy; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getBilledToName() { return billedToName; }
    public void setBilledToName(String billedToName) { this.billedToName = billedToName; }
    public String getBilledToEmail() { return billedToEmail; }
    public void setBilledToEmail(String billedToEmail) {
        this.billedToEmail = billedToEmail;
    }
    public String getBilledToAddress() { return billedToAddress; }
    public void setBilledToAddress(String billedToAddress) {
        this.billedToAddress = billedToAddress;
    }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public Instant getVoidedAt() { return voidedAt; }
    public void setVoidedAt(Instant voidedAt) { this.voidedAt = voidedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
