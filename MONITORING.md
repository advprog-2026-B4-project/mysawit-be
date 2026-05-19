# Monitoring — Prometheus + Grafana

## How it works

The Spring Boot app exposes metrics at `:9090/actuator/prometheus` (separate management port from the API on `:8080`). Prometheus scrapes that endpoint every 15 s. Grafana reads from Prometheus and displays dashboards.

```
Browser → Grafana :3000 → Prometheus :9090 → backend :9090/actuator/prometheus
```

---

## Local dev (`docker compose up`)

```
Grafana  → http://localhost:3002   (admin / admin by default)
Prometheus → http://localhost:9000
Metrics endpoint → http://localhost:9090/actuator/prometheus
```

Prometheus datasource and folder provider are pre-wired via `grafana/provisioning/`. Import dashboard **4701** (JVM Micrometer) from grafana.com for an instant Spring Boot overview.

---

## Production on Coolify

Coolify runs each service as a separate Docker resource. You deploy three services manually rather than using the full `docker-compose.yml` (Coolify has its own managed postgres/redis you can reuse).

### 1. Backend env vars to set in Coolify

| Variable | Value |
|----------|-------|
| `MANAGEMENT_PORT` | `9090` |
| `SERVER_PORT` | `8080` |

The management port **must be open** inside the Coolify network so Prometheus can reach it. In Coolify, add port `9090` to the backend service's exposed ports (no public domain needed — internal only).

### 2. Deploy Prometheus

In Coolify → **New Service → Docker Image** → `prom/prometheus:latest`

- **Port:** 9090 (internal only, no public domain required unless you want a UI)
- **Volume:** mount a persistent volume to `/prometheus`
- **Config file:** paste the contents of `prometheus.yml` into a Coolify config-file mount at `/etc/prometheus/prometheus.yml`, changing the target to the **internal Coolify hostname** of your backend service:

```yaml
scrape_configs:
  - job_name: mysawit-be
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['<backend-internal-hostname>:9090']
```

Coolify gives every service an internal hostname shown in the service's **Network** tab (e.g. `mysawit-backend`). Use that.

- **Start command:** `--config.file=/etc/prometheus/prometheus.yml --storage.tsdb.retention.time=15d`

### 3. Deploy Grafana

In Coolify → **New Service → Docker Image** → `grafana/grafana:latest`

- **Port:** 3000
- **Domain:** assign a public domain (e.g. `grafana.yourdomain.com`) through Coolify — it will handle TLS automatically
- **Volumes:** mount a persistent volume to `/var/lib/grafana`
- **Environment variables:**

| Variable | Value |
|----------|-------|
| `GF_SECURITY_ADMIN_USER` | `admin` |
| `GF_SECURITY_ADMIN_PASSWORD` | a strong password |
| `GF_USERS_ALLOW_SIGN_UP` | `false` |
| `GF_SERVER_ROOT_URL` | `https://grafana.yourdomain.com` |

- **Config mount:** mount `grafana/provisioning/datasources/prometheus.yml` to `/etc/grafana/provisioning/datasources/prometheus.yml`, editing the Prometheus URL to the **internal Coolify hostname** of your Prometheus service:

```yaml
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://<prometheus-internal-hostname>:9090
    isDefault: true
```

### 4. Grafana dashboard

After Grafana is up, log in and import dashboard ID **4701** (JVM Micrometer) from grafana.com. It uses the standard Micrometer metric names that Spring Boot exports and will work without any extra configuration.

### Network summary

```
Public internet
  └─ Grafana (public domain, :443 via Coolify proxy)
       └─ Prometheus (internal :9090)
            └─ Backend management (internal :9090/actuator/prometheus)

Public internet
  └─ Backend API (public domain, :443 → :8080)
```

Prometheus and the backend management port are **never exposed publicly** — only Grafana and the API are.
