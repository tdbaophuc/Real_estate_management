package com.javaweb.commission.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.commission.enums.CommissionCalculationType;
import com.javaweb.contract.enums.ContractType;
import com.javaweb.property.entity.AuditableEntity;
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
import java.time.LocalDate;

@Entity
@Table(name = "commission_rules")
public class CommissionRule extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 30)
    private ContractType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_type", nullable = false, length = 30)
    private CommissionCalculationType calculationType;

    @Column(precision = 7, scale = 4)
    private BigDecimal rate;

    @Column(name = "fixed_amount", precision = 19, scale = 2)
    private BigDecimal fixedAmount;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "min_transaction_value", precision = 19, scale = 2)
    private BigDecimal minTransactionValue;

    @Column(name = "max_transaction_value", precision = 19, scale = 2)
    private BigDecimal maxTransactionValue;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    protected CommissionRule() {
    }

    public CommissionRule(
            String code,
            String name,
            CommissionCalculationType calculationType,
            User createdBy
    ) {
        this.code = code;
        this.name = name;
        this.calculationType = calculationType;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ContractType getTransactionType() { return transactionType; }
    public void setTransactionType(ContractType transactionType) {
        this.transactionType = transactionType;
    }
    public CommissionCalculationType getCalculationType() { return calculationType; }
    public void setCalculationType(CommissionCalculationType calculationType) {
        this.calculationType = calculationType;
    }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getFixedAmount() { return fixedAmount; }
    public void setFixedAmount(BigDecimal fixedAmount) { this.fixedAmount = fixedAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getMinTransactionValue() { return minTransactionValue; }
    public void setMinTransactionValue(BigDecimal minTransactionValue) {
        this.minTransactionValue = minTransactionValue;
    }
    public BigDecimal getMaxTransactionValue() { return maxTransactionValue; }
    public void setMaxTransactionValue(BigDecimal maxTransactionValue) {
        this.maxTransactionValue = maxTransactionValue;
    }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getCreatedBy() { return createdBy; }
}
