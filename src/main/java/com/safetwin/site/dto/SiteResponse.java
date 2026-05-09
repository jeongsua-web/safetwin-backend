package com.safetwin.site.dto;

import com.safetwin.entity.Site;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SiteResponse {

    private Long id;
    private String name;
    private String address;
    private String bizNumber;
    private String status;
    private Long managerId;
    private String managerName;
    private LocalDateTime createdAt;

    public static SiteResponse from(Site site) {
        return SiteResponse.builder()
                .id(site.getId())
                .name(site.getName())
                .address(site.getAddress())
                .bizNumber(site.getBizNumber())
                .status(site.getStatus().name())
                .managerId(site.getManager().getId())
                .managerName(site.getManager().getName())
                .createdAt(site.getCreatedAt())
                .build();
    }
}
