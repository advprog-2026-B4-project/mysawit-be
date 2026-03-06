#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
cd "$SCRIPT_DIR"

if [ -z "$1" ]; then
  echo "Usage: ./change-admin-password.sh <newPassword>"
  exit 1
fi

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
  echo "[WARN] .env not found. Using default DB connection settings."
fi

if [ -x "./gradlew" ]; then
  ./gradlew changeAdminPassword -Ppassword="$1"
else
  sh ./gradlew changeAdminPassword -Ppassword="$1"
fi
