package com.safetwin.publicdata.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.safetwin.publicdata.dto.LawResponse.LawItem;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LawGoKrService {

    private final RestClient restClient;

    @Value("${law.api-key:}")
    private String apiKey;

    @Value("${law.api-url}")
    private String apiUrl;

    public LawGoKrService(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public List<LawItem> searchLaws(String keyword) {
        if (apiKey == null || apiKey.isBlank()) {
            return Collections.emptyList();
        }
        try {
            String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = apiUrl + "?OC=" + apiKey
                    + "&target=efArticle&type=JSON&query=" + encoded + "&display=5";

            LawSearchResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(LawSearchResponse.class);

            if (response == null || response.getArticles() == null) {
                return Collections.emptyList();
            }

            return response.getArticles().stream()
                    .map(this::toItem)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private LawItem toItem(ArticleDto dto) {
        String article = "제" + dto.getArticleNumber() + "조";
        if (dto.getArticleSubNumber() != null && !dto.getArticleSubNumber().isBlank()
                && !"0".equals(dto.getArticleSubNumber().trim())) {
            article += "의" + dto.getArticleSubNumber().trim();
        }
        if (dto.getArticleTitle() != null && !dto.getArticleTitle().isBlank()) {
            article += " (" + dto.getArticleTitle().trim() + ")";
        }

        String content = dto.getArticleContent() != null
                ? dto.getArticleContent().replaceAll("<[^>]+>", "").trim()
                : "";

        return LawItem.builder()
                .title(dto.getLawName() != null ? dto.getLawName().trim() : "")
                .article(article)
                .content(content)
                .category("")
                .build();
    }

    // ── Response DTOs ─────────────────────────────────────────────────────────

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class LawSearchResponse {
        @JsonProperty("efArticle")
        private List<ArticleDto> articles;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ArticleDto {
        @JsonProperty("법령명한글")
        private String lawName;

        @JsonProperty("조문번호")
        private String articleNumber;

        @JsonProperty("조문가지번호")
        private String articleSubNumber;

        @JsonProperty("조문제목")
        private String articleTitle;

        @JsonProperty("조문내용")
        private String articleContent;
    }
}
