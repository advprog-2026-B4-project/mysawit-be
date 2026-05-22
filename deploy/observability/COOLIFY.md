# MySawit Observability — Coolify Deployment Guide

## Architecture

```
┌─────────────┐     scrape /actuator/prometheus     ┌──────────────┐
│  mysawit-be │ ────────────────────────────────────→│  Prometheus   │
│  (main)     │                                      │  :9090        │
└─────────────┘                                      └──────┬───────┘
                                                           │ alerts
┌─────────────┐     scrape /actuator/prometheus     ┌──────▼───────┐
│  mysawit-be │ ────────────────────────────────────→│ Alertmanager │──→ Discord
│  (staging)  │                                      │  :9093       │   webhooks
└─────────────┘                                      └──────────────┘

┌─────────────┐     read /var/log/mysawit/*.log     ┌──────┐     ┌──────┐
│  Alloy      │ ────────────────────────────────────→│ Loki │────→│Grafana│
└─────────────┘                                      │:3100 │     │:3000 │
                                                     └──────┘     └──────┘
```

**4 services total** — Prometheus, Alertmanager, Grafana, Loki (+ Alloy for log shipping).

---

## Prerequisites

- A Coolify server with Docker installed
- Your backend apps (`mysawit-be-main`, `mysawit-be-staging`) already deployed on Coolify and reachable by hostname
- Discord webhook URLs for alert notifications
- Port `3000` accessible if you want to reach Grafana externally

---

## Step 1 — Create Environment File

Create `.env` in the `deploy/observability/` directory:

```bash
# ── Backend targets (hostnames as reachable from within Coolify's Docker network) ──
BACKEND_HOST_MAIN=mysawit-be-main        # container or service hostname
BACKEND_HOST_STAGING=mysawit-be-staging

# ── Grafana admin credentials ──────────────────────────────────────────────────
GF_ADMIN_USER=admin
GF_ADMIN_PASSWORD=<generate-a-strong-password>

# ── Grafana public URL (for OAuth callbacks, share links) ──────────────────────
GF_ROOT_URL=https://grafana-mysawit.yourdomain.com

# ── Discord webhooks for alerts ────────────────────────────────────────────────
DISCORD_WEBHOOK_URL_CRITICAL=https://discord.com/api/webhooks/...
DISCORD_WEBHOOK_URL_WARNING=https://discord.com/api/webhooks/...
```

> **Important**: `BACKEND_HOST_MAIN` and `BACKEND_HOST_STAGING` must match the container/service hostnames as resolvable from within Coolify's Docker network. For Coolify, this is typically the service name you gave the app in the Coolify dashboard.

---

## Step 2 — Deploy on Coolify

1. Open your Coolify dashboard
2. Go to **Projects** → select your project (or create one)
3. Click **+ New** → **Docker Compose**
4. Fill in:
   - **Name**: `mysawit-observability`
   - **Base Directory**: `deploy/observability`
   - **Docker Compose File**: select or paste the compose file
5. Under **Environment Variables**, paste the contents of your `.env` file
6. Under **Domains**, add a domain for Grafana (e.g. `grafana-mysawit.yourdomain.com`) pointing to port `3000`
7. Click **Deploy**

Coolify will:
- Pull images for Prometheus, Alertmanager, Grafana, Loki, Alloy
- Template the configs with your env vars
- Start all services with health checks
- Route the Grafana domain through Coolify's built-in proxy

---

## Step 3 — Verify

### 3.1 Check service health

```bash
# From the Coolify terminal (or SSH into the server)
docker compose -f deploy/observability/docker-compose.yml ps
```

All 5 services should show `healthy`.

### 3.2 Check Prometheus targets

Visit `https://grafana-mysawit.yourdomain.com` → login → go to **Explore** → select **Prometheus** → query:

```promql
up
```

You should see `up=1` for `mysawit-be-main` and `mysawit-be-staging`.

### 3.3 Check alerts

```promql
ALERTS
```

Should show any firing alerts (initially empty if everything is healthy).

### 3.4 Check logs

In Grafana Explore, switch to **Loki** datasource and run:

```logql
{composite_name=~".+"}
```

You should see application logs if Alloy has access to your app's log directory.

### 3.5 Dashboard

Open **Dashboards** → **MySawit** folder. Two dashboards are provisioned:
- **MySawit Backend** — latency histograms, error rates, JVM metrics, APDEX
- **MySawit Business** — payroll pending count, wallet balances

---

## Alert Rules (pre-configured)

| Alert | Severity | Trigger |
|---|---|---|
| `PanenCreateLatencyHigh` | critical | p95 createPanen > 300ms for 5m |
| `PanenApproveLatencyHigh` | critical | p95 approvePanen > 300ms for 5m |
| `HighServerErrorRate` | critical | HTTP 5xx > 1% for 5m |
| `MySawitBackendDown` | critical | Target unreachable for 1m |
| `ApdexScoreLow` | warning | APDEX < 0.7 for 10m |
| `PayrollPendingBacklog` | warning | > 50 PENDING payrolls for 15m |

Critical alerts go to the `DISCORD_WEBHOOK_URL_CRITICAL` channel. Warnings go to `DISCORD_WEBHOOK_URL_WARNING`.

---

## Retention

| Data | Retention |
|---|---|
| Prometheus metrics | 30 days |
| Loki logs | 7 days |
| Alertmanager silences | Persistent (Docker volume) |

Adjust in `prometheus.yml` (`--storage.tsdb.retention.time`) or `loki-config.yaml` (`retention_period`).

---

## Troubleshooting

### Alertmanager shows "no alerts"
Prometheus evaluates rules every 60s. Wait a minute after deployment. If still empty, check `Prometheus → Alerts` in Grafana.

### Alloy can't find logs
Ensure the host directory `/var/log/mysawit/` exists and contains `.log` files. If your app writes logs to a different path, update the `path_targets` in `alloy/config.alloy` and update the volume mount.

### "Hostname not resolved" for backend targets
The `BACKEND_HOST_MAIN` / `BACKEND_HOST_STAGING` values must be resolvable from within the Docker network. For Coolify, use the exact service name from the Coolify dashboard. If unsure, SSH into the server and run `docker network ls` then `docker network inspect <coolify-network>` to see connected services.

### Grafana anonymous access
By default anonymous access is disabled. Edit `GF_AUTH_ANONYMOUS_ENABLED=true` in the compose env if you want public dashboards (not recommended).
