package com.safetwin.topview.dto;

import com.safetwin.entity.Risk;
import com.safetwin.entity.Zone;
import lombok.Builder;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;

@Getter
@Builder
public class ZoneDetailResponse {

    private Long id;
    private String name;
    private String description;
    private Integer floorNumber;
    private Double x;
    private Double y;
    private Double w;
    private Double h;
    private String area;
    private Long siteId;
    private String siteName;
    private long riskCount;
    private String riskLevel;
    private List<RiskTagResponse> riskTags;

    public static ZoneDetailResponse of(Zone zone, List<Risk> risks) {
        String riskLevel = risks.stream()
                .map(Risk::getLevel)
                .max(Comparator.comparingInt(ZoneDetailResponse::levelWeight))
                .map(Enum::name)
                .orElse("NONE");

        return ZoneDetailResponse.builder()
                .id(zone.getId())
                .name(zone.getName())
                .description(zone.getDescription())
                .floorNumber(zone.getFloorNumber())
                .x(zone.getX())
                .y(zone.getY())
                .w(zone.getW())
                .h(zone.getH())
                .area(zone.getArea())
                .siteId(zone.getSite().getId())
                .siteName(zone.getSite().getName())
                .riskCount(risks.size())
                .riskLevel(riskLevel)
                .riskTags(risks.stream().map(RiskTagResponse::from).toList())
                .build();
    }

    private static int levelWeight(Risk.Level level) {
        return switch (level) {
            case CRITICAL -> 3;
            case HIGH -> 2;
            case MEDIUM -> 1;
            case LOW -> 0;
        };
    }
}
