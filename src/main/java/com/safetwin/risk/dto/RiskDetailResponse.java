package com.safetwin.risk.dto;

import com.safetwin.entity.Risk;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RiskDetailResponse {

    private Long id;
    private Long analysisId;
    private Long siteId;
    private String siteName;
    private String zoneLocation;
    private String label;
    private String detail;
    private String level;
    private String status;
    private String law;
    private String action;
    private Double x;
    private Double y;
    private LocalDateTime createdAt;

    public static RiskDetailResponse from(Risk risk) {
        return RiskDetailResponse.builder()
                .id(risk.getId())
                .analysisId(risk.getAnalysis().getId())
                .siteId(risk.getAnalysis().getZone().getSite().getId())
                .siteName(risk.getAnalysis().getZone().getSite().getName())
                .zoneLocation(risk.getAnalysis().getZone().getSite().getName()
                        + " > " + risk.getAnalysis().getZone().getName())
                .label(risk.getLabel())
                .detail(risk.getDescription())
                .level(risk.getLevel().name())
                .status(risk.getStatus().name())
                .law(risk.getLaw())
                .action(risk.getAction())
                .x(risk.getX())
                .y(risk.getY())
                .createdAt(risk.getCreatedAt())
                .build();
    }
}
