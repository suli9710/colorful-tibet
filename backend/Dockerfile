# Multi-stage build: compile with Maven, then run with a slim JRE image.
FROM maven:3.9-eclipse-temurin-17@sha256:e8ef73dbd33b69fe497fd96b3bbbd85aff84ac4c564a5784ab02ad941b32c12b AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine@sha256:b0ae54a36f82e04dc6c45e40ca5c55762e20b9a0858ee457faf557d440a9b571
WORKDIR /app

ARG APP_UID=10001
ARG APP_GID=10001

# Install runtime tools and create the non-root application user.
RUN apk add --no-cache curl \
    && addgroup -S -g "$APP_GID" app \
    && adduser -S -D -H -u "$APP_UID" -G app app \
    && mkdir -p /app/data /app/logs \
    && chown -R app:app /app

COPY --from=builder --chown=app:app /app/target/tourism-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
