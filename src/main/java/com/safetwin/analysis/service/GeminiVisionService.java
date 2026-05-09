package com.safetwin.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiVisionService {

    private static final String LAYOUT_PROMPT = """
            이 사진에서 공간을 구역으로 분류하고 각 구역의 정보를 JSON 배열로만 반환해 (마크다운 없이 순수 JSON만):
            [
              {
                "label": "구역 이름 (간결하게, 예: 작업구역 A, 자재 적치 구역)",
                "x": 구역 왼쪽 상단의 가로 위치 (0~100 실수),
                "y": 구역 왼쪽 상단의 세로 위치 (0~100 실수),
                "w": 구역의 가로 크기 (0~100 실수),
                "h": 구역의 세로 크기 (0~100 실수)
              }
            ]
            구역이 없으면 빈 배열 []을 반환해.
            """;

    private static final String SAFETY_PROMPT = """
            이 사진에서 산업안전보건법 기준으로 위험 요소를 탐지해줘.
            각 위험 요소마다 다음 필드를 포함한 JSON 배열만 반환해 (마크다운 없이 순수 JSON만):
            [
              {
                "label": "위험 요소 이름 (간결하게)",
                "detail": "상세 설명",
                "level": "HIGH | MEDIUM | LOW",
                "law": "관련 산업안전보건법 조항",
                "action": "즉시 취해야 할 조치사항",
                "x": 위험 요소 중심의 이미지 내 가로 위치 (0~100 실수),
                "y": 위험 요소 중심의 이미지 내 세로 위치 (0~100 실수)
              }
            ]
            위험 요소가 없으면 빈 배열 []을 반환해.
            """;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.max-tokens}")
    private int maxTokens;

    private final ObjectMapper objectMapper;
    private RestClient restClient;

    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public List<GeminiRiskItem> analyze(String imageUrl) {
        return callWithPrompt(imageUrl, SAFETY_PROMPT, new TypeReference<>() {});
    }

    public List<GeminiLayoutItem> extractLayout(String imageUrl) {
        try {
            return callWithPrompt(imageUrl, LAYOUT_PROMPT, new TypeReference<>() {});
        } catch (SafeTwinException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Gemini layout API call failed: {}", e.getMessage());
            throw new SafeTwinException(ErrorCode.ANALYSIS_FAILED);
        }
    }

    private <T> List<T> callWithPrompt(String imageUrl, String prompt, TypeReference<List<T>> typeRef) {
        String base64Image = fetchImageAsBase64(imageUrl);
        Map<String, Object> requestBody = buildRequestBody(base64Image, prompt);
        String uri = "/v1beta/models/" + model + ":generateContent?key=" + apiKey;
        try {
            String responseBody = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            return parseJsonResponse(responseBody, typeRef);
        } catch (RestClientException e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new SafeTwinException(ErrorCode.ANALYSIS_FAILED);
        }
    }

    private String fetchImageAsBase64(String imageUrl) {
        try (InputStream is = URI.create(imageUrl).toURL().openStream()) {
            return Base64.getEncoder().encodeToString(is.readAllBytes());
        } catch (IOException e) {
            log.error("Failed to fetch image: {}", imageUrl, e);
            throw new SafeTwinException(ErrorCode.ANALYSIS_FAILED);
        }
    }

    private Map<String, Object> buildRequestBody(String base64Image, String prompt) {
        return Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of(
                                                "inlineData", Map.of(
                                                        "mimeType", "image/jpeg",
                                                        "data", base64Image
                                                )
                                        ),
                                        Map.of("text", prompt)
                                )
                        )
                ),
                "generationConfig", Map.of("maxOutputTokens", maxTokens)
        );
    }

    private <T> List<T> parseJsonResponse(String responseBody, TypeReference<List<T>> typeRef) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
            return objectMapper.readValue(stripMarkdown(text), typeRef);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse Gemini response: {}", e.getMessage());
            throw new SafeTwinException(ErrorCode.ANALYSIS_FAILED);
        }
    }

    private String stripMarkdown(String text) {
        String stripped = text.trim();
        if (stripped.startsWith("```")) {
            int start = stripped.indexOf('\n');
            int end = stripped.lastIndexOf("```");
            if (start != -1 && end > start) {
                stripped = stripped.substring(start + 1, end).trim();
            }
        }
        return stripped;
    }

    public record GeminiRiskItem(
            String label,
            String detail,
            String level,
            String law,
            String action,
            Double x,
            Double y
    ) {}

    public record GeminiLayoutItem(
            String label,
            Double x,
            Double y,
            Double w,
            Double h
    ) {}
}
