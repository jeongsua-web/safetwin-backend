package com.safetwin.analysis.service;

import com.safetwin.analysis.dto.AnalysisResponse;
import com.safetwin.analysis.dto.AnalysisStatusResponse;
import com.safetwin.common.exception.ErrorCode;
import com.safetwin.common.exception.SafeTwinException;
import com.safetwin.entity.Analysis;
import com.safetwin.entity.User;
import com.safetwin.entity.Zone;
import com.safetwin.repository.AnalysisRepository;
import com.safetwin.repository.RiskRepository;
import com.safetwin.repository.UserRepository;
import com.safetwin.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final AnalysisRepository analysisRepository;
    private final RiskRepository riskRepository;
    private final UserRepository userRepository;
    private final ZoneRepository zoneRepository;
    private final S3Service s3Service;
    private final AnalysisAsyncProcessor asyncProcessor;

    // ── 분석 생성 ─────────────────────────────────────────────────────────────

    @Transactional
    public AnalysisResponse create(MultipartFile image, Long zoneId, Long requesterId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.NOT_FOUND));

        // 현재 사용자가 해당 site의 manager인지 확인
        if (!zone.getSite().getManager().getId().equals(requesterId)) {
            throw new SafeTwinException(ErrorCode.FORBIDDEN);
        }

        User requester = userRepository.getReferenceById(requesterId);
        String imageUrl = s3Service.upload(image, requesterId);

        Analysis analysis = Analysis.builder()
                .zone(zone)
                .requester(requester)
                .imageUrl(imageUrl)
                .status(Analysis.Status.PENDING)
                .build();

        analysis = analysisRepository.save(analysis);

        // 비동기 분석 실행 (별도 트랜잭션에서 처리)
        asyncProcessor.process(analysis.getId());

        return AnalysisResponse.pending(analysis);
    }

    // ── 분석 목록 조회 ────────────────────────────────────────────────────────

    public Page<AnalysisResponse> list(Long managerId, Long siteId, Pageable pageable) {
        return analysisRepository
                .findByManagerIdWithFilter(managerId, siteId, pageable)
                .map(a -> AnalysisResponse.from(a, riskRepository.findByAnalysisId(a.getId())));
    }

    // ── 분석 상세 조회 ────────────────────────────────────────────────────────

    public AnalysisResponse getDetail(Long analysisId, Long managerId) {
        Analysis analysis = findWithAccessCheck(analysisId, managerId);
        return AnalysisResponse.from(analysis, riskRepository.findByAnalysisId(analysisId));
    }

    // ── 분석 상태 폴링 ────────────────────────────────────────────────────────

    public AnalysisStatusResponse getStatus(Long analysisId, Long managerId) {
        Analysis analysis = findWithAccessCheck(analysisId, managerId);
        return AnalysisStatusResponse.from(analysis);
    }

    // ── 분석 삭제 ─────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long analysisId, Long managerId) {
        Analysis analysis = findWithAccessCheck(analysisId, managerId);

        riskRepository.deleteAllByAnalysisId(analysisId);
        analysisRepository.delete(analysis);
        s3Service.delete(analysis.getImageUrl());
    }

    // ── 공통 조회 + 권한 체크 ─────────────────────────────────────────────────

    private Analysis findWithAccessCheck(Long analysisId, Long managerId) {
        return analysisRepository.findByIdAndManagerId(analysisId, managerId)
                .orElseThrow(() -> new SafeTwinException(ErrorCode.NOT_FOUND));
    }
}
