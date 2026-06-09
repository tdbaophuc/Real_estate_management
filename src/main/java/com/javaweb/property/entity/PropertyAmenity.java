package com.javaweb.property.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "property_amenities")
public class PropertyAmenity extends CreatedEntity {
    @EmbeddedId
    private PropertyAmenityId id;

    @MapsId("propertyId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @MapsId("amenityId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "amenity_id", nullable = false)
    private Amenity amenity;

    @Column(length = 255)
    private String details;

    protected PropertyAmenity() {
    }

    public PropertyAmenity(Property property, Amenity amenity, String details) {
        this.property = property;
        this.amenity = amenity;
        this.details = details;
        this.id = new PropertyAmenityId(property.getId(), amenity.getId());
    }

    public PropertyAmenityId getId() {
        return id;
    }

    public Property getProperty() {
        return property;
    }

    public Amenity getAmenity() {
        return amenity;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
