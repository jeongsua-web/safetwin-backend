package com.safetwin.topview.service;

import com.safetwin.analysis.service.GeminiVisionService;
import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import com.safetwin.entity.Analysis;
import com.safetwin.entity.Risk;
import com.safetwin.entity.Site;
import com.safetwin.entity.Zone;
import com.safetwin.repository.AnalysisRepository;
import com.safetwin.repository.RiskRepository;
import com.safetwin.repository.SiteRepository;
import com.safetwin.repository.ZoneRepository;
import com.safetwin.topview.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopviewService {

    private final ZoneRepository zoneRepository;
    private final SiteRepository siteRepository;
    private final RiskRepository riskRepository;
    private final AnalysisRepository analysisRepository;
    private final GeminiVisionService geminiVisionService;

    // ── 구역 목록 ─────────────────────────────────────────────────────────────

    public List<ZoneListResponse> listZones(Long siteId, Long managerId) {
        List<Zone> zones = zoneRepository.findBySiteIdAndManagerId(siteId, managerId);
        return zones.stream()
                .map(zone -> {
                    long count = riskRepository.countOpenRisksByZoneId(zone.getId());
                    List<Risk> risks = riskRepository.findOpenRisksByZoneId(zone.getId());
                    String level = resolveRiskLevel(risks);
                    return ZoneListResponse.of(zone, count, level);
                })
                .toList();
    }

    // ── 구역 상세 ─────────────────────────────────────────────────────────────

    public ZoneDetailResponse getZone(Long zoneId, Long managerId) {
        Zone zone = findWithAccessCheck(zoneId, managerId);
        List<Risk> risks = riskRepository.findOpenRisksByZoneId(zoneId);
        return ZoneDetailResponse.of(zone, risks);
    }

    // ── 구역 등록 ─────────────────────────────────────────────────────────────

    @Transactional
    public ZoneDetailResponse createZone(ZoneCreateRequest request, Long managerId) {
        Site site = siteRepository.findByIdAndManagerId(request.getSiteId(), managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.NOT_FOUND));

        Zone zone = Zone.builder()
                .site(site)
                .name(request.getName())
                .description(request.getDescription())
                .floorNumber(request.getFloorNumber())
                .x(request.getX())
                .y(request.getY())
                .w(request.getW())
                .h(request.getH())
                .area(request.getArea())
                .build();

        return ZoneDetailResponse.of(zoneRepository.save(zone), List.of());
    }

    // ── 구역 수정 ─────────────────────────────────────────────────────────────

    @Transactional
    public ZoneDetailResponse updateZone(Long zoneId, ZoneUpdateRequest request, Long managerId) {
        Zone zone = findWithAccessCheck(zoneId, managerId);
        zone.update(request.getName(), request.getDescription(), request.getFloorNumber(),
                request.getX(), request.getY(), request.getW(), request.getH(), request.getArea());
        List<Risk> risks = riskRepository.findOpenRisksByZoneId(zoneId);
        return ZoneDetailResponse.of(zoneRepository.save(zone), risks);
    }

    // ── 탑뷰 자동 생성 ────────────────────────────────────────────────────────

    @Transactional
    public List<ZoneDetailResponse> generateTopview(TopviewGenerateRequest request, Long managerId) {
        Analysis analysis = analysisRepository.findByIdAndManagerId(request.getAnalysisId(), managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.NOT_FOUND));

        if (analysis.getStatus() != Analysis.Status.COMPLETED) {
            throw new SafeTwinException(ErrorCode.ANALYSIS_FAILED);
        }

        Site site = analysis.getZone().getSite();
        List<GeminiVisionService.GeminiLayoutItem> layouts =
                geminiVisionService.extractLayout(analysis.getImageUrl());

        List<Zone> savedZones = layouts.stream()
                .map(item -> Zone.builder()
                        .site(site)
                        .name(item.label())
                        .x(item.x())
                        .y(item.y())
                        .w(item.w())
                        .h(item.h())
                        .build())
                .map(zoneRepository::save)
                .toList();

        return savedZones.stream()
                .map(z -> ZoneDetailResponse.of(z, List.of()))
                .toList();
    }

    // ── 공통 ─────────────────────────────────────────────────────────────────

    private Zone findWithAccessCheck(Long zoneId, Long managerId) {
        return zoneRepository.findByIdAndManagerId(zoneId, managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.NOT_FOUND));
    }

    private String resolveRiskLevel(List<Risk> risks) {
        return risks.stream()
                .map(Risk::getLevel)
                .max(Comparator.comparingInt(level -> switch (level) {
                    case CRITICAL -> 3;
                    case HIGH -> 2;
                    case MEDIUM -> 1;
                    case LOW -> 0;
                }))
                .map(Enum::name)
                .orElse("NONE");
    }
}
