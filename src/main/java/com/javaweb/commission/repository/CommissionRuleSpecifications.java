package com.javaweb.commission.repository;

import com.javaweb.commission.dto.CommissionRuleSearchRequest;
import com.javaweb.commission.entity.CommissionRule;
import org.springframework.data.jpa.domain.Specification;

public final class CommissionRuleSpecifications {
    private CommissionRuleSpecifications() {
    }

    public static Specification<CommissionRule> search(
            CommissionRuleSearchRequest request
    ) {
        Specification<CommissionRule> specification = Specification.where(null);
        if (request.active() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("active"), request.active()));
        }
        if (request.transactionType() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("transactionType"),
                            request.transactionType()
                    ));
        }
        return specification;
    }
}
