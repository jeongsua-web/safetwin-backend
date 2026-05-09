package com.safetwin.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safetwin.entity.Analysis;
import com.safetwin.entity.Risk;
import com.safetwin.repository.AnalysisRepository;
import com.safetwin.repository.RiskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalysisAsyncProcessorTest {

    @Mock AnalysisRepository analysisRepository;
    @Mock RiskRepository riskRepository;
    @Mock GeminiVisionService geminiVisionService;
    @Mock ObjectMapper objectMapper;

    @InjectMocks AnalysisAsyncProcessor processor;

    // ── calculateScore ────────────────────────────────────────

    @Test
    @DisplayName("위험 없으면 100점")
    void calculateScore_noRisks_returns100() {
        assertThat(processor.calculateScore(List.of())).isEqualTo(100);
    }

    @Test
    @DisplayName("HIGH 위험 1개 → 85점")
    void calculateScore_oneHigh_returns85() {
        assertThat(processor.calculateScore(List.of(risk(Risk.Level.HIGH)))).isEqualTo(85);
    }

    @Test
    @DisplayName("CRITICAL 위험 1개 → HIGH와 동일하게 85점")
    void calculateScore_oneCritical_returns85() {
        assertThat(processor.calculateScore(List.of(risk(Risk.Level.CRITICAL)))).isEqualTo(85);
    }

    @Test
    @DisplayName("MEDIUM 위험 1개 → 92점")
    void calculateScore_oneMedium_returns92() {
        assertThat(processor.calculateScore(List.of(risk(Risk.Level.MEDIUM)))).isEqualTo(92);
    }

    @Test
    @DisplayName("LOW 위험 1개 → 97점")
    void calculateScore_oneLow_returns97() {
        assertThat(processor.calculateScore(List.of(risk(Risk.Level.LOW)))).isEqualTo(97);
    }

    @Test
    @DisplayName("HIGH + MEDIUM + LOW 혼합 → 74점")
    void calculateScore_mixedRisks_correctDeduction() {
        List<Risk> risks = List.of(
                risk(Risk.Level.HIGH),   // -15
                risk(Risk.Level.MEDIUM), // -8
                risk(Risk.Level.LOW)     // -3
        );
        assertThat(processor.calculateScore(risks)).isEqualTo(74);
    }

    @Test
    @DisplayName("점수가 0 미만이 되면 0으로 클램프")
    void calculateScore_overflow_clampsAtZero() {
        // HIGH 7개 = -105점 → 100 - 105 = -5 → 0
        List<Risk> risks = Collections.nCopies(7, risk(Risk.Level.HIGH));
        assertThat(processor.calculateScore(risks)).isEqualTo(0);
    }

    // ── process (상태 전이) ───────────────────────────────────

    @Test
    @DisplayName("분석 처리 성공 - PENDING → COMPLETED 상태 전이")
    void process_success_completesAnalysis() throws Exception {
        Analysis analysis = Analysis.builder()
                .zone(null)
                .requester(null)
                .imageUrl("https://s3.example.com/img.jpg")
                .status(Analysis.Status.PENDING)
                .build();
        ReflectionTestUtils.setField(analysis, "id", 1L);

        given(analysisRepository.findById(1L)).willReturn(java.util.Optional.of(analysis));
        given(geminiVisionService.analyze("https://s3.example.com/img.jpg"))
                .willReturn(List.of());
        given(objectMapper.writeValueAsString(List.of())).willReturn("[]");

        processor.process(1L);

        assertThat(analysis.getStatus()).isEqualTo(Analysis.Status.COMPLETED);
        assertThat(analysis.getOverallScore()).isEqualTo(100);
        verify(analysisRepository, org.mockito.Mockito.times(2)).save(analysis);
    }

    @Test
    @DisplayName("Claude API 오류 - FAILED 상태 전이")
    void process_claudeError_failsAnalysis() {
        Analysis analysis = Analysis.builder()
                .zone(null)
                .requester(null)
                .imageUrl("https://s3.example.com/img.jpg")
                .status(Analysis.Status.PENDING)
                .build();
        ReflectionTestUtils.setField(analysis, "id", 2L);

        given(analysisRepository.findById(2L)).willReturn(java.util.Optional.of(analysis));
        given(geminiVisionService.analyze(anyString()))
                .willThrow(new RuntimeException("Gemini API 오류"));

        processor.process(2L);

        assertThat(analysis.getStatus()).isEqualTo(Analysis.Status.FAILED);
    }

    // ── helper ────────────────────────────────────────────────

    private Risk risk(Risk.Level level) {
        return Risk.builder()
                .analysis(null)
                .label("테스트위험")
                .description("설명")
                .level(level)
                .status(Risk.Status.OPEN)
                .build();
    }
}
