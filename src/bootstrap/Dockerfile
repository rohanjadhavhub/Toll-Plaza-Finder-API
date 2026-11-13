# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src/main ./src/main
COPY src/test ./src/test
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode --no-transfer-progress package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home app

COPY --from=build --chown=app:app /workspace/target/toll-plaza-finder-*.jar /app/application.jar

ENV SERVER_ADDRESS=0.0.0.0 \
    PORT=8080
USER app:app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/application.jar"]
