# Keepick 2단계: 실제 웹의 양방향 영상·음성 검증

2026-10-29. 범위는 **AI 인식·영상 효과를 켜지 않은 실제 웹 회의 화면 두 개의 기본 통화**다. 테스트용 Socket.IO HTML이 아니라 Next.js의 `/groupchat/step2-final` 로비·참여 버튼·영상 카드·미디어 제어를 사용했다.

## 결과

두 독립 Chrome 세션에서 실제 Socket.IO/mediasoup transport → RTP producer/consumer → 원격 video/audio 요소까지 통과했다. 종료 consumer 참조가 남는 서버 결함 1개를 수정했고, 실제 worker 회귀 검사는 7/7 통과했다.

| 검증 | 확인 결과 |
| --- | --- |
| 양방향 영상 | 실제 수신·VP8 디코딩·화면 재생. 합성 입력 크기 640×360, 15fps |
| A의 약 3.013초 수신 | 영상 641,744 bytes, 추가 디코딩 46프레임, 실제 렌더 프레임 1,131 → 1,176 |
| B의 약 3.009초 수신 | 영상 648,916 bytes, 추가 디코딩 45프레임, 실제 렌더 프레임 1,146 → 1,191 |
| 영상 정지 여부 | 두 수신 영상 모두 샘플 간 픽셀 hash 변화 |
| A의 오디오 수신 | 12,231 bytes 증가, totalAudioEnergy 약 0.07049 증가 |
| B의 오디오 수신 | 12,231 bytes 증가, totalAudioEnergy 약 0.06875 증가 |
| 실제 음성 신호 전달 | A는 B의 660Hz 톤을 약 662.11Hz로, B는 A의 440Hz 톤을 약 439.45Hz로 관측. 수신 RMS 각각 약 0.106 |
| 마이크 off/on | B 음소거 시 A의 수신 RMS=0, 재개 시 약 0.10635 |
| 카메라 off/on | 약 26.638초 동안 상대 렌더 프레임 3,736 유지, 재개 후 4,666까지 증가 |
| B 퇴장 | A만 남고 서버 consumer 수 0. A의 로컬 영상 유지 |
| B 재입장 | 각 참가자 producer 2·consumer 2·transport 2로 복구, 양방향 영상·음성 수신 재확인 |

영상 입력이 15fps이므로 이 결과를 실제 카메라의 최대 성능이나 모바일 성능으로 해석하지 않는다. 오디오 byte 증가만으로 성공 처리하지 않고 신호 에너지·파형 주파수와 재생 요소를 함께 확인했다. RTP 통계 필드의 의미는 [W3C WebRTC Stats](https://www.w3.org/TR/webrtc-stats/)를 참고했다.

## 수정한 서버 결함

상대가 퇴장하면 mediasoup worker가 관련 consumer를 종료하지만, 수신자의 `peer.consumers` Map에서는 참조를 제거하지 않았다. 실제 웹에서 B가 나간 뒤 A의 consumer 수가 2로 남았다. 별도의 worker 회귀 검사에서도 송신자 퇴장 후 consumer 수가 1로 남아 실패했다.

`consumer.observer.once("close", ...)`에서 Map 참조를 제거하도록 수정했다. 네이티브 미디어 자원이 계속 송수신 중이었다고 주장하는 것은 아니며, 종료된 JavaScript 객체 참조와 잘못된 서버 상태 집계를 정리한 변경이다.

- [서버 수정](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/livechat-server/src/domain/media/media-events.handler.ts)
- [실제 worker 회귀 검사](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/livechat-server/tests/signaling.test.cjs)
- 수정 전 7개 중 1개 실패, 수정 후 7/7 통과. TypeScript 빌드 통과.
- 회귀 검사의 미디어 객체 생성은 lifecycle 확인용이며, 실제 RTP·재생 검증은 위 브라우저 검사에서 별도로 수행했다.

## 테스트 입력과 실제 검증 경계

카메라는 FFmpeg로 만든 서로 다른 색상의 움직이는 Y4M 패턴을 Chromium의 fake capture 장치로 공급했다. 초기 WAV 가상 마이크에서는 입력 자체가 0으로 관측되어 사용하지 않았다. 최종 오디오는 테스트용 getUserMedia 입력 경계에서 Web Audio가 생성하는 native MediaStreamTrack을 공급했다.

대체한 것은 **카메라·마이크 입력**이다. 실제 앱의 로그인 상태 관리, 로비·회의 UI, 미디어 트랙 관리, Socket.IO 신호, mediasoup 네이티브 worker, 코덱, RTP 송수신, 원격 video/audio 요소는 그대로 사용했다. RTCPeerConnection 생성 관찰과 getStats 읽기는 계측용이다. 프로덕션 앱 소스에 테스트 입력·디버그 전역을 넣지 않았다.

이번 테스트는 독립된 비로그인 브라우저와 로컬 테스트 방에서 수행했다. OAuth·그룹 접근 권한 검증은 이 결과에 포함하지 않는다. 실제 장치 입력·음성 처리·스피커의 체감 품질도 검증한 것이 아니다.

## 재현 도구

프로젝트 루트에서 실행한다. FFmpeg, Chrome, Node/npm과 Playwright CLI가 필요하다. Node 서버와 웹은 기존 개발 스크립트로 실행한다.

```sh
bash scripts/dev-realtime.sh
# 다른 터미널
bash scripts/dev-web.sh
# 다른 터미널: 합성 영상과 CLI 설정 생성
python3 livechat-server/tests/browser/prepare-media.py
```

이 작업은 output/playwright/keepick-media에 합성 영상·설정을 생성한다. 개인 카메라나 마이크 파일을 읽지 않는다. 아래의 pw는 Playwright CLI 단축 함수다.

```sh
pw() { npx --yes --package @playwright/cli playwright-cli "$@"; }
pw --session keepick-media-a open about:blank --config output/playwright/keepick-media/config-a.json
pw --session keepick-media-a run-code --filename livechat-server/tests/browser/setup-media.js
pw --session keepick-media-a goto 'http://localhost:3107/groupchat/step2-final?testTone=440'

pw --session keepick-media-b open about:blank --config output/playwright/keepick-media/config-b.json
pw --session keepick-media-b run-code --filename livechat-server/tests/browser/setup-media.js
pw --session keepick-media-b goto 'http://localhost:3107/groupchat/step2-final?testTone=660'
```

각 화면에서 다른 이름을 입력하고 참여한다. AI 미리보기를 누르지 않고, 회의 화면에서 마이크를 켠다.

```sh
pw --session keepick-media-a run-code --filename livechat-server/tests/browser/sample-call.js
pw --session keepick-media-b run-code --filename livechat-server/tests/browser/sample-call.js
pw --session keepick-media-a run-code --filename livechat-server/tests/browser/analyze-remote-audio.js
pw --session keepick-media-b run-code --filename livechat-server/tests/browser/analyze-remote-audio.js
```

`sample-call.js`는 한 번의 브라우저 실행 안에서 3초 간격의 before/after를 수집한다. 연결 상태가 connected인지, 원격 video/audio가 재생되는지, 수신 byte·frame·energy가 증가하는지 확인한다. `collect-stats.js`는 단일 시점 디버깅용이다. 테스트 완료 후 각 세션에 close 명령을 실행하면 가상 장치와 계측도 사라진다.

## 로컬 증거

아래 파일은 현재 PC의 output/playwright/keepick-media에 있으며 Git에서 제외한다. 생성 스크립트와 계측 코드는 저장소에 보존했다.

- [통합 측정 결과 JSON](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-media/verification-summary.json)
- [A의 3초 연속 샘플](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-media/a-bounded-sample.json)
- [B의 3초 연속 샘플](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-media/b-bounded-sample.json)
- [초기 양방향 실제 회의 화면 A](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-media/client-a-active.png)
- [초기 양방향 실제 회의 화면 B](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-media/client-b-active.png)
- [수정 후 퇴장한 방 상태](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-media/room-after-fixed-leave.json)
- [수정 후 재입장한 방 상태](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-media/room-after-rejoin.json)
- [정리 결함 수정 전 실패](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-media/cleanup-before.log)
- [수정 후 7/7 회귀 검사](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-media/cleanup-after.log)

## 제외한 구간과 남은 작업

최종 추가 샘플 중 두 브라우저가 연결을 잃고 로비로 돌아간 구간이 있었다. 사용자가 그동안 Mac 잠자기 또는 네트워크 변경이 있었다고 확인했다. 해당 구간의 a-rejoined-2.log/b-rejoined-2.log는 연속 수신 평가에서 제외했고, 깨어 있는 환경에서 한 명령 안의 3초 샘플로 다시 검증했다. 원인을 앱 단독 결함이라고 확정하지 않는다.

잠자기·네트워크 단절 후 자동으로 통화에 복귀하는 동작은 확보하지 않았다. 페이지를 다시 연 후 재입장해 복구했다. 자동 재접속·상태 복원과 장시간/망 변경 시험은 별도 안정화 항목이다. 이번 결과를 장시간 안정성, 외부망 연결 또는 홈서버 공개 배포 검증으로 표현하지 않는다.

AI 인식과 영상 효과는 꺼져 있었지만 기존 앱의 모델 사전 초기화는 실행됐다. 표정 모듈의 FaceMesh is not a constructor와 뷰티 모듈 로딩 오류가 확인되며, 이번에는 AI 코드를 수정하지 않았다. 다음 AI 단계에서 초기화 분리와 실패 처리를 우선 해결한다. 채팅 입장 알림 중복도 관측됐으며 별도 채팅 정리 항목으로 남긴다.

화면공유 REST 경로는 이전 정정대로 보존했다. Android, AI 모델, Vercel 배포 설정은 변경하지 않았다. 실제 기기 두 대의 카메라·마이크 품질, Android 발열/배터리, 외부 NAT/TURN 경로는 후속 실기기 검증 대상이다.
