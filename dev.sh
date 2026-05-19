#!/bin/sh
./gradlew bootJar && docker compose up -d --build backend prometheus grafana
