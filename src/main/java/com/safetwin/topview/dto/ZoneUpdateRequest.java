package com.safetwin.topview.dto;

import lombok.Getter;

@Getter
public class ZoneUpdateRequest {

    private String name;
    private String description;
    private Integer floorNumber;
    private Double x;
    private Double y;
    private Double w;
    private Double h;
    private String area;
}
