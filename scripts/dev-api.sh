#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../backend"
if [[ ! -f .env.local ]]; then
  echo 'Create backend/.env.local using backend/.env.example first.' >&2
  exit 1
fi
exec bash ./gradlew bootRun --args='--spring.profiles.active=local'
