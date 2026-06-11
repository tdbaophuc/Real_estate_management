package com.javaweb.transaction.repository;

import com.javaweb.transaction.dto.TransactionSearchRequest;
import com.javaweb.transaction.entity.Transaction;
import org.springframework.data.jpa.domain.Specification;

public final class TransactionSpecifications {
    private TransactionSpecifications() {
    }

    public static Specification<Transaction> search(
            TransactionSearchRequest request,
            Long visibleAgentId
    ) {
        Specification<Transaction> specification = Specification.where(null);
        if (visibleAgentId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("agent").get("id"), visibleAgentId));
        }
        if (request.status() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), request.status()));
        }
        if (request.transactionType() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("transactionType"), request.transactionType()));
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
