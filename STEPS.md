# SafeTwin 남은 작업 목록

> 최종 업데이트: 2026-05-04  
> 완료된 작업: STEP 1~10 (기반세팅, 인증, 분석, 문서/대시보드/통계, 탑뷰, 공공데이터, API 명세서, Site/Worker API, Risk 상태 API, Docker/배포, Google OAuth2 검증, 국세청 API, 테스트 코드)

---

## STEP 5 — Site / Worker API (🔴 필수)

프론트에서 `siteId`, `workerIds`를 API에 넘겨야 하는데 생성·조회 엔드포인트가 없음.

- [x] `SiteController` + `SiteService` + DTO 구현
  - `POST /api/sites` — 사업장 등록
  - `GET /api/sites` — 사업장 목록
  - `GET /api/sites/{id}` — 사업장 상세
  - `PUT /api/sites/{id}` — 사업장 수정
  - `DELETE /api/sites/{id}` — 사업장 삭제
- [x] `WorkerController` + `WorkerService` + DTO 구현
  - `POST /api/sites/{siteId}/workers` — 근로자 등록
  - `GET /api/sites/{siteId}/workers` — 근로자 목록
  - `PUT /api/workers/{id}` — 근로자 정보 수정
  - `DELETE /api/workers/{id}` — 근로자 삭제
- [x] `API_SPEC.md`에 Site / Worker 섹션 추가

---

## STEP 6 — Risk 상태 업데이트 API (🔴 필수)

위험 요소 탐지는 되지만 `OPEN → IN_PROGRESS → RESOLVED` 처리 엔드포인트 없음.  
대시보드 `unresolvedCount`가 있는데 실제로 해결 처리를 할 수가 없는 상태.

- [x] `RiskController` + `RiskService` + DTO 구현
  - `GET /api/risks?siteId={id}&status={status}` — 위험 목록 조회
  - `PATCH /api/risks/{id}/status` — 상태 변경 (`OPEN` / `IN_PROGRESS` / `RESOLVED`)
- [x] `API_SPEC.md`에 Risk 섹션 추가

---

## STEP 7 — Docker / 배포 설정 (🟡 중요)

- [x] `Dockerfile` 작성 (multi-stage build, Java 17)
- [x] `docker-compose.yml` 작성 (app + PostgreSQL)
- [x] `application-prod.yml` 작성 (`DB_HOST` 환경변수 분리)
- [x] DB 마이그레이션 스크립트 작성 (`src/main/resources/sql/schema.sql`)
- [x] `.env.example` 작성

---

## STEP 8 — Google OAuth2 서명 검증 (🟡 중요)

- [x] `GoogleTokenVerifier` 컴포넌트 신규 작성 (nimbus-jose-jwt 9.37.3)
  - Google JWK Set 조회 + 5시간 캐시
  - RS256 서명 검증, iss/aud/exp 클레임 검증
- [x] `AuthService.googleOAuth2()` → stub 제거, `GoogleTokenVerifier` 주입
- [x] `build.gradle`에 `nimbus-jose-jwt` 의존성 추가
- [x] `application.yml`에 `google.client-id` 설정 추가
- [x] `.env.example`, `docker-compose.yml`에 `GOOGLE_CLIENT_ID` 추가

---

## STEP 9 — 국세청 사업자번호 API 연동 (🟡 중요)

- [x] `NtsApiService` 신규 작성
  - POST `https://api.odcloud.kr/api/nts-businessman/v1/status` 호출
  - 계속사업자(01) / 휴업(02) / 폐업(03) 상태 반환
  - API 키 미설정 시 체크섬 통과로 폴백 (개발 환경 대응)
- [x] `AuthService.verifyBizNumber()` stub 제거 → `NtsApiService` 연동
- [x] `application.yml`에 `nts.api-key`, `nts.api-url` 추가
- [x] `.env.example`, `docker-compose.yml`에 `NTS_API_KEY` 추가

---

## STEP 10 — 테스트 코드 (🟢 권장)

현재 `SafeTwinApplicationTests.java` (컨텍스트 로드)만 존재.

- [x] `AuthServiceTest` — 회원가입(성공/중복/잘못된 사업자번호), 로그인(성공/실패), 토큰 재발급, 사업자번호 검증 (8개 케이스)
- [x] `AnalysisAsyncProcessorTest` — 점수 계산(레벨별/혼합/클램프), 상태 전이 COMPLETED/FAILED (8개 케이스)
- [x] `RiskRepositoryIntegrationTest` — Testcontainers PostgreSQL + @DataJpaTest, JPQL 필터 쿼리 검증 (6개 케이스)
- [x] `JpaAuditingTestConfig` — @DataJpaTest용 JPA Auditing 설정
- [x] `build.gradle` — Testcontainers 의존성 추가

---

## STEP 11 — 공공데이터 실API 연동 (🟢 장기)

- [x] 국가법령정보 공공 API (`law.go.kr`) 연동 — `LawGoKrService` 신규 작성, `PublicDataService.searchLaws()` 연동 (API 키 미설정 시 큐레이션 stub으로 폴백)
- [x] `LAW_API_KEY` `.env.example`, `docker-compose.yml` 추가
- [x] 고용노동부 사고 사례 API: 개별 사례 서술 형식을 제공하는 공개 API 없음 → 큐레이션 데이터 유지 (TODO 제거 및 사유 주석 추가)

---

## 완료 체크리스트 요약

| Step | 내용 | 상태 |
|---|---|---|
| STEP 1 | 기반 세팅 (엔티티, 레포지토리, 공통) | ✅ 완료 |
| STEP 2 | 인증 모듈 (JWT, OAuth2) | ✅ 완료 |
| STEP 3 | 분석 모듈 + 문서/대시보드/통계 | ✅ 완료 |
| STEP 4 | 탑뷰 + 공공데이터 + 마무리 | ✅ 완료 |
| API 명세서 | Kotlin 팀용 `API_SPEC.md` | ✅ 완료 |
| STEP 5 | Site / Worker API | ✅ 완료 |
| STEP 6 | Risk 상태 업데이트 API | ✅ 완료 |
| STEP 7 | Docker / 배포 설정 | ✅ 완료 |
| STEP 8 | Google OAuth2 서명 검증 | ✅ 완료 |
| STEP 9 | 국세청 사업자번호 API 연동 | ✅ 완료 |
| STEP 10 | 테스트 코드 | ✅ 완료 |
| STEP 11 | 공공데이터 실API 연동 | ✅ 완료 |
