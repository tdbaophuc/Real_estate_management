package com.javaweb.transaction.repository;

import com.javaweb.transaction.entity.Deposit;
import com.javaweb.transaction.enums.DepositStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepositRepository extends JpaRepository<Deposit, Long> {
    List<Deposit> findAllByTransactionIdOrderByCreatedAtDesc(Long transactionId);

    List<Deposit> findAllByTransactionIdAndStatus(
            Long transactionId,
            DepositStatus status
    );

    Optional<Deposit> findByIdempotencyKey(String idempotencyKey);
}
