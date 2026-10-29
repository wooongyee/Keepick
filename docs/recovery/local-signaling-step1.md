# Keepick 1단계: 로컬 실시간 서버와 방 입장 검증

2026-10-29. 대상 브랜치 `restore/local-runtime`. 웹과 Android 안정화 중 첫 단계다.

**결과:** Node/Socket.IO와 실제 mediasoup worker를 로컬에서 실행했다. 독립된 Chrome A/B가 같은 방에 서로 다른 소켓 ID로 입장해 상대 참가자를 확인했다. 통합 검사 6개가 통과했다. 실제 회의 UI·카메라·마이크·RTP 영상/음성 송수신·AI는 이번 검증에 포함하지 않았다.

## 실행 주소

| 용도 | 주소 | 상태 |
| --- | --- | --- |
| 실제 실시간 서버 | http://localhost:18443 | HTTP, loopback에 바인딩 |
| worker 상태 | http://localhost:18443/health | 200, ready=true |
| 브라우저 테스트 페이지 | http://127.0.0.1:3108 | 카메라/마이크 없는 Socket.IO 클라이언트 |
| 기존 웹 | http://localhost:3107 | 기존 실행 유지 |
| 기존 Spring API | http://localhost:18080 | 기존 실행 유지 |

현재 개발 PC Darwin arm64, Node 24.5.0, lockfile의 mediasoup 3.18.0으로 실행했다. 의존성 설치와 native worker 실행이 성공했다. 기존 런타임 의존성의 버전 변경은 없으며, 검사 전용 socket.io-client와 그 의존성 8개를 추가했다.

## 실행과 재현

프로젝트 루트 기준이다. Node 22 이상과 npm을 권장한다. 기존 Dockerfile도 Node 22를 사용한다. 아래 명령은 Vercel이나 홈서버를 변경하지 않는다.

```sh
cd livechat-server
npm ci
cp .env.example .env.local
cd ..
bash scripts/dev-realtime.sh
```

환경 파일이 이미 있다면 덮어쓰지 않고 필요한 키만 반영한다. 스크립트는 TypeScript를 빌드한 다음 Node의 `--env-file=.env.local`로 실행한다. 중지는 실행 터미널의 Ctrl+C다. 빌드 출력 dist와 .env.local은 Git에서 제외한다.

별도 터미널에서 서버가 실행 중인 상태로:

```sh
cd livechat-server
npm run test:signaling
npm run test:client
```

`test:client`는 loopback 3108에 테스트 HTML만 제공한다. 실제 웹/Vercel 라우트에는 추가하지 않았다. 다음 두 주소를 별도 브라우저 세션에서 열고 각각 **연결 → 입장**을 누른다.

- [클라이언트 A](http://127.0.0.1:3108/?name=Client%20A&room=manual-step1)
- [클라이언트 B](http://127.0.0.1:3108/?name=Client%20B&room=manual-step1)

방 이름이 같아야 한다. 두 번째 입장 후 양쪽에서 참가자 2명과 상대 이름이 표시되어야 한다. B의 퇴장을 누르면 A는 1명으로 바뀌고, B는 같은 소켓으로 재입장할 수 있다. 테스트 완료 후 연결 종료를 누른다.

다른 주소를 대상으로 통합 검사할 때:

```sh
SIGNALING_URL=http://localhost:18443 npm run test:signaling
```

## 변경과 이유

1. **실행을 막던 미완성 화면공유 REST 라우트 제거.** 이 파일은 저장소에 없는 ScreenShareController를 import해 TypeScript 빌드부터 실패했다. 또한 Router 생성 함수를 호출하지 않고 app.use에 전달하고 있었다. 이 경로를 제거해 서버를 실행할 수 있게 했으며 화면공유 기능을 복구했다고 간주하지 않는다.
2. **HTTP/HTTPS·host·port·Origin을 환경 변수화.** 기존 개발 모드는 certs/server.key와 server.cert가 필수였다. 로컬 프로필은 HTTP, 127.0.0.1:18443으로 명시한다. 기존 웹 개발 스크립트의 NEXT_PUBLIC_SOCKET_SERVER_URL이 이미 localhost:18443을 가리키므로 웹 소스 변경은 필요하지 않았다. 운영 배포나 Preview 설정은 변경하지 않았다.
3. **미디어 IP와 포트 범위 환경 변수화.** 고정된 192.168.0.7 대신 로컬 listen/announced IP를 127.0.0.1로 설정했다. 포트 범위는 10000–10100이며 잘못된 포트/범위는 시작 전에 거절한다.
4. **worker readiness와 WebSocket Origin 확인 추가.** HTTP CORS 외에도 allowRequest로 브라우저 WebSocket Origin을 검사한다. CORS만으로 WebSocket을 제한할 수 없기 때문이다. 이것은 사용자 인증/방 권한 확인을 대신하지 않는다. [Socket.IO 공식 설명](https://socket.io/docs/v4/handling-cors/).
5. **동시 첫 입장 경합 수정.** router 생성이 비동기인 동안 두 요청이 각각 방을 만들어 Map을 덮어쓰고 한 참가자가 누락됐다. 같은 roomId의 생성 Promise를 공유하도록 변경했다.
6. **퇴장 시 실제 Socket.IO 방 멤버십 해제.** 애플리케이션의 peers Map에서만 제거해 퇴장한 소켓도 후속 user_joined 알림을 받았다. 미디어 상태 정리와 함께 socket.leave(room.id)를 수행한다.

## 검증 증거

통합 검사는 mock 서버가 아닌 실행 중인 Node 서버와 native mediasoup worker에 연결한다. 처음에는 5개 중 동시 입장·퇴장 후 알림 차단 2개가 실패했고 수정 후 통과했다. 마지막에는 연결 종료 정리 검사를 추가해 6개를 실행했다.

| 경계/시나리오 | 결과 | 확인 내용 |
| --- | --- | --- |
| worker 상태·HTTP CORS | 통과 | ready=true, 웹 localhost/127.0.0.1:3107와 fixture 3108 Origin 허용, 다른 Origin 응답에 허용 헤더 없음 |
| WebSocket Origin 제한 | 통과 | 허용되지 않은 Origin의 연결 거절 |
| A/B 순차 입장 | 통과 | joined_room, user_joined, 서로 다른 ID, 같은 chatSessionId, 서버 peersCount=2 |
| 실제 미디어 transport 준비 | 통과 | worker에서 transport 생성, ICE 후보 IP=127.0.0.1, 포트=10000–10100 |
| 동시 첫 입장 | 통과 | 같은 방 생성 결과를 공유하고 두 참가자가 모두 보존됨 |
| 명시적 퇴장 | 통과 | user_left/left_room, peersCount=1, 마지막 퇴장 후 방 404 |
| 퇴장 이후의 알림 | 통과 | 새 참가자가 입장해도 퇴장한 소켓은 user_joined를 받지 않음 |
| 예기치 않은 disconnect | 통과 | 남은 참가자에게 user_left 전달, 서버 참가자 수 감소 |
| 독립된 실제 Chrome A/B | 통과 | 양쪽 참가자 2명, B 퇴장 시 A 1명, B 재입장 후 2명 |
| 브라우저 console | 통과 | A/B 각각 Error 0, Warning 0 |

위 시나리오들은 6개의 Node 통합 테스트와 별도의 브라우저 조작에 나눠 검증되어 있다. 기본 주소 127.0.0.1과 실제 웹 개발 설정과 같은 localhost 양쪽에서 6/6 통과했다. 미디어 transport를 생성한 것은 DTLS 연결이나 실제 영상/음성 수신이 완료됐다는 뜻이 아니다.

로컬 증거 파일은 output/playwright/keepick-signaling에 있고 Git에서 제외한다.

- [A: 참가자 두 명](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-signaling/client-a-joined.png)
- [B: 참가자 두 명](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-signaling/client-b-joined.png)
- [B 퇴장 후 A: 한 명](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-signaling/client-a-after-b-left.png)
- [실제 서버 방 상태](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-signaling/browser-room.json)
- [최종 통합 검사 로그](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-signaling/integration-tests.log)
- [localhost 통합 검사 로그](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-signaling/integration-localhost-tests.log)
- [수정 전 실패 재현 로그](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-signaling/integration-tests-before-fix.log)

## 범위와 다음 단계

이번 단계의 통과 기준은 소켓 연결과 같은 방의 참가자 상태 유지다. 테스트 HTML은 실제 앱의 로그인·Redux·회의 UI·AI 초기화를 실행하지 않는다. 실제 웹 회의 화면과 미디어 송수신의 다음 단계 검증을 대체하지 않는다.

다음 단계는 AI를 끈 실제 웹 회의 화면 두 개에서 영상·음성의 producer/consumer 생성과 양방향 수신을 확인하는 것이다. 전송/수신 byte·frame 증가와 화면 변화로 판정한다. 기본 통화가 확보된 뒤 AI 트랙·초기화 수정을 진행한다.

로컬 설정에는 참가자 인증·그룹 권한 확인이 아직 없다. Origin 검사도 인증이 아니며 Origin 없는 네이티브 요청은 허용된다. 현재는 loopback에서만 실행한다. 홈서버 외부 공개 전에는 인증·권한, TLS, 실제 reachable media IP, 포트 전달/NAT/TURN 경로를 별도로 검증해야 한다. npm ci의 기존 취약점 집계 15개도 남아 있으며 이번 단계에서 임의의 의존성 일괄 업그레이드는 하지 않았다.

AI 소스와 모델, Android 소스, Vercel 배포는 변경하지 않았다.
