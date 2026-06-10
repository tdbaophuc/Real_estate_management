package com.javaweb.customer.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.listing.entity.Listing;
import com.javaweb.property.entity.CreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "customer_favorite_listings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_customer_favorite_listings_customer_listing",
                columnNames = {"customer_id", "listing_id"}
        )
)
public class CustomerFavoriteListing extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private User addedBy;

    @Column(length = 1000)
    private String notes;

    protected CustomerFavoriteListing() {
    }

    public CustomerFavoriteListing(Listing listing, User addedBy) {
        this.listing = listing;
        this.addedBy = addedBy;
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

    public Listing getListing() {
        return listing;
    }

    public User getAddedBy() {
        return addedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
