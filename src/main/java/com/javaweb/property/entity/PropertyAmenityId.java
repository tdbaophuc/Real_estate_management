package com.javaweb.property.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PropertyAmenityId implements Serializable {
    @Column(name = "property_id")
    private Long propertyId;

    @Column(name = "amenity_id")
    private Long amenityId;

    protected PropertyAmenityId() {
    }

    public PropertyAmenityId(Long propertyId, Long amenityId) {
        this.propertyId = propertyId;
        this.amenityId = amenityId;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public Long getAmenityId() {
        return amenityId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof PropertyAmenityId that)) {
            return false;
        }
        return Objects.equals(propertyId, that.propertyId)
                && Objects.equals(amenityId, that.amenityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyId, amenityId);
    }
}
