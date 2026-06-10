package com.javaweb.contract.repository;

import com.javaweb.contract.dto.ContractSearchRequest;
import com.javaweb.contract.entity.Contract;
import org.springframework.data.jpa.domain.Specification;

public final class ContractSpecifications {
    private ContractSpecifications() {
    }

    public static Specification<Contract> search(
            ContractSearchRequest request,
            Long visibleAgentId
    ) {
        Specification<Contract> specification = Specification.where(null);
        if (visibleAgentId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("agent").get("id"), visibleAgentId));
        }
        if (request.status() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), request.status()));
        }
        if (request.contractType() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("contractType"), request.contractType()));
        }
        if (request.propertyId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("property").get("id"), request.propertyId()));
        }
        if (request.customerId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("customer").get("id"), request.customerId()));
        }
        if (request.agentId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("agent").get("id"), request.agentId()));
        }
        return specification;
    }
}
