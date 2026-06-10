package com.javaweb.listing.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.property.entity.CreatedEntity;
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
        name = "listing_favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_listing_favorites_listing_user",
                columnNames = {"listing_id", "user_id"}
        )
)
public class ListingFavorite extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected ListingFavorite() {
    }

    public ListingFavorite(Listing listing, User user) {
        this.listing = listing;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public User getUser() {
        return user;
    }
}
