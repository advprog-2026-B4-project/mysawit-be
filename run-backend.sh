#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [ -f ".env" ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%$'\r'}"
    [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
    [[ "$line" == export\ * ]] && line="${line#export }"

    key="${line%%=*}"
    value="${line#*=}"

    if [[ -n "$key" && "$key" != "$line" ]]; then
      export "$key=$value"
    fi
  done < ".env"
else
  echo "[WARN] .env not found. Running with current environment variables."
fi

if [ -x "./gradlew" ]; then
  ./gradlew bootRun
else
  sh ./gradlew bootRun
fi
