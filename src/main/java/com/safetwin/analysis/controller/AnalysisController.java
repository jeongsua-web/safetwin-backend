package com.safetwin.analysis.controller;

import com.safetwin.analysis.dto.AnalysisResponse;
import com.safetwin.analysis.dto.AnalysisStatusResponse;
import com.safetwin.analysis.service.AnalysisService;
import com.safetwin.common.response.ApiResponse;
import com.safetwin.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<AnalysisResponse> create(
            @RequestPart("image") MultipartFile image,
            @RequestParam("zoneId") Long zoneId) {

        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(analysisService.create(image, zoneId, userId));
    }

    @GetMapping
    public ApiResponse<Page<AnalysisResponse>> list(
            @RequestParam(required = false) Long siteId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(analysisService.list(userId, siteId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<AnalysisResponse> getDetail(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(analysisService.getDetail(id, userId));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<AnalysisStatusResponse> getStatus(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(analysisService.getStatus(id, userId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        analysisService.delete(id, userId);
        return ApiResponse.ok();
    }
}
