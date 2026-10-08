# ==============================================================================
# AuthEase - Multi-Stage Container Build
# Target Architecture: JVM 17 Runtime on Alpine Linux
# Default Memory Cap: 350MB Heap for Render/Railway/Fly.io Free Tiers
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Maven Builder
# ------------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Cache dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true

# Copy source code and static UI resources
COPY src ./src

# Build production executable uber-jar
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal Distroless / Hardened Alpine Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

# Security: Create non-root system user and group
RUN addgroup -S authease && adduser -S authease -G authease

WORKDIR /app

# Copy artifact from builder stage
COPY --from=builder --chown=authease:authease /build/target/authease-1.0.0-SNAPSHOT.jar /app/authease.jar

# Security & Operations Configuration
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-Xmx350m -Xms128m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Switch to unprivileged user
USER authease:authease

# Service port
EXPOSE 8080

# Container Healthcheck (matches GET /api/health)
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/authease.jar"]
