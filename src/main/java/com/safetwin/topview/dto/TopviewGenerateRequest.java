package com.safetwin.topview.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class TopviewGenerateRequest {

    @NotNull(message = "분석 ID는 필수입니다.")
    private Long analysisId;
}
