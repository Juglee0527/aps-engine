FROM gradle:8.14.4-jdk21-alpine AS builder

WORKDIR /workspace

COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle src ./src

RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S aps \
    && adduser -S -G aps aps

WORKDIR /app

COPY --from=builder \
    --chown=aps:aps \
    /workspace/build/libs/aps-engine-0.0.1-SNAPSHOT.jar \
    /app/aps-engine.jar

USER aps

EXPOSE 8080

HEALTHCHECK \
    --interval=10s \
    --timeout=3s \
    --start-period=30s \
    --retries=6 \
    CMD wget --quiet --spider http://127.0.0.1:8080/actuator/health \
        || exit 1

ENTRYPOINT ["java", "-jar", "/app/aps-engine.jar"]
