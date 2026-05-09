package com.safetwin.site.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SiteCreateRequest {

    @NotBlank(message = "사업장 이름은 필수입니다.")
    private String name;

    private String address;

    private String bizNumber;
}
