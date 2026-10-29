# 로컬 Google 로그인·사진 저장 테스트

2026-10-29. 웹과 API는 개발 PC에서 실행하고 MySQL/Redis만 Docker로 실행한다. `local` 프로필에서는 S3/SQS를 사용하지 않는다. Node 통화 서버와 Python 분석 서버는 이번 실행 범위에 포함하지 않는다.

## 주소와 실행

- 웹: http://localhost:3107
- API: http://localhost:18080
- 상태 확인: http://localhost:18080/api/local-health (실제 DB SELECT/Redis ping 포함)
- MySQL/Redis: loopback 13306/16379. Compose 프로젝트 `keepick-local`, 별도 named volumes 사용.

루트에서 최초 한 번 `backend/.env.example`을 `backend/.env.local`로 복사하고 로컬 비밀번호·JWT/state/storage 서명을 위한 무작위 키를 설정한다. 이 파일은 Git에서 제외된다. `.env.local`은 Spring properties 문법으로 읽으므로 값에 shell용 따옴표를 추가하지 않는다. 키를 채팅·로그에 출력하지 않는다.

```sh
docker compose --env-file backend/.env.local -f compose.local.yml up -d --wait
# 별도 터미널, Java 21 필요
JAVA_HOME=/opt/homebrew/opt/openjdk@21 bash scripts/dev-api.sh
# 별도 터미널, frontend npm ci 선행
bash scripts/dev-web.sh
```

위 JAVA_HOME은 이번 Mac의 설치 경로다. 다른 머신은 해당 Java 21 설치 경로를 사용한다. `dev-api.sh`는 변경된 소스를 빌드해서 bootRun하고, `.env.local`은 local 프로필에서만 로드한다. 환경 파일을 바꾸면 API를 재시작해야 한다. OAuth 인증 도중 API를 재시작하면 인증 요청 세션이 사라지므로 로그인 시작부터 다시 진행한다.

중지는 각 개발 서버 터미널의 Ctrl+C와 아래 Compose 명령으로 한다. 볼륨 삭제 옵션을 붙이지 않으면 DB와 Redis 데이터가 유지된다. 사진 파일은 `backend/.local-data/images`에 유지되며 Git에서 제외된다.

```sh
docker compose --env-file backend/.env.local -f compose.local.yml stop
```

## Google 콘솔

Google Auth Platform에서 웹 애플리케이션 OAuth client를 만든다. Authorized redirect URI는 다음 문자열과 정확히 일치시킨다.

```text
http://localhost:18080/api/login/oauth2/code/google
```

이 흐름은 Spring의 서버 OAuth이므로 JavaScript origin을 사용하는 Google 브라우저 SDK와는 구분한다. origin 입력란을 함께 구성한다면 `http://localhost:3107`이다. 테스트 Audience를 사용하는 경우 로그인할 계정을 Test users에 추가한다.

Client ID와 Secret은 `backend/.env.local`의 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`에 둔다. Secret은 웹 환경 변수나 Vercel의 NEXT_PUBLIC 변수에 넣지 않는다. 웹은 기본적으로 Google 버튼만 표시하도록 dev script에서 설정한다. 기존 production 환경은 기본 제공자 목록을 유지한다.

확인할 실제 흐름: 웹 Google 버튼 → accounts.google.com 계정 선택/동의 → localhost API callback → 로컬 웹으로 복귀 → 회원 정보 API 성공. 계정 선택과 동의는 사용자가 진행한다. 302 redirect만으로 로그인 완료라고 판단하지 않는다.

## 파일 처리

기존 응답 필드인 `presignedUrl`/`publicUrl`을 유지한다. local 프로필의 URL은 S3가 아니라 API의 `/api/local-files`다. PUT은 파일 key·만료 시간·content type을 묶은 15분짜리 서명으로 검증하고, GET은 별도의 읽기 서명을 검증한다. 읽기 URL 자체가 접근 권한이므로 공개 게시하거나 로그에 남기지 않는다.

업로드된 이미지를 확인한 뒤 디스크에 쓰고 그룹 사진에는 JPEG 썸네일을 생성해 DB를 갱신한다. 최대 10MB/4천만 픽셀로 제한한다. 기본 Java ImageIO로 읽을 수 있는 이미지부터 지원하며 WebP 등 미지원 인코딩은 415로 거절할 수 있다. 그룹 사진이 삭제되면 기존 원본·썸네일 URL의 읽기/재업로드도 차단한다. 삭제는 기존 API처럼 soft delete이며 파일을 디스크에서 즉시 지우지 않는다. 용량 정리는 후속 작업이다.

서명 키를 바꾸면 DB에 저장된 기존 읽기 URL이 무효화된다. DB·사진 디렉터리·서명 설정을 함께 보존한다. Python 분석을 별도 PC로 다시 연결할 때는 localhost URL을 그대로 넘기지 않고 해당 PC가 접근할 수 있는 다운로드 경로를 마련해야 한다.

## 이번 확인 결과

- API 실행, MySQL/Redis 상태 확인 성공. AWS 없이 기동.
- 실제 HTTP로 합성 테스트 계정의 그룹 생성·업로드 URL 발급·PNG PUT 204·원본 바이트 일치·JPEG 썸네일 조회 성공. 변조한 읽기 서명은 403. Google 로그인 검증과 구분하는 테스트 fixture이며 로그인 우회 API는 추가하지 않았다.
- `LocalImageStorageTest` 4개, `LocalFileControllerTest` 3개, 기존 `PhotoUploadServiceTest` 3개 통과.
- 웹 `next build` 성공. 기존 설정에서 build 중 lint를 생략하므로 전체 lint 통과를 뜻하지 않는다.
- 실제 Google Client ID/Secret 설정 후 Google 인증 진입 302, 정확한 callback, OAuth 세션 쿠키 발급 확인. Chrome의 실제 계정 선택 화면까지 진입. 로그인 완료·회원 저장은 사용자 동의 후 확인할 항목이다.
- 최신 추가된 삭제 URL 차단과 로그 마스킹은 소스·테스트/JAR 빌드에 반영했다. Google 동의를 기다리는 동안 인증 세션을 보존하기 위해 실행 중 API JAR는 재시작하지 않았다. 현재 로그인 검증이 끝나면 최신 JAR로 교체해 재검증한다.

## Vercel Preview와 로컬 API

Vercel의 `fe-keepick` 프로젝트는 현재 Git 저장소 연결이 없다. production은 변경하지 않았다. 복구 브랜치 지정 Preview 환경 변수 생성은 Git 연결 부재로 400을 반환했다. 브랜치 제한을 제거한 Preview 전용 설정 요청은 자동 승인 검토에서 거절돼 적용하지 않았다. 재시도·다른 경로를 이용한 변경도 하지 않았다.

제안한 Preview 변수는 아래 3개이며 전부 공개 가능한 웹 설정이다. production을 대상으로 하지 않는다.

```text
NEXT_PUBLIC_API_BASE_URL=http://localhost:18080
NEXT_PUBLIC_AUTH_PROVIDERS=google
NEXT_PUBLIC_LOCAL_DEMO=true
```

이 방식에서 localhost는 방문자의 PC다. 개발 PC에서만 시험하는 Preview이며 다른 기기용 공개 데모 주소가 아니다. Chrome의 로컬 네트워크 접근 정책에 따라 허용 단계가 필요할 수 있다. Preview URL을 확정하면 해당 origin을 API의 `LOCAL_ALLOWED_ORIGINS`에 정확히 추가하고 재시작해야 한다. 다른 기기에서도 접속하려면 이후 홈서버 또는 명시적으로 구성한 HTTPS 터널 주소로 API URL과 Google callback을 함께 바꾼다.

`NEXT_PUBLIC_LOCAL_DEMO=true`에서는 Next 이미지 최적화를 끈다. Vercel 서버에서 개발 PC localhost 이미지를 가져올 수 없으므로 브라우저가 서명 URL을 직접 읽도록 하는 점검 모드다. 일반 production 설정에서는 기존 이미지 최적화를 유지한다.

웹 빌드만 별도로 확인할 때는 실행 중 개발 서버와 출력 폴더를 분리한다.

```sh
cd frontend
KEEPICK_NEXT_DIST_DIR=.next-buildcheck \
NEXT_PUBLIC_API_BASE_URL=http://localhost:18080 \
NEXT_PUBLIC_AUTH_PROVIDERS=google NEXT_PUBLIC_LOCAL_DEMO=true npm run build
```

근거: [Google 서버 OAuth redirect URI](https://developers.google.com/identity/protocols/oauth2/web-server).
