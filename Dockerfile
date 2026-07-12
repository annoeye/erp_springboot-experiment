# =============================================================================
# Stage 1: Build dependencies (layer caching optimization)
# =============================================================================
FROM eclipse-temurin:21-jdk AS deps
WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies (layer cached unless pom.xml changes)
RUN --mount=type=cache,target=/root/.m2,rw \
    ./mvnw dependency:go-offline -B

# =============================================================================
# Stage 2: Build the application
# =============================================================================
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY --from=deps /app/.mvn .mvn/
COPY --from=deps /app/mvnw .
COPY --from=deps /app/pom.xml .
COPY src/ src/

RUN --mount=type=cache,target=/root/.m2,rw ./mvnw clean package -Dmaven.test.skip=true -B

# =============================================================================
# Stage 3: Production runtime (minimal image)
# =============================================================================
FROM eclipse-temurin:21-jre AS production
WORKDIR /app

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# JVM options (configurable via environment)
ARG JAVA_OPTS="-Xmx512m -XX:+UseG1GC -XX:+UseStringDeduplication"
ENV JAVA_OPTS=${JAVA_OPTS}
ENV SPRING_PROFILES_ACTIVE=docker

# Copy only the JAR from build stage
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -sSf http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
