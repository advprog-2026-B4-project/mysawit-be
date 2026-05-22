#!/bin/sh
set -e

MAIN="${BACKEND_HOST_MAIN:-mysawit-be-main}"
STAGING="${BACKEND_HOST_STAGING:-mysawit-be-staging}"

sed \
  -e "s/BACKEND_HOST_MAIN_PLACEHOLDER/${MAIN}/g" \
  -e "s/BACKEND_HOST_STAGING_PLACEHOLDER/${STAGING}/g" \
  /etc/prometheus/prometheus.yml > /tmp/prometheus.yml

exec /bin/prometheus \
  --config.file=/tmp/prometheus.yml \
  --storage.tsdb.path=/prometheus \
  --storage.tsdb.retention.time=30d \
  --storage.tsdb.wal-compression \
  --web.enable-lifecycle \
  --log.level=info
