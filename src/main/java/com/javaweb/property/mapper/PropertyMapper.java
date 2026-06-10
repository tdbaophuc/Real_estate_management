package com.javaweb.property.mapper;

import com.javaweb.auth.entity.User;
import com.javaweb.property.dto.PropertyAddressRequest;
import com.javaweb.property.dto.PropertyAddressResponse;
import com.javaweb.property.dto.PropertyAmenityResponse;
import com.javaweb.property.dto.PropertyResponse;
import com.javaweb.property.dto.PropertyUpsertRequest;
import com.javaweb.property.entity.Address;
import com.javaweb.property.entity.District;
import com.javaweb.property.entity.Property;
import com.javaweb.property.entity.PropertyAmenity;
import com.javaweb.property.entity.PropertyType;
import com.javaweb.property.entity.Province;
import com.javaweb.property.entity.Ward;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class PropertyMapper {

    public Property toEntity(
            PropertyUpsertRequest request,
            PropertyType propertyType,
            Address address,
            User createdBy
    ) {
        Property property = new Property(
                request.code(),
                request.name(),
                propertyType,
                address,
                createdBy,
                request.purpose()
        );
        applyScalars(property, request);
        return property;
    }

    public void updateEntity(
            Property property,
            PropertyUpsertRequest request,
            PropertyType propertyType
    ) {
        property.setCode(request.code());
        property.setName(request.name());
        property.setPropertyType(propertyType);
        property.setPurpose(request.purpose());
        applyScalars(property, request);
    }

    public Address toAddress(
            PropertyAddressRequest request,
            Province province,
            District district,
            Ward ward
    ) {
        Address address = new Address(province, request.streetAddress());
        updateAddress(address, request, province, district, ward);
        return address;
    }

    public void updateAddress(
            Address address,
            PropertyAddressRequest request,
            Province province,
            District district,
            Ward ward
    ) {
        address.setProvince(province);
        address.setDistrict(district);
        address.setWard(ward);
        address.setStreetAddress(request.streetAddress());
        address.setFullAddress(request.fullAddress());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
    }

    public PropertyResponse toResponse(Property property) {
        Address address = property.getAddress();
        Province province = address.getProvince();
        District district = address.getDistrict();
        Ward ward = address.getWard();
        User owner = property.getOwner();
        User assignedAgent = property.getAssignedAgent();

        return new PropertyResponse(
                property.getId(),
                property.getCode(),
                property.getName(),
                property.getDescription(),
                property.getPropertyType().getId(),
                property.getPropertyType().getCode(),
                property.getPropertyType().getName(),
                property.getPurpose(),
                property.getStatus(),
                property.getPrice(),
                property.getCurrency(),
                property.getLandArea(),
                property.getFloorArea(),
                property.getBedrooms(),
                property.getBathrooms(),
                property.getFloors(),
                property.getDirection(),
                property.getLegalStatus(),
                property.getFurnitureStatus(),
                property.getVideoUrl(),
                property.getVirtualTourUrl(),
                property.getAvailableFrom(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getFullName(),
                property.getCreatedBy().getId(),
                property.getCreatedBy().getFullName(),
                assignedAgent == null ? null : assignedAgent.getId(),
                assignedAgent == null ? null : assignedAgent.getFullName(),
                new PropertyAddressResponse(
                        address.getId(),
                        province.getId(),
                        province.getName(),
                        district == null ? null : district.getId(),
                        district == null ? null : district.getName(),
                        ward == null ? null : ward.getId(),
                        ward == null ? null : ward.getName(),
                        address.getStreetAddress(),
                        address.getFullAddress(),
                        address.getLatitude(),
                        address.getLongitude()
                ),
                property.getAmenities().stream()
                        .sorted(Comparator.comparing(link -> link.getAmenity().getDisplayOrder()))
                        .map(this::toAmenityResponse)
                        .toList(),
                property.getCreatedAt(),
                property.getUpdatedAt()
        );
    }

    private void applyScalars(Property property, PropertyUpsertRequest request) {
        property.setDescription(request.description());
        property.setPrice(request.price());
        property.setCurrency(request.currency());
        property.setLandArea(request.landArea());
        property.setFloorArea(request.floorArea());
        property.setBedrooms(request.bedrooms());
        property.setBathrooms(request.bathrooms());
        property.setFloors(request.floors());
        property.setDirection(request.direction());
        property.setLegalStatus(request.legalStatus());
        property.setFurnitureStatus(request.furnitureStatus());
        property.setVideoUrl(request.videoUrl());
        property.setVirtualTourUrl(request.virtualTourUrl());
        property.setAvailableFrom(request.availableFrom());
    }

    private PropertyAmenityResponse toAmenityResponse(PropertyAmenity link) {
        return new PropertyAmenityResponse(
                link.getAmenity().getId(),
                link.getAmenity().getCode(),
                link.getAmenity().getName(),
                link.getAmenity().getCategory(),
                link.getDetails()
        );
    }
}
