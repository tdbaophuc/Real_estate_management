package com.javaweb.dashboard.service;

import com.javaweb.common.exception.BusinessException;
import com.javaweb.dashboard.dto.CommissionReportResponse;
import com.javaweb.dashboard.dto.LeadReportResponse;
import com.javaweb.dashboard.dto.RevenueReportResponse;
import com.javaweb.dashboard.dto.StatusCountResponse;
import com.javaweb.dashboard.dto.TransactionReportResponse;
import com.javaweb.dashboard.repository.DashboardQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ReportService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DashboardQueryRepository queryRepository;

    public ReportService(DashboardQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    @Transactional(readOnly = true)
    public RevenueReportResponse revenue(LocalDate from, LocalDate to) {
        DateRange range = dateRange(from, to);
        return new RevenueReportResponse(
                from,
                to,
                queryRepository.revenueSummary(range.start(), range.endExclusive())
        );
    }

    @Transactional(readOnly = true)
    public LeadReportResponse leads(LocalDate from, LocalDate to) {
        DateRange range = dateRange(from, to);
        List<StatusCountResponse> counts =
                queryRepository.leadCounts(range.start(), range.endExclusive());
        return new LeadReportResponse(from, to, total(counts), counts);
    }

    @Transactional(readOnly = true)
    public TransactionReportResponse transactions(LocalDate from, LocalDate to) {
        DateRange range = dateRange(from, to);
        List<StatusCountResponse> counts =
                queryRepository.transactionCounts(range.start(), range.endExclusive());
        return new TransactionReportResponse(
                from,
                to,
                total(counts),
                counts,
                queryRepository.completedTransactionValues(
                        range.start(),
                        range.endExclusive()
                )
        );
    }

    @Transactional(readOnly = true)
    public CommissionReportResponse commissions(LocalDate from, LocalDate to) {
        DateRange range = dateRange(from, to);
        List<StatusCountResponse> counts =
                queryRepository.commissionCounts(range.start(), range.endExclusive());
        return new CommissionReportResponse(
                from,
                to,
                total(counts),
                counts,
                queryRepository.commissionAmounts(
                        range.start(),
                        range.endExclusive()
                )
        );
    }

    private DateRange dateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("from must not be after to");
        }
        return new DateRange(
                from.atStartOfDay(BUSINESS_ZONE).toInstant(),
                to.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant()
        );
    }

    private long total(List<StatusCountResponse> counts) {
        return counts.stream().mapToLong(StatusCountResponse::count).sum();
    }

    private record DateRange(Instant start, Instant endExclusive) {
    }
}
