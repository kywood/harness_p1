# 바로 재생 시작 지연·조각 흩어짐 조사 (2026-11)

- 이슈: #150
- 담당: program
- 범위: 원인 조사와 개선안. 코드는 고치지 않았다.
- 기준 코드: `origin/main` 73fc2e4 (엔진 파일은 이 시점 기준 줄 번호)

## 사용자 보고

> 다운받는 게 조각이 랜덤인지, 바로 재생이 가능한 경우가 잘 없음

1. [바로 재생]을 눌러도 Playing까지 오래 걸리거나 시작하지 않는다.
2. 조각이 순서대로가 아니라 흩어져 받아지는 것처럼 보인다.

## 요약

| 순위 | 원인 | 영향 | 근거 |
|---|---|---|---|
| 1 | 받기 전용 작업(받아 두기, 재생 전 대기열)은 우선순위 없이 libtorrent 기본 rarest-first로 받는다. 몇십 % 받아도 파일 앞부분·MP4 moov가 없어 "바로 재생"이 즉시 되지 않고, 피스 맵도 흩어져 보인다. | 큼 | `DownloadController.kt:61`, `TorrentEngine.kt:266, 274-296, 303` |
| 2 | 메타데이터를 받은 핸들을 지우고 같은 토렌트를 새로 추가한다. 이미 연결된 피어를 모두 끊고 트래커·DHT 탐색과 핸드셰이크·unchoke를 처음부터 다시 한다. | 큼(마그넷 첫 재생) | `MetadataFetch.kt:44-46`, `TorrentEngine.kt:250-257`, `TorrentCheckpoint.kt:62-86` |
| 3 | 업로드 0(unchoke slot 0, allowed fast 0). 다른 leecher에게는 optimistic unchoke로만 받는다. seed가 적은 스웜에서 첫 바이트가 늦고 속도가 낮다. | 중간(스웜에 따라 큼) | `DownloadOnlyPolicy.kt:4-5`, `TorrentEngine.kt:100-103` |
| 4 | 파일 끝 조각 TOP_PRIORITY가 재생 우선순위 적용 때 바로 덮어써진다. MP4 moov가 끝에 있으면 박스 헤더 → moov 전체 순서로 1초 폴링마다 한 단계씩 찾아간다. MKV Cues는 아예 미리 받지 않는다. | 중간(moov가 끝인 MP4) | `TorrentEngine.kt:289-296` vs `418-421`, `StartupIndexState.kt:14-30`, `TorrentEngine.kt:368, 380-387`, `VideoFormat.kt:22` |
| 5 | 재생 창 밖의 나머지 조각을 priority 1로 계속 받는다. deadline 조각을 가진 피어는 그것을 먼저 요청하므로 낭비는 작지만, 피스 맵에서는 흩어진 조각으로 보인다. | 작음(속도) / 큼(보이는 모습) | `PlaybackDownloadPolicy.kt:4-10`, `TorrentEngine.kt:412-421` |
| 6 | 동시 다운로드 3개가 재생 작업과 대역폭을 나눈다. 재생 중인 작업을 우선하는 장치가 없다. | 상황에 따라 큼 | `DownloadManager.kt:131-144` |
| - | `isPlaybackReady` 조건이 지나치게 엄격한가 | 아님 | 시작 구간 2초(약 2MB, 조각 단위 올림) + moov. 아래 "판단 근거" |
| - | 시작 버퍼 크기 | 조각 크기에 좌우 | 조각이 16MB면 첫 조각 16MB 전체가 필요하다(검증 단위가 조각) |

측정 결과는 "실측" 절에 있다.

## 1. 재생 시작 전 다운로드 순서

### 1.1 재생도 받기 전용 엔진으로 시작한다

`MainViewModel.startResolved`(`MainViewModel.kt` `startResolved`)는 `downloads.start(magnet)`로 다운로드 작업을 만들고, 그 작업의 엔진을 `downloads.borrow(key)`로 빌린다. 다운로드 작업은 항상 `prioritizePlayback = false`로 엔진을 시작한다(`DownloadController.kt:61`). 그래서 `TorrentEngine.startStreaming` 안에서는:

- `startupPieces = emptySet()`, `startupIndex = null` (`TorrentEngine.kt:226-229`)
- 네이티브 sequential 끄기 (`:266`), 선택 영상만 `Priority.DEFAULT`, 나머지 파일 `IGNORE` (`:274-282`)
- 첫 2조각, 마지막 2조각만 `TOP_PRIORITY` (`:289-296`), deadline 없음
- `prioritizeOffset(0)` 호출 안 함 (`:303`의 `if (prioritizePlayback)`), 시작 조각 deadline 없음 (`:304`, `startupPieces`가 비어 있음)

재생 우선순위는 ViewModel이 250ms마다 돌리는 루프에서 `selectedVideoFile`이 생긴 뒤 `requestPlayback()`을 불러야 걸린다(`MainViewModel.kt` `startSharedPlayback`, `TorrentEngine.kt:491-502`). `requestPlayback`은 `currentHandle`이 없으면 false를 돌려 다음 폴링에서 다시 시도한다(`:494`). 정상 흐름에서는 핸들이 생긴 뒤 250ms 안에 걸리므로 재생 경로 자체의 지연은 작다.

받기 전용 작업(재생 목록의 받아 두기, 링크 추가 창, 링크 확인 시트의 [받아 두기])은 `requestPlayback`이 불리지 않으므로 **끝까지 rarest-first**로 받는다. 이 앱은 업로드를 하지 않으므로(`DownloadOnlyPolicy`) rarest-first가 스웜에 주는 이점도 없다.

### 1.2 requestPlayback 뒤 우선순위

`requestPlayback` → `beginSeek(0, 1)` → `prioritizeOffset(0)` (`TorrentEngine.kt:474-488, 394-442`).

| 조각 | priority | deadline | 근거 |
|---|---|---|---|
| 시작 구간 `StartupBufferPlan.required` (2초 × 1MB/s = 2MB, 조각 단위 올림) | 7 | 1000ms | `StartupBufferPlan.kt:6-13`, `TorrentEngine.kt:431-433` |
| MP4 인덱스 탐색 중 필요한 조각(`StartupIndexState.update`) | 7 | 1000ms | `TorrentEngine.kt:380-387` |
| 재생 위치 앞 긴급 창(30초 분량, 4~64조각, 최대 64MB) | 7 → 4 (앞에서부터 7,6,5,4,4…) | `index × msPerPiece` (첫 조각 0ms) | `SequentialPiecePlan.kt:19-27, 43-48`, `TorrentEngine.kt:435-438` |
| VLC가 연 Range 읽기 위치마다의 창 | 7 | 1000ms | `TorrentEngine.kt:406-411` |
| 나머지 영상 조각 | 1 (재생 위치 앞 8MB를 모두 받은 뒤에는 4) | 없음 | `PlaybackDownloadPolicy.kt:4-10`, `TorrentEngine.kt:412-421` |

- 시작 구간이 긴급 창 첫 조각과 겹치면 긴급 창의 deadline(0ms)이 나중에 덮어쓴다(`:431-438` 순서). 문제는 아니다.
- **파일 끝 2조각의 TOP_PRIORITY는 여기서 사라진다.** `backgroundPiecePriority`가 처음에 `-1`이라 `:418-421`이 모든 조각에 `StartupBufferPlan.priority(...)`를 다시 매기고, 끝 조각은 pins·창에 없으므로 priority 1이 된다. `:288`의 주석("index metadata")이 의도한 끝부분 선행 요청은 재생 경로에서 동작하지 않는다.
- `initial_picker_threshold = 0`(`TorrentEngine.kt:99`)이라 처음 몇 조각을 무작위로 고르는 동작은 꺼져 있다.

### 1.3 대역폭이 흩어지는가

- 나머지 조각이 priority 1로 남아 있으므로 0(받지 않음)은 아니다. libtorrent 피커는 각 피어가 가진 조각 중 priority가 높은 것부터 요청하고, deadline 조각은 time-critical 경로(`request_time_critical_pieces`)로 가장 빠른 피어에게 먼저 요청한다. 따라서 **긴급 조각을 가진 피어의 대역폭은 대부분 긴급 조각에 쓰인다.**
- 긴급 조각을 갖지 않은 피어는 priority 1 조각을 rarest-first로 받는다. 이것이 재생 시작을 직접 늦추지는 않지만 피스 맵에서는 흩어진 조각으로 보인다.
- 실제로 대역폭을 빼앗는 것은 **같은 기기에서 함께 돌아가는 다른 다운로드 작업**이다. 작업 사이에는 우선순위가 없고 세션 하나(`NativeSessionLease`)를 공유한다(`DownloadManager.kt:131-144`는 슬롯이 꽉 찼을 때 재생 작업을 위해 하나를 멈출 뿐이다).

## 2. 바로 재생이 늦어지는 원인 후보

### 2.1 메타데이터 핸들을 지우고 다시 추가 (큼)

- `MetadataFetch.fetch`는 메타데이터를 받으면 `finally`에서 자기가 추가한 핸들을 `session.remove(handle)` 한다(`MetadataFetch.kt:44-46`).
- 이어서 `TorrentCheckpoint.restore`가 같은 info-hash를 새로 `addTorrent` 한다(`TorrentCheckpoint.kt:62-86`, `TorrentEngine.kt:250-257`).
- 결과: 메타데이터 단계에서 이미 연결한 피어(메타데이터를 준 피어 포함)를 모두 끊고, 트래커 announce·DHT get_peers·핸드셰이크·interested/unchoke를 처음부터 다시 한다. 업로드 0이라 unchoke를 다시 얻는 데도 오래 걸린다(2.3).
- 캐시된 메타데이터가 있는 두 번째 재생부터는 이 단계가 없다(`MetadataStartup.kt:6`). 마그넷 첫 재생만 해당한다. `.torrent` HTTP 링크는 `resolveInput`이 메타데이터를 캐시하므로(`TorrentEngine.kt:565-568`) 해당하지 않는다.

### 2.2 받기 전용으로 받던 영상 (큼, 사용자 보고와 가장 잘 맞음)

- 1.1처럼 받아 두기 중인 작업은 rarest-first다. 재생 목록에서 50%를 받은 항목이라도 첫 조각과 moov를 받았을 가능성은 50% 정도에 그친다(조각마다 독립에 가깝다고 보면 첫 조각 + moov 1~2조각을 모두 가졌을 확률은 그보다 낮다).
- 사용자는 "꽤 받았는데 바로 재생이 안 된다"로 느끼고, 피스 맵은 흩어져 보인다. 보고 문장의 두 부분이 같은 원인으로 설명된다.

### 2.3 업로드 0 (중간, 스웜에 따라 큼)

- `DownloadOnlyPolicy.settings`: `fixed_slots_choker`, `unchoke_slots_limit = 0`, `allowed_fast_set_size = 0` (`DownloadOnlyPolicy.kt:4-5`, 적용 `TorrentEngine.kt:100-103`).
- seed는 상대의 업로드 기여를 보지 않고 돌아가며 unchoke하므로 seed가 많은 스웜에서는 영향이 작다.
- leecher는 tit-for-tat으로 자기에게 보내 주는 피어를 unchoke하고, 나머지는 optimistic unchoke(보통 30초마다 1슬롯)로만 준다. seed가 적고 leecher가 많은 스웜에서는 첫 바이트까지 수십 초가 걸릴 수 있다.
- allowed fast 0은 우리가 남에게 주는 것이라 받는 속도와는 무관하다.
- 업로드를 하지 않는 것은 이용 안내·개인정보 문서에 적힌 제품 정책이므로 바꾸려면 PM·launch 결정이 필요하다.

### 2.4 MP4 moov가 파일 끝 (중간)

- `StartupIndexState.update`는 `Mp4IndexProbe`로 최상위 박스 헤더를 앞에서부터 하나씩 읽는다. 헤더가 들어 있는 조각이 없으면 그 조각을 요청(`Need`)하고, 다음 엔진 폴링(1초, `TorrentEngine.kt:368`)에서 다시 읽는다.
- moov가 끝에 있으면: 첫 조각(ftyp, mdat 헤더) → 1초 폴링 → moov 헤더가 든 끝 조각 요청 → 도착 → 1초 폴링 → moov 전체 조각 요청 → 도착 → Playing. 끝 조각 왕복이 두 번 직렬로 일어난다.
- 1.2처럼 끝 조각 선행 요청이 덮어써지므로, 이 왕복을 줄여 줄 장치가 실제로는 없다.
- moov 크기는 2시간 영상에서 수 MB~수십 MB다. 조각이 크면(8~16MB) 1~3조각이다.

### 2.5 MKV Cues (작음, 미검증)

- `VideoFormat.MKV`에는 `needsMoovIndex`가 없어 `StartupIndexState.ready = true`로 시작한다(`StartupIndexState.kt:9`, `VideoFormat.kt:22`). Playing 판단은 시작 구간만 본다.
- VLC가 열면서 파일 끝 Cues를 읽으러 가면 그 Range 요청이 `prioritizeRequest` → `prioritizeOffset(끝 위치)`로 긴급 창을 끝으로 옮긴다(`TorrentStreamServer.kt` serve, `TorrentEngine.kt:459-463`). 이 동안 화면은 Playing인데 영상이 멈춘 상태가 된다. VLC의 HTTP 입력에서 MKV를 열 때 Cues를 바로 읽는지는 확인하지 못했다(실측 대상).

### 2.6 시작 버퍼 크기 / `isPlaybackReady` 엄격성 (원인 아님)

- `playbackReady()`(`TorrentEngine.kt:512-530`): moov 확보(`startupIndex.ready`) + 시작 구간 조각 전부. 시작 구간은 `STARTUP_BUFFER_SEC = 2` × `DEFAULT_BYTES_PER_SECOND = 1MB/s` = 2MB(`EngineSpec.kt:20, 29`)로 작다.
- 조각 검증 단위가 조각이므로 실제로 필요한 양은 `max(2MB, 조각 크기)`다. 4MB 조각이면 1조각, 16MB 조각이면 16MB.
- 이보다 줄이면 VLC가 첫 Range에서 바로 막혀 Playing 뒤 멈춤으로 바뀔 뿐이다. 조건을 느슨하게 하는 것은 개선안으로 권하지 않는다.

### 2.7 메타데이터 단계 (스웜에 따라)

- 피어 0명이 5초 이어지면 DHT·트래커를 다시 요청하고 간격을 15초 → 30초로 늘린다(`MetadataDiscovery.kt`). 최대 120초(`MetadataFetch.kt:9`).
- 엔진 시작 직후 세션을 만들고 `Thread.sleep(500)`을 둔다(`TorrentEngine.kt:149`). 첫 실행에서 DHT 부트스트랩이 끝나기 전에 시작하면 DHT로 피어를 찾는 데 수 초가 더 걸린다. 세션은 엔진들이 공유하므로 두 번째부터는 해당하지 않는다.

### 2.8 경합: 파일 우선순위가 재생 우선순위를 덮을 수 있음 (드묾)

- `currentHandle`은 `:254`에서 생기고, 파일 우선순위는 그 뒤 `:274-282`에서 매긴다. ViewModel 폴링이 그 사이에 `requestPlayback`을 부르면 이후 `filePriority(i, DEFAULT)`가 조각 priority를 4로 되돌린다(deadline은 남는다).
- `prioritizeOffset`은 창·pins·background가 같으면 일찍 돌아가므로(`:416`) 다시 매기지 않는다. 간격이 수 ms라 드물다. 개선안 A를 넣을 때 함께 막는다.

## 3. 피스 맵이 보여 주는 것

- `PieceMapSnapshot.capture`(`PieceMapSnapshot.kt:7-33`): 영상 파일 조각을 최대 256칸으로 묶고, 칸마다 **해시 검증이 끝난 조각의 비율**을 보여 준다(완료 / 1–33% / 34–66% / 67–99% / 미완료). 받는 중인 조각, priority, deadline, 재생 위치는 보이지 않는다.
- 엔진은 1초마다 `havePiece`로 갱신한다(`TorrentEngine.kt:315-318`). 재생 목록 카드는 이 스냅샷(또는 저장된 진행 상태)을 그대로 그린다(`MagnetHistoryScreen.kt:102-105, 330`).
- 그래서 흩어져 보이는 이유는 실제로 흩어져 받기 때문이다.
  - 받기 전용 작업: rarest-first라 전 구간에 고르게 흩어진다(2.2).
  - 재생 중: 긴급 창은 앞에서부터 차지만 나머지 priority 1 조각이 여기저기 함께 찬다(1.3). moov가 끝인 MP4는 맨 끝 칸이 먼저 찬다.
  - 256칸으로 묶으므로 조각이 많은 토렌트에서는 한 칸에 여러 조각이 들어가 "부분" 색이 여기저기 생긴다.
- 표시만의 문제는 아니다. 다만 "받은 순서"가 아니라 "가진 조각"이므로 재생 위치 앞이 차고 있는지는 이 그림으로 알기 어렵다.

## 4. 실측

### 조건

- 에뮬레이터 emulator-5554(`Medium_Phone`, x86_64), 디버그 빌드(`origin/main` 73fc2e4), 앱 데이터를 지운 뒤 1회.
- 입력: Sintel 공개 마그넷(info-hash `08ada5a7…5a10`, UDP 트래커 3개, webseed `ws=https://webtorrent.io/torrents/`). 링크 열기 인텐트로 메인 입력란에 넣고 [바로 재생]을 눌렀다.
- 기록: logcat(디버그 로그), 측정 뒤 `run-as`로 받은 파일을 읽어 128KB 조각마다 0이 아닌 바이트가 있는지 확인했다(검증 여부가 아니라 디스크에 쓰인 조각이다).

### 결과

| 시각 | 경과 | 사건 |
|---|---|---|
| 11:36:50.4 | 0s | 엔진 시작("Fetching magnet metadata…") |
| 11:36:54.65 | 4.2s | 메타데이터 수신(MetadataFetch 기준 3.0s, 앞의 0.5s sleep·세션 시작 포함). 조각 987개 × 128KB, 영상 129,241,752바이트, 영상 시작 오프셋 7,884 |
| 11:36:54.78 | 4.4s | 영상 선택. 받기 전용 엔진이라 "Initial playback requires 0 verified pieces" |
| 11:37:08.7 | 18.3s | **다른 작업이 같은 에뮬레이터에 앱을 다시 설치하며 프로세스가 종료됨.** Playing 로그("Initial verified playback buffer ready")는 그때까지 나오지 않았다 |

종료 시점에 디스크에 있던 조각(영상 범위 0~986 중 14개, 약 1.8MB):

```
1-8, 29, 30, 43, 63, 494, 649
```

- 영상 첫 조각인 **0번이 없다.** 0번은 앞 파일(7,884바이트, `IGNORE`)과 걸치는 조각이다. 시작 구간(0~16번)과 MP4 헤더 확인이 모두 이 조각을 기다린다.
- 1~8, 29, 30, 43, 63은 긴급 창(0~63번, 1MB/s 가정 30초 = 64조각 상한) 안이다. 창 안에서도 순서대로가 아니라 deadline이 가까운 것부터 가능한 피어에게 흩어져 요청된다.
- 494, 649는 창 밖의 priority 1 조각이다(1.3). 재생과 무관한 조각이 처음 14초 안에 이미 받아진다.
- 메타데이터 후 14초 동안 약 1.8MB(약 130KB/s). 이 토렌트의 스웜은 대부분 브라우저 WebTorrent(WebRTC) 피어라 libtorrent가 붙을 수 있는 피어가 적고, 실질 공급원은 webseed다. 일반 공개 토렌트보다 불리한 조건이다.

### 측정의 한계

- 측정 중 다른 작업이 에뮬레이터에 앱을 재설치·강제 종료해 **Playing까지 걸린 시간은 얻지 못했다.** 지시대로 측정은 한 번으로 끝냈고 다시 하지 않았다.
- 1회·1토렌트 결과다. 메타데이터 핸들 재추가(2.1)의 비용, 업로드 0의 영향(2.3)은 이 측정으로 구분할 수 없다.
- 0번 조각이 늦은 이유(다른 파일과 걸치는 조각의 webseed 요청, 또는 피어 부족)는 확인하지 못했다. 다음 측정에서 `piece_finished` 알림 시각을 기록해야 한다(개선안 I).

## 5. 개선안

변경 크기는 `engine/` 한 파일·10줄 규칙 기준 단계 수다. 모든 단계는 JVM 단위 테스트를 함께 넣고, libtorrent 동작이 걸린 것은 에뮬레이터 테스트를 덧붙인다.

| 안 | 내용 | 예상 효과 | 위험 | 변경 크기 | 필요한 테스트 |
|---|---|---|---|---|---|
| **A** | 받기 전용 작업도 앞에서부터 받기: `prioritizePlayback = false`일 때 네이티브 sequential을 끄지 않고(`TorrentEngine.kt:266`), 첫·끝 조각 TOP을 유지. `requestPlayback`에서 sequential을 끄고, `releasePlaybackPriority`에서 다시 켬 | 받아 두던 영상을 재생하면 앞부분이 이미 있어 바로 Playing. 피스 맵이 왼쪽부터 찬다(보고 1·2 모두) | 스웜 전체에는 rarest-first가 낫지만 이 앱은 업로드하지 않아 손해가 거의 없다. 드문 조각이 끝에 몰리면 완료 시간이 조금 늘 수 있다 | 순수 정책 1개 새 파일(`DownloadOrderPolicy`) + `TorrentEngine` 3단계 | 정책 단위 테스트, androidTest: 받기 전용 시작 후 flag 확인, 재생 전환·해제 때 flag 전환 |
| **B** | 메타데이터 핸들 재사용: `MetadataFetch`가 메타데이터를 받으면 핸들을 지우지 않고 넘기고, `TorrentCheckpoint.restore`가 같은 info-hash 핸들이 있으면 새로 추가하지 않고 플래그·파일 우선순위만 바꾼다 | 마그넷 첫 재생에서 피어 재탐색·재연결 시간 절약(추정 수 초~십수 초, 미측정) | 취소·실패 때 핸들 정리 누락, 다른 엔진이 같은 hash를 받는 경우(`session.find` 공유), UPLOAD_MODE·STOP_WHEN_READY 해제 누락 | `MetadataFetch` 1단계, `TorrentCheckpoint` 1~2단계, `TorrentEngine` 1단계 | androidTest(네이티브): 메타데이터 후 같은 핸들 유지·피어 수 유지, 취소 시 핸들 제거. 단위: 소유권 결정 로직을 작은 정책으로 빼서 테스트 |
| **C** | 끝부분 선행 요청을 pins에 넣기: 형식별 끝부분(MP4는 마지막 1~2조각, MKV는 끝 N MB)을 Playing 조건이 아닌 "미리 받기" 집합으로 계산(`StartupBufferPlan.required`의 쓰지 않는 `extension` 인자를 쓰거나 새 `StartupPrefetchPlan`), `prioritizeOffset`의 pins에 포함 | moov가 끝인 MP4에서 왕복 1회+폴링 1초 절약. MKV는 VLC가 Cues를 읽을 때 기다리지 않음 | moov가 앞에 있는 파일은 1~2조각을 헛받음. pins가 Playing 조건과 섞이지 않게 분리 필요 | 새 파일 1개 + `TorrentEngine` 1단계 | 계획 단위 테스트(MP4/MKV/조각 크기별), 기존 `StartupBufferPlan` 테스트 유지 |
| **D** | 인덱스 탐색 간격 단축: Playing 전에는 `updateStartupIndex`/`playbackReady` 확인을 1초 대신 250ms로, 또는 `piece_finished` 알림에서 바로 | moov 탐색 단계마다 최대 1초 절약(끝 moov면 최대 2초) | 폴링 증가로 잠금 경합(`@Synchronized`)이 늘어 `MainThreadResponsivenessTest` 확인 필요 | `TorrentEngine` 1단계 | 간격 결정 정책 단위 테스트, `MainThreadResponsivenessTest` |
| **E** | 재생 중 다른 다운로드 억제: 재생에 빌려 준 작업이 있으면 나머지 실행 중 작업의 속도를 제한하거나 일시정지 | 다른 작업이 있을 때 시작 시간 크게 단축 | 받기 속도 저하를 사용자가 원하지 않을 수 있음(기획 결정 필요) | `DownloadManager`(ui) 1~2단계 + 엔진에 속도 제한 진입점 1단계 | `DownloadManagerTest`에 빌림/반납 시 억제·해제 |
| **F** | 업로드 정책 재검토(예: 재생 중에만 작은 unchoke 슬롯과 업로드 속도 상한) | leecher 위주 스웜에서 unchoke를 더 빨리 얻음 | 제품 약속("올려 보내지 않음"), 이용 안내·개인정보 문서, 데이터 사용, 법적 위험. **PM·launch 결정 사항** | `DownloadOnlyPolicy` 1단계 + 문서 | 정책 단위 테스트, 업로드 바이트 확인 androidTest |
| **G** | 피스 맵 표시 보완: 재생 위치·긴급 창 또는 "처음부터 이어서 받은 구간"을 함께 표시 | "랜덤으로 받는다"는 인상 완화, 재생 가능 여부를 알 수 있음 | 표시 요소 추가(design 소유 색·문구 필요) | planning·design 산출물 후 program 1~2단계 | 스냅샷 계산 단위 테스트 |
| **H** | 경합(2.8) 막기: 파일 우선순위를 매긴 뒤 `currentHandle`을 공개하거나, `requestPlayback`에서 `backgroundPiecePriority = -1`로 다시 매기게 함 | 드문 경우의 "재생 우선순위가 안 걸림" 제거 | 낮음 | `TorrentEngine` 1단계 | 순서 확인 단위 테스트는 어렵다. androidTest로 확인 |
| **I** | 측정 도구: 디버그 빌드에서만 `requestPlayback` → Playing 경과, 조각 완료 순서(처음 N개)를 로그로 남김(파일 이름·마그넷 없이) | A~E 효과를 수치로 비교 가능 | 릴리스 로그에 남지 않도록 `BuildConfig.DEBUG` 확인 | `TorrentEngine` 1단계(또는 새 리스너 파일) | 로그 문구 계산을 순수 객체로 빼서 단위 테스트 |

### 권하는 순서

1. **I**(측정 도구) → 같은 조건으로 기준값 측정
2. **A**(사용자 보고와 가장 직접 맞음, 작고 위험 낮음)
3. **C + D**(moov가 끝인 MP4, MKV)
4. **B**(효과 크지만 수명 관리 위험이 있어 네이티브 테스트와 함께)
5. **E, F, G**는 기획·정책 결정 뒤
6. **H**는 A를 넣을 때 함께

## 확인하지 못한 것

- Playing까지 걸린 시간(측정 중단). 0번 조각이 늦은 이유.
- VLC가 HTTP로 MKV를 열 때 끝부분 Cues를 바로 읽는지.
- 메타데이터 핸들 재추가(2.1)의 실제 비용, 업로드 0(2.3)의 실제 영향. 개선안 I로 측정 도구를 넣은 뒤 seed가 많은 토렌트와 leecher가 많은 토렌트에서 비교해야 한다.
- 실기기(Wi-Fi·모바일 데이터)에서의 수치.
