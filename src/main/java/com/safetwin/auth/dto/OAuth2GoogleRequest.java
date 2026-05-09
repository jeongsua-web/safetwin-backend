package com.safetwin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class OAuth2GoogleRequest {

    @NotBlank(message = "Google ID Token은 필수입니다.")
    private String idToken;
}
