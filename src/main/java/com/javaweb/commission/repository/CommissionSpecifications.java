package com.javaweb.commission.repository;

import com.javaweb.commission.dto.CommissionSearchRequest;
import com.javaweb.commission.entity.Commission;
import org.springframework.data.jpa.domain.Specification;

public final class CommissionSpecifications {
    private CommissionSpecifications() {
    }

    public static Specification<Commission> search(
            CommissionSearchRequest request,
            Long visibleBeneficiaryId
    ) {
        Specification<Commission> specification = Specification.where(null);
        if (visibleBeneficiaryId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("beneficiaryUser").get("id"),
                            visibleBeneficiaryId
                    ));
        }
        if (request.status() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), request.status()));
        }
        if (request.transactionId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("transaction").get("id"),
                            request.transactionId()
                    ));
        }
        if (request.beneficiaryUserId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(
                            root.get("beneficiaryUser").get("id"),
                            request.beneficiaryUserId()
                    ));
        }
        return specification;
    }
}
