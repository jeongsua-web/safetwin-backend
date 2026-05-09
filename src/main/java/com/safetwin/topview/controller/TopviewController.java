package com.safetwin.topview.controller;

import com.safetwin.common.response.ApiResponse;
import com.safetwin.common.util.SecurityUtils;
import com.safetwin.topview.dto.*;
import com.safetwin.topview.service.TopviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topview")
@RequiredArgsConstructor
public class TopviewController {

    private final TopviewService topviewService;

    @GetMapping("/zones")
    public ApiResponse<List<ZoneListResponse>> listZones(
            @RequestParam Long siteId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(topviewService.listZones(siteId, userId));
    }

    @GetMapping("/zones/{id}")
    public ApiResponse<ZoneDetailResponse> getZone(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(topviewService.getZone(id, userId));
    }

    @PostMapping("/zones")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ZoneDetailResponse> createZone(
            @Valid @RequestBody ZoneCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(topviewService.createZone(request, userId));
    }

    @PutMapping("/zones/{id}")
    public ApiResponse<ZoneDetailResponse> updateZone(
            @PathVariable Long id,
            @RequestBody ZoneUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(topviewService.updateZone(id, request, userId));
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<ZoneDetailResponse>> generate(
            @Valid @RequestBody TopviewGenerateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(topviewService.generateTopview(request, userId));
    }
}
