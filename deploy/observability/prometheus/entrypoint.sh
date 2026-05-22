#!/bin/sh

echo "=== entrypoint start ==="
echo "BACKEND_HOST_MAIN=${BACKEND_HOST_MAIN}"
echo "BACKEND_HOST_STAGING=${BACKEND_HOST_STAGING}"
echo "--- /etc/prometheus ---"
ls /etc/prometheus/ || echo "ls FAILED"
echo "--- template ---"
cat /etc/prometheus/prometheus.yml || echo "cat FAILED"

MAIN="${BACKEND_HOST_MAIN:-mysawit-be-main}"
STAGING="${BACKEND_HOST_STAGING:-mysawit-be-staging}"

if sed \
  -e "s/BACKEND_HOST_MAIN_PLACEHOLDER/${MAIN}/g" \
  -e "s/BACKEND_HOST_STAGING_PLACEHOLDER/${STAGING}/g" \
  /etc/prometheus/prometheus.yml > /tmp/prometheus.yml; then
  echo "sed ok"
else
  echo "sed FAILED, falling back to template copy"
  cp /etc/prometheus/prometheus.yml /tmp/prometheus.yml
fi

[ -s /tmp/prometheus.yml ] || { echo "output is empty, aborting"; exit 1; }

echo "=== final config ==="
cat /tmp/prometheus.yml

exec /bin/prometheus \
  --config.file=/tmp/prometheus.yml \
  --storage.tsdb.path=/prometheus \
  --storage.tsdb.retention.time=30d \
  --web.enable-lifecycle
