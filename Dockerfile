# ---- Build Stage ----
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 캐싱 레이어 (소스 변경 시 재다운로드 방지)
RUN ./gradlew dependencies --no-daemon 2>/dev/null || true

COPY src src

# 테스트 스킵하고 빌드 (CI에서 테스트는 별도 job에서 실행)
RUN ./gradlew bootJar -x test -x asciidoctor --no-daemon

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# 파일 업로드 저장 경로
RUN mkdir -p /app/files

COPY --from=builder /app/build/libs/seboard-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
