package com.javaweb.payment.repository;

import com.javaweb.payment.entity.Invoice;
import com.javaweb.payment.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findAllByTransactionIdOrderByIssueDateDesc(Long transactionId);

    List<Invoice> findAllByStatusOrderByDueDateAsc(InvoiceStatus status);
}
