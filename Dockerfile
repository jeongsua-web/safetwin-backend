# ── Stage 1: Build ──────────────────────────────────────────
FROM gradle:8.7-jdk17-alpine AS builder

WORKDIR /app

# 의존성 캐시 레이어: build.gradle만 먼저 복사
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

# 소스 전체 복사 후 빌드 (테스트 제외)
COPY src ./src
RUN gradle build -x test --no-daemon

# ── Stage 2: Runtime ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 한글 폰트 지원
RUN apk add --no-cache fontconfig ttf-dejavu

# 빌드 결과물 복사
COPY --from=builder /app/build/libs/safetwin-0.0.1-SNAPSHOT.jar app.jar

# 비루트 유저로 실행
RUN addgroup -S safetwin && adduser -S safetwin -G safetwin
USER safetwin

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
