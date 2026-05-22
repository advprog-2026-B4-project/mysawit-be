#!/bin/sh
set -e

: "${BACKEND_HOST_MAIN:=mysawit-be-main}"
: "${BACKEND_HOST_STAGING:=mysawit-be-staging}"

sed \
  -e "s/BACKEND_HOST_MAIN_PLACEHOLDER/${BACKEND_HOST_MAIN}/g" \
  -e "s/BACKEND_HOST_STAGING_PLACEHOLDER/${BACKEND_HOST_STAGING}/g" \
  /tmp/prometheus.yml.tmpl > /tmp/prometheus.yml

echo "=== Prometheus config ==="
cat /tmp/prometheus.yml

exec /bin/prometheus \
  --config.file=/tmp/prometheus.yml \
  --storage.tsdb.path=/prometheus \
  --storage.tsdb.retention.time=30d \
  --web.enable-lifecycle
