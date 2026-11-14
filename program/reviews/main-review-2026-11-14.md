# main 전체 코드 리뷰 (2026-11-14, 1차 글로벌 출시 제출 전)

- 대상: `origin/main` d37e3f7 (PR #189 병합 시점). 중점 범위 `7e2ffcc..origin/main`(앱 소스 193개 파일, +8968/−756)
- 리뷰어: reviewer 에이전트. 코드는 고치지 않았다.
- 이슈: #194

## 판정: 출시 가능

출시를 막을 높음 항목은 없다. 크래시·ANR·개인정보 노출로 이어지는 경로는 찾지 못했다. 중간 항목 2건은 드문 조건에서만 생기며, 출시 후 첫 패치에서 고치면 된다. 단, M1(재생 목록 전체 유실)은 고치는 양이 작으므로 제출 전에 넣을 여유가 있으면 넣는 것을 권한다.

## 돌린 검증

| 항목 | 결과 |
|---|---|
| `:app:testDebugUnitTest` | 통과(643개, 실패 0) |
| `:app:lintDebug` | 오류 0, 경고 33(GradleDependency 6, UnusedResources 5, IconLauncherShape 5, UseTomlInstead 4, UseKtx 4 등). 출시를 막는 항목 없음 |
| `code-review` 스킬(effort high, `7e2ffcc..origin/main`) | 9건 보고 → 직접 코드로 다시 확인해 아래에 반영(중복·의도된 동작은 제외 사유 표기) |
| `security-review` 스킬(같은 구간) | 신뢰도 8 이상 취약점 0건. 낮은 후보는 L 항목에 반영 |
| 에뮬레이터·실기기 | 지시에 따라 쓰지 않았다(계측 테스트 미실행) |

## 출시 전에 꼭 고칠 것

없음.

## 출시 후 고칠 것

### 중간

#### M1. 재생 목록을 한 번 읽지 못하면, 다음 기록이 목록 전체를 빈 목록으로 덮어쓴다
- 위치: `engine/MagnetHistory.kt:9-26`(`list()`가 모든 예외를 `emptyList()`로 바꿈), `engine/TorrentCache.kt:21-24`(`read()`가 복호화 실패를 `null`, 즉 "파일 없음"과 같은 값으로 돌려줌). 이 결과를 받아 다시 쓰는 곳: `add`·`markPlayed`·`delete`·`savePosition`·`updateTitle`, 그리고 이번에 새로 생긴 `setDownloadOrder`(`MagnetHistory.kt:57-60`)
- 실패 시나리오: Keystore AES-GCM 복호화가 한 번이라도 실패하면(Keystore 서비스 일시 오류, 기기 업데이트 직후 등) `list()`가 `[]`를 돌려준다. 바로 이어서 재생(`markPlayed`/`addForPlayback`), 위치 저장(재생 중 주기적으로 일어남), 받는 순서 변경 중 하나가 일어나면 `write([])` 또는 `write([새 항목 1개])`가 그대로 저장된다. 그러면 저장한 링크 최대 50개와 이어보기 위치가 모두 사라진다. 파일 쓰기 자체는 임시 파일 + move로 원자적이라, 실제 유실 경로는 이 읽기 실패뿐이다.
- 확인 수준: 코드로 확인(유실 경로). 실제로 얼마나 자주 일어나는지는 추정(드묾)
- 고치는 방향: `TorrentCache.read`에서 "파일 없음"(null)과 "읽기·복호화 실패"(예외)를 나눈다. `MagnetHistory`의 쓰기 함수들은 읽기에 실패하면 쓰지 않고 예외를 던진다(호출하는 쪽은 이미 `runCatching`·`try`로 감싸 둠). 화면 표시용 `list()`는 지금처럼 빈 목록을 돌려도 된다. 변경량은 `engine/` 2파일, 파일마다 10줄 이하. 단위 테스트 1개(복호화가 실패하는 cipher로 `add` 후 원본 바이트가 그대로인지)
- 비고: 이 패턴은 이번 구간 전부터 있었다. 이번 구간에서는 사용자가 직접 누르는 진입점(`setDownloadOrder`)이 하나 늘었다.

#### M2. 자막 2편짜리 영화(Movie.1 / Movie.2, CD1 / CD2)에서 다른 편의 자막이 자동으로 켜진다
- 위치: `engine/SubtitleFilePlan.kt:51-52`(`singleVideo` 판정, `PACK_MIN_BIG_VIDEOS`)
- 실패 시나리오: 한 폴더에 `Movie.1.mkv`(4GB), `Movie.2.mkv`(4GB), `Movie.1.ko.srt`가 있다. `Movie.2`를 재생하면 다른 큰 영상이 1개뿐이라 `PACK_MIN_BIG_VIDEOS`(2) 미만이 되어 `singleVideo = true`로 판정된다. 회차 표기(SxxEyy)도 없어서 시즌 팩으로 보지 않는다. 그래서 언어 표기가 있는 `Movie.1.ko.srt`가 대체 후보로 허용되고, 한국어 자동 선택이 1편 자막을 2편 재생에 켠다. 이번 구간 전에는 25% 이상인 다른 영상이 하나만 있어도 대체 후보를 막았다.
- 확인 수준: 코드로 확인(판정식을 그대로 따라감). 단위 테스트에는 이 배치가 없다
- 고치는 방향: 같은 폴더에 크기가 비슷한 영상(예: 80% 이상)이 하나라도 있고 이름의 마지막 숫자(편 번호)가 다르면 시즌 팩처럼 이름 일치만 허용한다. `SubtitleFilePlan` 한 파일 10줄 이하 + `SubtitleFilePlanTest`에 케이스 2개. 시즌 팩 기준값 문제는 #138에 이미 있으므로 그 이슈에 같이 묶어도 된다
- 비고: 사용자가 자막 시트에서 끌 수 있으므로 피해가 작아 중간으로 둔다.

### 낮음

| # | 위치 | 내용 | 확인 수준 | 고치는 방향(크기) |
|---|---|---|---|---|
| L1 | `ui/SubtitleSession.kt:143-153` (`matchNewTracks`) | 처음 목록을 받은 뒤 새로 나타난 자막 트랙 id를, 기다리는 외부 파일에 도착 순서대로 붙인다. TS처럼 내장 자막 ES가 늦게 나타나는 스트림에서 외부 `ko.srt`를 추가한 직후 내장 영어 트랙이 먼저 보이면, 그 트랙이 "한국어(파일)"로 표시되고 자동 선택된다 | 추정(VLC가 트랙을 늦게 알리는 순서에 따라 다름. MKV는 보통 처음에 한꺼번에 알림) | 새 트랙의 이름·언어(`addSlave`가 붙이는 파일 이름)로 맞추고, 맞지 않으면 내장 트랙으로 둔다(작음) |
| L2 | `ui/IncomingLinkParser.kt:81` | magnet은 공백에서만 끝나므로 `magnet:?xt=urn:btih:<hex>확인하세요`처럼 바로 뒤에 붙은 한글이 링크에 포함된다. 그러면 `INVALID_MAGNET`가 나거나 `tr`/`dn` 값이 오염된다. #138의 "한글 `dn`" 결정과 맞바꾼 결과다 | 코드로 확인 | `xt` 값만은 hex/base32 길이에서 끊는다(작음). #138에 덧붙이면 충분 |
| L3 | `ui/DownloadController.kt:43` | `order(key)`(`DownloadOrderLookup`: 재생 목록 복호화·파싱)를 앱 전체 엔진 명령 큐 위에서 부른다. 작업 3개를 동시에 시작하면 재생 준비 폴링(250ms)이 그만큼 밀린다. 메인 스레드는 아니므로 ANR은 아니다 | 코드로 확인 | `withContext(Dispatchers.IO)`에서 순서를 먼저 구한 뒤 큐로 넘긴다(2~3줄) |
| L4 | `engine/TorrentEngine.kt:564` | 뒤쪽 `moov` MP4는 인덱스 조각이 올 때까지 250ms마다 `refreshStartupIndex()`(파일 읽기)를 엔진 잠금을 잡은 채로 한다. 스트림 서버의 `prioritizeRequest`·`openRead`가 그 뒤에서 잠깐씩 기다린다. 읽는 양은 박스 헤더 몇 개뿐이라 체감은 작다 | 코드로 확인, 영향은 추정 | 마지막으로 확인한 조각 수가 늘었을 때만 다시 읽는다(작음) |
| L5 | `engine/TorrentEngine.kt:559-571` | `playbackReady()`는 `@Synchronized`가 아닌데 `startupPieces = emptySet()`를 쓴다. 엔진 작업 스레드의 `updateStartupIndex()`(잠금 안)가 같은 필드에 조각을 더하는 것과 겹치면 더한 조각이 사라질 수 있다. 인덱스가 준비된 뒤에만 이 줄에 오므로 실제로 겹칠 여지는 좁다 | 추정 | `playbackReady` 본문을 엔진 잠금 안으로 옮긴다(1줄, 다른 잠금 진입점과 같은 큐 스레드라 추가 대기 없음) |
| L6 | `engine/TorrentEngine.kt:164` | 이전 정지를 기다리다 시간이 넘으면 `check(...)`(IllegalStateException) → 바깥 catch에서 일반 `TorrentFailure.ENGINE` 문구가 나온다. 사용자는 "잠시 뒤 다시 시도"(`STOP_PENDING`) 대신 엔진 오류를 본다 | 코드로 확인 | `throw TorrentFailure.STOP_PENDING.blocked()`로 바꾸고 바깥에서 그대로 전달(작음) |
| L7 | `engine/TorrentStreamServer.kt:9` | `/stream`은 `127.0.0.1` 임의 포트에 묶이지만 토큰이 없다. 같은 기기에서 INTERNET 권한이 있는 다른 앱이 포트를 찾아 지금 재생 중인 영상 바이트를 읽을 수 있다. 다른 기기에서는 접근할 수 없다. 이번 구간에서 바뀌지 않았고, 콘텐츠는 공개 토렌트다 | 코드로 확인 | 경로에 재생마다 바뀌는 무작위 토큰(`/stream/<token>`)을 넣고 불일치면 404(작음). 개인정보 관점의 방어선이라 출시 후 |
| L8 | 커밋 단위 | `engine/` 한 파일·10줄 규칙을 넘은 단일 커밋: `StartupPrefetchPlan`(+36, 6f7ac5e), `DownloadOrderPreference`(+32, ec50626), `SameKeyTeardowns`(+28, 66d67b8). 새 순수 객체 파일이라 위험은 낮다 | 코드로 확인 | 규칙에 "새 순수 객체 파일은 예외"를 적거나 PR 리뷰에서 걸러 낸다(문서) |

## 다시 적지 않은 항목(이미 이슈에 있음 또는 의도된 동작)

- `SameKeyTeardowns`가 같은 키를 다시 시작할 때만 항목을 지움, 정지 직후 다른 항목 재생 시 엔진 큐 정체, 대기 상한 최악 20초 → #186 댓글(PR #189 리뷰 ①②③). 참고로 code-review 스킬은 "A의 `EngineTeardown.pending`이 다른 키의 정지로 바뀌어 과대 대기"를 지적했다. 정지를 처리하는 실행기가 스레드 하나라 순서대로 끝나므로, 결과는 기다림이 길어지는 것뿐이고 잘못된 해제는 아니다. #186 ③과 같은 후속으로 묶는다.
- 재생이 시작될 때 꼬리 prefetch를 비우는 것(code-review 스킬이 "MKV Cues를 버린다"고 지적) → `TailPrefetchNativeTest.playingClearsThePrefetchSoSeeksDoNotPinTheTailAgain`이 검증하는 의도된 동작이다. VLC가 Cues를 읽으면 Range 요청으로 다시 우선순위를 받으므로 실패가 아니라 왕복 한 번이 늘 뿐이다. 측정은 #150에서 한다.
- 시즌 팩 25% 기준, `Show.srt` 과일치, 32,768자 경계, 한글 `dn` → #138
- libtorrent `TorrentInfo.bdecode` SIGSEGV(계측 전체 실행 중 간헐) → #169. 원인 미확정. 출시 후 Play Console 비정상 종료 보고에서 `bdecode` 스택이 나오는지 지켜봐야 한다.
- 정지 시 logcat에 남는 `InterruptedException` "Unhandled error" → #186 댓글

## 영역별 확인 결과(문제 없음)

- **엔진 상호작용**
  - `startStreaming` → `stop()` → `activeRun++` 뒤 각 비동기 단계(180·230·239·271·274·342줄)에서 세대를 다시 확인한다.
  - `requestPlayback`은 `prioritiesSetRun == activeRun`일 때만 sequential을 끄므로, 작업 스레드의 `applyDownloadOrder`(288줄)가 재생 설정을 덮지 않는다.
  - `setDownloadOrder`는 재생 중이면 `orderPlaybackFocused`를 유지해 sequential을 켜지 않는다. `releasePlaybackPriority`는 사용자 순서로 돌려놓는다.
  - `EngineStopPlan`: `worker`가 한 번이라도 시작됐으면 항상 정지 처리를 넣으므로, `ActiveTorrentFiles` 해제가 빠지지 않는다.
- **ViewModel·큐**
  - `stopStream`·`DownloadController.stop`·`applyOrder`는 모두 `commands.post`로 엔진을 부른다. 메인 스레드에서 엔진 잠금을 잡는 호출을 찾지 못했다.
  - `deleteSavedMagnet`은 `PlaylistWriteGate` 안에서 멈춤(메인) → `awaitIdle`/`awaitStoppedWrites`(IO) → 삭제 순서를 지킨다.
  - `DownloadOrderSync`는 Mutex로 계산과 적용을 묶는다.
  - `WhileSubscribed(0)` 흐름은 모두 `playbackSource`에서 `flatMapLatest`하므로 엔진을 바꿔도 남는 구독이 없다.
- **보안**
  - 링크(VIEW/SEND)는 시트 확인 전에는 시작하지 않는다. `http://`를 거부하고, 리다이렉트도 프로토콜을 다시 확인한다(`TorrentInputResolver`).
  - 클립보드는 버튼을 누를 때 텍스트만 읽는다.
  - 자막·자막 사본 경로는 `DownloadPathGuard.resolve`를 거친다(`SubtitleDownloads.kt:52`, `SubtitleCopies`).
  - 새 로그는 `BuildConfig.DEBUG`로 막혀 있거나 `EngineFailureLog.describe`(릴리스에서는 클래스 이름만)를 거친다. `PlaybackStartTrace`는 키 앞 8자만 남긴다.
  - `MagnetHistory` -4 포맷은 계속 `TorrentCache` 암호화를 거치고 -1..-4를 읽는다.
  - 설정·재생 목록은 `noBackupFilesDir`에 있다. 매니페스트 변경은 주석뿐이다.
- **광고**
  - 배너(`TestBannerAd`)와 전면 광고 preload(`MainActivity`)는 `canRequestAds`가 참일 때만 SDK를 초기화하고 로드한다.
  - 전면 광고 표시는 `onPlaybackFinished`에서만 한다. 재생 중 preload는 로드만 하고 화면에 띄우지 않는다.
- **리소스**: `VlcPlayerScreen`이 화면을 떠날 때 `detachViews` → `mediaPlayer.release` → `libVlc.release` 순서로 해제한다. 자막 `Media`도 `VlcTrackPlayer.kt:51`에서 해제한다.

## 확인하지 못한 것

- 계측 테스트(`connectedDebugAndroidTest`)와 실기기 동작: 지시에 따라 실행하지 않았다. 정지 후 2초 안에 다른 항목 재생, 같은 항목 정지 후 바로 재생 같은 수동 시나리오는 #186에 남아 있다.
- 릴리스 빌드(`packageReleaseFiles`)와 R8 결과물: 이번에는 빌드하지 않았다.
- M1이 실제로 얼마나 자주 일어나는지(Keystore 일시 오류 빈도), L1의 VLC 트랙 알림 순서: 기기 로그가 있어야 확인할 수 있다.
