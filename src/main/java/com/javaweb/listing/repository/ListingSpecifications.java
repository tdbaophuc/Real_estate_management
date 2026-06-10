package com.javaweb.listing.repository;

import com.javaweb.listing.dto.ListingSearchRequest;
import com.javaweb.listing.entity.Listing;
import com.javaweb.listing.enums.ListingStatus;
import com.javaweb.listing.enums.ListingVisibility;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class ListingSpecifications {
    private ListingSpecifications() {
    }

    public static Specification<Listing> publicSearch(ListingSearchRequest request) {
        Specification<Listing> specification = (root, query, builder) -> builder.and(
                builder.equal(root.get("status"), ListingStatus.PUBLISHED),
                builder.equal(root.get("visibility"), ListingVisibility.PUBLIC),
                builder.isNull(root.get("deletedAt")),
                builder.isNull(root.get("property").get("deletedAt"))
        );

        if (StringUtils.hasText(request.keyword())) {
            String keyword = "%" + request.keyword().trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("code")), keyword),
                    builder.like(builder.lower(root.get("title")), keyword),
                    builder.like(builder.lower(root.get("description")), keyword),
                    builder.like(builder.lower(root.get("property").get("name")), keyword),
                    builder.like(
                            builder.lower(root.get("property").get("address").get("fullAddress")),
                            keyword
                    )
            ));
        }
        if (request.propertyTypeId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("property").get("propertyType").get("id"),
                            request.propertyTypeId()
                    ));
        }
        if (request.purpose() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("purpose"), request.purpose()));
        }
        if (request.provinceId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("property").get("address").get("province").get("id"),
                            request.provinceId()
                    ));
        }
        if (request.districtId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("property").get("address").get("district").get("id"),
                            request.districtId()
                    ));
        }
        if (request.wardId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("property").get("address").get("ward").get("id"),
                            request.wardId()
                    ));
        }
        if (request.minPrice() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("askingPrice"), request.minPrice()));
        }
        if (request.maxPrice() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("askingPrice"), request.maxPrice()));
        }
        if (request.minArea() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(
                            root.get("property").get("landArea"),
                            request.minArea()
                    ));
        }
        if (request.maxArea() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(
                            root.get("property").get("landArea"),
                            request.maxArea()
                    ));
        }
        if (request.bedrooms() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("property").get("bedrooms"),
                            request.bedrooms()
                    ));
        }
        if (request.bathrooms() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("property").get("bathrooms"),
                            request.bathrooms()
                    ));
        }
        return specification;
    }
}
