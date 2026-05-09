package com.safetwin.topview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ZoneCreateRequest {

    @NotNull(message = "사업장 ID는 필수입니다.")
    private Long siteId;

    @NotBlank(message = "구역 이름은 필수입니다.")
    private String name;

    private String description;
    private Integer floorNumber;
    private Double x;
    private Double y;
    private Double w;
    private Double h;
    private String area;
}
