package com.safetwin.topview.dto;

import com.safetwin.entity.Zone;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ZoneListResponse {

    private Long id;
    private String name;
    private String description;
    private Integer floorNumber;
    private Double x;
    private Double y;
    private Double w;
    private Double h;
    private String area;
    private long riskCount;
    private String riskLevel;

    public static ZoneListResponse of(Zone zone, long riskCount, String riskLevel) {
        return ZoneListResponse.builder()
                .id(zone.getId())
                .name(zone.getName())
                .description(zone.getDescription())
                .floorNumber(zone.getFloorNumber())
                .x(zone.getX())
                .y(zone.getY())
                .w(zone.getW())
                .h(zone.getH())
                .area(zone.getArea())
                .riskCount(riskCount)
                .riskLevel(riskLevel)
                .build();
    }
}
