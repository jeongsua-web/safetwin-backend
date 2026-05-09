# SafeTwin Backend

산업 현장 안전 관리 플랫폼 백엔드 API 서버.  
Claude Vision AI로 현장 이미지를 분석하고 위험 요소를 탐지하며, 법적 증빙 서류를 자동 생성합니다.

## 기술 스택

| 항목 | 버전 |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Build Tool | Gradle (Groovy) |
| Database | PostgreSQL |
| ORM | Spring Data JPA (Hibernate 6) |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Storage | AWS S3 (AWS SDK v2) |
| PDF | iText 7.2.6 |
| AI | Anthropic Claude API (claude-opus-4-5) |

---

## 로컬 실행 방법

### 1. 사전 요구사항

- Java 17+
- PostgreSQL 14+
- (선택) AWS S3 버킷
- (선택) Anthropic API 키

### 2. PostgreSQL 데이터베이스 생성

```sql
CREATE DATABASE safetwin;
CREATE USER safetwin WITH PASSWORD 'safetwin';
GRANT ALL PRIVILEGES ON DATABASE safetwin TO safetwin;
```

### 3. 환경변수 설정

아래 환경변수를 설정하거나 `.env` 파일을 사용하세요.

```bash
# Database
export DB_USERNAME=safetwin
export DB_PASSWORD=safetwin

# JWT (256비트 이상의 시크릿)
export JWT_SECRET=my-super-secret-key-for-safetwin-at-least-256-bits-long

# AWS S3
export AWS_ACCESS_KEY=your-aws-access-key
export AWS_SECRET_KEY=your-aws-secret-key
export AWS_S3_BUCKET=your-s3-bucket-name
export AWS_REGION=ap-northeast-2

# Claude AI
export CLAUDE_API_KEY=your-anthropic-api-key

# 이메일 (선택)
export MAIL_USERNAME=your-gmail@gmail.com
export MAIL_PASSWORD=your-app-password
```

### 4. 개발 모드로 실행 (DDL 자동 생성)

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 5. 프로덕션 빌드

```bash
./gradlew build
java -jar build/libs/safetwin-0.0.1-SNAPSHOT.jar
```

### 6. 한글 PDF 지원 (선택)

한글 폰트를 `src/main/resources/fonts/NanumGothic.ttf`에 배치하면  
PDF 문서에서 한글이 정상 출력됩니다. 파일이 없으면 Helvetica로 폴백됩니다.

---

## 환경변수 전체 목록

| 변수명 | 필수 | 기본값 | 설명 |
|---|---|---|---|
| `DB_USERNAME` | ✓ | `safetwin` | PostgreSQL 사용자명 |
| `DB_PASSWORD` | ✓ | `safetwin` | PostgreSQL 비밀번호 |
| `JWT_SECRET` | ✓ | (내장 기본값) | JWT 서명 시크릿 (256비트 이상) |
| `AWS_ACCESS_KEY` | | `` | AWS 액세스 키 (IAM Role 사용 시 불필요) |
| `AWS_SECRET_KEY` | | `` | AWS 시크릿 키 |
| `AWS_S3_BUCKET` | ✓ | `safetwin-bucket` | S3 버킷명 |
| `AWS_REGION` | | `ap-northeast-2` | AWS 리전 |
| `CLAUDE_API_KEY` | ✓ | `` | Anthropic API 키 |
| `MAIL_USERNAME` | | `` | 이메일 발송 계정 |
| `MAIL_PASSWORD` | | `` | 이메일 앱 비밀번호 |
| `CORS_ALLOWED_ORIGINS` | | `` | 추가 CORS 허용 도메인 |
| `SPRING_PROFILES_ACTIVE` | | `prod` | 활성 프로파일 (`dev` / `prod`) |

---

## API 엔드포인트 목록

### 인증 (공개)

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 → Access + Refresh Token |
| POST | `/api/auth/refresh` | Access Token 재발급 |
| POST | `/api/auth/logout` | 로그아웃 (Refresh Token 삭제) |
| POST | `/api/auth/biz-verify` | 사업자번호 유효성 검증 |
| POST | `/api/auth/oauth2/google` | Google OAuth2 로그인 |

### 안전 분석 (인증 필요)

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/analyses` | 이미지 업로드 → Claude 분석 시작 (202) |
| GET | `/api/analyses` | 분석 목록 (페이징, `siteId` 필터) |
| GET | `/api/analyses/{id}` | 분석 상세 (위험 요소 포함) |
| GET | `/api/analyses/{id}/status` | 분석 상태 폴링 |
| DELETE | `/api/analyses/{id}` | 분석 삭제 (S3 파일 포함) |

### 법적 문서 (인증 필요)

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/docs/risk-assessment` | 위험성 평가서 PDF 생성 |
| POST | `/api/docs/education-cert` | 안전보건교육 확인서 PDF 생성 |
| POST | `/api/docs/{id}/group-photo` | 단체 사진 첨부 |
| GET | `/api/docs` | 문서 목록 (`type`, `status` 필터) |
| GET | `/api/docs/{id}/pdf` | PDF 다운로드 (S3 redirect) |
| POST | `/api/docs/{id}/sign` | 서명 등록 |
| GET | `/api/docs/{id}/sign/status` | 서명 현황 조회 |

### 대시보드 (인증 필요)

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/dashboard/summary` | 안전 점수, 위험 현황, TBM 가이드 등 |

### 통계 (인증 필요)

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/stats/score-trend` | 안전 점수 추이 (`period=monthly\|weekly`) |
| GET | `/api/stats/compare` | 두 분석 비교 (`before={id}&after={id}`) |

### 탑뷰 (인증 필요)

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/topview/zones` | 구역 목록 + 위험 현황 (`siteId` 필수) |
| GET | `/api/topview/zones/{id}` | 구역 상세 + riskTags |
| POST | `/api/topview/zones` | 구역 등록 |
| PUT | `/api/topview/zones/{id}` | 구역 수정 |
| POST | `/api/topview/generate` | Claude 이미지 기반 구역 자동 생성 |

### 공공데이터 (인증 필요)

| Method | URL | 설명 |
|---|---|---|
| GET | `/api/public-data/accident-cases` | 업종별 사고 사례 (`industryType=건설\|제조`) |
| GET | `/api/public-data/laws` | 안전 법령 검색 (`keyword=추락` 등) |

---

## 패키지 구조

```
com.safetwin
├── auth          # 인증/인가 (JWT, OAuth2)
├── analysis      # 안전 분석 (Claude Vision, S3)
├── docs          # 법적 문서 생성 (PDF, 서명)
├── dashboard     # 대시보드 집계
├── stats         # 통계 (추이, 비교)
├── topview       # 탑뷰 구역 관리
├── publicdata    # 공공데이터 연동
├── entity        # JPA 엔티티
├── repository    # Spring Data JPA 레포지토리
├── config        # Spring 설정 (S3, Async 등)
└── common        # 공통 (응답 형식, 예외, 유틸)
```

---

## 공통 응답 형식

```json
// 성공
{ "success": true, "data": { ... } }

// 실패
{ "success": false, "code": "ERROR_CODE", "message": "에러 메시지" }
```

## 인증 방식

모든 보호 API는 `Authorization: Bearer {accessToken}` 헤더를 필요로 합니다.
