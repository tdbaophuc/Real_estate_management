package com.javaweb.property.entity;

import com.javaweb.auth.entity.User;
import com.javaweb.property.enums.FurnitureStatus;
import com.javaweb.property.enums.PropertyDirection;
import com.javaweb.property.enums.PropertyLegalStatus;
import com.javaweb.property.enums.PropertyPurpose;
import com.javaweb.property.enums.PropertyStatus;
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
@Table(name = "properties")
public class Property extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_type_id", nullable = false)
    private PropertyType propertyType;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id", nullable = false, unique = true)
    private Address address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PropertyPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PropertyStatus status = PropertyStatus.DRAFT;

    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(name = "land_area", precision = 12, scale = 2)
    private BigDecimal landArea;

    @Column(name = "floor_area", precision = 12, scale = 2)
    private BigDecimal floorArea;

    private Integer bedrooms;

    private Integer bathrooms;

    private Integer floors;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PropertyDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "legal_status", length = 50)
    private PropertyLegalStatus legalStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "furniture_status", length = 50)
    private FurnitureStatus furnitureStatus;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @Column(name = "virtual_tour_url", length = 500)
    private String virtualTourUrl;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyAmenity> amenities = new ArrayList<>();

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "property", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyLegalDocument> legalDocuments = new ArrayList<>();

    protected Property() {
    }

    public Property(
            String code,
            String name,
            PropertyType propertyType,
            Address address,
            User createdBy,
            PropertyPurpose purpose
    ) {
        this.code = code;
        this.name = name;
        this.propertyType = propertyType;
        setAddress(address);
        this.createdBy = createdBy;
        this.purpose = purpose;
    }

    public void setAddress(Address address) {
        if (this.address != null) {
            this.address.setProperty(null);
        }
        this.address = address;
        if (address != null) {
            address.setProperty(this);
        }
    }

    public PropertyAmenity addAmenity(Amenity amenity, String details) {
        PropertyAmenity propertyAmenity = new PropertyAmenity(this, amenity, details);
        amenities.add(propertyAmenity);
        amenity.getProperties().add(propertyAmenity);
        return propertyAmenity;
    }

    public void removeAmenity(PropertyAmenity propertyAmenity) {
        amenities.remove(propertyAmenity);
        propertyAmenity.getAmenity().getProperties().remove(propertyAmenity);
    }

    public void addImage(PropertyImage image) {
        images.add(image);
        image.setProperty(this);
    }

    public void removeImage(PropertyImage image) {
        images.remove(image);
        image.setProperty(null);
    }

    public void addLegalDocument(PropertyLegalDocument document) {
        legalDocuments.add(document);
        document.setProperty(this);
    }

    public void removeLegalDocument(PropertyLegalDocument document) {
        legalDocuments.remove(document);
        document.setProperty(null);
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

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    public Address getAddress() {
        return address;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public User getAssignedAgent() {
        return assignedAgent;
    }

    public void setAssignedAgent(User assignedAgent) {
        this.assignedAgent = assignedAgent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PropertyPurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(PropertyPurpose purpose) {
        this.purpose = purpose;
    }

    public PropertyStatus getStatus() {
        return status;
    }

    public void setStatus(PropertyStatus status) {
        this.status = status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getLandArea() {
        return landArea;
    }

    public void setLandArea(BigDecimal landArea) {
        this.landArea = landArea;
    }

    public BigDecimal getFloorArea() {
        return floorArea;
    }

    public void setFloorArea(BigDecimal floorArea) {
        this.floorArea = floorArea;
    }

    public Integer getBedrooms() {
        return bedrooms;
    }

    public void setBedrooms(Integer bedrooms) {
        this.bedrooms = bedrooms;
    }

    public Integer getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(Integer bathrooms) {
        this.bathrooms = bathrooms;
    }

    public Integer getFloors() {
        return floors;
    }

    public void setFloors(Integer floors) {
        this.floors = floors;
    }

    public PropertyDirection getDirection() {
        return direction;
    }

    public void setDirection(PropertyDirection direction) {
        this.direction = direction;
    }

    public PropertyLegalStatus getLegalStatus() {
        return legalStatus;
    }

    public void setLegalStatus(PropertyLegalStatus legalStatus) {
        this.legalStatus = legalStatus;
    }

    public FurnitureStatus getFurnitureStatus() {
        return furnitureStatus;
    }

    public void setFurnitureStatus(FurnitureStatus furnitureStatus) {
        this.furnitureStatus = furnitureStatus;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getVirtualTourUrl() {
        return virtualTourUrl;
    }

    public void setVirtualTourUrl(String virtualTourUrl) {
        this.virtualTourUrl = virtualTourUrl;
    }

    public LocalDate getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDate availableFrom) {
        this.availableFrom = availableFrom;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public List<PropertyAmenity> getAmenities() {
        return amenities;
    }

    public List<PropertyImage> getImages() {
        return images;
    }

    public List<PropertyLegalDocument> getLegalDocuments() {
        return legalDocuments;
    }
}
