# 화면 공유 종료 전파 수정

2026-10-30. 공유 영상은 도착하지만 종료가 상대 화면에 남는 문제를 수정했다. 스크린셰어 구현과 보존해 둔 REST 라우트는 유지한다.

## 원인과 수정

서버는 송신자에게 `producer_closed_success`, 다른 참가자에게 `producer_closed`를 보낸다. 클라이언트의 closeProducer는 자신에게 오지 않는 `producer_closed`를 기다려 3초 뒤 시간 초과가 발생했다. SocketManager는 이때 `off(responseEvent)`로 해당 이벤트의 모든 리스너를 지워 WebRTC의 공용 종료 수신기도 제거했다. 자신의 공유를 종료한 클라이언트가 이후 상대의 종료 알림을 처리하지 못할 수 있었다.

요청 응답을 `producer_closed_success`로 맞추고 producerId로 응답을 대응시킨다. 시간 초과·오류·성공 시 해당 요청의 콜백만 해제한다. 다른 producer 종료 알림과 동시 요청을 보존한다.

함께 확인한 두 정합성 문제도 수정했다. 원격 producer 조회는 consumer의 id가 아니라 consumer.producerId로 찾고, 원격 공유 트랙 hook은 MediaTrackManager의 변경 알림을 구독해 같은 참가자가 공유를 중지·재개해도 최신 트랙을 읽는다.

## 검증

최종 production 웹 3110과 별도 통화 서버 18444, headless Chrome 두 클라이언트로 다음을 확인했다. 카메라는 합성 Y4M, 화면은 움직이는 Canvas captureStream을 getDisplayMedia 입력 경계에 공급했다. Socket.IO·mediasoup worker·RTP·실제 웹 UI는 수정 없는 실행 경로를 사용한다.

1. A가 공유하고 앱 버튼으로 중지 → B의 공유 카드 제거.
2. A가 자신의 공유를 중지한 뒤 기존 timeout보다 긴 3.2초 경과 → B가 공유하고 중지 → A의 공유 카드 제거.
3. 화면 트랙을 종료하고 브라우저의 종료 동작에 해당하는 `ended` 이벤트 주입 → 상대 카드와 로컬 공유 상태 제거.

각 공유는 상대 video에서 최소 2프레임을 디코딩한 뒤 중지했다. 공유 종료 후 video 수 3→2, transport connected, 보관된 카메라 live를 확인했다. 기본 영상·합성 음성 수신 증가도 별도로 측정했다. 세 번째 검사는 native 트랙과 실제 종료 처리기를 사용하지만 Chrome의 OS 화면 선택창·공유 중지 버튼을 자동 클릭한 검사는 아니다.

- 실제 소켓·worker의 종료 ack/원격 알림/카메라 producer 유지: `livechat-server/tests/signaling.test.cjs`.
- 공용 리스너 보존·오류 정리·동시 응답 대응 3개: `frontend/tests/socket-requests.test.cjs`.
- 실제 UI 재현: `livechat-server/tests/browser/setup-screen.js`, `verify-screen-stop.js`.
- [로컬 측정 원본](/Users/woongyee/dev/seedwoofer/project-recovery/checkouts/keepick/output/playwright/keepick-step4/screen-stop.log).

사용자가 직접 테스트할 때는 [수동 테스트 실행 방법](./manual-webcam-test.md)으로 시작한 뒤 두 탭을 같은 방에 입장시킨다. 앱 중지와 Chrome 자체 공유 중지를 번갈아 수행하고 카메라·음성이 유지되는지 확인한다. 이번 자동 검증이 실제 OS 캡처 권한·장치 차이를 대신하지 않는다.
