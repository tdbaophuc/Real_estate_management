package com.javaweb.dashboard.controller;

import com.javaweb.common.response.ApiResponse;
import com.javaweb.dashboard.dto.CommissionReportResponse;
import com.javaweb.dashboard.dto.LeadReportResponse;
import com.javaweb.dashboard.dto.RevenueReportResponse;
import com.javaweb.dashboard.dto.TransactionReportResponse;
import com.javaweb.dashboard.service.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reports")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/revenue")
    public ApiResponse<RevenueReportResponse> revenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ApiResponse.success(reportService.revenue(from, to));
    }

    @GetMapping("/leads")
    public ApiResponse<LeadReportResponse> leads(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ApiResponse.success(reportService.leads(from, to));
    }

    @GetMapping("/transactions")
    public ApiResponse<TransactionReportResponse> transactions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ApiResponse.success(reportService.transactions(from, to));
    }

    @GetMapping("/commissions")
    public ApiResponse<CommissionReportResponse> commissions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return ApiResponse.success(reportService.commissions(from, to));
    }
}
