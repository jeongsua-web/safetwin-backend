package com.safetwin.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BizVerifyResponse {

    private String bizNumber;
    private boolean valid;
    private String companyName;
    private String status;
}
