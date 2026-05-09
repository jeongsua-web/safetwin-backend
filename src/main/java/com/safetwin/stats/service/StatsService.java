package com.safetwin.stats.service;

import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import com.safetwin.entity.Analysis;
import com.safetwin.entity.Risk;
import com.safetwin.repository.AnalysisRepository;
import com.safetwin.repository.RiskRepository;
import com.safetwin.stats.dto.CompareResponse;
import com.safetwin.stats.dto.ScoreTrendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private static final int TREND_TARGET_SCORE = 90;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter WEEK_FMT = DateTimeFormatter.ofPattern("yyyy-'W'ww");

    private final AnalysisRepository analysisRepository;
    private final RiskRepository riskRepository;

    // ── 점수 추이 ─────────────────────────────────────────────────────────────

    public ScoreTrendResponse getScoreTrend(Long managerId, String period) {
        boolean isMonthly = !"weekly".equalsIgnoreCase(period);
        LocalDateTime since = isMonthly
                ? LocalDateTime.now().minusMonths(12)
                : LocalDateTime.now().minusWeeks(12);

        List<Object[]> rawData = analysisRepository.findScoreDataSince(managerId, since);

        // Group by period label → average score
        Map<String, List<Integer>> grouped = new LinkedHashMap<>();
        for (Object[] row : rawData) {
            LocalDateTime createdAt = (LocalDateTime) row[0];
            Integer score = (Integer) row[1];
            if (score == null) continue;

            String label = isMonthly
                    ? YearMonth.from(createdAt).format(MONTH_FMT)
                    : "%d-W%02d".formatted(
                    createdAt.getYear(),
                    createdAt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));

            grouped.computeIfAbsent(label, k -> new ArrayList<>()).add(score);
        }

        // Fill in missing periods with null
        List<String> labels = buildPeriodLabels(isMonthly);
        List<Integer> scores = labels.stream()
                .map(label -> {
                    List<Integer> vals = grouped.get(label);
                    if (vals == null || vals.isEmpty()) return null;
                    return (int) vals.stream().mapToInt(Integer::intValue).average().orElse(0);
                })
                .collect(Collectors.toList());

        int current = scores.stream()
                .filter(Objects::nonNull)
                .reduce((a, b) -> b)
                .orElse(0);

        int min = scores.stream()
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .min()
                .orElse(0);

        return ScoreTrendResponse.builder()
                .labels(labels)
                .scores(scores)
                .target(TREND_TARGET_SCORE)
                .min(min)
                .current(current)
                .build();
    }

    // ── 두 분석 비교 ──────────────────────────────────────────────────────────

    public CompareResponse compare(Long beforeId, Long afterId, Long managerId) {
        Analysis before = analysisRepository.findByIdAndManagerId(beforeId, managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.NOT_FOUND));
        Analysis after = analysisRepository.findByIdAndManagerId(afterId, managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.NOT_FOUND));

        List<Risk> beforeRisks = riskRepository.findByAnalysisId(beforeId);
        List<Risk> afterRisks = riskRepository.findByAnalysisId(afterId);

        Set<String> beforeLabels = beforeRisks.stream()
                .map(Risk::getLabel).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> afterLabels = afterRisks.stream()
                .map(Risk::getLabel).filter(Objects::nonNull).collect(Collectors.toSet());

        List<String> improved = beforeLabels.stream()
                .filter(l -> !afterLabels.contains(l))
                .sorted().toList();

        List<String> newItems = afterLabels.stream()
                .filter(l -> !beforeLabels.contains(l))
                .sorted().toList();

        List<String> persisted = beforeLabels.stream()
                .filter(afterLabels::contains)
                .sorted().toList();

        int beforeScore = Objects.requireNonNullElse(before.getOverallScore(), 0);
        int afterScore = Objects.requireNonNullElse(after.getOverallScore(), 0);

        return CompareResponse.builder()
                .beforeId(beforeId)
                .afterId(afterId)
                .beforeScore(beforeScore)
                .afterScore(afterScore)
                .scoreDelta(afterScore - beforeScore)
                .beforeRiskCount(beforeRisks.size())
                .afterRiskCount(afterRisks.size())
                .riskDelta(afterRisks.size() - beforeRisks.size())
                .improvedItems(improved)
                .newItems(newItems)
                .persistedItems(persisted)
                .build();
    }

    // ── 기간 레이블 생성 ──────────────────────────────────────────────────────

    private List<String> buildPeriodLabels(boolean isMonthly) {
        List<String> labels = new ArrayList<>();
        if (isMonthly) {
            YearMonth cursor = YearMonth.now().minusMonths(11);
            for (int i = 0; i < 12; i++) {
                labels.add(cursor.plusMonths(i).format(MONTH_FMT));
            }
        } else {
            LocalDateTime cursor = LocalDateTime.now().minusWeeks(11);
            for (int i = 0; i < 12; i++) {
                LocalDateTime w = cursor.plusWeeks(i);
                labels.add("%d-W%02d".formatted(
                        w.getYear(), w.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)));
            }
        }
        return labels;
    }
}
