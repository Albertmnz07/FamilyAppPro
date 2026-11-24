# syntax=docker/dockerfile:1

# Back-end image (TN-26): a `runtime` target that serves the API and the built
# front-end from one jar (ADR-0006), and a `dev` target for compose.dev.yml.
# The build context is the repository root, because the Spring Boot project and
# the front-end source both live there.

# Node 24 is copied into the Maven builder because the `package` phase runs
# `npm ci` and `npm run build` in apps/frontend/ (see pom.xml).
FROM node:24-bookworm-slim AS node

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s /usr/local/lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm \
    && ln -s /usr/local/lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx \
    && node --version \
    && npm --version

# The production build bakes the API base URL into the SPA. The default keeps
# the standalone image usable on localhost; compose.prod.yml overrides it with
# --build-arg VITE_API_BASE_URL=/api/v1 for same-origin LAN deployments.
ARG VITE_API_BASE_URL=http://localhost:8070/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# --- Dependency layers -------------------------------------------------------
# Copy only the files that describe the dependencies before the source. Docker
# caches these layers as long as the POM and the lockfile do not change, so a
# Java-only or TypeScript-only change reuses the downloaded Maven and npm
# artifacts instead of fetching them again. This is what makes a code-only
# redeploy fast.
COPY apps/backend/.mvn/ apps/backend/.mvn/
COPY apps/backend/mvnw apps/backend/mvnw.cmd apps/backend/pom.xml apps/backend/
RUN ./apps/backend/mvnw -f apps/backend/pom.xml -B -q dependency:go-offline

COPY apps/frontend/package.json apps/frontend/package-lock.json apps/frontend/
RUN npm ci --prefix apps/frontend

# --- Source layer ------------------------------------------------------------
# Everything else. A source change invalidates this layer and the package step
# below, but the dependency layers above stay cached. The package phase still
# runs `npm ci` (see pom.xml); it now resolves from the warmed npm cache and
# reinstalls only when the lockfile changes.
COPY . .
RUN ./apps/backend/mvnw -f apps/backend/pom.xml -B package -DskipTests

# Runtime: only the jar and a JRE. The front-end is already inside the jar's
# static resources, so no Node or Maven reaches production.
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY --from=builder /workspace/apps/backend/target/*.jar /app/app.jar
EXPOSE 8070
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Development: `spring-boot:run` over a bind-mounted source tree. compose.dev.yml
# mounts the repository at /workspace and keeps the image's Maven cache. The
# wrapper keeps dev on the same Maven 3.9.9 as the builder.
FROM maven:3.9-eclipse-temurin-21 AS dev
WORKDIR /workspace/apps/backend
EXPOSE 8070
CMD ["./mvnw", "spring-boot:run"]
