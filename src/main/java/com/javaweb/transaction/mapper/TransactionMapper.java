package com.javaweb.transaction.mapper;

import com.javaweb.payment.entity.Invoice;
import com.javaweb.payment.entity.Payment;
import com.javaweb.payment.entity.Receipt;
import com.javaweb.payment.enums.PaymentStatus;
import com.javaweb.transaction.dto.DepositResponse;
import com.javaweb.transaction.dto.InvoiceResponse;
import com.javaweb.transaction.dto.PaymentResponse;
import com.javaweb.transaction.dto.PaymentScheduleResponse;
import com.javaweb.transaction.dto.ReceiptResponse;
import com.javaweb.transaction.dto.TransactionResponse;
import com.javaweb.transaction.entity.Deposit;
import com.javaweb.transaction.entity.PaymentSchedule;
import com.javaweb.transaction.entity.Transaction;
import com.javaweb.transaction.enums.DepositStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;

@Component
public class TransactionMapper {
    public TransactionResponse toResponse(Transaction transaction) {
        BigDecimal confirmedAmount = confirmedAmount(transaction);
        BigDecimal remainingAmount = transaction.getAgreedValue()
                .subtract(confirmedAmount)
                .max(BigDecimal.ZERO);
        return new TransactionResponse(
                transaction.getId(),
                transaction.getCode(),
                transaction.getTransactionType(),
                transaction.getStatus(),
                transaction.getContract() == null ? null : transaction.getContract().getId(),
                transaction.getContract() == null ? null : transaction.getContract().getCode(),
                transaction.getProperty().getId(),
                transaction.getProperty().getCode(),
                transaction.getProperty().getName(),
                transaction.getCustomer().getId(),
                transaction.getCustomer().getCode(),
                transaction.getCustomer().getFullName(),
                transaction.getOwner().getId(),
                transaction.getOwner().getFullName(),
                transaction.getAgent().getId(),
                transaction.getAgent().getFullName(),
                transaction.getCreatedBy().getId(),
                transaction.getCreatedBy().getFullName(),
                transaction.getAgreedValue(),
                confirmedAmount,
                remainingAmount,
                transaction.getCurrency(),
                transaction.getTransactionDate(),
                transaction.getExpectedCompletionDate(),
                transaction.getCompletedAt(),
                transaction.getCancelledAt(),
                transaction.getRefundedAt(),
                transaction.getCancellationReason(),
                transaction.getNotes(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt(),
                transaction.getDeposits().stream()
                        .sorted(Comparator.comparing(Deposit::getCreatedAt).reversed())
                        .map(this::toDepositResponse)
                        .toList(),
                transaction.getPaymentSchedules().stream()
                        .sorted(Comparator.comparingInt(PaymentSchedule::getInstallmentNumber))
                        .map(this::toScheduleResponse)
                        .toList(),
                transaction.getPayments().stream()
                        .sorted(Comparator.comparing(Payment::getCreatedAt).reversed())
                        .map(this::toPaymentResponse)
                        .toList(),
                transaction.getInvoices().stream()
                        .sorted(Comparator.comparing(Invoice::getIssueDate).reversed())
                        .map(this::toInvoiceResponse)
                        .toList()
        );
    }

    public DepositResponse toDepositResponse(Deposit deposit) {
        return new DepositResponse(
                deposit.getId(),
                deposit.getAmount(),
                deposit.getCurrency(),
                deposit.getPaymentMethod(),
                deposit.getStatus(),
                deposit.getReferenceNumber(),
                deposit.getIdempotencyKey(),
                deposit.getDueDate(),
                deposit.getReceivedAt(),
                deposit.getVerifiedAt(),
                deposit.getReceivedBy().getId(),
                deposit.getReceivedBy().getFullName(),
                deposit.getNotes(),
                deposit.getCreatedAt()
        );
    }

    public PaymentScheduleResponse toScheduleResponse(PaymentSchedule schedule) {
        return new PaymentScheduleResponse(
                schedule.getId(),
                schedule.getInstallmentNumber(),
                schedule.getLabel(),
                schedule.getDueDate(),
                schedule.getAmount(),
                schedule.getPaidAmount(),
                schedule.getCurrency(),
                schedule.getStatus(),
                schedule.getPaidAt(),
                schedule.getNotes(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }

    public PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentSchedule() == null
                        ? null
                        : payment.getPaymentSchedule().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getReferenceNumber(),
                payment.getIdempotencyKey(),
                payment.getPaidAt(),
                payment.getConfirmedAt(),
                payment.getReceivedBy().getId(),
                payment.getReceivedBy().getFullName(),
                payment.getNotes(),
                payment.getReceipt() == null ? null : toReceiptResponse(payment.getReceipt()),
                payment.getCreatedAt()
        );
    }

    public InvoiceResponse toInvoiceResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getSubtotal(),
                invoice.getTaxAmount(),
                invoice.getTotalAmount(),
                invoice.getCurrency(),
                invoice.getBilledToName(),
                invoice.getBilledToEmail(),
                invoice.getBilledToAddress(),
                invoice.getIssuedBy().getId(),
                invoice.getIssuedBy().getFullName(),
                invoice.getNotes(),
                invoice.getCreatedAt()
        );
    }

    public ReceiptResponse toReceiptResponse(Receipt receipt) {
        return new ReceiptResponse(
                receipt.getId(),
                receipt.getReceiptNumber(),
                receipt.getIssuedAt(),
                receipt.getAmount(),
                receipt.getCurrency(),
                receipt.getPayerName(),
                receipt.getIssuedBy().getId(),
                receipt.getIssuedBy().getFullName(),
                receipt.getNotes(),
                receipt.getCreatedAt()
        );
    }

    public BigDecimal confirmedAmount(Transaction transaction) {
        BigDecimal deposits = transaction.getDeposits().stream()
                .filter(item -> item.getStatus() == DepositStatus.VERIFIED)
                .map(Deposit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payments = transaction.getPayments().stream()
                .filter(item -> item.getStatus() == PaymentStatus.COMPLETED)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return deposits.add(payments);
    }
}
