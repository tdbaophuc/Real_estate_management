package com.javaweb.lead.repository;

import com.javaweb.lead.dto.FollowUpTaskSearchRequest;
import com.javaweb.lead.entity.FollowUpTask;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class FollowUpTaskSpecifications {
    private FollowUpTaskSpecifications() {
    }

    public static Specification<FollowUpTask> search(
            FollowUpTaskSearchRequest request,
            Long visibleUserId
    ) {
        Specification<FollowUpTask> specification = (root, query, builder) ->
                builder.isNull(root.get("lead").get("deletedAt"));

        if (visibleUserId != null) {
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.equal(root.get("assignedTo").get("id"), visibleUserId),
                    builder.equal(root.get("createdBy").get("id"), visibleUserId),
                    builder.equal(root.get("lead").get("currentAssignee").get("id"), visibleUserId),
                    builder.equal(root.get("lead").get("createdBy").get("id"), visibleUserId)
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
        if (request.leadId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("lead").get("id"), request.leadId()));
        }
        if (request.assignedAgentId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("assignedTo").get("id"), request.assignedAgentId()));
        }
        if (request.dueFrom() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("dueAt"), request.dueFrom()));
        }
        if (request.dueTo() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("dueAt"), request.dueTo()));
        }
        if (StringUtils.hasText(request.keyword())) {
            String keyword = "%" + request.keyword().trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("title")), keyword),
                    builder.like(builder.lower(root.get("description")), keyword),
                    builder.like(builder.lower(root.get("lead").get("code")), keyword),
                    builder.like(builder.lower(root.get("lead").get("fullName")), keyword)
            ));
        }
        return specification;
    }
}
