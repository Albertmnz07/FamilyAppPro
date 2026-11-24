# FamilyAppPro developer commands. One place for the day-to-day loop: the
# container stacks, the test suites, the database reset and the single-artifact
# build. Run `make` on its own to list every target.
#
# The two compose files publish the same port (8070), so only one runs at a
# time. The production `up` targets recreate only the services whose image or
# config changed, so repeated deploys never pile up containers.
#
# Data lives outside the containers: the `pgdata` named volume holds the
# database and the bind-mounted `./data/photos` holds the DNI photos. `down`
# keeps both; the reset targets drop the database volume and leave the photos.

DEV_COMPOSE  := docker compose -f compose.dev.yml
PROD_COMPOSE := docker compose -f compose.prod.yml
JAR          := apps/backend/target/familyapp-0.0.1-SNAPSHOT.jar
BACKEND      := apps/backend
FRONTEND     := apps/frontend

.DEFAULT_GOAL := help

.PHONY: help setup \
        dev dev-detached dev-down dev-restart dev-logs \
        prod prod-start prod-rebuild prod-down prod-logs \
        db-reset-dev db-reset-prod \
        test test-backend test-frontend check \
        format format-check \
        build run clean

## help: list the available targets
help:
	@grep -E '^## ' $(MAKEFILE_LIST) | sed 's/## //'

# --- Setup ------------------------------------------------------------------

## setup: enable the repository git hooks in this clone (required once)
setup:
	git config core.hooksPath .githooks

# --- Containers -------------------------------------------------------------

## dev: start the development stack (Postgres, API, Vite) and follow the logs
dev:
	$(DEV_COMPOSE) up --build

## dev-detached: start the development stack in the background
dev-detached:
	$(DEV_COMPOSE) up -d --build

## dev-down: stop the development stack (keeps pgdata and data/photos)
dev-down:
	$(DEV_COMPOSE) down

## dev-restart: restart the API to recompile after editing Java sources
dev-restart:
	$(DEV_COMPOSE) restart api

## dev-logs: follow the development stack logs
dev-logs:
	$(DEV_COMPOSE) logs -f

## prod: rebuild and redeploy the changed production services
prod:
	$(PROD_COMPOSE) up -d --build

## prod-start: start the production stack without rebuilding images
prod-start:
	$(PROD_COMPOSE) up -d --no-build

## prod-rebuild: force a full rebuild of the API image, then redeploy
prod-rebuild:
	$(PROD_COMPOSE) build --no-cache api
	$(PROD_COMPOSE) up -d

## prod-down: stop the production stack (keeps pgdata and data/photos)
prod-down:
	$(PROD_COMPOSE) down

## prod-logs: follow the production API logs
prod-logs:
	$(PROD_COMPOSE) logs -f api

# --- Database ---------------------------------------------------------------

## db-reset-dev: recreate the development database from scratch (photos kept)
db-reset-dev:
	$(DEV_COMPOSE) down -v
	$(DEV_COMPOSE) up -d --build

## db-reset-prod: recreate the production database from scratch (photos kept)
db-reset-prod:
	$(PROD_COMPOSE) down -v
	$(PROD_COMPOSE) up -d --build

# --- Tests ------------------------------------------------------------------

## test: run the backend and front-end test suites
test: test-backend test-frontend

## test-backend: run the backend suite (H2 in PostgreSQL mode)
test-backend:
	cd $(BACKEND) && ./mvnw test

## test-frontend: typecheck, lint and run the front-end suite
test-frontend:
	npm --prefix $(FRONTEND) run check

## check: alias for test-frontend, matching the git hook
check: test-frontend

# --- Formatting -------------------------------------------------------------

## format: apply Spotless (back-end) and Prettier (front-end) to the whole tree
format:
	cd $(BACKEND) && ./mvnw -q spotless:apply
	npm --prefix $(FRONTEND) run format

## format-check: verify formatting without writing (what pre-push runs)
format-check:
	cd $(BACKEND) && ./mvnw -q spotless:check
	npm --prefix $(FRONTEND) run format:check

# --- Build and run ----------------------------------------------------------

## build: build the single artifact with the front-end inside the jar
build:
	cd $(BACKEND) && ./mvnw package

## run: build the artifact and run it on http://localhost:8070
run: build
	java -jar $(JAR)

## clean: remove build artifacts (apps/backend/target and apps/frontend/dist)
clean:
	cd $(BACKEND) && ./mvnw clean
