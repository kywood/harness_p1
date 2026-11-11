# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

PiecePlay — 마그넷 또는 `.torrent` 파일의 HTTP(S) 링크를 넣으면 전체 다운로드를 기다리지 않고 바로 재생하는 Android 앱. Google Play 프로덕션 출시 직전 단계다.

- Android 프로젝트는 `program/android/` 아래의 단일 `:app` 모듈이다. 저장소 루트의 `scripts/`, `tasks/`, `hello.py`, `logs/`는 초기 Aider 하네스 잔재이며 `scripts/build.sh`는 다른 PC 경로를 가리켜 동작하지 않는다.
- 코드 `namespace`는 `com.example.torrentstreamplayer`, 스토어 `applicationId`는 `com.gamenuri.pieceplay`로 서로 다르다.
- `SPEC.md`·`PLAN.md`·`program/android/PROJECT_STATUS.md`·`program/android/TODO.md`는 초기 시점 문서로 현재 코드와 다르다(예: SPEC은 Media3 재생이라 적혀 있으나 실제는 LibVLC). 현재 동작의 기준은 코드와 `program/android/PATCH_NOTES.md`다.

## 명령어

`program/android/`에서 PowerShell로 실행한다. PATH에 Java가 없으므로 먼저 JDK를 지정한다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'

.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.torrentstreamplayer.engine.MagnetIdentityTest"

# 에뮬레이터/기기 필요. --offline 을 붙이면 UTP 의존성 해석에 실패한다.
.\gradlew.bat :app:connectedDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.torrentstreamplayer.NavigationBackTest"
```

릴리스:

```powershell
.\gradlew.bat :app:nextVersion          # 반드시 단독 실행. app/version.properties 의 날짜·리비전·versionCode 증가
.\gradlew.bat :app:verifyVersioning     # 버전 규칙 자체 검사
.\gradlew.bat :app:packageReleaseFiles  # 서명 APK+AAB → app/build/outputs/pieceplay-release/PiecePlay-<버전>-<코드>.*
```

- 버전명은 `v.YYYY.MM.DD.NNN`(서울 날짜 + 당일 리비전)이며 `version.properties`를 손으로 고치지 않는다.
- 릴리스 서명은 `program/android/.signing/upload.properties`(gitignore 대상)에서 읽는다.
- 릴리스 빌드는 R8 최적화가 꺼져 있다(`optimization { enable = false }`).

## 아키텍처

### 입력에서 재생까지

1. `MainViewModel.startStreaming` → `TorrentEngine.resolveInput`(`TorrentInputResolver`)가 마그넷은 그대로, HTTP(S) `.torrent`는 내려받아 마그넷으로 변환한다.
2. `MagnetIdentity.key()`가 만든 64자리 hex 키가 다운로드 폴더명, 캐시 키, `DownloadManager` 작업 키로 앱 전체에서 쓰인다.
3. 재생도 항상 다운로드 작업으로 시작한다. `DownloadManager`(동시 3개, 초과분은 대기열)가 작업마다 `DownloadController` → `TorrentEngine`을 하나씩 만든다. 엔진 인스턴스는 여러 개지만 libtorrent 세션은 `NativeSessionLease`로 하나를 공유한다.
4. 재생은 해당 작업의 엔진을 `downloads.borrow(key)`로 빌려 쓴다(`MainViewModel.sharedPlayback`). `viewModel.torrentEngine`은 빌린 엔진이 있으면 그것을, 없으면 기본 엔진을 돌려주므로 항상 같은 객체가 아니다.
5. ViewModel이 250ms마다 엔진을 폴링해 `StreamUiState`를 `FetchingMetadata → Buffering → Playing`으로 올린다. `Playing` 전환 조건은 `TorrentEngine.isPlaybackReady()`(시작 구간 조각 + 컨테이너 인덱스 확보: `StartupBufferPlan`, `StartupIndexState`, `Mp4IndexProbe`).

### 부분 다운로드 재생 경로

- 재생기는 LibVLC(`ui/VlcPlayerScreen.kt`)다.
- 완료된 파일(`engine.localVideo != null`)은 파일 URI로 직접 연다.
- 미완료 파일은 `TorrentStreamServer`(NanoHTTPD, `127.0.0.1` 임의 포트 `/stream`)를 통해 연다. Range 요청마다 `VerifiedTorrentInputStream`이 해당 조각의 해시 검증이 끝날 때까지 블로킹하면서 엔진에 우선순위를 요청한다. 탐색(seek)은 새 Range 요청 → `prioritizeRequest`/`beginSeek`로 다운로드 창을 옮기는 방식이다.
- 엔진은 네이티브 sequential 모드를 끄고, 재생 위치를 따라가는 조각 deadline 창(`prioritizeOffset`, `advanceDownloadWindow`, `SequentialPiecePlan`)으로 순서를 제어한다. 토렌트 안에서는 가장 큰 영상 파일 하나만 받고 나머지는 `IGNORE`다.
- 재생기는 LibVLC 하나다. 예전에 있던 Media3/ExoPlayer 경로는 제거했으므로 다시 추가하지 않는다.

### 취소·경합 처리 규칙

비동기 작업은 취소 토큰 대신 세대 카운터로 무효화한다: `TorrentEngine.activeRun`, `MainViewModel.requestVersion`, `DownloadController.generation`. 비동기 단계 뒤에는 반드시 `if (run != activeRun) return` 형태로 다시 확인한다. 엔진 작업은 코루틴이 아닌 raw `Thread`이고, `stop()` 후에도 쓰기가 남을 수 있어 파일 삭제 전에는 `awaitStoppedWrites()`를 거친다.

### 저장소

- 영상: `cacheDir/torrent_downloads/<key>/`, 엔진 상태: `cacheDir/torrent_state/<key>/*.bin`
- 재생 목록(마그넷 기록, 최대 50개, 이어보기 위치 포함): `noBackupFilesDir/magnet_history` — `MagnetHistory`의 버전 있는 바이너리 포맷(현재 `-3`). 필드를 추가하면 버전을 올리고 이전 버전 읽기를 유지해야 한다.
- 디스크에 쓰는 모든 상태는 `TorrentCache`를 거쳐 `TorrentCacheCipher`(Android Keystore AES-GCM)로 암호화한다. 마그넷을 평문으로 저장하지 않는다는 보안 규칙의 구현 지점이다.

### UI 구조

- 내비게이션 라이브러리가 없다. 화면은 `ui/navigation/AppScreen`(sealed) 하나로 정하고, `AppNavigator`가 이동 상태와 `StreamUiState`를 합쳐 `MainViewModel.screen`으로 노출한다. `MainActivity`는 테마·광고 동의·전면 광고와 `ui/screens/AppRoot`만 맡고, 화면은 `ui/screens/`(메인·준비 등)에 있다. 재생 목록·위치 저장은 `data/PlaylistRepository`를 거친다.
- 사용자에게 "재생 목록"으로 보이는 화면이 `MagnetHistoryScreen`, "저장 파일 관리"가 `StoredFilesScreen`이다.
- 판단 로직은 Compose 밖의 작은 순수 객체(`*Policy`, `*Plan`, `*State`, `*Label`)로 빼고 각각 JVM 단위 테스트를 둔다. 에뮬레이터 테스트(`androidTest`)는 실제 libtorrent 네이티브와 화면 흐름을 검증한다.
- 문자열은 전부 Kotlin 코드에 하드코딩되어 있고(`strings.xml`에는 앱 이름만 있음) 한국어와 영어가 섞여 있다.
- 테마는 Android Studio 기본 템플릿(보라색)이며 `dynamicColor = true`라 Android 12+에서는 기기 배경화면 색이 적용된다. 재생 화면 색은 `ui/theme/PlaybackColors`에 따로 있고, 그 밖에도 화면 파일 곳곳에 색·dp 값이 직접 적혀 있다.

### 광고

AdMob. `BuildConfig.DEBUG`이면 Google 테스트 단위 ID, 릴리스면 실제 단위 ID를 쓴다(`TestAdPolicy`, `CompletionAdPolicy` — 이름의 `Test`는 과거 흔적). 배너는 메인·재생 목록에만, 전면 광고는 재생 종료/완료 시 `CompletionAdPolicy.eligible`(5분 이상 시청 또는 5회 완료, 10분 쿨다운)을 통과할 때만 띄운다. 재생·준비·버퍼링 화면에는 광고를 넣지 않는다.

## 팀과 컨벤션

공통 규칙은 `CONVENTIONS.md`다. 세션을 연 위치와 맡은 역할에 따라 아래 지침을 먼저 읽는다.

| 팀 | 형태 | 지침 | 맡는 일 |
|---|---|---|---|
| PM | 루트에서 연 메인 세션 | `PM.md` | 우선순위, 에이전트에 일 배정, 결과 확인, 이슈, 출시 결정 |
| launch | `launch/`에서 연 별도 세션 | `launch/CLAUDE.md` | 스토어 등록정보·자료, Play Console 양식, 출시 체크리스트, 심사·출시 후 대응 |
| planning | `planning/`에서 연 별도 세션 | `planning/CLAUDE.md` | 요구사항, 화면 정의, 사용자 흐름, 스펙 |
| design | 서브에이전트 `design` | `design/CLAUDE.md` | 테마·토큰·문구·화면의 시각 표현 |
| program | 서브에이전트 `program` | `program/CLAUDE.md` | 엔진·상태·정책·테스트·빌드 |

- 루트에서 연 세션은 PM이다. 코드를 직접 고치지 않고 `program`·`design` 에이전트에 맡긴다. 서브에이전트로 실행 중이면 이 줄은 해당하지 않고 자기 팀 지침을 따른다.
- 팀 디렉터리(`planning/`, `design/`, `program/`, `launch/`)에는 지침과 그 팀의 문서 산출물을 둔다. 앱 코드(Gradle 프로젝트)는 `program/android/`에 있다.
- 순서는 planning → PM → design → program → PM → launch다.
- 테스터 이메일 등 개인정보는 `launch/private/`(Git 무시)에만 둔다.
- `.claude/rules/`가 `engine/`과 `ui/` 경로별로 해당 팀 지침을 가리킨다.

## 작업 규칙 (AGENTS.md)

- 루트 `AGENTS.md`: `engine/` 변경은 한 파일·최대 10줄, UI는 화면·컴포넌트 단위, 로직 변경에는 단위 테스트 동반, 승인 없는 외부 의존성 추가 금지. 기존 코드가 import 대신 전체 경로 이름을 인라인으로 쓰고 한 줄이 긴 이유는 과거에 10줄 규칙이 전체에 적용됐기 때문이다.
- `program/android/AGENTS.md`: 변경 전 GitHub 이슈 생성(또는 재사용) → `codex/<이슈번호>-<작업명>` 브랜치 → 관련 파일만 스테이징 → 커밋 메시지에 `Refs #<이슈번호>` → 푸시. main 병합과 이슈 종료는 사용자 요청 시에만 한다. 기존 미커밋 변경은 건드리지 않는다.
- 사용자에게 보이는 변경은 `program/android/PATCH_NOTES.md`에 버전별로 기록한다.
