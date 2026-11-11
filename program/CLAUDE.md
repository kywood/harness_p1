# program 팀 지침 (개발)

루트 `CONVENTIONS.md`와 함께 적용한다. 코드 형식·이름·리소스 풀링 규칙은 `program/code-style.md`, 구조 설명은 루트 `CLAUDE.md`의 아키텍처 절을 본다.

## 변경 범위

앱 코드는 이 디렉터리 아래 `android/`(루트 기준 `program/android/`)에 있다. 그 밖의 `program/`에는 지침과 개발 문서만 둔다.

- `engine/`: 한 번에 한 파일, 최대 10줄(루트 `AGENTS.md`). 더 큰 변경이 필요하면 작은 단계로 나누고 단계마다 테스트를 통과시킨다.
- `ui/`·`MainActivity.kt`: 화면 또는 컴포넌트 하나 단위로 변경한다.
- 사용되지 않는 Media3 경로(`ui/PlayerScreen.kt`, `engine/TorrentFileDataSource.kt`)는 유지 여부가 결정되기 전까지 삭제하거나 다시 쓰지 않는다.

## 엔진·상태 규칙

- 비동기 단계 뒤에는 세대 값(`activeRun`, `requestVersion`, `generation`)을 다시 확인하고, 달라졌으면 상태를 건드리지 않고 빠져나온다.
- 디스크에 쓰는 상태는 `TorrentCache` + `TorrentCacheCipher`를 거친다. 새 저장 위치를 만들지 않는다.
- `MagnetHistory` 포맷에 필드를 추가하면 버전 값을 올리고 이전 버전 읽기를 유지한다.
- 토렌트가 준 경로는 canonical path가 다운로드 폴더 안인지 확인한 뒤 쓴다.
- 파일 삭제 전에는 `awaitStoppedWrites()`로 이전 엔진의 쓰기가 끝났는지 기다린다.
- 외부 입력(마그넷, URL, 토렌트 메타데이터)은 길이와 형식을 검증한 뒤 처리한다.

## 코드 형태

- 판단 로직은 Compose와 Android에 의존하지 않는 작은 순수 객체(`*Policy`, `*Plan`, `*State`, `*Label`)로 뺀다.
- 광고 단위 ID는 `BuildConfig.DEBUG`로 테스트/실제를 가르는 기존 정책 객체를 통해서만 얻는다.
- 화면 코드에 색·간격·문구를 새로 직접 적지 않는다. 필요한 값이 없으면 design에 토큰 추가를 요청한다.

## 테스트

- 로직 변경에는 `app/src/test`의 JVM 단위 테스트를 함께 넣는다.
- libtorrent 네이티브나 화면 흐름이 걸린 변경은 `app/src/androidTest`의 에뮬레이터 테스트로 확인한다.
- 완료 전 `:app:testDebugUnitTest`와 `:app:assembleDebug`를 실행하고, 실행하지 못한 검증은 보고에 적는다.

## 도구

설치되어 있으면 쓴다. 없으면 건너뛰고 보고에 적는다.

- `kotlin-lsp`: 수정 후 타입 오류·누락 import 확인
- `code-review`: 커밋 전 변경 검토
- Android Emulator QA: 에뮬레이터에서 화면 확인, 스크린샷, logcat 수집
- Android 공식 스킬(`jetpack-compose-performance`, `anr-audit-and-fix`, `play-policy-insights`): Compose 성능, ANR, 출시 전 정책 점검
