package com.javaweb.listing.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.listing.enums.ListingPurpose;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import com.javaweb.property.entity.AuditableEntity;
import com.javaweb.property.entity.Property;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "listings")
public class Listing extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_package_id")
    private ListingPackage listingPackage;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(nullable = false, unique = true, length = 300)
    private String slug;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ListingPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ListingStatus status = ListingStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingVisibility visibility = ListingVisibility.PUBLIC;

    @Column(name = "asking_price", precision = 19, scale = 2)
    private BigDecimal askingPrice;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "seo_title", length = 250)
    private String seoTitle;

    @Column(name = "seo_description", length = 500)
    private String seoDescription;

    @Column(name = "seo_keywords", length = 500)
    private String seoKeywords;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "unpublished_at")
    private Instant unpublishedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "featured_until")
    private Instant featuredUntil;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "listing", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListingStatusHistory> statusHistories = new ArrayList<>();

    @OneToMany(mappedBy = "listing", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListingView> views = new ArrayList<>();

    @OneToMany(mappedBy = "listing", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ListingFavorite> favorites = new ArrayList<>();

    protected Listing() {
    }

    public Listing(
            String code,
            Property property,
            User createdBy,
            String title,
            String slug,
            String description,
            ListingPurpose purpose
    ) {
        this.code = code;
        this.property = property;
        this.createdBy = createdBy;
        this.title = title;
        this.slug = slug;
        this.description = description;
        this.purpose = purpose;
    }

    public ListingStatusHistory addStatusHistory(
            ListingStatus fromStatus,
            ListingStatus toStatus,
            User changedBy,
            String reason
    ) {
        ListingStatusHistory history = new ListingStatusHistory(this, fromStatus, toStatus, changedBy, reason);
        statusHistories.add(history);
        return history;
    }

    public ListingView addView(User viewer, String sessionId) {
        ListingView view = new ListingView(this, viewer, sessionId);
        views.add(view);
        return view;
    }

    public ListingFavorite addFavorite(User user) {
        ListingFavorite favorite = new ListingFavorite(this, user);
        favorites.add(favorite);
        return favorite;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Property getProperty() {
        return property;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public ListingPackage getListingPackage() {
        return listingPackage;
    }

    public void setListingPackage(ListingPackage listingPackage) {
        this.listingPackage = listingPackage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ListingPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(ListingPurpose purpose) {
        this.purpose = purpose;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(ListingStatus status) {
        this.status = status;
    }

    public ListingVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ListingVisibility visibility) {
        this.visibility = visibility;
    }

    public BigDecimal getAskingPrice() {
        return askingPrice;
    }

    public void setAskingPrice(BigDecimal askingPrice) {
        this.askingPrice = askingPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSeoTitle() {
        return seoTitle;
    }

    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    public String getSeoDescription() {
        return seoDescription;
    }

    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }

    public String getSeoKeywords() {
        return seoKeywords;
    }

    public void setSeoKeywords(String seoKeywords) {
        this.seoKeywords = seoKeywords;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getUnpublishedAt() {
        return unpublishedAt;
    }

    public void setUnpublishedAt(Instant unpublishedAt) {
        this.unpublishedAt = unpublishedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getFeaturedUntil() {
        return featuredUntil;
    }

    public void setFeaturedUntil(Instant featuredUntil) {
        this.featuredUntil = featuredUntil;
    }

    public long getViewCount() {
        return viewCount;
    }

    public void setViewCount(long viewCount) {
        this.viewCount = viewCount;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<ListingStatusHistory> getStatusHistories() {
        return statusHistories;
    }

    public List<ListingView> getViews() {
        return views;
    }

    public List<ListingFavorite> getFavorites() {
        return favorites;
    }
}
