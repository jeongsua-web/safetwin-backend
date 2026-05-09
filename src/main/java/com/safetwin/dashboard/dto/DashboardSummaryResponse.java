package com.safetwin.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class DashboardSummaryResponse {

    private double safetyScore;
    private double safetyScoreDelta;
    private long weeklyRiskCount;
    private long weeklyRiskDelta;
    private long unresolvedCount;
    private double educationRate;
    private String safetyGrade;
    private String tbmGuide;
    private List<RecentAnalysisSummary> recentAnalyses;

    @Getter
    @Builder
    public static class RecentAnalysisSummary {
        private Long id;
        private String imageUrl;
        private String location;
        private String status;
        private Integer overallScore;
        private String riskLevel;
        private LocalDateTime analyzedAt;
    }
}
