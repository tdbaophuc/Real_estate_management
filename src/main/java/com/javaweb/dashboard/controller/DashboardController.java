package com.javaweb.dashboard.controller;

import com.javaweb.common.response.ApiResponse;
import com.javaweb.dashboard.dto.AdminDashboardResponse;
import com.javaweb.dashboard.dto.ManagerDashboardResponse;
import com.javaweb.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDashboardResponse> adminDashboard() {
        return ApiResponse.success(dashboardService.adminDashboard());
    }

    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<ManagerDashboardResponse> managerDashboard() {
        return ApiResponse.success(dashboardService.managerDashboard());
    }
}
