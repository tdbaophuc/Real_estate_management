package com.javaweb.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

public record LeadReportResponse(
        LocalDate from,
        LocalDate to,
        long totalLeads,
        List<StatusCountResponse> leadsByStatus
) {
}
