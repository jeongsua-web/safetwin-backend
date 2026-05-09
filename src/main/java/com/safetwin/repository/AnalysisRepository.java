package com.safetwin.repository;

import com.safetwin.entity.Analysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    @Query("""
            SELECT a FROM Analysis a
            JOIN a.zone z JOIN z.site s
            WHERE s.manager.id = :managerId
            AND (:siteId IS NULL OR s.id = :siteId)
            ORDER BY a.createdAt DESC
            """)
    Page<Analysis> findByManagerIdWithFilter(
            @Param("managerId") Long managerId,
            @Param("siteId") Long siteId,
            Pageable pageable);

    @Query("SELECT a FROM Analysis a JOIN a.zone z JOIN z.site s WHERE a.id = :id AND s.manager.id = :managerId")
    Optional<Analysis> findByIdAndManagerId(@Param("id") Long id, @Param("managerId") Long managerId);

    boolean existsByIdAndZoneSiteManagerId(Long id, Long managerId);

    // 대시보드: 최근 N개 분석
    @Query("""
            SELECT a FROM Analysis a JOIN a.zone z JOIN z.site s
            WHERE s.manager.id = :managerId AND a.status = 'COMPLETED'
            ORDER BY a.createdAt DESC
            """)
    List<Analysis> findRecentCompleted(@Param("managerId") Long managerId, Pageable pageable);

    // 대시보드/통계: 기간 내 평균 점수
    @Query("""
            SELECT AVG(a.overallScore) FROM Analysis a JOIN a.zone z JOIN z.site s
            WHERE s.manager.id = :managerId AND a.status = 'COMPLETED'
            AND a.createdAt BETWEEN :from AND :to
            """)
    Double avgScoreBetween(
            @Param("managerId") Long managerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // 통계: 점수 추이 원시 데이터
    @Query("""
            SELECT a.createdAt, a.overallScore FROM Analysis a JOIN a.zone z JOIN z.site s
            WHERE s.manager.id = :managerId AND a.status = 'COMPLETED'
            AND a.createdAt >= :since
            ORDER BY a.createdAt ASC
            """)
    List<Object[]> findScoreDataSince(
            @Param("managerId") Long managerId,
            @Param("since") LocalDateTime since);
}
