# SafeTwin API 명세서

> **대상:** Kotlin Android 프론트엔드 팀  
> **기준 버전:** Spring Boot 3.2.5 / Java 17  
> **Base URL (개발):** `http://10.0.2.2:8080` (Android 에뮬레이터) / `http://localhost:8080` (실기기 로컬)

---

## 목차

1. [공통 규격](#1-공통-규격)
2. [인증 (Auth)](#2-인증-auth)
3. [사업장 (Site)](#3-사업장-site)
4. [근로자 (Worker)](#4-근로자-worker)
5. [위험 요소 (Risk)](#5-위험-요소-risk)
6. [안전 분석 (Analysis)](#6-안전-분석-analysis)
7. [법적 문서 (Docs)](#7-법적-문서-docs)
8. [대시보드 (Dashboard)](#8-대시보드-dashboard)
9. [통계 (Stats)](#9-통계-stats)
10. [탑뷰 (Topview)](#10-탑뷰-topview)
11. [공공데이터 (Public Data)](#11-공공데이터-public-data)
12. [에러 코드 목록](#12-에러-코드-목록)

---

## 1. 공통 규격

### 1.1 공통 응답 형식

모든 API는 동일한 래퍼 구조를 사용합니다.

**성공 응답**
```json
{
  "success": true,
  "data": { ... }
}
```

**실패 응답**
```json
{
  "success": false,
  "code": "INVALID_CREDENTIALS",
  "message": "이메일 또는 비밀번호가 올바르지 않습니다."
}
```

### 1.2 인증 헤더

로그인 이후 모든 API 호출에는 아래 헤더가 필요합니다.

```
Authorization: Bearer {accessToken}
```

- Access Token 유효기간: **30분**
- 만료 시 `401 EXPIRED_TOKEN` 응답 → `/api/auth/refresh` 로 재발급

### 1.3 날짜 형식

| 타입 | 포맷 | 예시 |
|---|---|---|
| `LocalDateTime` | ISO 8601 | `"2024-07-15T09:30:00"` |

### 1.4 페이지네이션

목록 API에 Spring Page 응답이 사용됩니다.

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "totalElements": 100,
    "totalPages": 5,
    "number": 0,
    "size": 20,
    "first": true,
    "last": false
  }
}
```

쿼리 파라미터로 `page=0&size=20&sort=createdAt,desc` 형식을 사용합니다.

---

## 2. 인증 (Auth)


### 2.1 회원가입

```
POST /api/auth/signup
Content-Type: application/json
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `email` | String | ✓ | 이메일 형식 |
| `password` | String | ✓ | 최소 8자 |
| `name` | String | ✓ | 이름 |
| `phone` | String | | 전화번호 |
| `bizNumber` | String | | 사업자번호 (숫자 10자리) |
| `industry` | String | | 업종 |
| `companySize` | String | | 회사 규모 |

```json
{
  "email": "manager@site.com",
  "password": "pass1234!",
  "name": "김현장",
  "phone": "010-1234-5678",
  "bizNumber": "1234567890",
  "industry": "건설",
  "companySize": "중소기업"
}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**에러**

| HTTP | code | 설명 |
|---|---|---|
| 409 | `DUPLICATE_EMAIL` | 이미 가입된 이메일 |
| 400 | `VALIDATION_ERROR` | 유효성 검사 실패 |

---

### 2.2 로그인

```
POST /api/auth/login
Content-Type: application/json
```

**Request Body**

```json
{
  "email": "manager@site.com",
  "password": "pass1234!"
}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**에러**

| HTTP | code | 설명 |
|---|---|---|
| 401 | `INVALID_CREDENTIALS` | 이메일/비밀번호 불일치 |

---

### 2.3 Access Token 재발급

```
POST /api/auth/refresh
Content-Type: application/json
```

**Request Body**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

> 재발급 시 **기존 Refresh Token은 무효화**되고 새 Refresh Token이 발급됩니다. 저장된 값을 교체해야 합니다.

**에러**

| HTTP | code | 설명 |
|---|---|---|
| 401 | `INVALID_TOKEN` | 유효하지 않은 토큰 |
| 401 | `EXPIRED_TOKEN` | 만료된 Refresh Token |

---

### 2.4 로그아웃

```
POST /api/auth/logout
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Request Body**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response (200)**
```json
{
  "success": true,
  "data": null
}
```

---

### 2.5 사업자번호 검증

```
POST /api/auth/biz-verify
Content-Type: application/json
```

**Request Body**

```json
{
  "bizNumber": "1234567890"
}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "bizNumber": "1234567890",
    "valid": true,
    "companyName": "SafeTwin 건설(주)",
    "status": "계속사업자"
  }
}
```

> `valid: false`인 경우 `companyName`, `status`는 `null`

---

### 2.6 Google OAuth2 로그인

```
POST /api/auth/oauth2/google
Content-Type: application/json
```

**Request Body**

```json
{
  "idToken": "구글에서_받은_ID_토큰"
}
```

**Response (200)**  
→ [2.1 회원가입](#21-회원가입) 응답과 동일한 `TokenResponse` 구조

---

## 3. 사업장 (Site)

### 3.1 사업장 등록

```
POST /api/sites
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `name` | String | ✓ | 사업장 이름 |
| `address` | String | | 주소 |
| `bizNumber` | String | | 사업자번호 |

```json
{
  "name": "A건설현장",
  "address": "서울특별시 강남구 테헤란로 123",
  "bizNumber": "1234567890"
}
```

**Response (201)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "A건설현장",
    "address": "서울특별시 강남구 테헤란로 123",
    "bizNumber": "1234567890",
    "status": "ACTIVE",
    "managerId": 5,
    "managerName": "김현장",
    "createdAt": "2024-07-15T09:00:00"
  }
}
```

**Site Status 값**

| 값 | 설명 |
|---|---|
| `ACTIVE` | 운영 중 (기본값) |
| `INACTIVE` | 일시 중단 |
| `COMPLETED` | 완료/종료 |

---

### 3.2 사업장 목록 조회

```
GET /api/sites
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "A건설현장",
      "address": "서울특별시 강남구 테헤란로 123",
      "bizNumber": "1234567890",
      "status": "ACTIVE",
      "managerId": 5,
      "managerName": "김현장",
      "createdAt": "2024-07-15T09:00:00"
    }
  ]
}
```

---

### 3.3 사업장 상세 조회

```
GET /api/sites/{id}
Authorization: Bearer {accessToken}
```

**Response (200)** → [3.1](#31-사업장-등록) `SiteResponse`와 동일 구조

---

### 3.4 사업장 수정

```
PUT /api/sites/{id}
Content-Type: application/json
Authorization: Bearer {accessToken}
```

변경할 필드만 포함 (모두 선택).

```json
{
  "name": "A건설현장 (변경)",
  "status": "INACTIVE"
}
```

**Response (200)** → `SiteResponse`

---

### 3.5 사업장 삭제

```
DELETE /api/sites/{id}
Authorization: Bearer {accessToken}
```

**Response (204 No Content)**

**에러**

| HTTP | code | 설명 |
|---|---|---|
| 404 | `SITE_NOT_FOUND` | 사업장 없음 또는 접근 권한 없음 |

---

## 4. 근로자 (Worker)

### 4.1 근로자 등록

```
POST /api/sites/{siteId}/workers
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `name` | String | ✓ | 근로자 이름 |
| `phone` | String | | 연락처 |
| `occupation` | String | | 직종 (예: `철근공`, `목수`) |
| `startDate` | String | | 투입일 (`yyyy-MM-dd`) |
| `endDate` | String | | 철수 예정일 (`yyyy-MM-dd`) |

```json
{
  "name": "이근로",
  "phone": "010-9876-5432",
  "occupation": "철근공",
  "startDate": "2024-07-01",
  "endDate": "2024-12-31"
}
```

**Response (201)**
```json
{
  "success": true,
  "data": {
    "id": 101,
    "siteId": 1,
    "siteName": "A건설현장",
    "name": "이근로",
    "phone": "010-9876-5432",
    "occupation": "철근공",
    "startDate": "2024-07-01",
    "endDate": "2024-12-31",
    "createdAt": "2024-07-15T10:00:00"
  }
}
```

---

### 4.2 근로자 목록 조회

```
GET /api/sites/{siteId}/workers
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "siteId": 1,
      "siteName": "A건설현장",
      "name": "이근로",
      "phone": "010-9876-5432",
      "occupation": "철근공",
      "startDate": "2024-07-01",
      "endDate": "2024-12-31",
      "createdAt": "2024-07-15T10:00:00"
    }
  ]
}
```

---

### 4.3 근로자 수정

```
PUT /api/workers/{id}
Content-Type: application/json
Authorization: Bearer {accessToken}
```

변경할 필드만 포함 (모두 선택).

```json
{
  "occupation": "목수",
  "endDate": "2025-03-31"
}
```

**Response (200)** → `WorkerResponse`

---

### 4.4 근로자 삭제

```
DELETE /api/workers/{id}
Authorization: Bearer {accessToken}
```

**Response (204 No Content)**

**에러**

| HTTP | code | 설명 |
|---|---|---|
| 404 | `SITE_NOT_FOUND` | 사업장 없음 또는 접근 권한 없음 |
| 404 | `WORKER_NOT_FOUND` | 근로자 없음 또는 접근 권한 없음 |

---

## 5. 위험 요소 (Risk)

### 5.1 위험 목록 조회

```
GET /api/risks?siteId={siteId}&status={status}
Authorization: Bearer {accessToken}
```

**Query Parameter**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `siteId` | Long | 사업장 필터 (없으면 전체) |
| `status` | String | 상태 필터 (`OPEN` / `IN_PROGRESS` / `RESOLVED`) |

**Response (200)**
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "analysisId": 42,
      "siteId": 1,
      "siteName": "A건설현장",
      "zoneLocation": "A건설현장 > 3층 작업구역",
      "label": "안전모 미착용",
      "detail": "작업자 3명이 안전모를 착용하지 않고 있음",
      "level": "HIGH",
      "status": "OPEN",
      "law": "산업안전보건법 제38조",
      "action": "즉시 작업 중단 후 안전모 착용 지시",
      "x": 0.45,
      "y": 0.32,
      "createdAt": "2024-07-15T10:35:00"
    }
  ]
}
```

> 응답은 `status ASC (OPEN → IN_PROGRESS → RESOLVED)` 후 분석 최신순으로 정렬됩니다.

---

### 5.2 위험 상태 변경

```
PATCH /api/risks/{id}/status
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | String | ✓ | `OPEN` / `IN_PROGRESS` / `RESOLVED` |

```json
{
  "status": "IN_PROGRESS"
}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 10,
    "analysisId": 42,
    "siteId": 1,
    "siteName": "A건설현장",
    "zoneLocation": "A건설현장 > 3층 작업구역",
    "label": "안전모 미착용",
    "detail": "작업자 3명이 안전모를 착용하지 않고 있음",
    "level": "HIGH",
    "status": "IN_PROGRESS",
    "law": "산업안전보건법 제38조",
    "action": "즉시 작업 중단 후 안전모 착용 지시",
    "x": 0.45,
    "y": 0.32,
    "createdAt": "2024-07-15T10:35:00"
  }
}
```

**Risk Status 흐름**

```
OPEN → IN_PROGRESS → RESOLVED
```

**에러**

| HTTP | code | 설명 |
|---|---|---|
| 404 | `RISK_NOT_FOUND` | 위험 요소 없음 또는 접근 권한 없음 |
| 400 | `VALIDATION_ERROR` | status 값이 유효하지 않음 |

---

## 6. 안전 분석 (Analysis)

### 3.1 이미지 업로드 & 분석 시작

```
POST /api/analyses
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}
```

**Request Parts**

| 파트 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `image` | MultipartFile | ✓ | JPG, PNG, HEIC, HEIF (최대 20MB) |

**Query Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `zoneId` | Long | ✓ | 분석 대상 구역 ID |

**Kotlin 예시 (OkHttp)**
```kotlin
val requestBody = MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart(
        "image",
        "photo.jpg",
        imageBytes.toRequestBody("image/jpeg".toMediaType())
    )
    .build()

val request = Request.Builder()
    .url("$BASE_URL/api/analyses?zoneId=$zoneId")
    .addHeader("Authorization", "Bearer $accessToken")
    .post(requestBody)
    .build()
```

**Response (202 Accepted)**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "imageUrl": "https://s3.amazonaws.com/.../analyses/1/uuid.jpg",
    "location": "A현장 > 3층 작업구역",
    "status": "PENDING",
    "analyzedAt": null,
    "overallScore": null,
    "riskLevel": null,
    "risks": []
  }
}
```

> **비동기 처리:** 202를 받은 후 분석이 백그라운드에서 진행됩니다.  
> `status`가 `COMPLETED`가 될 때까지 [3.4 상태 폴링](#34-분석-상태-폴링)을 호출하세요.

**Analysis Status 값**

| 값 | 설명 |
|---|---|
| `PENDING` | 분석 대기 중 |
| `IN_PROGRESS` | Claude AI 분석 중 |
| `COMPLETED` | 분석 완료 |
| `FAILED` | 분석 실패 |

---

### 3.2 분석 목록 조회

```
GET /api/analyses?siteId={siteId}&page=0&size=20
Authorization: Bearer {accessToken}
```

**Query Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `siteId` | Long | | 사업장 ID 필터 (없으면 전체) |
| `page` | int | | 페이지 번호 (기본 0) |
| `size` | int | | 페이지 크기 (기본 20) |

**Response (200)** — `Page<AnalysisResponse>` ([1.4 페이지네이션](#14-페이지네이션) 참고)

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 42,
        "imageUrl": "https://s3.amazonaws.com/...",
        "location": "A현장 > 3층 작업구역",
        "status": "COMPLETED",
        "analyzedAt": "2024-07-15T10:35:00",
        "overallScore": 72,
        "riskLevel": "MEDIUM",
        "risks": []
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

---

### 3.3 분석 상세 조회

```
GET /api/analyses/{id}
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "imageUrl": "https://s3.amazonaws.com/...",
    "location": "A현장 > 3층 작업구역",
    "status": "COMPLETED",
    "analyzedAt": "2024-07-15T10:35:00",
    "overallScore": 72,
    "riskLevel": "MEDIUM",
    "risks": [
      {
        "id": 10,
        "label": "안전모 미착용",
        "detail": "작업자 3명이 안전모를 착용하지 않고 있음",
        "level": "HIGH",
        "law": "산업안전보건법 제38조",
        "action": "즉시 작업 중단 후 안전모 착용 지시",
        "x": 0.45,
        "y": 0.32
      }
    ]
  }
}
```

**Risk 필드 설명**

| 필드 | 타입 | 설명 |
|---|---|---|
| `level` | String | `LOW` / `MEDIUM` / `HIGH` / `CRITICAL` |
| `x`, `y` | Double | 이미지 내 위험 위치 (0.0 ~ 1.0 비율) |

**riskLevel (overallScore 기준)**

| riskLevel | overallScore 범위 |
|---|---|
| `LOW` | 80 이상 |
| `MEDIUM` | 50 ~ 79 |
| `HIGH` | 0 ~ 49 |

---

### 3.4 분석 상태 폴링

```
GET /api/analyses/{id}/status
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 42,
    "status": "IN_PROGRESS"
  }
}
```

> **권장 폴링 주기:** 3초마다 호출, `COMPLETED` 또는 `FAILED` 수신 시 중단

---

### 3.5 분석 삭제

```
DELETE /api/analyses/{id}
Authorization: Bearer {accessToken}
```

**Response (204 No Content)**
```json
{
  "success": true,
  "data": null
}
```

> S3에 업로드된 이미지 파일도 함께 삭제됩니다.

---

## 7. 법적 문서 (Docs)

### 4.1 위험성 평가서 PDF 생성

```
POST /api/docs/risk-assessment
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Request Body**

```json
{
  "analysisId": 42
}
```

**Response (201)**
```json
{
  "success": true,
  "data": {
    "id": 7,
    "title": "위험성 평가서_2024-07-15",
    "type": "RISK_ASSESSMENT",
    "status": "DRAFT",
    "fileUrl": "https://s3.amazonaws.com/.../docs/risk_assessment_7.pdf",
    "fileSize": 245760,
    "siteId": 1,
    "siteName": "A건설현장",
    "analysisId": 42,
    "createdAt": "2024-07-15T11:00:00"
  }
}
```

**Document Type 값**

| 값 | 설명 |
|---|---|
| `SAFETY_PLAN` | 안전보건관리계획서 |
| `INSPECTION_REPORT` | 점검 보고서 |
| `RISK_ASSESSMENT` | 위험성 평가서 |
| `EDUCATION_CERT` | 안전보건교육 확인서 |
| `GROUP_PHOTO` | 단체 사진 |
| `OTHER` | 기타 |

**Document Status 값**

| 값 | 설명 |
|---|---|
| `DRAFT` | 초안 (서명 전) |
| `SIGNED` | 서명 완료 |

---

### 4.2 안전보건교육 확인서 PDF 생성

```
POST /api/docs/education-cert
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `siteId` | Long | ✓ | 사업장 ID |
| `educationDate` | String | ✓ | 교육 일시 (예: `"2024-07-15 09:00"`) |
| `content` | String | ✓ | 교육 내용 |
| `workerIds` | List\<Long\> | | 교육 이수자 근로자 ID 목록 |

```json
{
  "siteId": 1,
  "educationDate": "2024-07-15 09:00",
  "content": "고소작업 안전수칙 및 추락방지 교육",
  "workerIds": [101, 102, 103]
}
```

**Response (201)** → [4.1](#41-위험성-평가서-pdf-생성) `DocResponse`와 동일 구조

---

### 4.3 단체 사진 첨부

```
POST /api/docs/{id}/group-photo
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}
```

**Request Parts**

| 파트 | 타입 | 설명 |
|---|---|---|
| `photo` | MultipartFile | 단체 사진 파일 |

**Response (201)** → `DocResponse`

---

### 4.4 문서 목록 조회

```
GET /api/docs?type={type}&status={status}&page=0&size=20
Authorization: Bearer {accessToken}
```

**Query Parameter**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `type` | String | 문서 타입 필터 (예: `RISK_ASSESSMENT`) |
| `status` | String | 상태 필터 (`DRAFT` / `SIGNED`) |
| `page`, `size` | int | 페이지네이션 |

**Response (200)** — `Page<DocResponse>`

---

### 4.5 PDF 다운로드 (S3 Redirect)

```
GET /api/docs/{id}/pdf
Authorization: Bearer {accessToken}
```

> 서버가 `302 Redirect` → S3 Presigned URL로 이동합니다.  
> OkHttp는 자동으로 리다이렉트를 따르지만, Retrofit 사용 시 `@Streaming` + `ResponseBody` 처리를 권장합니다.

**Kotlin 예시**
```kotlin
// Retrofit 인터페이스
@GET("docs/{id}/pdf")
@Streaming
suspend fun downloadPdf(@Path("id") id: Long): Response<ResponseBody>

// 사용
val response = api.downloadPdf(docId)
val pdfBytes = response.body()?.bytes()
```

---

### 4.6 서명 등록

```
POST /api/docs/{id}/sign
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `signerName` | String | ✓ | 서명자 이름 |
| `signatureData` | String | ✓ | 서명 이미지 (Base64 또는 SVG 경로 데이터) |

```json
{
  "signerName": "김현장",
  "signatureData": "data:image/png;base64,iVBORw0KGgo..."
}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "documentId": 7,
    "documentStatus": "SIGNED",
    "signatureCount": 1,
    "signatures": [
      {
        "id": 1,
        "signerName": "김현장",
        "signedAt": "2024-07-15T14:00:00"
      }
    ]
  }
}
```

---

### 4.7 서명 현황 조회

```
GET /api/docs/{id}/sign/status
Authorization: Bearer {accessToken}
```

**Response (200)** → [4.6](#46-서명-등록) `SignStatusResponse`와 동일 구조

---

## 8. 대시보드 (Dashboard)

### 5.1 대시보드 요약 조회

```
GET /api/dashboard/summary
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "safetyScore": 82.5,
    "safetyScoreDelta": 3.2,
    "weeklyRiskCount": 12,
    "weeklyRiskDelta": -3,
    "unresolvedCount": 5,
    "educationRate": 0.75,
    "safetyGrade": "B+",
    "tbmGuide": "오늘 TBM 핵심 주의사항: 안전모 미착용, 추락 위험, 전기 감전에 특히 주의하세요.",
    "recentAnalyses": [
      {
        "id": 42,
        "imageUrl": "https://s3.amazonaws.com/...",
        "location": "A현장 > 3층",
        "status": "COMPLETED",
        "overallScore": 72,
        "riskLevel": "MEDIUM",
        "analyzedAt": "2024-07-15T10:35:00"
      }
    ]
  }
}
```

**필드 설명**

| 필드 | 설명 |
|---|---|
| `safetyScore` | 이번 주 평균 안전 점수 (0~100) |
| `safetyScoreDelta` | 지난주 대비 변화량 (양수=상승) |
| `weeklyRiskCount` | 이번 주 탐지된 위험 수 |
| `weeklyRiskDelta` | 지난주 대비 변화량 (음수=감소=개선) |
| `unresolvedCount` | 미해결 위험 건수 |
| `educationRate` | 교육 이수율 (0.0~1.0) |
| `safetyGrade` | 안전 등급 (A+/A/B+/B/C/D/F) |
| `tbmGuide` | 오늘의 TBM(Tool Box Meeting) 가이드 문구 |

**안전 등급 기준**

| 등급 | 점수 |
|---|---|
| A+ | 95 이상 |
| A | 90 이상 |
| B+ | 85 이상 |
| B | 80 이상 |
| C | 70 이상 |
| D | 60 이상 |
| F | 60 미만 |

---

## 9. 통계 (Stats)

### 6.1 안전 점수 추이

```
GET /api/stats/score-trend?period={period}
Authorization: Bearer {accessToken}
```

**Query Parameter**

| 파라미터 | 값 | 설명 |
|---|---|---|
| `period` | `monthly` (기본) | 최근 12개월 |
| `period` | `weekly` | 최근 12주 |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "labels": ["2024-01", "2024-02", "2024-03", "...", "2024-07"],
    "scores": [85, 78, null, 90, 88, null, 82],
    "target": 80,
    "min": 78,
    "current": 82
  }
}
```

> `scores` 배열에서 **`null`은 해당 기간에 분석 데이터가 없음**을 의미합니다 (0점과 구별).  
> 차트 렌더링 시 `null` 구간은 선을 끊어서 표시하세요.

**weekly 예시 labels:** `["2024-W01", "2024-W02", ...]`

---

### 6.2 두 분석 비교

```
GET /api/stats/compare?before={id}&after={id}
Authorization: Bearer {accessToken}
```

**Query Parameter**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `before` | Long | 이전 분석 ID |
| `after` | Long | 이후 분석 ID |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "beforeId": 30,
    "afterId": 42,
    "beforeScore": 65,
    "afterScore": 82,
    "scoreDelta": 17,
    "beforeRiskCount": 8,
    "afterRiskCount": 4,
    "riskDelta": -4,
    "improvedItems": ["안전모 미착용", "안전망 미설치"],
    "newItems": ["비계 고정 불량"],
    "persistedItems": ["전기 배선 노출"]
  }
}
```

**필드 설명**

| 필드 | 설명 |
|---|---|
| `scoreDelta` | 점수 변화 (양수=개선) |
| `riskDelta` | 위험 건수 변화 (음수=감소=개선) |
| `improvedItems` | 해결된 위험 항목 (before에만 있음) |
| `newItems` | 새로 발생한 위험 항목 (after에만 있음) |
| `persistedItems` | 지속 중인 위험 항목 (양쪽 모두 존재) |

---

## 10. 탑뷰 (Topview)

### 7.1 구역 목록 조회

```
GET /api/topview/zones?siteId={siteId}
Authorization: Bearer {accessToken}
```

**Query Parameter**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `siteId` | Long | ✓ | 사업장 ID |

**Response (200)**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "3층 작업구역",
      "description": "철근 작업 구역",
      "floorNumber": 3,
      "x": 0.1,
      "y": 0.2,
      "w": 0.3,
      "h": 0.25,
      "area": "외부 작업 구역",
      "riskCount": 3,
      "riskLevel": "HIGH"
    }
  ]
}
```

**좌표 설명 (x, y, w, h)**
- 이미지 크기 대비 비율 (0.0 ~ 1.0)
- `x`, `y`: 구역 좌상단 꼭짓점
- `w`, `h`: 구역 너비/높이

---

### 7.2 구역 상세 조회

```
GET /api/topview/zones/{id}
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "3층 작업구역",
    "description": "철근 작업 구역",
    "floorNumber": 3,
    "x": 0.1,
    "y": 0.2,
    "w": 0.3,
    "h": 0.25,
    "area": "외부 작업 구역",
    "siteId": 1,
    "siteName": "A건설현장",
    "riskCount": 3,
    "riskLevel": "HIGH",
    "riskTags": [
      {
        "riskId": 10,
        "label": "안전모 미착용",
        "x": 0.45,
        "y": 0.32,
        "law": "산업안전보건법 제38조",
        "accidentCase": null
      }
    ]
  }
}
```

---

### 7.3 구역 등록

```
POST /api/topview/zones
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `siteId` | Long | ✓ | 사업장 ID |
| `name` | String | ✓ | 구역 이름 |
| `description` | String | | 구역 설명 |
| `floorNumber` | Integer | | 층 번호 |
| `x`, `y`, `w`, `h` | Double | | 탑뷰 좌표 및 크기 |
| `area` | String | | 구역 특성 설명 |

```json
{
  "siteId": 1,
  "name": "5층 거푸집 작업구역",
  "description": "거푸집 설치 및 해체 작업",
  "floorNumber": 5,
  "x": 0.5,
  "y": 0.1,
  "w": 0.2,
  "h": 0.3,
  "area": "고소 작업 구역"
}
```

**Response (201)** → `ZoneDetailResponse` ([7.2](#72-구역-상세-조회) 참고)

---

### 7.4 구역 수정

```
PUT /api/topview/zones/{id}
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Request Body** — 변경할 필드만 포함 (모두 선택)

```json
{
  "name": "5층 작업구역 (수정)",
  "floorNumber": 5,
  "x": 0.55
}
```

**Response (200)** → `ZoneDetailResponse`

---

### 7.5 Claude AI 구역 자동 생성

```
POST /api/topview/generate
Content-Type: application/json
Authorization: Bearer {accessToken}
```

**Request Body**

```json
{
  "analysisId": 42
}
```

> 기존 분석 이미지를 Claude AI로 재분석하여 공간 구역을 자동으로 추출합니다.

**Response (201)**
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "name": "출입구",
      "x": 0.05,
      "y": 0.4,
      "w": 0.1,
      "h": 0.2,
      ...
    },
    {
      "id": 11,
      "name": "자재 적재 구역",
      "x": 0.3,
      "y": 0.6,
      ...
    }
  ]
}
```

---

## 11. 공공데이터 (Public Data)

### 8.1 업종별 사고 사례 조회

```
GET /api/public-data/accident-cases?industryType={type}
Authorization: Bearer {accessToken}
```

**Query Parameter**

| 파라미터 | 값 | 설명 |
|---|---|---|
| `industryType` | `건설` (기본) | 건설업 사고 사례 |
| `industryType` | `제조` | 제조업 사고 사례 |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "industryType": "건설",
    "cases": [
      {
        "title": "고소작업 추락 사망사고",
        "year": 2022,
        "cause": "안전대 미착용 및 작업발판 불량",
        "law": "산업안전보건법 제38조, 제63조",
        "source": "고용노동부 재해조사의견서"
      }
    ]
  }
}
```

---

### 8.2 안전 법령 검색

```
GET /api/public-data/laws?keyword={keyword}
Authorization: Bearer {accessToken}
```

**Query Parameter**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `keyword` | String | 검색어 (예: `추락`, `안전모`, `비계`) |

**Response (200)**
```json
{
  "success": true,
  "data": {
    "keyword": "추락",
    "laws": [
      {
        "title": "산업안전보건법",
        "article": "제38조(안전조치)",
        "content": "사업주는 추락하거나 넘어질 위험이 있는 장소에서 작업 시 안전난간, 울, 손잡이를 설치하여야 한다.",
        "category": "산업안전보건법"
      },
      {
        "title": "중대재해처벌법",
        "article": "제4조(사업주와 경영책임자등의 안전 및 보건 확보의무)",
        "content": "사업주 또는 경영책임자등은 사업주나 법인 또는 기관이 실질적으로 지배·운영·관리하는 사업 또는 사업장에서 안전·보건 관계 법령에 따른 의무를 이행해야 한다.",
        "category": "중대재해처벌법"
      }
    ]
  }
}
```

---

## 12. 에러 코드 목록

| code | HTTP | 설명 |
|---|---|---|
| `DUPLICATE_EMAIL` | 409 | 이미 가입된 이메일 |
| `SITE_NOT_FOUND` | 404 | 사업장 없음 또는 접근 권한 없음 |
| `WORKER_NOT_FOUND` | 404 | 근로자 없음 또는 접근 권한 없음 |
| `RISK_NOT_FOUND` | 404 | 위험 요소 없음 또는 접근 권한 없음 |
| `INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호 불일치 |
| `EXPIRED_TOKEN` | 401 | Access/Refresh Token 만료 |
| `INVALID_TOKEN` | 401 | 잘못된 토큰 형식 |
| `UNAUTHORIZED` | 403 | 접근 권한 없음 |
| `RESOURCE_NOT_FOUND` | 404 | 요청한 리소스 없음 |
| `VALIDATION_ERROR` | 400 | 요청 필드 유효성 검사 실패 |
| `FILE_SIZE_EXCEEDED` | 400 | 파일 크기 20MB 초과 |
| `UNSUPPORTED_FILE_TYPE` | 400 | 지원하지 않는 파일 형식 |
| `ANALYSIS_NOT_COMPLETED` | 409 | 분석이 아직 완료되지 않음 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 내부 오류 |

---

## 부록: Kotlin Retrofit 공통 설정 예시

```kotlin
// build.gradle.kts
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// ApiClient.kt
object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"  // 에뮬레이터

    private val tokenInterceptor = Interceptor { chain ->
        val token = TokenManager.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else chain.request()
        chain.proceed(request)
    }

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(
            OkHttpClient.Builder()
                .addInterceptor(tokenInterceptor)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()
        )
        .build()
}
```

```kotlin
// TokenRefreshInterceptor: 401 수신 시 자동 재발급
class TokenRefreshInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            val newToken = runBlocking { refreshToken() }
            if (newToken != null) {
                val newRequest = chain.request().newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                return chain.proceed(newRequest)
            }
        }
        return response
    }
}
```

```kotlin
// 공통 응답 래퍼
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?
)

data class ApiErrorResponse(
    val success: Boolean,
    val code: String,
    val message: String
)
```
