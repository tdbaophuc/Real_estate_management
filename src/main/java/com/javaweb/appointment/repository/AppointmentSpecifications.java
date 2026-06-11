package com.javaweb.appointment.repository;

import com.javaweb.appointment.dto.AppointmentSearchRequest;
import com.javaweb.appointment.entity.Appointment;
import org.springframework.data.jpa.domain.Specification;

public final class AppointmentSpecifications {
    private AppointmentSpecifications() {
    }

    public static Specification<Appointment> search(
            AppointmentSearchRequest request,
            Long visibleAgentId
    ) {
        Specification<Appointment> specification = Specification.where(null);
        if (visibleAgentId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("agent").get("id"), visibleAgentId));
        }
        if (request.status() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), request.status()));
        }
        if (request.agentId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("agent").get("id"), request.agentId()));
        }
        if (request.customerId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("customer").get("id"), request.customerId()));
        }
        if (request.propertyId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("property").get("id"), request.propertyId()));
        }
        if (request.from() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("startAt"), request.from()));
        }
        if (request.to() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThan(root.get("startAt"), request.to()));
        }
        return specification;
    }
}
