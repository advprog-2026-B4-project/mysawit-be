#!/usr/bin/env bash
# =============================================================================
# MySawit Load Test Suite Orchestrator
#
# Usage (from mysawit-be/ directory):
#   chmod +x run-loadtest-suite.sh
#   ./run-loadtest-suite.sh
#
# Prerequisites:
#   - Docker + Docker Compose
#   - k6  (https://k6.io/docs/get-started/installation/)
#   - Python 3.9+ with: pip install psycopg2-binary PyJWT python-dotenv
# =============================================================================

set -e  # Exit immediately on any error — prevents k6 from running on bad seed

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

mkdir -p k6/data k6/results

# ---------------------------------------------------------------------------
echo ""
echo "=== 1. TEARDOWN — Stop containers and wipe volumes ==="
docker compose down -v --remove-orphans || true

# ---------------------------------------------------------------------------
echo ""
echo "=== 2. START DB & REDIS ==="
docker compose up -d postgres redis

echo "Waiting for PostgreSQL to be ready..."
until docker compose exec -T postgres pg_isready -U postgres -d mysawit 2>/dev/null; do
  sleep 2
done
echo "PostgreSQL is ready."

# ---------------------------------------------------------------------------
echo ""
echo "=== 3. START BACKEND (Spring Boot + Flyway migrations) ==="
docker compose up -d backend

echo "Waiting for backend /actuator/health to return UP..."
RETRIES=30
while [ $RETRIES -gt 0 ]; do
  STATUS=$(curl -sf http://localhost:9090/actuator/health 2>/dev/null | grep -o '"status":"UP"' || true)
  if [ -n "$STATUS" ]; then
    echo "Backend is UP."
    break
  fi
  RETRIES=$((RETRIES - 1))
  sleep 5
done

if [ $RETRIES -eq 0 ]; then
  echo "[ERROR] Backend did not start in time. Check: docker compose logs backend"
  exit 1
fi

# ---------------------------------------------------------------------------
echo ""
echo "=== 4. SEED DATABASE ==="
docker compose exec -T postgres psql -U postgres -d mysawit < k6/seed-loadtest.sql

# ---------------------------------------------------------------------------
echo ""
echo "=== 5. ANALYZE DATABASE ==="
docker compose exec -T postgres psql -U postgres -d mysawit -c "ANALYZE;"

# ---------------------------------------------------------------------------
echo ""
echo "=== 6. GENERATE JWT TOKENS ==="
python3 k6/generate-tokens.py > k6/data/test_data.csv
echo "Tokens written to k6/data/test_data.csv ($(wc -l < k6/data/test_data.csv) rows)"

# ---------------------------------------------------------------------------
echo ""
echo "=== 7. START MONITORING STACK ==="
docker compose up -d prometheus grafana

echo ""
echo "  Grafana : http://localhost:3002  (admin / admin)"
echo "  Prometheus: http://localhost:9000"
echo ""
echo "  BEFORE CONTINUING:"
echo "  Open Grafana and navigate to the JVM dashboard."
echo "  You need the 'JVM Heap Used' panel open BEFORE Skenario 3 starts."
echo ""
read -r -p "Press Enter when Grafana is open and you are ready to start tests... "

# ---------------------------------------------------------------------------
echo ""
echo "=== 8a. SKENARIO 1 — Write-Heavy (500 VU, 8 min total) ==="
k6 run --out json=k6/results/write-heavy.json k6/scripts/write-heavy.js

echo ""
echo "=== 8b. SKENARIO 2 — Read-Heavy (100 VU, 7 min total) ==="
k6 run --out json=k6/results/read-heavy.json k6/scripts/read-heavy.js

echo ""
echo "=== 8c. SKENARIO 3 — Async Approval (20 VU, 30 min) ==="
echo "  Monitor JVM Heap in Grafana during this run."
k6 run --out json=k6/results/async-approval.json k6/scripts/async-approval.js

# ---------------------------------------------------------------------------
echo ""
echo "=== 9. CLEANUP — Remove load test data ==="
docker compose exec -T postgres psql -U postgres -d mysawit < k6/cleanup-loadtest.sql

# ---------------------------------------------------------------------------
echo ""
echo "==================================================================="
echo " LOAD TEST SUITE COMPLETE"
echo "  Results : k6/results/"
echo "  Summaries: k6/results/*-summary.json"
echo "==================================================================="
