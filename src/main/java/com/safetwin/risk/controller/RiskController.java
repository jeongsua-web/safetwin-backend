package com.safetwin.risk.controller;

import com.safetwin.common.response.ApiResponse;
import com.safetwin.common.util.SecurityUtils;
import com.safetwin.entity.Risk;
import com.safetwin.risk.dto.RiskDetailResponse;
import com.safetwin.risk.dto.RiskStatusUpdateRequest;
import com.safetwin.risk.service.RiskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/risks")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @GetMapping
    public ApiResponse<List<RiskDetailResponse>> list(
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Risk.Status status) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(riskService.list(userId, siteId, status));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<RiskDetailResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RiskStatusUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(riskService.updateStatus(id, request, userId));
    }
}
