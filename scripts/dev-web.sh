#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../frontend"
export NEXT_PUBLIC_API_BASE_URL="${NEXT_PUBLIC_API_BASE_URL:-http://localhost:18080}"
export NEXT_PUBLIC_SOCKET_SERVER_URL="${NEXT_PUBLIC_SOCKET_SERVER_URL:-http://localhost:18443}"
export NEXT_PUBLIC_AUTH_PROVIDERS="${NEXT_PUBLIC_AUTH_PROVIDERS:-google}"
export NEXT_PUBLIC_LOCAL_DEMO=true
exec node node_modules/next/dist/bin/next dev --hostname 127.0.0.1 --port 3107
