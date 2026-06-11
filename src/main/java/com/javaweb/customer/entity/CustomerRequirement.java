package com.javaweb.customer.entity;

import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.property.entity.AuditableEntity;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
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

@Entity
@Table(name = "customer_requirements")
public class CustomerRequirement extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_type_id")
    private PropertyType propertyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "province_id")
    private Province province;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ward_id")
    private Ward ward;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ListingPurpose purpose;

    @Column(name = "min_budget", precision = 19, scale = 2)
    private BigDecimal minBudget;

    @Column(name = "max_budget", precision = 19, scale = 2)
    private BigDecimal maxBudget;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "min_area", precision = 12, scale = 2)
    private BigDecimal minArea;

    @Column(name = "max_area", precision = 12, scale = 2)
    private BigDecimal maxArea;

    @Column(name = "min_bedrooms")
    private Integer minBedrooms;

    @Column(name = "min_bathrooms")
    private Integer minBathrooms;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    protected CustomerRequirement() {
    }

    public CustomerRequirement(ListingPurpose purpose) {
        this.purpose = purpose;
    }

    void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    public Province getProvince() {
        return province;
    }

    public void setProvince(Province province) {
        this.province = province;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public Ward getWard() {
        return ward;
    }

    public void setWard(Ward ward) {
        this.ward = ward;
    }

    public ListingPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(ListingPurpose purpose) {
        this.purpose = purpose;
    }

    public BigDecimal getMinBudget() {
        return minBudget;
    }

    public void setMinBudget(BigDecimal minBudget) {
        this.minBudget = minBudget;
    }

    public BigDecimal getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(BigDecimal maxBudget) {
        this.maxBudget = maxBudget;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getMinArea() {
        return minArea;
    }

    public void setMinArea(BigDecimal minArea) {
        this.minArea = minArea;
    }

    public BigDecimal getMaxArea() {
        return maxArea;
    }

    public void setMaxArea(BigDecimal maxArea) {
        this.maxArea = maxArea;
    }

    public Integer getMinBedrooms() {
        return minBedrooms;
    }

    public void setMinBedrooms(Integer minBedrooms) {
        this.minBedrooms = minBedrooms;
    }

    public Integer getMinBathrooms() {
        return minBathrooms;
    }

    public void setMinBathrooms(Integer minBathrooms) {
        this.minBathrooms = minBathrooms;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
