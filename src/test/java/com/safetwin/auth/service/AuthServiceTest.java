package com.safetwin.auth.service;

import com.safetwin.auth.dto.*;
import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import com.safetwin.entity.RefreshToken;
import com.safetwin.entity.User;
import com.safetwin.repository.RefreshTokenRepository;
import com.safetwin.repository.UserRepository;
import com.safetwin.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtProvider jwtProvider;
    @Mock PasswordEncoder passwordEncoder;
    @Mock GoogleTokenVerifier googleTokenVerifier;
    @Mock NtsApiService ntsApiService;

    @InjectMocks AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);
    }

    // ── signup ────────────────────────────────────────────────

    @Test
    @DisplayName("회원가입 성공 - 유저 저장")
    void signup_success_savesUser() {
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);

        authService.signup(signupRequest("new@test.com", "pass1234!", "홍길동", null));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_duplicateEmail_throws() {
        given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

        assertThatThrownBy(() ->
                authService.signup(signupRequest("dup@test.com", "pass1234!", "홍길동", null)))
                .isInstanceOf(SafeTwinException.class)
                .satisfies(e -> assertThat(((SafeTwinException) e).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_EMAIL));
    }

    @Test
    @DisplayName("회원가입 실패 - 잘못된 사업자번호 체크섬")
    void signup_invalidBizNumber_throws() {
        given(userRepository.existsByEmail(anyString())).willReturn(false);

        // "1234567890": 체크섬 결과는 1이어야 하므로 0으로 끝나는 번호는 무효
        assertThatThrownBy(() ->
                authService.signup(signupRequest("new@test.com", "pass1234!", "홍길동", "1234567890")))
                .isInstanceOf(SafeTwinException.class)
                .satisfies(e -> assertThat(((SafeTwinException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_BIZ_NUMBER));
    }

    @Test
    @DisplayName("회원가입 성공 - 유효한 사업자번호 체크섬 통과")
    void signup_validBizNumber_savesUser() {
        given(userRepository.existsByEmail(anyString())).willReturn(false);

        // "1234567891": 체크섬 검증 통과 (마지막 자리 1)
        authService.signup(signupRequest("new@test.com", "pass1234!", "홍길동", "1234567891"));

        verify(userRepository).save(any(User.class));
    }

    // ── login ─────────────────────────────────────────────────

    @Test
    @DisplayName("로그인 성공 - AccessToken + RefreshToken 반환")
    void login_success_returnsTokenPair() {
        User user = testUser(1L, "test@test.com", "encodedPw");
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pass1234!", "encodedPw")).willReturn(true);
        given(jwtProvider.generateAccessToken(any(), any(), any())).willReturn("access");
        given(jwtProvider.generateRefreshToken(any())).willReturn("refresh");

        TokenResponse response = authService.login(loginRequest("test@test.com", "pass1234!"));

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_userNotFound_throws() {
        given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest("noone@test.com", "pass1234!")))
                .isInstanceOf(SafeTwinException.class)
                .satisfies(e -> assertThat(((SafeTwinException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_wrongPassword_throws() {
        User user = testUser(1L, "test@test.com", "encodedPw");
        given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encodedPw")).willReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("test@test.com", "wrong")))
                .isInstanceOf(SafeTwinException.class)
                .satisfies(e -> assertThat(((SafeTwinException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    // ── refresh ───────────────────────────────────────────────

    @Test
    @DisplayName("토큰 재발급 성공")
    void refresh_success_returnsNewAccessToken() {
        User user = testUser(1L, "test@test.com", "encodedPw");
        RefreshToken saved = RefreshToken.builder()
                .user(user)
                .token("validRefresh")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        given(refreshTokenRepository.findByToken("validRefresh")).willReturn(Optional.of(saved));
        given(jwtProvider.validateAndGetClaims("validRefresh")).willReturn(null);
        given(jwtProvider.isRefreshToken(null)).willReturn(true);
        given(jwtProvider.generateAccessToken(any(), any(), any())).willReturn("newAccess");

        RefreshRequest request = new RefreshRequest();
        ReflectionTestUtils.setField(request, "refreshToken", "validRefresh");
        TokenResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("newAccess");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 존재하지 않는 토큰")
    void refresh_tokenNotFound_throws() {
        given(refreshTokenRepository.findByToken(anyString())).willReturn(Optional.empty());

        RefreshRequest request = new RefreshRequest();
        ReflectionTestUtils.setField(request, "refreshToken", "unknown");

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(SafeTwinException.class)
                .satisfies(e -> assertThat(((SafeTwinException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    // ── verifyBizNumber ───────────────────────────────────────

    @Test
    @DisplayName("사업자번호 검증 - 체크섬 실패 시 API 미호출하고 invalid 반환")
    void verifyBizNumber_invalidChecksum_returnsInvalidWithoutApiCall() {
        BizVerifyRequest request = new BizVerifyRequest();
        ReflectionTestUtils.setField(request, "bizNumber", "1234567890");

        BizVerifyResponse response = authService.verifyBizNumber(request);

        assertThat(response.isValid()).isFalse();
        verifyNoInteractions(ntsApiService);
    }

    @Test
    @DisplayName("사업자번호 검증 - 체크섬 통과 + API 키 없으면 valid 폴백")
    void verifyBizNumber_validChecksum_noApiKey_returnsValidFallback() {
        BizVerifyRequest request = new BizVerifyRequest();
        ReflectionTestUtils.setField(request, "bizNumber", "1234567891");
        given(ntsApiService.queryStatus("1234567891")).willReturn(Optional.empty());

        BizVerifyResponse response = authService.verifyBizNumber(request);

        assertThat(response.isValid()).isTrue();
    }

    // ── helpers ───────────────────────────────────────────────

    private SignupRequest signupRequest(String email, String password, String name, String bizNumber) {
        SignupRequest req = new SignupRequest();
        ReflectionTestUtils.setField(req, "email", email);
        ReflectionTestUtils.setField(req, "password", password);
        ReflectionTestUtils.setField(req, "name", name);
        if (bizNumber != null) {
            ReflectionTestUtils.setField(req, "bizNumber", bizNumber);
        }
        return req;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        ReflectionTestUtils.setField(req, "email", email);
        ReflectionTestUtils.setField(req, "password", password);
        return req;
    }

    private User testUser(Long id, String email, String encodedPassword) {
        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .name("테스트유저")
                .role(User.Role.OWNER)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
