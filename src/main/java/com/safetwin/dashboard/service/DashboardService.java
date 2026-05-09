package com.safetwin.dashboard.service;

import com.safetwin.dashboard.dto.DashboardSummaryResponse;
import com.safetwin.entity.Analysis;
import com.safetwin.repository.AnalysisRepository;
import com.safetwin.repository.DocumentRepository;
import com.safetwin.repository.RiskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import static com.safetwin.entity.Document.DocStatus.SIGNED;
import static com.safetwin.entity.Document.DocumentType.EDUCATION_CERT;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final AnalysisRepository analysisRepository;
    private final RiskRepository riskRepository;
    private final DocumentRepository documentRepository;

    public DashboardSummaryResponse getSummary(Long managerId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thisWeekStart = now.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime lastWeekStart = thisWeekStart.minusWeeks(1);

        // 안전 점수
        Double thisWeekScore = analysisRepository.avgScoreBetween(managerId, thisWeekStart, now);
        Double lastWeekScore = analysisRepository.avgScoreBetween(managerId, lastWeekStart, thisWeekStart);
        double safetyScore = Objects.requireNonNullElse(thisWeekScore, 0.0);
        double lastScore = Objects.requireNonNullElse(lastWeekScore, 0.0);

        // 위험 요소 수
        long thisWeekRiskCount = riskRepository.countByManagerIdBetween(managerId, thisWeekStart, now);
        long lastWeekRiskCount = riskRepository.countByManagerIdBetween(managerId, lastWeekStart, thisWeekStart);

        // 미조치 위험
        long unresolvedCount = riskRepository.countOpenByManagerId(managerId);

        // 교육 이수율
        long totalEdu = documentRepository.countByManagerIdAndType(managerId, EDUCATION_CERT);
        long signedEdu = documentRepository.countByManagerIdAndTypeAndStatus(managerId, EDUCATION_CERT, SIGNED);
        double educationRate = totalEdu > 0 ? (double) signedEdu / totalEdu * 100 : 0.0;

        // TBM 가이드
        List<Object[]> topLabels = riskRepository.findTopRiskLabelsSince(
                managerId, now.minusWeeks(1), PageRequest.of(0, 3));
        String tbmGuide = buildTbmGuide(topLabels);

        // 최근 5개 분석
        List<Analysis> recent = analysisRepository.findRecentCompleted(
                managerId, PageRequest.of(0, 5));
        List<DashboardSummaryResponse.RecentAnalysisSummary> recentSummaries = recent.stream()
                .map(this::toRecentSummary)
                .toList();

        return DashboardSummaryResponse.builder()
                .safetyScore(round(safetyScore))
                .safetyScoreDelta(round(safetyScore - lastScore))
                .weeklyRiskCount(thisWeekRiskCount)
                .weeklyRiskDelta(thisWeekRiskCount - lastWeekRiskCount)
                .unresolvedCount(unresolvedCount)
                .educationRate(round(educationRate))
                .safetyGrade(calcGrade(safetyScore))
                .tbmGuide(tbmGuide)
                .recentAnalyses(recentSummaries)
                .build();
    }

    private DashboardSummaryResponse.RecentAnalysisSummary toRecentSummary(Analysis a) {
        String location = a.getZone().getSite().getName() + " > " + a.getZone().getName();
        return DashboardSummaryResponse.RecentAnalysisSummary.builder()
                .id(a.getId())
                .imageUrl(a.getImageUrl())
                .location(location)
                .status(a.getStatus().name())
                .overallScore(a.getOverallScore())
                .riskLevel(resolveRiskLevel(a.getOverallScore()))
                .analyzedAt(a.getUpdatedAt())
                .build();
    }

    private String buildTbmGuide(List<Object[]> topLabels) {
        if (topLabels.isEmpty()) {
            return "오늘도 안전한 현장을 위해 기본 안전수칙을 준수해 주세요.";
        }
        List<String> labels = topLabels.stream()
                .map(row -> (String) row[0])
                .toList();
        String topics = String.join(", ", labels);
        return "오늘의 중점 안전 사항: " + topics + " 관련 위험을 주의하고, 보호구 착용 및 안전 절차를 반드시 준수하세요.";
    }

    private String calcGrade(double score) {
        if (score >= 95) return "A+";
        if (score >= 90) return "A";
        if (score >= 85) return "B+";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }

    private String resolveRiskLevel(Integer score) {
        if (score == null) return null;
        if (score >= 80) return "LOW";
        if (score >= 50) return "MEDIUM";
        return "HIGH";
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
