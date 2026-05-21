# MySawit Backend — Makefile
# Works on Linux, macOS, and Windows (Git Bash / MSYS2).
#
# Quick start:
#   make help          Show all targets
#   make dev           Build & start backend + monitoring in Docker
#   make run           Start backend with Gradle bootRun
#   make test          Run all tests
#   make loadtest      Full load test suite (setup → run → cleanup)

# ---------------------------------------------------------------------------
# Platform detection
# ---------------------------------------------------------------------------
ifeq ($(OS),Windows_NT)
  GRADLE  = gradlew.bat
  PYTHON  = python
  RMDIR   = rmdir /S /Q
else
  GRADLE  = ./gradlew
  PYTHON  = python3
  RMDIR   = rm -rf
endif

# Load .env (don't fail if absent, useful for CI)
-include .env
export

# Default Gradle flags: daemon for speed, parallel for multi-core
GRADLE_FLAGS ?= --daemon --parallel

.DEFAULT_GOAL := help

.PHONY: help

help: ## Show this help
	@echo "MySawit Backend — available targets:"
	@echo ""
	@awk -F ':.*## ' \
	  '/^[a-zA-Z0-9_-]+:.*## / {printf "  \033[36m%-24s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# ===========================================================================
# Development
# ===========================================================================
.PHONY: dev run

dev: ## Build JAR + start backend, Prometheus, Grafana in Docker
	$(GRADLE) bootJar && docker compose up -d --build backend prometheus grafana

run: ## Start backend with Gradle bootRun (dev profile)
	$(GRADLE) bootRun $(GRADLE_FLAGS)

# ===========================================================================
# Build & Test
# ===========================================================================
.PHONY: build clean test lint

build: ## Compile, test, and package
	$(GRADLE) build $(GRADLE_FLAGS)

clean: ## Remove build artifacts
	$(GRADLE) clean

test: ## Run unit and integration tests
	$(GRADLE) test $(GRADLE_FLAGS)

lint: ## Check code style with Checkstyle (if configured)
	$(GRADLE) check $(GRADLE_FLAGS)

# ===========================================================================
# Data seeding & admin
# ===========================================================================
.PHONY: seed-payroll change-admin-password

seed-payroll: ## Seed payroll test data
	$(GRADLE) seedPayrollTestData

change-admin-password: ## Change admin password — make change-admin-password PASS=newpass
ifndef PASS
	$(error Usage: make change-admin-password PASS=<newPassword>)
endif
	$(GRADLE) changeAdminPassword -Ppassword="$(PASS)"

# ===========================================================================
# Docker infrastructure
# ===========================================================================
.PHONY: up down restart db-shell redis-shell logs ps

up: ## Start all services (Postgres, Redis, backend, Prometheus, Grafana, Loki, Alloy)
	docker compose up -d

down: ## Stop all services and remove volumes
	docker compose down -v --remove-orphans

restart: down up ## Full teardown and restart

db-shell: ## Open psql in the Postgres container
	docker compose exec postgres psql -U postgres -d mysawit

redis-shell: ## Open redis-cli in the Redis container
	docker compose exec redis redis-cli

logs: ## Tail all container logs
	docker compose logs -f

ps: ## Show running containers
	docker compose ps

# ===========================================================================
# Observability
# ===========================================================================
.PHONY: obs-up obs-down grafana prometheus

obs-up: ## Start monitoring stack only (Prometheus + Grafana + Loki + Alloy)
	docker compose up -d prometheus grafana loki alloy

obs-down: ## Stop monitoring stack
	docker compose stop prometheus grafana loki alloy

grafana: ## Open Grafana in browser (macOS / Linux)
	@echo "Grafana: http://localhost:3002 (admin / admin)"
	@command -v xdg-open >/dev/null 2>&1 && xdg-open http://localhost:3002 \
	  || command -v open   >/dev/null 2>&1 && open   http://localhost:3002 \
	  || true

prometheus: ## Open Prometheus in browser (macOS / Linux)
	@echo "Prometheus: http://localhost:9000"
	@command -v xdg-open >/dev/null 2>&1 && xdg-open http://localhost:9000 \
	  || command -v open   >/dev/null 2>&1 && open   http://localhost:9000 \
	  || true

# ===========================================================================
# Load testing
# ===========================================================================
define WAIT_POSTGRES
  @echo "Waiting for PostgreSQL..."
  @until docker compose exec -T postgres pg_isready -U postgres -d mysawit 2>/dev/null; do sleep 2; done
  @echo "PostgreSQL ready."
endef

define WAIT_BACKEND
  @echo "Waiting for backend /actuator/health..."
  @i=30; while [ $$i -gt 0 ]; do \
    curl -sf http://localhost:9090/actuator/health 2>/dev/null | grep -q '"status":"UP"' && break; \
    sleep 5; i=$$((i-1)); \
  done; [ $$i -gt 0 ] || (echo "ERROR: Backend did not start" && exit 1)
  @echo "Backend is UP."
endef

.PHONY: loadtest loadtest-setup loadtest-run loadtest-teardown

loadtest-setup: ## Setup: start infra, run migrations, seed data, generate JWT tokens
	docker compose down -v --remove-orphans 2>/dev/null || true
	docker compose up -d postgres redis
	$(WAIT_POSTGRES)
	docker compose up -d backend
	$(WAIT_BACKEND)
	docker compose exec -T postgres psql -U postgres -d mysawit < k6/seed-loadtest.sql
	docker compose exec -T postgres psql -U postgres -d mysawit -c "ANALYZE;"
	mkdir -p k6/data k6/results
	$(PYTHON) k6/generate-tokens.py > k6/data/test_data.csv
	docker compose up -d prometheus grafana
	@echo ""
	@echo "  Grafana  : http://localhost:3002  (admin / admin)"
	@echo "  Prometheus: http://localhost:9000"
	@echo "  Setup complete — run 'make loadtest-run' to start tests."

loadtest-run: ## Run all k6 load test scenarios (requires 'make loadtest-setup' first)
	k6 run --out json=k6/results/write-heavy.json k6/scripts/write-heavy.js
	k6 run --out json=k6/results/read-heavy.json k6/scripts/read-heavy.js
	k6 run --out json=k6/results/async-approval.json k6/scripts/async-approval.js

loadtest-teardown: ## Remove load test data from database
	docker compose exec -T postgres psql -U postgres -d mysawit < k6/cleanup-loadtest.sql

loadtest: loadtest-setup ## Full load test suite (setup → pause → run → cleanup)
	@echo ""
	@echo "  === Ready. Press Enter to start tests, Ctrl+C to abort ==="
	@read dummy
	$(MAKE) loadtest-run
	$(MAKE) loadtest-teardown
	@echo "  Results: k6/results/"
