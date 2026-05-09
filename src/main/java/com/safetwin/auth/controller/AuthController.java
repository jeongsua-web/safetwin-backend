package com.safetwin.auth.controller;

import com.safetwin.auth.dto.*;
import com.safetwin.auth.service.AuthService;
import com.safetwin.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ApiResponse.ok();
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ApiResponse.ok();
    }

    @PostMapping("/biz-verify")
    public ApiResponse<BizVerifyResponse> verifyBizNumber(@Valid @RequestBody BizVerifyRequest request) {
        return ApiResponse.ok(authService.verifyBizNumber(request));
    }

    @PostMapping("/oauth2/google")
    public ApiResponse<TokenResponse> googleOAuth2(@Valid @RequestBody OAuth2GoogleRequest request) {
        return ApiResponse.ok(authService.googleOAuth2(request));
    }
}
