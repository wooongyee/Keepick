#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../livechat-server"
if [[ ! -f .env.local ]]; then
  echo "Copy livechat-server/.env.example to livechat-server/.env.local first." >&2
  exit 1
fi
npm run build
exec node --env-file=.env.local dist/app.js
