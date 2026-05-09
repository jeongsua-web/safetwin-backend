package com.safetwin.repository;

import com.safetwin.config.JpaAuditingTestConfig;
import com.safetwin.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaAuditingTestConfig.class)
class RiskRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired TestEntityManager em;
    @Autowired RiskRepository riskRepository;

    Long managerId;
    Long siteId;

    @BeforeEach
    void setUp() {
        User manager = em.persist(User.builder()
                .email("manager@test.com")
                .password("pw")
                .name("매니저")
                .role(User.Role.OWNER)
                .build());
        managerId = manager.getId();

        Site site = em.persist(Site.builder()
                .manager(manager)
                .name("A현장")
                .status(Site.Status.ACTIVE)
                .build());
        siteId = site.getId();

        Zone zone = em.persist(Zone.builder()
                .site(site)
                .name("3층 구역")
                .build());

        Analysis analysis = em.persist(Analysis.builder()
                .zone(zone)
                .requester(manager)
                .imageUrl("https://s3.example.com/img.jpg")
                .status(Analysis.Status.COMPLETED)
                .overallScore(75)
                .build());

        em.persist(Risk.builder()
                .analysis(analysis)
                .label("안전모 미착용")
                .description("작업자 안전모 미착용")
                .level(Risk.Level.HIGH)
                .status(Risk.Status.OPEN)
                .build());

        em.persist(Risk.builder()
                .analysis(analysis)
                .label("비계 고정 불량")
                .description("비계 고정 상태 불량")
                .level(Risk.Level.MEDIUM)
                .status(Risk.Status.RESOLVED)
                .build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("필터 없음 - 해당 관리자의 모든 위험 반환")
    void findByManagerIdWithFilters_noFilter_returnsAll() {
        List<Risk> risks = riskRepository.findByManagerIdWithFilters(managerId, null, null);
        assertThat(risks).hasSize(2);
    }

    @Test
    @DisplayName("status 필터 - OPEN 위험만 반환")
    void findByManagerIdWithFilters_statusOpen_returnsOnlyOpen() {
        List<Risk> risks = riskRepository.findByManagerIdWithFilters(managerId, null, Risk.Status.OPEN);
        assertThat(risks).hasSize(1);
        assertThat(risks.get(0).getLabel()).isEqualTo("안전모 미착용");
    }

    @Test
    @DisplayName("status 필터 - RESOLVED 위험만 반환")
    void findByManagerIdWithFilters_statusResolved_returnsOnlyResolved() {
        List<Risk> risks = riskRepository.findByManagerIdWithFilters(managerId, null, Risk.Status.RESOLVED);
        assertThat(risks).hasSize(1);
        assertThat(risks.get(0).getLabel()).isEqualTo("비계 고정 불량");
    }

    @Test
    @DisplayName("siteId 필터 - 해당 사업장 위험만 반환")
    void findByManagerIdWithFilters_siteIdFilter_returnsCorrect() {
        List<Risk> risks = riskRepository.findByManagerIdWithFilters(managerId, siteId, null);
        assertThat(risks).hasSize(2);
    }

    @Test
    @DisplayName("다른 관리자 ID - 결과 없음")
    void findByManagerIdWithFilters_wrongManager_returnsEmpty() {
        List<Risk> risks = riskRepository.findByManagerIdWithFilters(999L, null, null);
        assertThat(risks).isEmpty();
    }

    @Test
    @DisplayName("소유자 확인 성공 - 위험 조회")
    void findByIdAndManagerId_correctOwner_returnsRisk() {
        Risk risk = riskRepository.findByManagerIdWithFilters(managerId, null, Risk.Status.OPEN).get(0);
        Optional<Risk> found = riskRepository.findByIdAndManagerId(risk.getId(), managerId);
        assertThat(found).isPresent();
        assertThat(found.get().getLabel()).isEqualTo("안전모 미착용");
    }

    @Test
    @DisplayName("소유자 불일치 - 빈 결과")
    void findByIdAndManagerId_wrongOwner_returnsEmpty() {
        Risk risk = riskRepository.findByManagerIdWithFilters(managerId, null, Risk.Status.OPEN).get(0);
        Optional<Risk> found = riskRepository.findByIdAndManagerId(risk.getId(), 999L);
        assertThat(found).isEmpty();
    }
}
