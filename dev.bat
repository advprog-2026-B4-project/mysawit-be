@echo off
call gradlew.bat bootJar && docker compose up -d --build backend prometheus grafana
