# 시안 D: 실패 시 복구 흐름 — 오류 문구 전체와 "다시 시도" 배치

- 요구사항: `planning/requests/error-recovery.md`
- 화면: 메인(입력 카드), 준비 화면(진행 카드), 재생 화면(오류 상태), 링크 추가 대화상자, 재생 목록·저장 파일 관리(삭제 오류)
- 리뷰: `design/reviews/planning-60.md` 1(c), Q1·Q4~Q7·Q13
- 상태: 시안. 오류 → 문구 대응 객체, 다시 시도 경로, 경과 시간 노출은 program

## 1. 문구 기준

| # | 기준 | 예 |
|---|---|---|
| 1 | **두 문장**: ① 무슨 일 ② 할 일. 원인은 ①에 붙인다 | "공유 중인 사용자를 찾지 못해 토렌트 정보를 받지 못했습니다. 잠시 후 다시 시도해 주세요." |
| 2 | ②는 동사로 끝낸다: "…해 주세요" / "…하세요" / "…합니다"(결과 안내) | ✕ "토렌트 정보를 읽지 못했습니다." → ○ "… 다른 링크로 시도해 주세요." |
| 3 | 60자 이내(공백 포함), 목표 45자. 메인 입력란 아래 3줄 이내 | |
| 4 | 합쇼체. 사과·느낌표·사용자 탓 없음 | ✕ "잘못된 주소입니다!" |
| 5 | 예외 원문, 영어, 경로, 클래스 이름을 넣지 않는다. 숫자는 행동에 도움이 될 때만 | HTTP 상태: "(서버 응답 404)" |
| 6 | 용어: 문장은 "공유 중인 사용자", 칩은 "피어". "토렌트 정보", "링크", "받다" | |
| 7 | ②가 가리키는 행동은 화면에 버튼으로 있어야 한다. 없으면 다른 행동을 쓴다 | "다시 시도해 주세요"인데 버튼이 없으면 ✕ |
| 8 | 진행 중 문구만 "…"로 끝난다. 오류는 마침표 | |

## 2. 문구 표 (전체)

열 설명: **다시 시도** = 화면에 "다시 시도"를 보일지(program의 대응 객체가 돌려줄 값). **보조 동작** = 다시 시도 대신/함께 보일 버튼.

### 2-1. 링크 입력 (메인 입력란, 링크 추가 대화상자 공통)

| 키 | 상황(지금 출처) | 새 문구 | 다시 시도 | 보조 동작 |
|---|---|---|---|---|
| `error_empty_link` | 빈 입력 | 마그넷 링크나 .torrent 파일 주소를 붙여넣어 주세요. | 아니오 | — |
| `error_invalid_magnet` | 잘못된 마그넷(`error_invalid_magnet`, `failure_invalid_magnet`, DownloadManager "유효한 마그넷 주소가 필요합니다.") | 마그넷 링크 형식이 아닙니다. 링크 전체를 복사했는지 확인해 주세요. | 아니오 | — |
| `error_unsupported_link` | http(s)·magnet 아님(TorrentInputResolver) | 마그넷 링크나 .torrent 파일 주소만 열 수 있습니다. 주소를 확인해 주세요. | 아니오 | — |
| `error_link_length` | 길이 오류(TorrentInputResolver "입력 주소의 길이가 올바르지 않습니다.") | 링크가 비어 있거나 너무 깁니다. 링크를 다시 복사해 붙여넣어 주세요. | 아니오 | — |
| `error_https_required` | HTTPS 불가 주소 | 보안 연결(https)을 쓰지 않는 주소입니다. https:// 주소를 넣어 주세요. | 아니오 | — |
| `error_not_torrent_file` | .torrent 아님 | .torrent 파일이 아닙니다. 파일을 바로 내려받는 주소를 넣어 주세요. | 아니오 | — |
| `error_torrent_http` | HTTP 오류("토렌트 다운로드 실패 (HTTP 404)") | 토렌트 파일을 받지 못했습니다(서버 응답 %1$d). 주소를 확인해 주세요. | 예(5xx만), 4xx는 아니오 | — |
| `error_torrent_too_large` | 파일이 너무 큼 | 토렌트 파일이 너무 큽니다. 다른 링크를 넣어 주세요. | 아니오 | — |
| `error_torrent_redirect` | 리디렉션 없음·너무 많음 | 토렌트 파일 주소로 연결하지 못했습니다. 파일을 바로 내려받는 주소를 넣어 주세요. | 아니오 | — |
| `error_offline` | 네트워크 없음(신규 판별) | 인터넷에 연결되어 있지 않습니다. 연결을 확인하고 다시 시도해 주세요. | 예 | — |
| `error_link_generic` | 그 밖의 해석 실패(MainViewModel "주소를 처리하지 못했습니다.", `quick_download_invalid`) | 링크를 열지 못했습니다. 주소를 확인하고 다시 시도해 주세요. | 예 | — |

### 2-2. 토렌트 정보·다운로드 (메인으로 돌아와 보이는 오류, 재생 목록 항목 오류)

| 키 | 상황 | 새 문구 | 다시 시도 | 보조 동작 |
|---|---|---|---|---|
| `failure_metadata_timeout` | 120초 안에 정보를 못 받음(연결은 있음) | 공유 중인 사용자를 찾지 못해 토렌트 정보를 받지 못했습니다. 잠시 후 다시 시도해 주세요. | 예 | — |
| `error_offline` | 위와 같은데 네트워크 없음 | (2-1과 같은 키) | 예 | — |
| `failure_metadata_decode` | 정보를 읽지 못함 | 토렌트 정보를 읽지 못했습니다. 다른 링크로 시도해 주세요. | 아니오 | — |
| `failure_no_video` | 영상 없음 | 이 토렌트에는 재생할 수 있는 영상이 없습니다. 다른 링크를 넣어 주세요. | 아니오 | — |
| `failure_download_setup` | 다운로드 순서 설정 실패 | 재생 준비를 마치지 못했습니다. 다시 시도해 주세요. | 예 | — |
| `failure_engine` | 엔진 예외(지금 원문 포함 `%1$s`) | 토렌트를 처리하지 못했습니다. 잠시 후 다시 시도해 주세요. | 예 | — (**`%1$s` 인자 삭제**) |
| `error_download_start` | 다운로드 시작·연결 실패(DownloadController "다운로드를 시작하지 못했습니다.", MainViewModel "다운로드 작업을 연결하지 못했습니다.") | 다운로드를 시작하지 못했습니다. 다시 시도해 주세요. | 예 | — |
| `error_download_stopped` | 다운로드가 중지됨(MainViewModel) | 다운로드가 중지되었습니다. 다시 시도하면 이어서 받습니다. | 예 | — |
| `error_storage_full` | 저장 공간 부족(지금 판별 없음, 엔진 원문으로 추정) | 저장 공간이 부족합니다. 저장 파일 관리에서 공간을 비운 뒤 다시 시도해 주세요. | 예 | 저장 파일 관리 |
| `info_already_downloading` | 재생 중인 영상을 또 받으려 함(DownloadController·DownloadManager) | 지금 재생 중인 영상이라 이미 받고 있습니다. | — (안내) | — |
| `failure_unknown` | "알 수 없는 오류" | **삭제.** 원문 대신 상황별 일반 문구(`error_link_generic`, `failure_engine`)를 쓴다 | | |

### 2-3. 재생 화면

| 키 | 상황 | 새 문구 | 다시 시도 | 버튼 |
|---|---|---|---|---|
| `player_error_playback` | 조각을 60초 못 받음 | 이 구간을 받지 못해 재생을 멈췄습니다. 공유 중인 사용자가 적을 수 있으니 다시 시도해 주세요. | 예 | [돌아가기] [다시 시도] |
| `player_error_init` | 재생기 시작 실패(지금 원문 포함) | 재생기를 시작하지 못했습니다. 다시 시도해 주세요. | 예 | [돌아가기] [다시 시도] (**`%1$s` 삭제**) |
| `player_error_start` | 영상 열기 실패(지금 원문 포함) | 영상을 열지 못했습니다. 다시 시도해 주세요. | 예 | [돌아가기] [다시 시도] (**`%1$s` 삭제**) |
| `player_error_file_missing` | 영상 파일 없음 | 영상 파일을 찾을 수 없습니다. 재생 목록에서 다시 받아 주세요. | 아니오 | [돌아가기] 하나 |

### 2-4. 삭제·목록 (재생 목록, 저장 파일 관리)

| 키 | 상황(지금 출처) | 새 문구 |
|---|---|---|
| `error_delete_in_use` | 재생·다운로드 중 파일 삭제(TorrentDiskStore 영어 "Stop playback or downloading before deleting this file", MainViewModel "파일을 삭제하려면 먼저 재생 목록의 다운로드를 중지해 주세요.") | 재생 중이거나 받고 있는 파일입니다. 재생 목록에서 다운로드를 중지한 뒤 삭제해 주세요. |
| `error_delete_busy` | 종료·기록 저장 대기(DownloadController·MainViewModel·TorrentEngine "다운로드 종료를 기다리는 중입니다…", TorrentCheckpoint "다운로드 기록 저장이 끝나지 않았습니다…") | 다운로드를 마무리하는 중입니다. 잠시 후 다시 삭제해 주세요. |
| `error_delete_shared` | 다른 작업이 쓰는 파일(ActiveTorrentFiles·TorrentRemoval) | 다른 다운로드가 쓰고 있는 파일입니다. 그 다운로드를 중지한 뒤 삭제해 주세요. |
| `stored_files_delete_missing` | 경로 오류·앱 저장 공간 밖·폴더·심볼릭 링크(TorrentRemoval 4종) | 이 파일은 찾을 수 없거나 앱에서 삭제할 수 없습니다. 목록을 새로고침해 주세요. |
| `playlist_delete_failed` | 재생 목록 삭제 실패 | 관련 파일을 삭제하지 못했습니다. 잠시 후 다시 시도해 주세요. |
| `stored_files_delete_failed` | 저장 파일 삭제 실패 | 파일을 삭제하지 못했습니다. 잠시 후 다시 시도해 주세요. |
| `playlist_load_error` | 재생 목록 읽기 실패 | 재생 목록을 읽지 못했습니다. 앱을 다시 열어 주세요. |
| `stored_files_load_error` | 파일 목록 읽기 실패 | 파일 목록을 읽지 못했습니다. 새로고침해 주세요. |
| `playlist_reset_failed` | 위치 초기화 실패 | 재생 위치를 초기화하지 못했습니다. 다시 시도해 주세요. |
| `bulk_delete_partial` | 일괄 삭제 일부 실패 | %1$d개를 삭제하지 못했습니다. 실패한 항목은 선택된 채로 남겨 두었습니다. |

- 2-4의 `localizedMessage ?: …` 형태(재생 목록·저장 파일 관리·링크 추가 대화상자)는 예외 원문을 그대로 보이므로, 대응 객체가 위 키 중 하나를 고르게 바꾼다(program).
- 로그 문구(`MetadataFetch`·`TorrentCheckpoint`의 `check` 메시지 등)는 사용자에게 보이지 않으면 그대로 둔다. 보인다면 `failure_engine`으로 덮는다.

### 2-5. 준비 화면 진행 안내 (오류 아님)

| 키 | 문구 | 비고 |
|---|---|---|
| `preparing_elapsed` | %1$d초째 찾는 중 · 최대 2분 | 정보·연결 단계에서 진행 카드 보조 줄(`subtitle_metadata_fetching` 자리). 최대 시간 표시는 Q5 |
| `preparing_no_peers` | 아직 공유 중인 사용자를 찾지 못했습니다. 계속 기다리거나, 재생 목록에서 받아 두고 나중에 보세요. | 피어 0명 30초 이상 |
| `action_continue_in_playlist` | 재생 목록에서 계속 받기 | 안내 줄 안 글자 버튼 |

## 3. 배치

### 3-1. 메인: 오류 + 다시 시도

```
┌ SectionCard (입력 카드) ─────────────────────────┐
│ ┌ LinkTextField (isError, 테두리 error) ───────┐ │
│ │ magnet:?xt=urn:btih:…                         │ │  입력이 남아 있음
│ └───────────────────────────────────────────────┘ │
│ (!) 공유 중인 사용자를 찾지 못해 토렌트 정보를      │  supportingText: bodySmall error
│     받지 못했습니다. 잠시 후 다시 시도해 주세요.     │  + 앞에 Warning 아이콘 iconSmall(error)
│                                     [붙여넣기]     │
└───────────────────────────────────────────────────┘
[           ↻ 다시 시도            ]   PrimaryButton (문구·아이콘만 바뀜)
```

- **별도 "다시 시도" 버튼을 두지 않는다**(Q1). 다시 시도할 수 있는 오류이고 입력란이 실패한 링크 그대로면, 주요 버튼 문구가 "바로 재생" → "다시 시도"(`action_retry`, 기존 키)로, 아이콘 `Icons.Filled.Refresh`(코어)가 붙는다. 누르면 같은 링크로 다시 시작한다(동작은 "바로 재생"과 같다).
- 입력을 한 글자라도 고치면 오류 문구가 사라지고 버튼은 "바로 재생"으로 돌아간다.
- 다시 시도할 수 없는 오류(2-1 대부분)는 버튼이 "바로 재생" 그대로이고 오류 문구만 보인다.
- 보조 동작이 있는 오류(`error_storage_full`)는 오류 문구 아래 `TextAction(DEFAULT)` "저장 파일 관리"(`action_open_stored_files` 기존 키)를 붙인다.
- 오류 문구 앞 경고 아이콘: 색만으로 오류를 알리지 않기 위해(WCAG 1.4.1). `LinkTextField`의 `supportingText`에 아이콘 슬롯을 더한다(design, 링크 추가 대화상자에도 같이 적용).

### 3-2. 준비 화면: 경과 시간과 피어 0명 안내

```
┌ SectionCard (진행 카드) ─────────────────────────┐
│ 토렌트 정보 가져오는 중…                bodyMedium │
│ ▓▓▓▓░░░░░░░░ (끝 없는 진행 막대)                  │
│ [피어 0명]                              StatusChip │
│ 42초째 찾는 중 · 최대 2분     bodySmall onSurfaceVariant, tnum │
│ ┌ InlineNotice ─────────────────────────────────┐ │
│ │ (i) 아직 공유 중인 사용자를 찾지 못했습니다.     │ │  bodySmall onSurface
│ │     계속 기다리거나, 재생 목록에서 받아 두고     │ │
│ │     나중에 보세요.                               │ │
│ │                    [재생 목록에서 계속 받기]     │ │  TextAction DEFAULT, 끝 정렬
│ └─────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────┘
```

- 경과 줄은 정보·연결 단계에서만. 버퍼링 단계에서는 지금처럼 속도·피어 칩만.
- `InlineNotice`는 피어 0명이 30초 이상 이어질 때 나타나고, 피어가 1명 이상 되면 사라진다(깜빡임 방지: 사라진 뒤 다시 0명이 되면 30초를 새로 센다 — program 판단 객체).
- 버튼은 하단 "재생 목록"과 같은 동작이다(Q4). 하단 버튼은 그대로 둔다.
- 나타날 때 레이아웃이 밀리므로 `animateContentSize`는 쓰지 않고 바로 나타나게 한다(애니메이션 라이브러리 범위 밖, 기본 Compose만).

### 3-3. 재생 화면 오류

```
          (⚠ 48dp, PlaybackColors.Error)
   이 구간을 받지 못해 재생을 멈췄습니다.          bodyLarge OnVideo, 가운데
   공유 중인 사용자가 적을 수 있으니 다시 시도해 주세요.
                                           ↕ Spacing.lg
     [ 돌아가기 ]   [ ↻ 다시 시도 ]
      Secondary       Primary
```

- 두 버튼은 `Row`, 사이 `Spacing.md`, 각각 최소 폭 120dp(**새 토큰 `Sizes.actionButtonMinWidth = 120.dp`**, 화면 파일에 dp 값을 직접 적지 않기 위해). 주요 버튼이 오른쪽(Material 대화상자 순서와 같음).
- 다시 시도할 수 없는 오류(`player_error_file_missing`)는 [돌아가기] 하나만, `PrimaryButton`으로.
- 다시 시도를 누르면 오류가 사라지고 #48의 대기 패널("재생 데이터를 버퍼링하고 있습니다…")이 가운데 버튼 위에 보인다. 다시 실패하면 같은 오류 화면.
- 가로 화면: 같은 배치(가운데 정렬). 높이 360dp에서도 아이콘 + 2줄 + 버튼이 들어간다.
- 오류 화면에서는 조작 UI(상·하단)를 숨기고 ←(상단)만 남긴다 — 지금 동작 확인 필요(구현 때).

### 3-4. 링크 추가 대화상자

- 오류 문구는 2-1 키를 그대로 쓴다(`quick_download_invalid` "주소를 확인해 주세요."는 `error_link_generic`으로 통합 후 삭제).
- 대화상자에는 "다시 시도" 문구 전환을 하지 않는다. "다운로드" 버튼을 다시 누르면 된다.

## 4. 상태별 모습 요약

| 화면 | 상태 | 모습 |
|---|---|---|
| 메인 | 오류 없음 | 지금과 같음 |
| 메인 | 다시 시도 가능 오류 + 입력 그대로 | 오류 문구(아이콘) + 주요 버튼 "다시 시도" |
| 메인 | 다시 시도 불가 오류 | 오류 문구만, 주요 버튼 "바로 재생" |
| 메인 | 공간 부족 | 오류 문구 + "저장 파일 관리" 글자 버튼 + 주요 버튼 "다시 시도" |
| 준비 | 0~29초, 피어 0 | 경과 줄만 |
| 준비 | 30초 이상, 피어 0 | 경과 줄 + 안내 |
| 준비 | 피어 ≥1 | 경과 줄, 안내 없음 |
| 재생 | 다시 시도 가능 오류 | 아이콘 + 문구 + [돌아가기][다시 시도] |
| 재생 | 파일 없음 | 아이콘 + 문구 + [돌아가기] |
| 전체 | 글자 200% | 메인 오류 문구 5~6줄(카드가 늘어남, 스크롤). 재생 오류 버튼 두 개가 한 줄에 안 들어가면 `FlowRow`로 세로 쌓기(주요 버튼 위) |

## 5. 토큰·컴포넌트

| 이름 | 종류 | 내용 |
|---|---|---|
| `Sizes.actionButtonMinWidth` | 크기 토큰 신규 | 120dp. 나란한 두 버튼의 최소 폭 |
| `InlineNotice` | 컴포넌트 신규(`ui/components/`) | `surfaceContainer` 바탕, `Shapes.small`, 안쪽 `Spacing.md`, 앞 아이콘 `Icons.Filled.Info` `iconSmall` `primaryLight`, 본문 `bodySmall` `onSurface`, 선택 동작 `TextAction` 끝 정렬. 준비 화면 피어 안내에 먼저 쓰고, 이후 이어 받기 질문·저장 공간 경고에 재사용 |
| `LinkTextField` | 기존 변경 | 오류 문구 앞 경고 아이콘(`Icons.Filled.Warning`, `iconSmall`, `error`) |
| `PrimaryButton` | 변경 없음 | `icon` 인자 이미 있음 |
| `PlayerErrorState` | 기존 변경 | `onRetry: (() -> Unit)?` — null이면 한 버튼. **콜백 추가는 program** |

색 토큰 추가 없음.

## 6. 접근성

- 오류 문구는 나타날 때 읽힌다(`liveRegion = Polite`). 메인 입력란은 `isError` + 오류 문구가 `error()` semantics로 연결.
- 주요 버튼 문구가 "다시 시도"로 바뀌면 버튼 이름도 바뀐다(아이콘은 장식).
- `InlineNotice`도 나타날 때 `Polite`로 읽는다(30초 뒤 갑자기 생기므로).
- 경과 줄은 1초마다 바뀌므로 `liveRegion`을 걸지 않는다(계속 읽어 방해).
- 재생 오류 상태에서는 TalkBack 초점이 문구로 간다.

## 7. 영향

- 화면 테스트: `androidTest`에서 "바로 재생"·"돌아가기"를 글자로 찾는 테스트는 없다(검색 확인). 메인 오류 상태 테스트가 생기면 "다시 시도" 전환을 기대값에 넣는다.
- 문자열: 위 표의 기존 키는 문구만 바꾸고, `failure_engine`·`player_error_init`·`player_error_start`는 인자(`%1$s`)를 없앤다 — 호출부가 같이 바뀌어야 컴파일 경고가 없다(program).
- 삭제할 키: `failure_unknown`, `quick_download_invalid`, `failure_invalid_magnet`(→ `error_invalid_magnet`로 통합).
- 신규 키: `error_unsupported_link`, `error_link_length`, `error_https_required`, `error_not_torrent_file`, `error_torrent_http`, `error_torrent_too_large`, `error_torrent_redirect`, `error_offline`, `error_link_generic`, `error_download_start`, `error_download_stopped`, `error_storage_full`, `info_already_downloading`, `error_delete_in_use`, `error_delete_busy`, `error_delete_shared`, `preparing_elapsed`, `preparing_no_peers`, `action_continue_in_playlist`.
