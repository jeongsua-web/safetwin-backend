package com.safetwin.risk.dto;

import com.safetwin.entity.Risk;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RiskStatusUpdateRequest {

    @NotNull(message = "상태 값은 필수입니다.")
    private Risk.Status status;
}
