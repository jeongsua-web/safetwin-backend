package com.safetwin.analysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safetwin.entity.Analysis;
import com.safetwin.entity.Risk;
import com.safetwin.repository.AnalysisRepository;
import com.safetwin.repository.RiskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisAsyncProcessor {

    private final AnalysisRepository analysisRepository;
    private final RiskRepository riskRepository;
    private final GeminiVisionService geminiVisionService;
    private final ObjectMapper objectMapper;

    @Async("analysisExecutor")
    @Transactional
    public void process(Long analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new IllegalStateException("Analysis not found: " + analysisId));

        analysis.startProcessing();
        analysisRepository.save(analysis);

        try {
            List<GeminiVisionService.GeminiRiskItem> items =
                    geminiVisionService.analyze(analysis.getImageUrl());

            List<Risk> risks = items.stream()
                    .map(item -> toRiskEntity(item, analysis))
                    .toList();

            riskRepository.saveAll(risks);

            int score = calculateScore(risks);
            analysis.complete(serializeItems(items), score);
            analysisRepository.save(analysis);

            log.info("Analysis {} completed: score={}, risks={}", analysisId, score, risks.size());

        } catch (Exception e) {
            log.error("Analysis {} failed", analysisId, e);
            analysis.fail();
            analysisRepository.save(analysis);
        }
    }

    private Risk toRiskEntity(GeminiVisionService.GeminiRiskItem item, Analysis analysis) {
        Risk.Level level;
        try {
            level = Risk.Level.valueOf(item.level().toUpperCase());
        } catch (IllegalArgumentException e) {
            level = Risk.Level.LOW;
        }

        return Risk.builder()
                .analysis(analysis)
                .label(item.label())
                .description(item.detail())
                .law(item.law())
                .action(item.action())
                .level(level)
                .x(item.x())
                .y(item.y())
                .location("%.1f%%, %.1f%%".formatted(
                        item.x() != null ? item.x() : 0.0,
                        item.y() != null ? item.y() : 0.0))
                .status(Risk.Status.OPEN)
                .build();
    }

    /** 100점 기준. HIGH/CRITICAL -15점, MEDIUM -8점, LOW -3점, 최소 0점. */
    int calculateScore(List<Risk> risks) {
        int score = 100;
        for (Risk risk : risks) {
            score -= switch (risk.getLevel()) {
                case HIGH, CRITICAL -> 15;
                case MEDIUM -> 8;
                case LOW -> 3;
            };
        }
        return Math.max(0, score);
    }

    private String serializeItems(List<GeminiVisionService.GeminiRiskItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
