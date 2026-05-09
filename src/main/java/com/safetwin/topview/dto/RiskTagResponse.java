package com.safetwin.topview.dto;

import com.safetwin.entity.Risk;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RiskTagResponse {

    private Long riskId;
    private String label;
    private Double x;
    private Double y;
    private String law;
    private String accidentCase;

    public static RiskTagResponse from(Risk risk) {
        return RiskTagResponse.builder()
                .riskId(risk.getId())
                .label(risk.getLabel())
                .x(risk.getX())
                .y(risk.getY())
                .law(risk.getLaw())
                // TODO: 공공데이터 API에서 해당 위험 유형의 실제 사고 사례 연동
                .accidentCase(null)
                .build();
    }
}
