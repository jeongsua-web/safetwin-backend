package com.safetwin.worker.controller;

import com.safetwin.common.response.ApiResponse;
import com.safetwin.common.util.SecurityUtils;
import com.safetwin.worker.dto.WorkerCreateRequest;
import com.safetwin.worker.dto.WorkerResponse;
import com.safetwin.worker.dto.WorkerUpdateRequest;
import com.safetwin.worker.service.WorkerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WorkerController {

    private final WorkerService workerService;

    @PostMapping("/api/sites/{siteId}/workers")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkerResponse> create(
            @PathVariable Long siteId,
            @Valid @RequestBody WorkerCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(workerService.create(siteId, request, userId));
    }

    @GetMapping("/api/sites/{siteId}/workers")
    public ApiResponse<List<WorkerResponse>> list(@PathVariable Long siteId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(workerService.list(siteId, userId));
    }

    @PutMapping("/api/workers/{id}")
    public ApiResponse<WorkerResponse> update(
            @PathVariable Long id,
            @RequestBody WorkerUpdateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(workerService.update(id, request, userId));
    }

    @DeleteMapping("/api/workers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        workerService.delete(id, userId);
        return ApiResponse.ok();
    }
}
