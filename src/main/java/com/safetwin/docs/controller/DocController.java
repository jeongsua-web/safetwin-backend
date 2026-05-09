package com.safetwin.docs.controller;

import com.safetwin.common.response.ApiResponse;
import com.safetwin.common.util.SecurityUtils;
import com.safetwin.docs.dto.*;
import com.safetwin.docs.service.DocService;
import com.safetwin.entity.Document;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocController {

    private final DocService docService;

    @PostMapping("/risk-assessment")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocResponse> createRiskAssessment(
            @Valid @RequestBody RiskAssessmentRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(docService.createRiskAssessment(request, userId));
    }

    @PostMapping("/education-cert")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocResponse> createEducationCert(
            @Valid @RequestBody EducationCertRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(docService.createEducationCert(request, userId));
    }

    @PostMapping(value = "/{id}/group-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocResponse> attachGroupPhoto(
            @PathVariable Long id,
            @RequestPart("photo") MultipartFile photo) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(docService.attachGroupPhoto(id, photo, userId));
    }

    @GetMapping
    public ApiResponse<Page<DocResponse>> list(
            @RequestParam(required = false) Document.DocumentType type,
            @RequestParam(required = false) Document.DocStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(docService.list(userId, type, status, pageable));
    }

    @GetMapping("/{id}/pdf")
    public void downloadPdf(@PathVariable Long id,
                            jakarta.servlet.http.HttpServletResponse response) throws Exception {
        Long userId = SecurityUtils.getCurrentUserId();
        String fileUrl = docService.getPdfUrl(id, userId);
        response.sendRedirect(fileUrl);
    }

    @PostMapping("/{id}/sign")
    public ApiResponse<SignStatusResponse> sign(
            @PathVariable Long id,
            @Valid @RequestBody SignRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(docService.sign(id, request, userId));
    }

    @GetMapping("/{id}/sign/status")
    public ApiResponse<SignStatusResponse> getSignStatus(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.ok(docService.getSignStatus(id, userId));
    }
}
