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

(작성 중)

## 5. 개선안

(작성 중)
