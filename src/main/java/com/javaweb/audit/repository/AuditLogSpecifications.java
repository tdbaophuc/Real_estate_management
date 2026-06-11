package com.javaweb.audit.repository;

import com.javaweb.audit.dto.AuditLogSearchRequest;
import com.javaweb.audit.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecifications {
    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> search(AuditLogSearchRequest request) {
        Specification<AuditLog> specification = Specification.where(null);
        if (request.actorId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("actor").get("id"), request.actorId()));
        }
        if (request.action() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("action"), request.action()));
        }
        if (request.resourceType() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("resourceType"), request.resourceType()));
        }
        if (request.resourceId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("resourceId"), request.resourceId()));
        }
        if (request.from() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(
                            root.get("createdAt"),
                            request.from()
                    ));
        }
        if (request.to() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("createdAt"), request.to()));
        }
        return specification;
    }
}
