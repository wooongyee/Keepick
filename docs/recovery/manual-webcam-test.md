# 실제 웹캠·AI 수동 테스트

Chrome에서 http://localhost:3107/groupchat/webcam-test 를 연다. 카메라·마이크 권한을 허용하고 로비의 `AI 기능 미리보기 → 켜기`로 테스트한다. 통화는 이름 입력 후 참여하고, 회의 화면에서 마이크를 켠다. 동일 주소를 다른 탭에서 열면 같은 방에 입장한다. 같은 기기 두 탭의 마이크·스피커는 피드백이 날 수 있으므로 이어폰이나 한쪽 음소거를 사용한다.

실제 장치를 사용하며 Playwright 가상 입력을 설치하지 않는다. 웹은 production build/start로 실행하여 개발 오류 오버레이와 HMR을 제외한다. 소스 변경은 실행 중인 웹에 즉시 반영되지 않으므로 종료 후 다시 시작한다.

저장소 루트에서 실행한다. Mac에서는 아래 command 파일을 더블클릭해 터미널에서 실행해도 된다.

```sh
bash scripts/dev-webcam.command
bash scripts/dev-webcam.command status
bash scripts/dev-webcam.command stop
```

시작 명령은 foreground에서 웹(3107)·API(18080)·통화 서버(18443)를 관리한다. 직접 실행한 터미널에서는 Ctrl+C로 세 프로세스 그룹을 종료한다. 로그와 supervisor PID는 `.local-data/webcam`에 저장되며 Git에서 제외된다. 예전 테스트 fixture(3108)는 실행하지 않는다. DB/Redis Docker 컨테이너와 데이터는 종료 대상에 포함하지 않는다.

이 Mac의 Java 21과 기존 `backend/build/libs/backend-0.0.1-SNAPSHOT.jar`를 사용한다. 다른 설치 경로는 `KEEPICK_JAVA_HOME`으로 지정한다. backend 소스를 수정했다면 먼저 Java 21로 bootJar를 빌드해야 한다. backend·realtime의 기존 `.env.local`은 그대로 사용하고 비밀 값을 출력하지 않는다. MySQL/Redis는 `compose.local.yml`의 기존 컨테이너가 실행 중이어야 한다.
