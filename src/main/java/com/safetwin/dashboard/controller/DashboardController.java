package com.safetwin.dashboard.controller;

import com.safetwin.common.response.ApiResponse;
import com.safetwin.common.util.SecurityUtils;
import com.safetwin.dashboard.dto.DashboardSummaryResponse;
import com.safetwin.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(dashboardService.getSummary(userId));
    }
}
