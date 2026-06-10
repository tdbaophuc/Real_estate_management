package com.javaweb.dashboard.service;

import com.javaweb.commission.enums.CommissionStatus;
import com.javaweb.dashboard.dto.AdminDashboardResponse;
import com.javaweb.dashboard.dto.ManagerDashboardResponse;
import com.javaweb.dashboard.dto.StatusCountResponse;
import com.javaweb.dashboard.repository.DashboardQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DashboardService {
    private final DashboardQueryRepository queryRepository;

    public DashboardService(DashboardQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse adminDashboard() {
        List<StatusCountResponse> leadsByStatus = queryRepository.leadCounts();
        List<StatusCountResponse> transactionsByStatus =
                queryRepository.transactionCounts();
        return new AdminDashboardResponse(
                queryRepository.countUsers(),
                queryRepository.countProperties(),
                queryRepository.countListings(),
                queryRepository.countPendingListings(),
                total(leadsByStatus),
                leadsByStatus,
                total(transactionsByStatus),
                transactionsByStatus,
                queryRepository.revenueSummary(),
                queryRepository.topAgents(5)
        );
    }

    @Transactional(readOnly = true)
    public ManagerDashboardResponse managerDashboard() {
        List<StatusCountResponse> leadsByStatus = queryRepository.leadCounts();
        List<StatusCountResponse> transactionsByStatus =
                queryRepository.transactionCounts();
        long pendingCommissions = queryRepository.countCommissions(
                CommissionStatus.PENDING
        ) + queryRepository.countCommissions(CommissionStatus.APPROVED);
        return new ManagerDashboardResponse(
                queryRepository.countAgents(),
                total(leadsByStatus),
                leadsByStatus,
                closeRate(leadsByStatus),
                total(transactionsByStatus),
                transactionsByStatus,
                pendingCommissions,
                queryRepository.countCommissions(CommissionStatus.PAID),
                queryRepository.revenueSummary(),
                queryRepository.topAgents(10)
        );
    }

    private long total(List<StatusCountResponse> counts) {
        return counts.stream().mapToLong(StatusCountResponse::count).sum();
    }

    private BigDecimal closeRate(List<StatusCountResponse> counts) {
        long won = count(counts, "CLOSED_WON");
        long lost = count(counts, "CLOSED_LOST");
        long closed = won + lost;
        if (closed == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(won)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(closed), 2, RoundingMode.HALF_UP);
    }

    private long count(List<StatusCountResponse> counts, String status) {
        return counts.stream()
                .filter(item -> item.status().equals(status))
                .mapToLong(StatusCountResponse::count)
                .findFirst()
                .orElse(0);
    }
}
