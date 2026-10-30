#!/bin/bash
# Local manual webcam session. All child process groups stop with Ctrl+C.
set -euo pipefail
set -m
cd "$(dirname "$0")/.."
repo_dir="$PWD"
log_dir="$repo_dir/.local-data/webcam"
java_bin="${KEEPICK_JAVA_HOME:-/opt/homebrew/opt/openjdk@21}/bin/java"
api_jar="$repo_dir/backend/build/libs/backend-0.0.1-SNAPSHOT.jar"
pid_file="$log_dir/supervisor.pid"
if [[ "${1:-start}" == status || "${1:-start}" == stop ]]; then
  if [[ ! -f "$pid_file" ]]; then echo '실행 중인 웹캠 테스트 세션이 없습니다.'; exit 0; fi
  read -r supervisor_pid < "$pid_file"
  if [[ ! "$supervisor_pid" =~ ^[0-9]+$ ]] || ! kill -0 "$supervisor_pid" 2>/dev/null; then
    echo '웹캠 테스트 세션이 종료된 상태입니다.'; exit 0
  fi
  supervisor_command="$(ps -p "$supervisor_pid" -o command=)"
  if [[ "$supervisor_command" != *dev-webcam.command* ]]; then echo 'PID가 다른 프로세스를 가리켜 중지하지 않습니다.' >&2; exit 1; fi
  if [[ "${1:-start}" == stop ]]; then kill -TERM "$supervisor_pid"; echo '웹캠 테스트 서버 종료를 요청했습니다.'; exit 0; fi
  echo "Keepick 웹캠 테스트 supervisor: $supervisor_pid"
  lsof -nP -iTCP:3107 -iTCP:18080 -iTCP:18443 -sTCP:LISTEN
  exit 0
fi

if [[ ! -x "$java_bin" || ! -f "$api_jar" ]]; then
  echo 'Java 21과 backend bootJar가 필요합니다. scripts/dev-api.sh로 먼저 빌드해 주세요.' >&2
  exit 1
fi
for port in 3107 18080 18443; do
  if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "포트 $port 를 사용하는 서버가 있습니다. 기존 Keepick 서버를 먼저 종료해 주세요." >&2
    exit 1
  fi
done
mkdir -p "$log_dir"
child_pids=()
cleanup() {
  trap - EXIT INT TERM
  echo
  echo 'Keepick 웹·API·통화 서버를 종료합니다. DB 데이터는 유지합니다.'
  for pid in "${child_pids[@]}"; do kill -TERM -- "-$pid" 2>/dev/null || true; done
  for pid in "${child_pids[@]}"; do wait "$pid" 2>/dev/null || true; done
  if [[ -f "$pid_file" ]] && [[ "$(cat "$pid_file")" == "$$" ]]; then rm "$pid_file"; fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
printf '%s\n' "$$" > "$pid_file"
printf '\033]0;Keepick 웹캠 테스트\007'
echo 'Keepick 웹캠 테스트 — 이 창에서 Ctrl+C를 누르면 서버 세 개가 종료됩니다.'
echo '수동 테스트용 production 웹을 빌드합니다. 준비 완료까지 기다려 주세요.'
echo "로그: $log_dir"
(
  cd "$repo_dir/backend"
  exec "$java_bin" -jar "$api_jar" --spring.profiles.active=local
) >"$log_dir/api.log" 2>&1 &
child_pids+=("$!")
bash scripts/dev-realtime.sh >"$log_dir/realtime.log" 2>&1 &
child_pids+=("$!")
(
  cd "$repo_dir/frontend"
  export KEEPICK_NEXT_DIST_DIR=.next-buildcheck
  export NEXT_PUBLIC_API_BASE_URL=http://localhost:18080
  export NEXT_PUBLIC_SOCKET_SERVER_URL=http://localhost:18443
  export NEXT_PUBLIC_AUTH_PROVIDERS=google
  export NEXT_PUBLIC_LOCAL_DEMO=true
  npm run build
  exec node node_modules/next/dist/bin/next start --hostname 127.0.0.1 --port 3107
) >"$log_dir/web.log" 2>&1 &
child_pids+=("$!")

ready=false
for ((attempt=0;attempt<90;attempt++)); do
  for pid in "${child_pids[@]}"; do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo '서버가 시작 도중 종료됐습니다. 로그 파일을 확인해 주세요.' >&2
      exit 1
    fi
  done
  if curl --noproxy '*' -fsS --max-time 1 http://localhost:3107/groupchat/webcam-test >/dev/null 2>&1 &&
     curl --noproxy '*' -fsS --max-time 1 http://localhost:18443/health >/dev/null 2>&1 &&
     curl --noproxy '*' -fsS --max-time 1 http://localhost:18080/api/local-health >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 1
done
if [[ "$ready" != true ]]; then
  echo '시작 제한 시간을 초과했습니다. API/DB·통화·웹 로그를 확인해 주세요.' >&2
  exit 1
fi
echo
echo '준비 완료'
echo '  웹캠·AI 로비: http://localhost:3107/groupchat/webcam-test'
echo '  웹: 3107 / API: 18080 / 통화 서버: 18443'
echo "  서버 PID: ${child_pids[*]}"
echo '  종료: 이 창에서 Ctrl+C'
echo
while true; do
  for pid in "${child_pids[@]}"; do
    if ! kill -0 "$pid" 2>/dev/null; then
      echo '서버 하나가 종료돼 나머지 서버도 정리합니다.' >&2
      exit 1
    fi
  done
  sleep 2
done
