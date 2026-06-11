package com.javaweb.transaction.repository;

import com.javaweb.transaction.entity.PaymentSchedule;
import com.javaweb.transaction.enums.PaymentScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, Long> {
    List<PaymentSchedule> findAllByTransactionIdOrderByInstallmentNumberAsc(
            Long transactionId
    );

    List<PaymentSchedule> findAllByStatusOrderByDueDateAsc(
            PaymentScheduleStatus status
    );

    Optional<PaymentSchedule> findByTransactionIdAndInstallmentNumber(
            Long transactionId,
            int installmentNumber
    );
}
