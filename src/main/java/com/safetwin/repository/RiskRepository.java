package com.safetwin.repository;

import com.safetwin.entity.Risk;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RiskRepository extends JpaRepository<Risk, Long> {

    // 위험 목록 조회 (managerId 필수, siteId·status 선택 필터)
    @Query("""
            SELECT r FROM Risk r
            JOIN r.analysis a JOIN a.zone z JOIN z.site s
            WHERE s.manager.id = :managerId
            AND (:siteId IS NULL OR s.id = :siteId)
            AND (:status IS NULL OR r.status = :status)
            ORDER BY r.status ASC, a.createdAt DESC
            """)
    List<Risk> findByManagerIdWithFilters(
            @Param("managerId") Long managerId,
            @Param("siteId") Long siteId,
            @Param("status") Risk.Status status);

    // 상태 변경 보안 체크: 해당 관리자 소유 여부 확인
    @Query("""
            SELECT r FROM Risk r
            JOIN r.analysis a JOIN a.zone z JOIN z.site s
            WHERE r.id = :riskId AND s.manager.id = :managerId
            """)
    Optional<Risk> findByIdAndManagerId(
            @Param("riskId") Long riskId,
            @Param("managerId") Long managerId);

    List<Risk> findByAnalysisId(Long analysisId);

    List<Risk> findByLevel(Risk.Level level);

    List<Risk> findByStatus(Risk.Status status);

    List<Risk> findByAnalysisIdAndStatus(Long analysisId, Risk.Status status);

    @Modifying
    @Query("DELETE FROM Risk r WHERE r.analysis.id = :analysisId")
    void deleteAllByAnalysisId(@Param("analysisId") Long analysisId);

    // 탑뷰: 구역별 미조치 위험 요소
    @Query("""
            SELECT r FROM Risk r JOIN r.analysis a
            WHERE a.zone.id = :zoneId AND r.status = 'OPEN'
            ORDER BY a.createdAt DESC
            """)
    List<Risk> findOpenRisksByZoneId(@Param("zoneId") Long zoneId);

    @Query("SELECT COUNT(r) FROM Risk r JOIN r.analysis a WHERE a.zone.id = :zoneId AND r.status = 'OPEN'")
    long countOpenRisksByZoneId(@Param("zoneId") Long zoneId);

    // 대시보드: 기간 내 위험 요소 수
    @Query("""
            SELECT COUNT(r) FROM Risk r JOIN r.analysis a JOIN a.zone z JOIN z.site s
            WHERE s.manager.id = :managerId
            AND a.createdAt BETWEEN :from AND :to
            """)
    long countByManagerIdBetween(
            @Param("managerId") Long managerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // 대시보드: 미조치 위험 요소 수
    @Query("""
            SELECT COUNT(r) FROM Risk r JOIN r.analysis a JOIN a.zone z JOIN z.site s
            WHERE s.manager.id = :managerId AND r.status = 'OPEN'
            """)
    long countOpenByManagerId(@Param("managerId") Long managerId);

    // 대시보드: 최근 빈발 위험 라벨 (TBM 가이드용)
    @Query("""
            SELECT r.label, COUNT(r) FROM Risk r JOIN r.analysis a JOIN a.zone z JOIN z.site s
            WHERE s.manager.id = :managerId AND a.createdAt >= :since
            GROUP BY r.label ORDER BY COUNT(r) DESC
            """)
    List<Object[]> findTopRiskLabelsSince(
            @Param("managerId") Long managerId,
            @Param("since") LocalDateTime since,
            Pageable pageable);
}
