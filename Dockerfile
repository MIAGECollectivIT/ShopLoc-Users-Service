# ==========================================
# Stage 1: Build the application (Maven + Eclipse Temurin 21)
# ==========================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache Maven dependencies (BuildKit Cache Mount)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:resolve

# Compile and package the runnable JAR
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests

# ==========================================
# Stage 2: Minimal runtime image ARM64 / Multi-Arch
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Security: Execute as a dedicated non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && chown appuser:appgroup /app

# Copy the executable JAR from the builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

USER appuser

# AppCDS Training at container build time (Architecture-dependent, Non-blocking)
RUN java -XX:ArchiveClassesAtExit=/app/app.jsa \
      -Dspring.context.exit=onRefresh \
      -Dspring.flyway.enabled=false \
      -Dspring.jpa.hibernate.ddl-auto=none \
      -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
      -jar app.jar > /tmp/cds-training.log 2>&1 \
    || (echo "AppCDS training failed (non bloquant):" && tail -n 20 /tmp/cds-training.log)

# Standard listening port
EXPOSE 8080

# JVM options optimized for containers (K3s / cgroups) + AppCDS
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
