package com.safetwin.auth.service;

import com.safetwin.auth.dto.*;
import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import com.safetwin.entity.RefreshToken;
import com.safetwin.entity.User;
import com.safetwin.repository.RefreshTokenRepository;
import com.safetwin.repository.UserRepository;
import com.safetwin.security.JwtProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final NtsApiService ntsApiService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // ── 회원가입 ──────────────────────────────────────────────────────────────

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new SafeTwinException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (request.getBizNumber() != null && !validateBizNumberChecksum(request.getBizNumber())) {
            throw new SafeTwinException(ErrorCode.INVALID_BIZ_NUMBER);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .bizNumber(request.getBizNumber())
                .industry(request.getIndustry())
                .companySize(request.getCompanySize())
                .role(User.Role.OWNER)
                .build();

        userRepository.save(user);
    }

    // ── 로그인 ────────────────────────────────────────────────────────────────

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new SafeTwinException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new SafeTwinException(ErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokenPair(user);
    }

    // ── 토큰 재발급 ───────────────────────────────────────────────────────────

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken saved = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new SafeTwinException(ErrorCode.INVALID_TOKEN));

        if (saved.isExpired()) {
            refreshTokenRepository.delete(saved);
            throw new SafeTwinException(ErrorCode.EXPIRED_TOKEN);
        }

        Claims claims = jwtProvider.validateAndGetClaims(request.getRefreshToken());
        if (!jwtProvider.isRefreshToken(claims)) {
            throw new SafeTwinException(ErrorCode.INVALID_TOKEN);
        }

        User user = saved.getUser();
        String newAccessToken = jwtProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .build();
    }

    // ── 로그아웃 ──────────────────────────────────────────────────────────────

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.deleteByToken(request.getRefreshToken());
    }

    // ── 사업자번호 검증 ───────────────────────────────────────────────────────

    public BizVerifyResponse verifyBizNumber(BizVerifyRequest request) {
        String bizNumber = request.getBizNumber();

        // 1단계: 체크섬 검증 (로컬, 항상 수행)
        if (!validateBizNumberChecksum(bizNumber)) {
            return BizVerifyResponse.builder()
                    .bizNumber(bizNumber)
                    .valid(false)
                    .build();
        }

        // 2단계: 국세청 API로 실제 사업자 상태 확인
        // API 키 미설정 시 체크섬 통과를 valid로 간주 (개발 환경 폴백)
        return ntsApiService.queryStatus(bizNumber)
                .map(status -> BizVerifyResponse.builder()
                        .bizNumber(bizNumber)
                        .valid(status.isActive())
                        .status(status.statusName())
                        .build())
                .orElseGet(() -> BizVerifyResponse.builder()
                        .bizNumber(bizNumber)
                        .valid(true)
                        .status("확인불가 (API 키 미설정)")
                        .build());
    }

    // ── Google OAuth2 ─────────────────────────────────────────────────────────

    @Transactional
    public TokenResponse googleOAuth2(OAuth2GoogleRequest request) {
        Map<String, Object> googlePayload = googleTokenVerifier.verify(request.getIdToken());

        String email = (String) googlePayload.get("email");
        String name = googlePayload.getOrDefault("name", email).toString();

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .email(email)
                                .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                                .name(name)
                                .role(User.Role.OWNER)
                                .build()
                ));

        return issueTokenPair(user);
    }

    // ── 공통 토큰 발급 ────────────────────────────────────────────────────────

    private TokenResponse issueTokenPair(User user) {
        String accessToken = jwtProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId());

        // 기존 토큰 삭제 후 새로 저장 (1 유저 1 refresh token 정책)
        refreshTokenRepository.deleteByUser(user);
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .user(user)
                        .token(refreshToken)
                        .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                        .build()
        );

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // ── 사업자번호 체크섬 검증 ────────────────────────────────────────────────

    /**
     * 국세청 사업자등록번호 체크섬 알고리즘 (10자리 숫자 검증).
     * 가중치: 1,3,7,1,3,7,1,3,5 → 합산 후 마지막 자리와 비교.
     */
    private boolean validateBizNumberChecksum(String bizNumber) {
        if (bizNumber == null || !bizNumber.matches("^\\d{10}$")) {
            return false;
        }
        int[] weights = {1, 3, 7, 1, 3, 7, 1, 3, 5};
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (bizNumber.charAt(i) - '0') * weights[i];
        }
        sum += (int) Math.floor(((bizNumber.charAt(8) - '0') * 5) / 10.0);
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == (bizNumber.charAt(9) - '0');
    }

}
