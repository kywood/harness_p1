# 스크린샷 촬영 중 본 동작 다섯 가지 조사 (2026-11-14)

- 이슈: #186
- 담당: program
- 범위: 재현·원인 확인·보고. 코드는 고치지 않았다.
- 기준 코드: `origin/main` b504f06 (줄 번호는 이 시점 기준)
- 빌드: 위 커밋의 디버그 APK(`:app:assembleDebug`), emulator-5554

## 요약

| # | 본 동작 | 재현 | 판정 | 심각도 | 원인(후보) | 고치는 크기 |
|---|---|---|---|---|---|---|
| 1 | 이동 뒤 "Buffering playback data…"에서 멈춤(webm) | 못 함(에뮬레이터 과부하) | 버그 가능성 있음, 원인 미확정 | 중간 | 후보 A: VLC가 webm/mkv 이동 지점을 찾으려고 목표 앞 구간을 읽는데 엔진은 목표 지점만 먼저 받는다. 후보 B: 막대가 "일부만 받은 칸"도 받은 것으로 칠한다(받아진 것처럼 보일 뿐) | 확인 먼저. B는 ui 1곳 |
| 2 | 재생 시작 전 막대를 누르면 00:00 버퍼링 지속 | 못 함 | 표시 버그(작음) + 원래 느린 시작 | 낮음 | 길이를 모를 때도 막대가 켜져 있다. 누르면 손잡이만 움직이고 이동은 일어나지 않는다 | ui 1줄 |
| 3 | Stop 직후 다른 항목 Play → "Couldn't process the torrent" 한 번 | 못 함 | 버그(코드로 설명됨) | 중간 | 새 엔진이 자기 정리 작업을 10초만 기다리는데, 그 작업이 방금 멈춘 엔진의 정리(세션 종료) 뒤에 줄 서 있다 | engine 1파일 10줄 이내 + 테스트 |
| 4 | Simple/Detailed가 앱을 다시 열면 Detailed | 코드로 확인(기기에서 다시 해 보지는 않음) | 버그 아님(저장 기능이 없음). 정할 일 | 낮음 | `rememberSaveable`만 쓴다. 정렬·표시 필터도 같다 | 설정 필드 추가(작음, engine 아님) |
| 5 | 오프라인 스플래시 20~40초 | 오프라인 고유 지연은 재현 안 됨 | 앱 버그 아님(에뮬레이터·디버그 빌드 영향) | 낮음 | 온라인 8.3~9.9초, 오프라인 9.1~9.5초로 같다. 동의 요청은 오프라인에서 바로 실패하고 화면을 막지 않는다 | 없음(선택: 동의 요청을 첫 화면 뒤로) |

## 재현 환경의 한계 (먼저 읽을 것)

조사 시간 동안 emulator-5554가 계속 과부하였다. 1~3번은 시간에 민감한 동작이라 이 상태에서는 재현 결과를 믿을 수 없어 시도를 중단했다.

- 게스트 `uptime` load average: 6.4 → 10.7 → 22.2 → 28.0 (05:30~05:35, 에뮬레이터 시계). `top`에서 `sys` 275%, `android.hardware.sensors-service.multihal` 49%.
- 앱 화면 한 프레임에 1~6초(`Davey! duration=6683ms`), 클래스 검증 한 건에 1.2초(`9.05 bytecodes/s`).
- 시스템 앱(`permissioncontroller`, `carrierdefaultapp`, `vending`)도 ANR이 났고, PiecePlay도 콜드 스타트 중 "isn't responding" 창이 떴다(`Input dispatching timed out … Waited 5001ms for FocusEvent`, 05:27:21). 메인 스레드 코드가 막힌 것이 아니라 기기 전체가 느린 상태였다.
- 같은 에뮬레이터를 다른 작업이 함께 써서 조사 중 앱이 여러 번 강제 종료·재설치됐다(05:20~05:35 사이 설치 2회, 강제 종료 4회 이상). adb 서버도 두 번 다시 떴다.

원래 관찰도 같은 에뮬레이터에서 나왔으므로, 1·2·5번은 이 과부하가 증상을 키웠을 가능성이 크다. 1~3번은 한가한 에뮬레이터나 실기기에서 다시 확인해야 한다(아래 "다시 확인하는 방법").

## 1. 이동 뒤 버퍼링 멈춤 (Tears of Steel, webm)

### 코드 경로

1. 막대를 놓으면 `seekTo`(`VlcPlayerScreen.kt:257-269`)가 `seekUi.requested(...)`로 "Preparing the part you jumped to…"를 켜고, 엔진 큐에 `beginSeek(position, length)` → `mediaPlayer.time = position`을 순서대로 넣는다.
2. `beginSeek`(`TorrentEngine.kt:500-514`)는 이동 세대를 올리고(`seekRequests.beginSeek()`), 읽기 수요·deadline을 지우고, **시간 비율로 어림한 바이트 위치**(`SeekDownloadHint.offset`, 시간/길이 × 파일 크기)부터 긴급 창을 건다.
3. VLC가 새 Range 요청을 보내면 `TorrentStreamServer.serve`(`:55-57`)가 그 위치를 `prioritizeRequest(exactRange = true)`로 다시 건다. 이전 세대 스트림은 없는 조각을 만나면 `canWait()`가 false라 끊긴다(`VerifiedTorrentInputStream.kt:36`).
4. 화면 문구: `SeekUiState.timeChanged`가 VLC 시간이 목표 ±1.5초에 들어오면 `targetMs`를 지운다(`SeekUiState.kt:13-16`). 그 뒤에는 VLC `Buffering` 이벤트가 100 미만이면 "Buffering playback data…"다(`:17`, `VlcPlayerScreen.kt:192`).

본 문구가 "Buffering playback data…"였다는 것은 VLC가 이미 목표 시간을 보고했고(이동 자체는 받아들임), 그 뒤 데이터를 기다리며 100%에 이르지 못했다는 뜻이다.

### 원인 후보

**A. VLC가 읽으려는 곳과 엔진이 먼저 받는 곳이 다르다 (가장 유력, 미확인)**

- webm/mkv는 이동할 때 목표 시간의 클러스터 위치를 알아야 한다. Cues(색인)를 쓸 수 없으면 VLC mkv 디먹서는 아는 지점부터 목표까지 클러스터를 차례로 읽어 위치를 찾는다. 이 경우 VLC는 목표 지점이 아니라 **현재 위치와 목표 사이**를 읽고, 그 구간 조각이 모두 필요하다.
- 엔진은 `beginSeek`에서 어림한 목표 위치만 긴급으로 받는다. 그래서 막대에는 목표 지점이 받아진 것으로 보이는데 VLC는 그 앞 구간을 기다린다. 스트림이 기다리는 조각은 250ms마다 우선순위를 다시 요청하므로(`VerifiedTorrentInputStream.kt:39-42`) 영원히 멈추지는 않지만, 건너뛴 구간이 길면 그만큼 다 받아야 풀린다.
- "10초 이동"으로 풀린 것도 맞는다: 버튼은 `holder.mediaPlayer.time ± 10000`으로 이동하고(`VlcPlayerScreen.kt:401-425`), 새 `beginSeek`가 세대를 올려 기다리던 읽기를 끊는다.
- 관련: `playback-start-2026-11.md` 2.5절이 MKV Cues를 미리 받지 않는다는 점을 "미검증"으로 남겼다. `VideoFormat.MKV`·webm은 `needsMoovIndex`가 없어 색인 선행 요청이 없다.
- 확인하지 못한 것: 이 파일의 Cues 위치, VLC가 HTTP 입력에서 Cues를 읽는 시점. VLC 3 소스 기억에 근거한 추정이며 로그로 확인하지 못했다.

**B. 막대가 일부만 받은 칸도 받은 것으로 칠한다 (확인됨, 표시 문제)**

- 조각 맵은 최대 256칸이고 칸 하나가 여러 조각이다. `PlayerSeekBar`는 `cell != PieceCell.MISSING`이면 받은 색으로 칠한다(`PlayerSeekBar.kt:62-66`). 1~33%만 받은 칸(`LOW`)도 다 받은 것처럼 보인다.
- 그래서 "그 지점 조각이 받아졌는데도"는 화면만으로는 단정할 수 없다. 필요한 조각이 아직 없었을 수 있다.

**C. 어림 위치가 실제 위치와 어긋남 (작음)**

- 가변 비트레이트 영상에서는 시간 비율 위치가 실제 바이트와 다르다. 다만 VLC의 실제 Range 요청이 오면 그 위치로 창을 옮기므로 멈춤의 원인이 되기는 어렵다.

**D. 끊긴 연결의 읽기 수요가 남음 (작음)**

- VLC가 버린 연결의 서버 스레드는 조각을 기다리는 동안 소켓이 닫힌 것을 모른다. 같은 세대면 최대 60초 동안 자기 위치를 계속 긴급으로 요청해(`:39-45`) 대역폭을 나눠 쓴다. 멈춤이 아니라 느려짐이다.

### 판정과 방향

- 원인 미확정. A가 맞으면 "이동 뒤 오래 기다림"은 webm/mkv에서 실제 사용자에게도 일어난다(심각도 중간).
- 먼저 확인(아래 방법)한 뒤 정한다. A가 맞을 때의 방향:
  - mkv/webm도 재생 시작 때 Cues 조각을 선행 요청한다(`StartupPrefetchPlan`에 형식 추가, `VideoFormat`에 속성 등록). engine 파일 2~3개, 파일마다 10줄 이내 단계로 나눈다.
  - VLC가 실제로 요청한 Range 위치를 우선하고 어림 위치 창은 그 요청이 오면 내린다(현재도 pins로 함께 유지된다).
- B는 별도 작은 수정: 재생 막대에서는 `VERIFIED`(또는 `HIGH` 이상)만 받은 색으로 칠한다. `PlayerSeekBar.kt` 1곳, 색·토큰 추가 없음. 부분 칸을 다른 색으로 보이려면 design 요청이 필요하다.

## 2. 재생 시작 전 이동 (Big Buck Bunny)

### 코드 경로

- 조작 패널의 재생 막대는 항상 켜져 있다: `seekEnabled = true`(`VlcPlayerScreen.kt:433`). 밀어서 이동할 때 잠깐 보이는 막대는 `totalLengthMs > 0`일 때만 켠다(`:368`).
- 길이를 아직 모를 때(`totalLengthMs == 0`, VLC `LengthChanged` 전) 막대를 누르면 `onSeekChange`가 손잡이 위치만 바꾸고, `onSeekFinished`는 `totalLengthMs > 0`이 아니어서 `seekTo`를 부르지 않는다(`:435-445`). `seekTo`도 길이가 0이면 바로 돌아간다(`:258`).
- 즉 이 시점의 탭은 **엔진과 VLC에 아무 영향이 없다**. 시간 표시는 00:00으로 남고, "Buffering playback data…"는 VLC의 첫 버퍼링 이벤트 때문에 원래 떠 있던 것이다.

### 판정

- 탭이 버퍼링을 만든 것이 아니라, 느린 시작(과부하 에뮬레이터) 중에 막대가 눌리는 것처럼 보였을 가능성이 크다. 재현하지 못해 단정하지는 않는다.
- 길이를 안 뒤(`totalLengthMs > 0`) 첫 화면이 나오기 전에 누른 경우는 1번과 같은 경로다. 이때는 시간 표시가 누른 위치로 바뀌므로 "00:00" 관찰과는 맞지 않는다.
- 표시 버그는 맞다: 이동할 수 없는데 막대가 움직이고 손잡이가 누른 자리에 남는다.

### 방향

- `seekEnabled = totalLengthMs > 0`으로 바꾼다(`VlcPlayerScreen.kt:433`, 1줄, 색·문구 추가 없음). `VlcPlayerScreen.kt`는 300줄이 넘는 파일이지만 새 기능이 아니라 값 하나 수정이다.
- 판단을 `PlayerOverlayPolicy`에 두면(`canSwipe`와 같은 자리) JVM 테스트를 붙일 수 있다.

## 3. Stop 직후 Play 오류

"Couldn't process the torrent. Try again in a moment."는 `failure_engine`(`strings.xml:59`)이고, 엔진 작업 스레드의 바깥 `catch`에서만 나온다(`TorrentEngine.kt:386-390`).

### 원인(코드로 설명됨, 재현은 못 함)

1. Stop: `DownloadManager.stop` → `DownloadController.stop` → 엔진 큐에 `A.stop()`. `stop()`은 정리 작업을 `teardown.submit`으로 넘긴다(`TorrentEngine.kt:677-684`): resume 저장 대기(최대 1.5초) → 체크포인트 닫기(최대 1.5초) → 핸들 제거 → `sharedSessions.release`. A가 마지막 사용자면 여기서 libtorrent 세션을 끝낸다(`SessionManager.stop()`, `:104`). 트래커 정지 알림 등으로 몇 초 이상 걸릴 수 있다.
2. 정리 작업은 **모든 엔진이 스레드 하나를 같이 쓰고** 넣은 순서대로 돈다(`EngineTeardown.kt:43-46`).
3. Play: 새 작업 B는 새 `TorrentEngine`을 만든다. `startStreaming`은 처음에 항상 `stop()`을 부르므로(`:149`) B도 빈 정리 작업을 넣고, 그것은 A의 정리 뒤에 줄 선다.
4. B의 작업 스레드는 `check(teardown.await()) { "Previous download is still stopping" }`로 시작한다(`:161`). 대기 한도는 `TEARDOWN_AWAIT_MS = 10_000`(`EngineSpec.kt:9`)이다. A의 정리가 10초 안에 끝나지 않으면 `check`가 예외를 던지고, 바깥 `catch`가 `TorrentFailure.ENGINE`을 알린다.
5. 재생 루프는 작업이 `FAILED`가 되면 그 문구로 오류 화면을 띄운다(`MainViewModel.kt:229-233`). 다시 누르면 A의 정리가 끝나 있어 정상 동작한다. "한 번 남"과 맞는다.

다운로드가 하나뿐일 때 그것을 멈추면 A가 세션의 마지막 사용자가 되므로 가장 잘 난다. 느린 기기일수록 잘 난다.

다른 예외 지점도 봤다. `ActiveTorrentFiles.acquire`(`:237`)와 `TorrentCheckpoint.restore`의 `check`(`TorrentCheckpoint.kt:82`)는 **같은 항목**을 멈췄다 바로 재생할 때만 걸릴 수 있어 "다른 항목" 관찰과 맞지 않는다.

### 판정

- 버그. 심각도 중간: 데이터 손상은 없고 다시 누르면 되지만, 출시 빌드에서도 느린 기기·느린 트래커에서 날 수 있고 사용자는 이유를 알 수 없다.

### 방향 (PM이 고를 것)

- (가) 한 번도 시작하지 않은 엔진은 `stop()`에서 정리 작업을 넣지 않는다. 새 엔진은 남의 정리를 기다릴 이유가 없다(세션 공유는 `NativeSessionLease`가 잠금으로 순서를 지킨다). `TorrentEngine.stop()` 한 파일 10줄 이내.
- (나) 시간 초과를 실패로 보지 않고, `run == activeRun`인 동안 계속 기다린다(상한을 따로 둔다). `TorrentEngine.kt:161` 근처 10줄 이내.
- 권장: (가). (나)는 같은 엔진을 다시 시작하는 경우의 보완으로 다음 단계에 넣을 수 있다.
- 테스트: `EngineTeardown`에 실행기를 주입할 수 있으므로 "앞선 정리가 오래 걸려도 새 엔진의 대기가 실패하지 않는다"를 JVM 테스트로 쓴다. 실제 세션 종료가 걸린 흐름은 `androidTest`(중지 직후 다른 항목 재생)로 확인한다.

## 4. 보기 방식이 저장되지 않음

- `AppRoot.kt:51-53`: `simplePlaylist`, `playlistSort`, `playlistFilter` 모두 `rememberSaveable`이다. 화면 회전이나 시스템이 프로세스를 되살릴 때만 유지되고, 앱을 닫았다 열면 기본값(Detailed, 최근 재생순, 전체)으로 돌아간다.
- `AppSettings`(`data/AppSettings.kt`)에는 Wi-Fi 전용·자막 자동·자막 언어만 있다. 보기 방식을 저장하는 코드는 없다.
- `planning/screens/playlist.md`와 `planning/spec.md:85`는 "간단히/자세히"가 있다고만 적고 유지 여부는 정하지 않았다.
- 판정: 구현 누락이 아니라 정해지지 않은 요구사항이다. 기기에서 다시 해 보지는 않았고 코드로만 확인했다.
- 저장하기로 하면: `AppSettings`에 필드 추가(`AppSettingsCodec.VERSION` 2 → 3, 이전 버전 읽기 유지) + `MainViewModel`에 설정 변경 메서드 + `AppRoot`가 설정을 구독. engine 변경 없음, 코덱 단위 테스트 추가. 정렬·표시 필터도 함께 저장할지는 planning이 정한다.

## 5. 오프라인 스플래시 지연

### 측정 (emulator-5554, 디버그 빌드, `am start -W`의 TotalTime = logcat `Displayed`)

| 조건 | 콜드 스타트 |
|---|---|
| 설치 직후 첫 실행(온라인) | 16.5초(`am start -W`는 13초에서 시간 초과로 표시) |
| 온라인 | 9.1초, 8.3초, 9.9초 |
| 오프라인(Wi-Fi·데이터 끔, `Active default network: none`) | 9.4초, 9.5초, 9.1초 |

- 오프라인이라서 늘어나는 시간은 없다.
- 오프라인에서 동의 요청은 바로 실패한다: `W/AdConsent: Consent: 2 Error making request.`(실행 후 약 2초). 실패 콜백은 `refresh()`만 부른다(`AdConsent.kt:33-37`). `setContent`는 그 결과를 기다리지 않는다(`MainActivity.kt:47-49`).
- 앱 코드에 스플래시 대기나 초기화 대기는 없다(`StartupInitialization`이라는 코드는 없음). 시스템 스플래시는 첫 프레임까지 보이는 것이다.
- 한 번의 실행 타임라인(05:29:04 시작): 프로세스 시작 → 2.0초에 UMP가 WebView를 메인 스레드에서 올림(약 0.7초) → 2.8초~8.7초 첫 Compose 화면 구성·그리기(디버그 빌드 JIT, `Compiler allocated 5250KB to compile … performTraversals`) → 9.9초 `Displayed`.

### 판정

- 오프라인 고유 버그는 아니다. 20~40초는 설치 직후 첫 실행 + 에뮬레이터 과부하 + 디버그 빌드(APK 약 290MB, 프로필 없음)가 겹친 것으로 본다. 과부하가 심할 때는 같은 실행이 ANR 창까지 갔다.
- 확인하지 못한 것: 릴리스 빌드·실기기의 시작 시간. 출시 전 실기기에서 한 번 재는 것을 권한다(`adb shell am start -W`).
- 선택 개선(작음): `adConsent.gather(this)`를 첫 프레임 뒤로 미루면 WebView 로드(이 에뮬레이터에서 약 0.7초)가 첫 화면 뒤로 간다. `MainActivity.kt` 한 곳. "광고 요청 전에 동의를 먼저"라는 순서는 유지된다(광고 초기화는 `canRequestAds`를 본다).

## 다시 확인하는 방법 (1~3번)

한가한 에뮬레이터(load average 2 이하)나 실기기에서 디버그 빌드로 한다. 디버그 빌드는 VLC `-vvv` 로그가 켜져 있다(`VideoPlaybackPolicy.options`).

1. 이동 멈춤: Tears of Steel(webm)을 재생하고 받지 않은 뒤쪽으로 막대를 옮긴다. `adb -s emulator-5554 logcat | grep -iE "mkv|matroska|cues|http|TorrentEngine"`로 VLC가 이동 뒤 어떤 위치를 읽는지 본다. Range 요청 위치를 보려면 `TorrentStreamServer.serve`에 디버그 전용 로그(위치만, 파일 이름 없이)를 임시로 넣는다. 같은 조작을 mp4(Big Buck Bunny)로 해 형식 차이를 비교한다.
2. 시작 전 이동: 재생 화면이 뜬 직후(시간 표시 `00:00 / 00:00`) 막대를 누르고, 누르지 않았을 때와 첫 화면까지의 시간을 비교한다.
3. Stop 직후 Play: 다운로드를 하나만 켜 둔 상태에서 Stop을 누르고 2초 안에 다른 항목을 Play한다. `logcat | grep "Unhandled error in torrent streaming thread"`에 `IllegalStateException`이 찍히면 3번 원인이 맞다.

## 검증 기록

- 실행: `:app:assembleDebug`(성공), 에뮬레이터 콜드 스타트 측정 8회(위 표), Big Buck Bunny 마그넷을 링크 시트로 열어 재생 시작 1회(메타데이터 단계에서 다른 작업이 앱을 종료).
- 실행하지 못함: 1·2·3번 재현(에뮬레이터 과부하·공유), 4번 기기 확인, 릴리스 빌드·실기기 시작 시간, `:app:testDebugUnitTest`(코드 변경 없음).
- 조사 중 에뮬레이터 네트워크를 두 번 껐다 켰다(오프라인 측정). 끝난 뒤 Wi-Fi·데이터를 다시 켰다. 화면 크기·언어는 바꾸지 않았다.
