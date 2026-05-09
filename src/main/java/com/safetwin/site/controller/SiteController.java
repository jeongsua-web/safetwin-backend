package com.safetwin.site.controller;

import com.safetwin.common.response.ApiResponse;
import com.safetwin.common.util.SecurityUtils;
import com.safetwin.site.dto.SiteCreateRequest;
import com.safetwin.site.dto.SiteResponse;
import com.safetwin.site.dto.SiteUpdateRequest;
import com.safetwin.site.service.SiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sites")
@RequiredArgsConstructor
public class SiteController {

    private final SiteService siteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SiteResponse> create(@Valid @RequestBody SiteCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(siteService.create(request, userId));
    }

    @GetMapping
    public ApiResponse<List<SiteResponse>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(siteService.list(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<SiteResponse> getDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(siteService.getDetail(id, userId));
    }

    @PutMapping("/{id}")
    public ApiResponse<SiteResponse> update(
            @PathVariable Long id,
            @RequestBody SiteUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(siteService.update(id, request, userId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        siteService.delete(id, userId);
        return ApiResponse.ok();
    }
}
