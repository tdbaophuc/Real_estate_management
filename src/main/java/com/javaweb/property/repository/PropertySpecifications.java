package com.javaweb.property.repository;

import com.javaweb.property.dto.PropertySearchRequest;
import com.javaweb.property.entity.Property;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class PropertySpecifications {
    private PropertySpecifications() {
    }

    public static Specification<Property> search(
            PropertySearchRequest request,
            Long visibleUserId
    ) {
        Specification<Property> specification =
                (root, query, builder) -> builder.isNull(root.get("deletedAt"));

        if (visibleUserId != null) {
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.equal(root.get("createdBy").get("id"), visibleUserId),
                    builder.equal(root.get("assignedAgent").get("id"), visibleUserId)
            ));
        }
        if (StringUtils.hasText(request.keyword())) {
            String keyword = "%" + request.keyword().trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("code")), keyword),
                    builder.like(builder.lower(root.get("name")), keyword),
                    builder.like(builder.lower(root.get("description")), keyword),
                    builder.like(builder.lower(root.get("address").get("streetAddress")), keyword),
                    builder.like(builder.lower(root.get("address").get("fullAddress")), keyword)
            ));
        }
        if (request.propertyTypeId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("propertyType").get("id"), request.propertyTypeId()));
        }
        if (request.purpose() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("purpose"), request.purpose()));
        }
        if (request.provinceId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("address").get("province").get("id"),
                            request.provinceId()
                    ));
        }
        if (request.districtId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("address").get("district").get("id"),
                            request.districtId()
                    ));
        }
        if (request.wardId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("address").get("ward").get("id"), request.wardId()));
        }
        if (request.minPrice() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("price"), request.minPrice()));
        }
        if (request.maxPrice() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("price"), request.maxPrice()));
        }
        if (request.minArea() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("landArea"), request.minArea()));
        }
        if (request.maxArea() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("landArea"), request.maxArea()));
        }
        if (request.bedrooms() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("bedrooms"), request.bedrooms()));
        }
        if (request.bathrooms() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("bathrooms"), request.bathrooms()));
        }
        if (request.status() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), request.status()));
        }
        return specification;
    }
}
