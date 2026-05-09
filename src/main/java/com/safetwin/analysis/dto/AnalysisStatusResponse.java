package com.safetwin.analysis.dto;

import com.safetwin.entity.Analysis;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalysisStatusResponse {

    private Long id;
    private String status;

    public static AnalysisStatusResponse from(Analysis analysis) {
        return AnalysisStatusResponse.builder()
                .id(analysis.getId())
                .status(analysis.getStatus().name())
                .build();
    }
}
