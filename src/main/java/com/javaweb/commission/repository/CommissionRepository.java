package com.javaweb.commission.repository;

import com.javaweb.commission.entity.Commission;
import com.javaweb.commission.enums.CommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CommissionRepository
        extends JpaRepository<Commission, Long>, JpaSpecificationExecutor<Commission> {
    List<Commission> findAllByTransactionId(Long transactionId);

    Page<Commission> findAllByBeneficiaryUserId(
            Long beneficiaryUserId,
            Pageable pageable
    );

    Page<Commission> findAllByStatus(CommissionStatus status, Pageable pageable);

    Optional<Commission> findByTransactionIdAndBeneficiaryUserId(
            Long transactionId,
            Long beneficiaryUserId
    );
}
