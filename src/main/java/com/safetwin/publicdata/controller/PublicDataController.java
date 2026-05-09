package com.safetwin.publicdata.controller;

import com.safetwin.common.response.ApiResponse;
import com.safetwin.publicdata.dto.AccidentCaseResponse;
import com.safetwin.publicdata.dto.LawResponse;
import com.safetwin.publicdata.service.PublicDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public-data")
@RequiredArgsConstructor
public class PublicDataController {

    private final PublicDataService publicDataService;

    @GetMapping("/accident-cases")
    public ApiResponse<AccidentCaseResponse> getAccidentCases(
            @RequestParam(defaultValue = "건설") String industryType) {
        return ApiResponse.ok(publicDataService.getAccidentCases(industryType));
    }

    @GetMapping("/laws")
    public ApiResponse<LawResponse> searchLaws(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(publicDataService.searchLaws(keyword));
    }
}
