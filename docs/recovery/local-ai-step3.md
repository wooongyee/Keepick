# Keepick 3단계: 선택적 AI 초기화와 카메라 수명주기

2026-10-30. 구현 커밋 `56dd718`. `restore/local-runtime`에서 기본 통화와 AI 준비를 분리했다. 실제 Next.js `/groupchat/step3-ai` 화면 두 개, 로컬 Socket.IO 서버와 mediasoup worker로 검증했다.

## 변경한 동작

기본 통화 초기화는 MediaSoup Device·소켓·카메라를 준비한다. AI를 켤 때 선택한 모듈과 효과 이미지를 로딩하며, `idle/loading/ready/error`를 UI에 전달한다. 초기화 실패를 성공으로 처리하지 않는다. 준비에 20초, 영상 시작에 3초, 비동기 분석에 2초 제한을 둔다. FaceMesh 결과 대기는 1.5초로 제한한다.

원본 카메라는 mediasoupManager가 소유한다. 송출 Producer와 AI 입력에는 각각 clone을 주고, AI는 자신이 만든 입력 clone·Canvas 출력·requestAnimationFrame만 종료한다. AI를 끄면 보관한 원본의 clone으로 복귀하므로 카메라 권한 요청을 반복하지 않는다. 카메라 교체 시에는 보관 원본도 교체한다. 방을 나가면 원본·모델·출력·콜백을 정리한다.

초기화와 전환 요청을 직렬화하고 취소 버전을 검사한다. 일부 SDK 로딩은 네트워크 단계에서 취소할 수 없으므로 늦게 끝난 후보도 정리하며, 후속 SDK 로딩과 겹치지 않게 한다. 같은 설정의 준비된 runtime은 방 안에서 재사용한다. AI를 끄면 분석·출력은 멈추지만 모델 캐시는 퇴장 시 해제한다.

영상 출력 루프는 비동기 추론 완료를 기다리지 않는다. 추론 오류·시간 초과를 UI에 알리고 원본 송출로 돌아간다. 이는 비동기 대기로 영상이 멈추는 경로를 막는 조치다. MediaPipe의 동기 추론·픽셀 작업은 여전히 메인 스레드에서 실행하므로 모든 UI 지연을 제거했다고 주장하지 않는다.

로비와 회의 화면이 결과 콜백을 각각 소유한다. 초기화가 화면의 콜백을 덮어쓰지 않으며 로비 종료 후 회의용 콜백을 다시 연결한다. AI 설정을 적용하는 통화 effect는 한 곳으로 합치고, 자기 자신이 바꾼 트랙 ID 때문에 재실행하지 않도록 영상 존재 여부를 구독한다.

## 실제 브라우저에서 발견해 수정한 SDK 충돌

기존 `@mediapipe/face_mesh`는 브라우저 전역 API를 제공한다. ESM named export로 바로 사용하는 경로를 버전 고정 classic-script loader로 바꿨다. 얼굴과 손 SDK가 공유하는 Emscripten `window.Module` 때문에 병렬 초기화 시 `FS_createPath` 오류, 순서만 바꾸면 `Module.arguments` 오류가 발생했다.

얼굴 SDK 초기화를 순차 완료한 뒤 자신이 남긴 Module 참조만 해제하고 Tasks Vision을 준비하도록 수정했다. 얼굴 SDK는 설치된 `0.4.1633559619`, Tasks Vision WASM은 설치된 `0.10.22-rc.20250304`와 일치시켰다. 기본 설정인 정적·동적 손동작과 표정 인식을 함께 초기화하고 영상을 송출하는 데 성공했다. 뷰티 필터는 코드를 보존하고 초기화·timeout·정리를 적용했지만, 이번 브라우저 통화의 효과 검증 대상에는 포함하지 않았다.

## 검증 결과

| 검사 | 결과 |
| --- | --- |
| 새 브라우저에서 AI 꺼짐 | `/models/`, MediaPipe, 제스처 이미지 요청 0건 |
| 로비 AI 미리보기 | 기본 세 모듈 초기화·미리보기 켜기 성공, 끈 뒤 원본 카메라 live |
| 통화 중 AI on/off 3회 | 원본 카메라 ID 유지, 추가 getUserMedia 요청 0회 |
| 준비된 설정 재사용 | 추가 모델·SDK·효과 이미지 요청 0건 |
| B 모델 다운로드 강제 실패 | 오류 표시·AI off, 기존 송출 트랙 ID와 원본 카메라 유지 |
| 실패 후 재시도 | 요청 차단 해제 후 같은 방에서 새로고침 없이 성공 |
| 두 클라이언트 모두 AI on | 각 transport connected, 양방향 VP8 영상·Opus 오디오 수신 |
| A: 약 3.006초 | 상대 영상 추가 디코딩 91프레임, 오디오 에너지 +0.06973 |
| B: 약 3.004초 | 상대 영상 추가 디코딩 91프레임, 오디오 에너지 +0.06883 |
| 실제 합성 음성 전달 | A 수신 약 662.11Hz/RMS 0.10578, B 수신 약 439.45Hz/RMS 0.10621 |
| A 퇴장 시 명시적 해제 | 보유 카메라·마이크 stop 호출 뒤 readyState=ended 관측 |
| 오프라인 회귀 검사 | 16/16 통과 |
| TypeScript / Next production build | 통과 |
| 변경 중심 파일 ESLint | 오류 0, 기존 미사용 코드 경고 13개 |

회귀 검사는 실제 TypeScript 코드를 변환해 모의 SDK·DOM·트랙 경계에서 실행한다. 중복 초기화, WebGL 예외/false 반환의 CPU 전환, 실패·시간 초과, 취소 뒤 늦은 완료, 원본 보호·RAF 해제, 느린 추론 중 렌더 지속, 반복 전환, 카메라 교체, legacy Module 정리를 검사한다. 실제 SDK·RTP 검증을 대체하지 않으므로 브라우저 검사도 별도로 수행했다.

수정 파일 전체를 포함한 ESLint 검사에는 기존 emotion/gesture/mediaTrackManager의 `any` 오류 12개가 남아 있다. 전체 lint가 통과한 상태는 아니다. Next 설정도 기존대로 빌드 중 lint를 생략하므로 위 검사들을 구분했다.

## 재현과 증거

기본 실행과 가짜 카메라·Web Audio 입력 설정은 [2단계 재현 절차](./local-media-step2.md)를 사용한다. 웹 3107, realtime 18443에서 같은 테스트 방에 입장한다. 앱 코드와 SDK, Socket.IO, native worker, RTP 송수신은 실제 경로를 사용하며 카메라·마이크 입력과 실패 주입만 테스트 환경에서 대체했다. 로비의 마이크 선택과 별개로 현재 통화는 마이크를 꺼서 시작하므로 **회의 화면에서 마이크를 켜고** 음성을 측정했다. 이 UI 상태 일치 문제는 별도 후속 항목이다.

```sh
cd frontend
npm run test:ai-lifecycle
npx tsc --noEmit
KEEPICK_NEXT_DIST_DIR=.next-buildcheck NEXT_PUBLIC_API_BASE_URL=http://localhost:18080 NEXT_PUBLIC_SOCKET_SERVER_URL=http://localhost:18443 NEXT_PUBLIC_AUTH_PROVIDERS=google NEXT_PUBLIC_LOCAL_DEMO=true npm run build
```

브라우저용 도구는 `livechat-server/tests/browser`에 있다. Playwright CLI의 `run-code --filename`으로 실행한다.

- `setup-media.js`: 합성 입력·native RTCPeerConnection 관찰, 캡처 원본 참조 기록.
- `inspect-ai-lifecycle.js`: 원본/송출 트랙과 AI 자원 요청 기록.
- `toggle-ai.js`: config-a를 쓰는 A가 AI off·설정 패널 열림 상태에서 on/off 두 회 추가 실행. native sender 전환을 기다린 뒤 캡처 수와 요청 수를 기록한다.
- `sample-call.js`, `analyze-remote-audio.js`: 양방향 RTP 증가량·수신 신호 확인.

로컬 원본 증거는 Git에서 제외된 `output/playwright/keepick-ai-step3`에 있다.

- [측정 결과 JSON](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-ai-step3/verification-summary.json)
- [최종 AI 통화 화면](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-ai-step3/final-b.png)
- `baseline-*.log`, `final-ai-*.log`, `final-audio-*.log`: 3초 연속 샘플·수신 주파수.
- `call-b-model-failed.log`, `call-b-retry-on.log`: 모델 실패 후 기존 트랙 유지와 재시도.
- `repeated-toggles.log`, `leave-and-inspect-a.log`: 반복 전환과 퇴장 시 해제.

## 다음 단계와 이번 검증의 한계

다음은 리액션 이벤트 계약·사용자 ID·중복 전송을 맞추는 단계다. 이후 인식 후처리와 실제 손동작 정확도를 평가한다. 이번 작업에서는 제스처 라벨·투표·임계값·동적 시퀀스 특징과 리액션 프로토콜을 변경하지 않았다. 합성 패턴은 사람의 손·표정 인식률을 검증하지 않는다.

실제 장치·Android·모바일 성능, 뷰티 필터 품질, 잠자기/네트워크 변경 뒤 자동 재입장, 인증·그룹 권한, 홈서버 NAT/TURN은 검증 범위 밖이다. AI 자원은 일부 외부 CDN에 의존하며 JS 번들 크기·이미지 용량·Worker 분리는 후속 개선으로 남는다. 배포나 운영 설정 변경 없이 로컬에서 완료했다. 스크린셰어 관련 구현과 보존 경로도 유지했다.
