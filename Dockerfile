# syntax=docker/dockerfile:1

# Back-end image (TN-26): a `runtime` target that serves the API and the built
# front-end from one jar (ADR-0006), and a `dev` target for compose.dev.yml.
# The build context is the repository root, because the Spring Boot project and
# the front-end source both live there.

# Node 24 is copied into the Maven builder because the `package` phase runs
# `npm ci` and `npm run build` in frontend/ (see pom.xml).
FROM node:24-bookworm-slim AS node

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
    && ln -s /usr/local/lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx \
    && node --version \
    && npm --version

COPY . .
RUN ./mvnw package -DskipTests

# Runtime: only the jar and a JRE. The front-end is already inside the jar's
# static resources, so no Node or Maven reaches production.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=builder /workspace/target/*.jar /app/app.jar
EXPOSE 8070
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Development: `spring-boot:run` over a bind-mounted source tree. compose.dev.yml
# mounts the repository at /workspace and keeps the image's Maven cache. The
# base image already provides Maven, so the dev target skips the wrapper.
FROM maven:3.9-eclipse-temurin-21 AS dev
WORKDIR /workspace
EXPOSE 8070
CMD ["mvn", "spring-boot:run"]
