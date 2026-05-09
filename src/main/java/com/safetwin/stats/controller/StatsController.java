package com.safetwin.stats.controller;

import com.safetwin.common.response.ApiResponse;
import com.safetwin.common.util.SecurityUtils;
import com.safetwin.stats.dto.CompareResponse;
import com.safetwin.stats.dto.ScoreTrendResponse;
import com.safetwin.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/score-trend")
    public ApiResponse<ScoreTrendResponse> getScoreTrend(
            @RequestParam(defaultValue = "monthly") String period) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(statsService.getScoreTrend(userId, period));
    }

    @GetMapping("/compare")
    public ApiResponse<CompareResponse> compare(
            @RequestParam Long before,
            @RequestParam Long after) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(statsService.compare(before, after, userId));
    }
}
