package com.javaweb.transaction.controller;

import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.common.response.ApiResponse;
import com.javaweb.common.response.PageResponse;
import com.javaweb.transaction.dto.DepositCreateRequest;
import com.javaweb.transaction.dto.DepositResponse;
import com.javaweb.transaction.dto.InvoiceCreateRequest;
import com.javaweb.transaction.dto.InvoiceResponse;
import com.javaweb.transaction.dto.PaymentCreateRequest;
import com.javaweb.transaction.dto.PaymentResponse;
import com.javaweb.transaction.dto.PaymentScheduleCreateRequest;
import com.javaweb.transaction.dto.PaymentScheduleResponse;
import com.javaweb.transaction.dto.ReceiptCreateRequest;
import com.javaweb.transaction.dto.ReceiptResponse;
import com.javaweb.transaction.dto.TransactionCreateRequest;
import com.javaweb.transaction.dto.TransactionResponse;
import com.javaweb.transaction.dto.TransactionSearchRequest;
import com.javaweb.transaction.dto.TransactionStatusUpdateRequest;
import com.javaweb.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/transactions")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('AGENT', 'MANAGER', 'ADMIN')")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> create(
            @Valid @RequestBody TransactionCreateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Transaction created successfully",
                transactionService.create(request, actor)
        );
    }

    @GetMapping
    public ApiResponse<PageResponse<TransactionResponse>> list(
            @Valid @ParameterObject TransactionSearchRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(transactionService.search(request, actor));
    }

    @GetMapping("/{transactionId}")
    public ApiResponse<TransactionResponse> get(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(transactionService.get(transactionId, actor));
    }

    @PatchMapping("/{transactionId}/status")
    public ApiResponse<TransactionResponse> updateStatus(
            @PathVariable Long transactionId,
            @Valid @RequestBody TransactionStatusUpdateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Transaction status updated successfully",
                transactionService.updateStatus(transactionId, request, actor)
        );
    }

    @PostMapping("/{transactionId}/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DepositResponse> addDeposit(
            @PathVariable Long transactionId,
            @Valid @RequestBody DepositCreateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Deposit recorded successfully",
                transactionService.addDeposit(transactionId, request, actor)
        );
    }

    @PostMapping("/{transactionId}/payment-schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentScheduleResponse> addPaymentSchedule(
            @PathVariable Long transactionId,
            @Valid @RequestBody PaymentScheduleCreateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Payment schedule created successfully",
                transactionService.addSchedule(transactionId, request, actor)
        );
    }

    @PostMapping("/{transactionId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PaymentResponse> addPayment(
            @PathVariable Long transactionId,
            @Valid @RequestBody PaymentCreateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "External payment recorded successfully",
                transactionService.addPayment(transactionId, request, actor)
        );
    }

    @PostMapping("/{transactionId}/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvoiceResponse> addInvoice(
            @PathVariable Long transactionId,
            @Valid @RequestBody InvoiceCreateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Invoice metadata created successfully",
                transactionService.addInvoice(transactionId, request, actor)
        );
    }

    @PostMapping("/{transactionId}/payments/{paymentId}/receipt")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReceiptResponse> addReceipt(
            @PathVariable Long transactionId,
            @PathVariable Long paymentId,
            @Valid @RequestBody ReceiptCreateRequest request,
            @AuthenticationPrincipal AuthUserPrincipal actor
    ) {
        return ApiResponse.success(
                "Receipt metadata created successfully",
                transactionService.addReceipt(
                        transactionId,
                        paymentId,
                        request,
                        actor
                )
        );
    }
}
