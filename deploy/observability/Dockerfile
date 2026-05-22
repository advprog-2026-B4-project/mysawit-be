FROM prom/prometheus:v2.55.0

USER root
COPY prometheus/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
COPY prometheus/prometheus.yml /etc/prometheus/prometheus.yml
COPY prometheus/alert_rules.yml /etc/prometheus/alert_rules.yml
USER nobody

EXPOSE 9090

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO /dev/null http://localhost:9090/-/healthy || exit 1

ENTRYPOINT ["/bin/sh", "/entrypoint.sh"]
