package com.javaweb.lead.repository;

import com.javaweb.lead.dto.LeadSearchRequest;
import com.javaweb.lead.entity.Lead;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class LeadSpecifications {
    private LeadSpecifications() {
    }

    public static Specification<Lead> search(
            LeadSearchRequest request,
            Long visibleUserId
    ) {
        Specification<Lead> specification =
                (root, query, builder) -> builder.isNull(root.get("deletedAt"));

        if (visibleUserId != null) {
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.equal(root.get("createdBy").get("id"), visibleUserId),
                    builder.equal(root.get("currentAssignee").get("id"), visibleUserId)
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
        if (request.sourceId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("source").get("id"), request.sourceId()));
        }
        if (request.assignedAgentId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("currentAssignee").get("id"),
                            request.assignedAgentId()
                    ));
        }
        return specification;
    }
}
