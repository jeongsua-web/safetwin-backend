package com.safetwin.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

/**
 * 국세청 사업자등록정보 상태 조회 API 연동.
 * 공공데이터포털(data.go.kr) → "국세청_사업자등록정보 서비스" 서비스키 필요.
 * API 키가 없으면 호출 없이 빈 Optional을 반환 → 체크섬 검증만으로 폴백.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NtsApiService {

    @Value("${nts.api-key:}")
    private String apiKey;

    @Value("${nts.api-url}")
    private String apiUrl;

    private final RestClient restClient = RestClient.create();

    /**
     * 사업자번호 상태를 조회한다.
     * @return 조회 결과. API 키 미설정 또는 오류 시 empty.
     */
    public Optional<BizStatus> queryStatus(String bizNumber) {
        if (apiKey == null || apiKey.isBlank()) {
            log.debug("NTS_API_KEY 미설정 — 체크섬 검증만 수행");
            return Optional.empty();
        }

        try {
            NtsResponse response = restClient.post()
                    .uri(apiUrl + "?serviceKey=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new NtsRequest(List.of(bizNumber)))
                    .retrieve()
                    .body(NtsResponse.class);

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                return Optional.empty();
            }

            NtsResponse.DataItem item = response.getData().get(0);
            return Optional.of(new BizStatus(item.getBStt(), item.getBSttCd()));

        } catch (Exception e) {
            log.warn("국세청 API 호출 실패 (bizNumber={}): {}", bizNumber, e.getMessage());
            return Optional.empty();
        }
    }

    // ── 결과 DTO ──────────────────────────────────────────────

    public record BizStatus(String statusName, String statusCode) {
        /** 계속사업자(01)만 유효로 판단 */
        public boolean isActive() {
            return "01".equals(statusCode);
        }
    }

    // ── 국세청 API 내부 요청/응답 ─────────────────────────────

    @Getter
    private static class NtsRequest {
        @JsonProperty("b_no")
        private final List<String> bNo;

        NtsRequest(List<String> bNo) {
            this.bNo = bNo;
        }
    }

    @Getter
    private static class NtsResponse {
        @JsonProperty("status_code")
        private String statusCode;

        @JsonProperty("data")
        private List<DataItem> data;

        @Getter
        static class DataItem {
            @JsonProperty("b_no")
            private String bNo;

            @JsonProperty("b_stt")
            private String bStt;       // 계속사업자 / 휴업자 / 폐업자

            @JsonProperty("b_stt_cd")
            private String bSttCd;     // 01=계속, 02=휴업, 03=폐업

            @JsonProperty("tax_type")
            private String taxType;
        }
    }
}
