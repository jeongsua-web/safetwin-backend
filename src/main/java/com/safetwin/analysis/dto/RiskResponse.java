package com.safetwin.analysis.dto;

import com.safetwin.entity.Risk;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RiskResponse {

    private Long id;
    private String label;
    private String detail;
    private String level;
    private String law;
    private String action;
    private Double x;
    private Double y;

    public static RiskResponse from(Risk risk) {
        return RiskResponse.builder()
                .id(risk.getId())
                .label(risk.getLabel())
                .detail(risk.getDescription())
                .level(risk.getLevel().name())
                .law(risk.getLaw())
                .action(risk.getAction())
                .x(risk.getX())
                .y(risk.getY())
                .build();
    }
}
