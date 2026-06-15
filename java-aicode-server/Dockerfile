# ============================================
# 本地 docker-compose 后端镜像 (Java 8 + Playwright)
# 构建方式: 由 docker-compose.yml 自动构建
# ============================================

FROM maven:3.6.3-jdk-8-slim AS build
WORKDIR /app
COPY pom.xml .
# 预下载依赖（利用 Docker 缓存加速重复构建）
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn package -Dmaven.test.skip=true -B -q

# Playwright 官方镜像已内置 Chromium + 所有系统依赖，仅需补装 JDK 8
FROM mcr.microsoft.com/playwright:v1.41.0-focal
RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-8-jre-headless curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8092
HEALTHCHECK --interval=30s --timeout=3s --retries=5 \
    CMD curl -f http://localhost:8092/api/system/get-ip || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
