package com.javaweb.payment.repository;

import com.javaweb.payment.entity.Payment;
import com.javaweb.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findAllByTransactionIdOrderByCreatedAtDesc(Long transactionId);

    List<Payment> findAllByTransactionIdAndStatus(
            Long transactionId,
            PaymentStatus status
    );

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
