@echo off
REM =============================================================================
REM MySawit Load Test Suite Orchestrator (Windows)
REM
REM Usage (from mysawit-be\ directory):
REM   run-loadtest-suite.bat
REM
REM Prerequisites:
REM   - Docker Desktop
REM   - k6  (https://k6.io/docs/get-started/installation/)
REM   - Python 3.9+ with: pip install psycopg2-binary PyJWT python-dotenv
REM   - curl (available in Windows 10/11 by default)
REM =============================================================================

setlocal enabledelayedexpansion

pushd "%~dp0"

if not exist "k6\data"    mkdir "k6\data"
if not exist "k6\results" mkdir "k6\results"

REM ---------------------------------------------------------------------------
echo.
echo === 1. TEARDOWN - Stop containers and wipe volumes ===
docker compose down -v --remove-orphans

REM ---------------------------------------------------------------------------
echo.
echo === 2. START DB and REDIS ===
docker compose up -d postgres redis

echo Waiting for PostgreSQL to be ready...
:wait_postgres
docker compose exec -T postgres pg_isready -U postgres -d mysawit >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
  timeout /t 2 /nobreak >nul
  goto wait_postgres
)
echo PostgreSQL is ready.

REM ---------------------------------------------------------------------------
echo.
echo === 3. START BACKEND (Spring Boot + Flyway) ===
docker compose up -d backend

echo Waiting for backend health check...
set /a RETRIES=30
:wait_backend
curl -sf http://localhost:9090/actuator/health 2>nul | findstr "UP" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
  echo Backend is UP.
  goto backend_ready
)
set /a RETRIES=RETRIES-1
if %RETRIES% EQU 0 (
  echo [ERROR] Backend did not start. Check: docker compose logs backend
  exit /b 1
)
timeout /t 5 /nobreak >nul
goto wait_backend
:backend_ready

REM ---------------------------------------------------------------------------
echo.
echo === 4. SEED DATABASE ===
docker compose exec -T postgres psql -U postgres -d mysawit < k6\seed-loadtest.sql
if %ERRORLEVEL% NEQ 0 (
  echo [ERROR] Seeding failed. Aborting.
  exit /b 1
)

REM ---------------------------------------------------------------------------
echo.
echo === 5. ANALYZE DATABASE ===
docker compose exec -T postgres psql -U postgres -d mysawit -c "ANALYZE;"

REM ---------------------------------------------------------------------------
echo.
echo === 6. GENERATE JWT TOKENS ===
python k6\generate-tokens.py > k6\data\test_data.csv
if %ERRORLEVEL% NEQ 0 (
  echo [ERROR] Token generation failed. Check Python dependencies.
  exit /b 1
)
echo Tokens written to k6\data\test_data.csv

REM ---------------------------------------------------------------------------
echo.
echo === 7. START MONITORING STACK ===
docker compose up -d prometheus grafana

echo.
echo   Grafana   : http://localhost:3002  (admin / admin)
echo   Prometheus: http://localhost:9000
echo.
echo   Open Grafana JVM Heap dashboard before continuing.
echo.
pause

REM ---------------------------------------------------------------------------
echo.
echo === 8a. SKENARIO 1 - Write-Heavy (500 VU, 8 min) ===
k6 run --out json=k6/results/write-heavy.json k6/scripts/write-heavy.js

echo.
echo === 8b. SKENARIO 2 - Read-Heavy (100 VU, 7 min) ===
k6 run --out json=k6/results/read-heavy.json k6/scripts/read-heavy.js

echo.
echo === 8c. SKENARIO 3 - Async Approval (20 VU, 30 min) ===
echo   Monitor JVM Heap in Grafana during this run.
k6 run --out json=k6/results/async-approval.json k6/scripts/async-approval.js

REM ---------------------------------------------------------------------------
echo.
echo === 9. CLEANUP ===
docker compose exec -T postgres psql -U postgres -d mysawit < k6\cleanup-loadtest.sql

REM ---------------------------------------------------------------------------
echo.
echo ===================================================================
echo  LOAD TEST SUITE COMPLETE
echo  Results : k6\results\
echo ===================================================================

popd
endlocal
