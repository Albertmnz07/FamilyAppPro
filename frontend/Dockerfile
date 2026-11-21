# syntax=docker/dockerfile:1

# Front-end image (TN-27): a `dev` target for the Vite dev server used by
# compose.dev.yml and a `build` target that emits the static production bundle
# into /app/dist. Both share a `deps` stage so `npm ci` runs once and stays
# cached until the lockfile changes. Production is served by the back-end image
# (ADR-0006), so no runtime/web-server stage is defined here.

FROM node:24-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# Development: Vite bound to 0.0.0.0:5173 so the host browser can reach it.
# compose.dev.yml bind-mounts the source over /app and keeps this image's
# /app/node_modules, so edits hot-reload without a rebuild.
FROM deps AS dev
COPY . .
ENV VITE_API_BASE_URL=http://localhost:8070/api/v1
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "5173"]

# Production build: `npm run build` (tsc -b && vite build) writes /app/dist with
# the API base baked in, so one bundle can be built per environment.
FROM deps AS build
ARG VITE_API_BASE_URL=/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
COPY . .
RUN npm run build
