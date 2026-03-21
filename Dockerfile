# ── Stage 1: Build the Spring Boot JAR ────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper + pom first (layer cache for dependencies)
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Ensure mvnw is executable (git may not preserve the bit on Windows checkouts)
RUN chmod +x mvnw

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Minimal runtime image ────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Non-root user for security
RUN addgroup -S llmops && adduser -S llmops -G llmops

# Copy JAR from builder
COPY --from=builder /app/target/*.jar app.jar

RUN chown llmops:llmops app.jar
USER llmops

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/auth/health || exit 1

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
