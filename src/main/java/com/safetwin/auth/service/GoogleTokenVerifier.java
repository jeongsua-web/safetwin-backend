package com.safetwin.auth.service;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class GoogleTokenVerifier {

    private static final String GOOGLE_JWK_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> VALID_ISSUERS = Set.of(
            "https://accounts.google.com", "accounts.google.com"
    );
    private static final Duration JWK_CACHE_TTL = Duration.ofHours(5);

    @Value("${google.client-id}")
    private String clientId;

    private final RestClient restClient = RestClient.create();

    private volatile JWKSet cachedJwkSet;
    private volatile Instant jwkFetchedAt;

    /**
     * Google ID Token을 검증하고 payload(claims)를 반환한다.
     * - RS256 서명 검증 (Google 공개키 JWK Set 사용)
     * - iss, aud, exp 클레임 검증
     */
    public Map<String, Object> verify(String idToken) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(idToken);

            // kid로 공개키 찾기
            String kid = signedJWT.getHeader().getKeyID();
            JWK jwk = getJwkSet().getKeyByKeyId(kid);
            if (jwk == null) {
                // 키가 없으면 캐시 무효화 후 한 번 더 시도 (Google이 키를 교체했을 때)
                invalidateCache();
                jwk = getJwkSet().getKeyByKeyId(kid);
            }
            if (jwk == null) {
                throw new SafeTwinException(ErrorCode.INVALID_TOKEN);
            }

            // RS256 서명 검증
            RSAKey rsaKey = (RSAKey) jwk;
            JWSVerifier verifier = new RSASSAVerifier(rsaKey.toRSAPublicKey());
            if (!signedJWT.verify(verifier)) {
                throw new SafeTwinException(ErrorCode.INVALID_TOKEN);
            }

            // 클레임 검증
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            validateClaims(claims);

            return claims.toJSONObject();

        } catch (SafeTwinException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Google ID Token 검증 실패: {}", e.getMessage());
            throw new SafeTwinException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validateClaims(JWTClaimsSet claims) {
        if (!VALID_ISSUERS.contains(claims.getIssuer())) {
            throw new SafeTwinException(ErrorCode.INVALID_TOKEN);
        }
        // clientId가 설정된 경우에만 aud 검증
        if (clientId != null && !clientId.isBlank()
                && !claims.getAudience().contains(clientId)) {
            throw new SafeTwinException(ErrorCode.INVALID_TOKEN);
        }
        if (claims.getExpirationTime() == null
                || new Date().after(claims.getExpirationTime())) {
            throw new SafeTwinException(ErrorCode.EXPIRED_TOKEN);
        }
    }

    private synchronized JWKSet getJwkSet() {
        if (cachedJwkSet == null || isCacheExpired()) {
            fetchAndCacheJwkSet();
        }
        return cachedJwkSet;
    }

    private synchronized void invalidateCache() {
        jwkFetchedAt = null;
        fetchAndCacheJwkSet();
    }

    private void fetchAndCacheJwkSet() {
        try {
            String json = restClient.get()
                    .uri(GOOGLE_JWK_URL)
                    .retrieve()
                    .body(String.class);
            cachedJwkSet = JWKSet.parse(json);
            jwkFetchedAt = Instant.now();
            log.debug("Google JWK Set 갱신 완료 (키 수: {})", cachedJwkSet.getKeys().size());
        } catch (Exception e) {
            log.error("Google JWK Set 조회 실패", e);
            throw new SafeTwinException(ErrorCode.INVALID_TOKEN);
        }
    }

    private boolean isCacheExpired() {
        return jwkFetchedAt == null
                || Instant.now().isAfter(jwkFetchedAt.plus(JWK_CACHE_TTL));
    }
}
