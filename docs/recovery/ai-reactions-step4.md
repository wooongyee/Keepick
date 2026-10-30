# Keepick 4단계: AI 리액션 전달과 화면 공유 종료

2026-10-30. 실제 회의의 AI 리액션 경로를 정리했고 화면 공유 종료 수정은 별도 커밋 `8722196`으로 분리했다. [화면 공유 종료 원인·검증](./screen-share-stop.md).

## 리액션 계약

기존 클라이언트는 모든 참가자를 `local`로 보내 상대 반응까지 자기 반응으로 판단해 버렸다. 같은 이벤트를 서로 다른 payload로 받는 경로가 둘이었고 UI 오버레이도 비활성 상태였다. cleanup은 등록 이벤트와 다른 이름을 해제하거나 공용 connect/disconnect 리스너 전체를 제거했다.

현재 회의 경로는 `ai_reaction` 요청 → `ai_reaction_received` 응답 하나를 사용한다. 요청에는 방과 감지 결과(id·emoji·aiType·confidence·duration)만 보낸다. 서버가 실제 입장한 socket으로 방 소속을 확인하고 참가자 ID·이름·timestamp를 확정한다. 클라이언트가 지정한 신원을 그대로 중계하지 않는다.

서버가 검증한 결과는 같은 방의 송신자와 수신자에게 전달한다. 로컬에서 먼저 Redux에 넣고 다시 echo를 받는 이중 처리를 없앴다. 실제 socket ID는 wire에서 사용하며 `local`은 자신의 영상 카드에 대응하는 UI 키로만 남긴다. 과거 gesture/effect API는 보존하고 해당 경로에도 방 소속과 서버 기준 신원을 적용했다.

서버는 이모지·AI 종류·신뢰도·ID를 검사한다. 표시 시간을 0.5~5초로 제한하고 socket당 200ms 간격 및 최근 100개 ID로 빈도·중복을 제어한다. 클라이언트도 전달 ID를 중복 처리하지 않고 자기 콜백만 해제한다. 방 전환·퇴장 시 반응을 지운다.

영상 카드 오버레이를 연결하고 표시 시간이 지나면 해당 반응 ID가 여전히 현재 반응일 때만 제거한다. 오래된 타이머가 새로운 반응을 지우는 경우를 막는다. 손 주변 Canvas 스티커 합성은 유지하고, UI 오버레이는 참가자별 짧은 반응 표시로 사용한다. 인식 임계값·학습 모델·동적 시퀀스 특징은 변경하지 않았다.

## 검증 결과

| 검사 | 결과 |
| --- | --- |
| 기존 AI 수명주기 검사 | 16/16 통과 |
| 소켓 요청·리액션 수명주기 검사 | 7/7 통과 |
| 실제 Socket.IO·mediasoup worker 검사 | 12/12 통과 |
| TypeScript·Node build·Next production build | 통과 |
| 새 소켓 처리기·수정한 회의/리액션 UI lint | 오류·경고 0. 기존 전체 프로젝트 lint 문제 해결을 뜻하지 않음 |
| 세 번째 테스트 참가자의 리액션 | 실제 웹 A/B에서 각각 `👍` 1개 표시 |
| 리액션 만료 | 참가자가 방에 남아 있는데 두 화면 모두 자동으로 제거 |
| 서버 신원 확정 | 위조 userId/name/timestamp 덮어쓰기, 표시 시간 상한 적용 |
| 잘못된 요청 | 미입장 사용자·다른 방·잘못된 payload 차단 |
| 동일 반응·빠른 반복 | 중복 및 빈도 제어 확인 |
| 공유 종료 후 기본 통화 | 3초 동안 상대 영상 +45프레임/+596,569 bytes, 음성 +12,150 bytes/energy +0.07067 |

회귀 검사 35개와 별도로 production 웹 3110/별도 소켓 서버 18444에서 headless Chrome 두 화면을 사용했다. 화면 공유 시작은 실제 상대 video에서 최소 2프레임 디코딩 후 종료했다. 앱 중지, 자신의 중지 이후 상대의 중지, 브라우저 종료에 해당하는 ended 이벤트 주입을 확인했다. 공유 종료 뒤 카메라 원본 live·transport connected를 유지했다.

리액션 UI 검사는 Socket.IO fixture가 결정적인 감지 결과를 전송하는 방식이다. 이는 이벤트 전달·서버 검증·Redux·카드 렌더링·만료 검증이며 실제 손동작/표정의 인식률 검사와 구분한다. 화면은 synthetic Canvas, 카메라는 Y4M, 음성은 Web Audio를 입력 경계에 공급했다. OS 캡처 선택창이나 Chrome의 실제 공유 중지 UI를 자동 클릭한 검사는 아니다. 사용자가 이전에 실제 화면 공유 시작·상대 수신을 확인한 결과와 함께 해석한다.

## 재현 도구와 원본

```sh
cd frontend
npm run test:ai-lifecycle
npm run test:sockets
# 별도 테스트 서버를 실행한 상태에서
cd ../livechat-server
SIGNALING_URL=http://127.0.0.1:18444 node --test tests/signaling.test.cjs tests/realtime-events.test.cjs
```

`livechat-server/tests/browser`의 `setup-media.js`, `setup-screen.js`, `verify-screen-stop.js`, `enable-microphones.js`, `sample-call.js`로 공유 종료와 미디어를 확인한다. `reaction-fixture.cjs`와 `verify-reaction-ui.js`는 감지 결과를 주입하고 표시·만료를 확인한다. Playwright CLI 전용 headless 세션에서 실행하며 평소 사용하는 Chrome 프로필이나 실제 카메라·화면을 사용하지 않는다.

- [통합 검증 JSON](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-step4/verification-summary.json)
- [리액션 표시 화면](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-step4/reaction-shown.png)
- `output/playwright/keepick-step4`의 원본 로그는 로컬 증거이며 Git에서 제외한다.

검증 종료 후 테스트 Chrome과 검증용 서버를 모두 종료했다. 이전에 남아 있던 1단계 Chrome 두 창과 이전 배포 확인 탭도 정리했다. 실제 장치 수동 테스트는 [단일 실행·상태·종료 방법](./manual-webcam-test.md)을 사용한다. 홈서버 배포·Android·잠자기 이후 자동 재입장은 이번 작업에 포함하지 않는다. 다음 인식 품질 검증은 실제 손동작과 기존 모델 전처리·후처리를 대상으로 진행한다.
