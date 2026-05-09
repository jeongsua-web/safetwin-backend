package com.safetwin.analysis.dto;

import com.safetwin.entity.Analysis;
import com.safetwin.entity.Risk;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AnalysisResponse {

    private Long id;
    private String imageUrl;
    private String location;
    private String status;
    private LocalDateTime analyzedAt;
    private Integer overallScore;
    private String riskLevel;
    private List<RiskResponse> risks;

    public static AnalysisResponse from(Analysis analysis, List<Risk> risks) {
        List<RiskResponse> riskResponses = risks.stream()
                .map(RiskResponse::from)
                .toList();

        LocalDateTime analyzedAt = analysis.getStatus() == Analysis.Status.COMPLETED
                ? analysis.getUpdatedAt()
                : null;

        String location = analysis.getZone().getSite().getName()
                + " > " + analysis.getZone().getName();

        return AnalysisResponse.builder()
                .id(analysis.getId())
                .imageUrl(analysis.getImageUrl())
                .location(location)
                .status(analysis.getStatus().name())
                .analyzedAt(analyzedAt)
                .overallScore(analysis.getOverallScore())
                .riskLevel(resolveRiskLevel(analysis.getOverallScore()))
                .risks(riskResponses)
                .build();
    }

    /** 분석 생성 직후 (PENDING, risks 없음) 용 */
    public static AnalysisResponse pending(Analysis analysis) {
        String location = analysis.getZone().getSite().getName()
                + " > " + analysis.getZone().getName();

        return AnalysisResponse.builder()
                .id(analysis.getId())
                .imageUrl(analysis.getImageUrl())
                .location(location)
                .status(analysis.getStatus().name())
                .analyzedAt(null)
                .overallScore(null)
                .riskLevel(null)
                .risks(List.of())
                .build();
    }

    private static String resolveRiskLevel(Integer score) {
        if (score == null) return null;
        if (score >= 80) return "LOW";
        if (score >= 50) return "MEDIUM";
        return "HIGH";
    }
}
