package com.safetwin.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenResponse {

    private final String tokenType = "Bearer";
    private String accessToken;
    private String refreshToken;
}
