package com.javaweb.customer.repository;

import com.javaweb.customer.dto.CustomerSearchRequest;
import com.javaweb.customer.entity.Customer;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class CustomerSpecifications {
    private CustomerSpecifications() {
    }

    public static Specification<Customer> search(
            CustomerSearchRequest request,
            Long visibleUserId
    ) {
        Specification<Customer> specification =
                (root, query, builder) -> builder.isNull(root.get("deletedAt"));

        if (visibleUserId != null) {
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.equal(root.get("createdBy").get("id"), visibleUserId),
                    builder.equal(root.get("assignedAgent").get("id"), visibleUserId)
            ));
        }
        if (StringUtils.hasText(request.keyword())) {
            String keyword = "%" + request.keyword().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("code")), keyword),
                    builder.like(builder.lower(root.get("fullName")), keyword),
                    builder.like(builder.lower(root.get("email")), keyword),
                    builder.like(builder.lower(root.get("phone")), keyword)
            ));
        }
        if (request.status() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), request.status()));
        }
        if (request.priority() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("priority"), request.priority()));
        }
        if (request.assignedAgentId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("assignedAgent").get("id"),
                            request.assignedAgentId()
                    ));
        }
        return specification;
    }
}
