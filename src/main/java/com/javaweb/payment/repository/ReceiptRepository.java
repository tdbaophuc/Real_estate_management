package com.javaweb.payment.repository;

import com.javaweb.payment.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReceiptRepository extends JpaRepository<Receipt, Long> {
    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    Optional<Receipt> findByPaymentId(Long paymentId);
}
