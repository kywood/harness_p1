# 시안 F: 링크로 바로 열기 확인 시트

- 요구사항: `planning/requests/open-links-from-other-apps.md`, 화면 정의 `planning/screens/main.md`
- 이슈: #127
- 리뷰: `design/reviews/planning-60.md` 2절 open-links 행
- PM 결정: 브라우저·메신저의 magnet 링크 누르기, 다른 앱 "공유"로 magnet 또는 http(s) `.torrent` 주소가 들어오면 앱이 열리고 메인 위에 확인 시트. 링크 요약(이름 또는 줄인 주소) + [바로 재생] [받아 두기] [취소]. 재생 목록에 있으면 "재생 목록에 있는 영상입니다"와 이어보기. 잘못된 링크는 `design/specs/error-recovery.md` 규칙. 기기의 `.torrent` 파일 열기는 범위 밖
- 상태: 시안. intent 받기, 공유 글에서 링크 뽑기, 검증, 재생 목록 조회는 program

## 1. 배치

메인 화면이 뒤에 있고, 들어온 링크는 메인 입력란에도 채워진다(planning 2-3). 그 위에 `AppBottomSheet`.

```
┌ AppBottomSheet (바로 전체 펼침) ─────────────────────┐
│                ───                                    │
│ 이 링크를 열까요?                         titleLarge    │
│                                                       │
│ ┌ 요약 카드: surfaceContainer, Shapes.medium ───────┐ │
│ │ [🎬]  Big.Buck.Bunny.2008.1080p.mkv               │ │  titleMedium onSurface 최대 2줄
│ │       마그넷 링크                                  │ │  bodySmall onSurfaceVariant
│ └───────────────────────────────────────────────────┘ │
│                                                       │
│ [              ▶ 바로 재생               ]  PrimaryButton │
│ [                받아 두기                ]  SecondaryButton │
│                      취소                   TextAction MUTED │
└───────────────────────────────────────────────────────┘
```

| 요소 | 컴포넌트·토큰 |
|---|---|
| 시트 | `AppBottomSheet(title = 문구, skipPartiallyExpanded = true)`. 내용 `Column(verticalScroll)`, 가로 `Spacing.screenGutter`, 요소 사이 `Spacing.lg`, 버튼 사이 `Spacing.sm` |
| 제목 | `AppBottomSheet` 제목 슬롯(`titleLarge`, heading) |
| 요약 카드 | `Surface(color = surfaceContainer, shape = Shapes.medium)`, 안쪽 `Spacing.lg`, 아이콘과 글자 사이 `Spacing.md` |
| 카드 아이콘 | `ic_movie`(기존) `Sizes.icon` `primaryLight`, 장식 |
| 이름 줄 | `titleMedium`(renewal-plan "카드 제목·파일 이름"), `onSurface`, 최대 2줄, 끝 말줄임 |
| 종류 줄 | `bodySmall`, `onSurfaceVariant`: "마그넷 링크" / ".torrent 파일 주소" |
| 바로 재생 | `PrimaryButton`(높이 52dp, `brandButtonGradient`), 앞 아이콘 `Icons.Filled.PlayArrow`, `fillMaxWidth` |
| 받아 두기 | `SecondaryButton`, 앞 아이콘 `ic_download`(기존), `fillMaxWidth` |
| 취소 | `TextAction(tone = MUTED)`, 가운데, 터치 48dp |

### 1-1. 요약 줄에 무엇을 쓰나

| 들어온 것 | 이름 줄 | 종류 줄 |
|---|---|---|
| 마그넷, `dn` 있음 | `dn` 값(URL 디코딩, 앞뒤 공백 제거) | 마그넷 링크 |
| 마그넷, `dn` 없음 | "마그넷 링크 · " + 정보 해시 앞 8자리 + "…" (예: "마그넷 링크 · 3F2A9C1D…") — 대문자 hex, tnum | (줄 숨김, 이름 줄에 이미 있음) |
| http(s) `.torrent` 주소 | 호스트 + "/…/" + 마지막 경로 조각(예: "example.org/…/ubuntu-24.04.torrent"). 경로가 한 단계면 "…/" 없이. 쿼리(`?`) 뒤는 빼고 보인다 | .torrent 파일 주소 |

- 주소를 통째로 보이지 않는다(길고, 쿼리에 토큰이 들어 있을 수 있다). 줄이는 규칙은 program의 순수 객체(`IncomingLinkSummary` 같은 이름)로, 단위 테스트에 위 예 3개를 넣는다.
- 공유 글에 링크 말고 다른 글이 섞여 있으면 첫 번째 `magnet:` 또는 `http(s)://` 토큰만 쓴다. 링크가 둘 이상이면 첫 번째만 연다(시트에 따로 알리지 않는다 — 6절 확인 필요).

## 2. 상태별 모습

### 2-1. 재생 목록에 있는 링크

같은 링크(마그넷 키 `MagnetIdentity.key()`가 같은 항목)가 재생 목록에 있으면 카드 아래에 안내 줄을 넣고 주요 버튼을 이어보기로 바꾼다.

```
│ ┌ 요약 카드 ─────────────────────────────────────┐ │
│ │ [🎬] Big.Buck.Bunny.2008.1080p.mkv              │ │
│ │      마그넷 링크                                 │ │
│ │      [● 다운로드 중]  이어보기 12:34             │ │  StatusChip(기존 상태) + labelMedium tnum
│ └─────────────────────────────────────────────────┘ │
│ ┌ InlineNotice ──────────────────────────────────┐ │
│ │ (i) 재생 목록에 있는 영상입니다.                  │ │
│ └─────────────────────────────────────────────────┘ │
│ [            ▶ 이어서 재생            ]  PrimaryButton │
│ [              받아 두기              ]  ← 상태에 따라 숨김 │
│                    취소                              │
```

| 재생 목록 항목 상태 | 주요 버튼 | 받아 두기 |
|---|---|---|
| 이어보기 위치 있음(> 0) | "이어서 재생"(`action_resume_play`) — 저장 위치부터 | 아래 행에 따름 |
| 이어보기 위치 없음 | "바로 재생" | 아래 행에 따름 |
| 다운로드 완료 | (위 두 행대로) | **숨김**(받을 것이 없다) |
| 다운로드 중·대기·Wi-Fi 대기 | (위 두 행대로) | **숨김**(이미 받는 중). 상태 칩이 그 사실을 보인다 |
| 미완료·실패(멈춤) | (위 두 행대로) | 보임. 누르면 그 항목의 다운로드를 다시 시작 |

- 상태 칩은 재생 목록 카드와 같은 `PlaylistItemStatus`(Wi-Fi 대기 포함, `settings-wifi.md`)를 쓴다.
- 이름 줄은 재생 목록에 저장된 이름(받은 영상 파일 이름)이 있으면 그것을 쓴다(`dn`보다 정확하다).
- http(s) `.torrent` 주소는 내려받기 전에는 마그넷 키를 알 수 없어 이 판별을 하지 못한다. 그 경우 일반 시트로 보이고, 실제 시작 뒤 같은 키가 있으면 지금처럼 기존 항목을 이어 쓴다(program).

### 2-2. 잘못된 링크

공유 글에서 링크를 찾지 못했거나 형식 검사에 실패하면 같은 시트를 오류 모양으로 연다. 메인 입력란은 채우지 않는다(뜻 없는 글이 입력란에 남지 않게).

```
│ 링크를 열 수 없습니다                         titleLarge │
│ (!) 마그넷 링크나 .torrent 파일 주소만 열 수 있습니다.   │  bodyMedium onSurface + Warning 아이콘 error
│     주소를 확인해 주세요.                              │
│ [                 확인                 ]  SecondaryButton │
```

| 경우 | 문구 키(`error-recovery.md` 2-1, 새로 만들지 않는다) |
|---|---|
| 공유 글에 `magnet:`·`http(s)://` 없음, 그 밖의 스킴 | `error_unsupported_link` |
| `magnet:`인데 정보 해시가 없거나 형식이 틀림 | `error_invalid_magnet` |
| 너무 김 | `error_link_length` |
| `http://`(보안 연결 아님) | `error_https_required` |

- 오류 아이콘 `Icons.Filled.Warning` `Sizes.iconSmall` `error` + 문구 `bodyMedium` `onSurface`(경고 색 글자는 아이콘만, 문장은 본문 색 — 긴 문장을 붉은 글자로 읽게 하지 않는다).
- 버튼은 "확인" 하나(`action_confirm`). 오류 문장 ②("주소를 확인해 주세요")는 보낸 앱에서 하는 행동이라 화면에 그 버튼이 없어도 규칙 1-7에 어긋나지 않는다.
- `.torrent` 주소 내려받기 실패(HTTP 오류·토렌트 아님 등)는 시트 단계에서는 알 수 없다. "바로 재생" 뒤 지금 흐름(준비 화면 → 메인 오류)대로 `error-recovery.md` 2-1 문구가 메인 입력란 아래에 보인다.

### 2-3. 재생·준비 중에 링크가 들어옴

planning 2-4: 재생을 끊지 않는다. 시트를 재생 화면(또는 준비 화면) 위에 띄우고 버튼을 줄인다.

| 요소 | 모습 |
|---|---|
| 주요 버튼 | "받아 두기"(`PrimaryButton`) |
| 바로 재생 | 두지 않는다(지금 재생을 끝내는 동작이라 실수로 누르면 손실이 크다) |
| 안내 | 카드 아래 `bodySmall` `onSurfaceVariant`: "지금 재생은 그대로 이어집니다." |
| 재생 | 시트가 떠 있는 동안에도 계속 재생 |

PM 결정 문서에는 "메인 위 시트"만 있어 이 경우는 planning 2-4를 따랐다(6절 확인 필요).

### 2-4. 그 밖의 상태

| 상태 | 모습 |
|---|---|
| 기본 | 1절 |
| Wi-Fi 전용 켬 + 모바일 데이터에서 "바로 재생"·"이어서 재생" | 시트를 닫고 확인 창(`settings-wifi.md` 4절)을 띄운다. "취소"면 메인에 링크가 채워진 채로 남는다 |
| Wi-Fi 전용 켬 + 모바일 데이터에서 "받아 두기" | 재생 목록에 추가하고 "Wi-Fi 대기"로 둔다. 스낵바 `info_waiting_for_wifi` |
| 받아 두기 성공 | 시트를 닫고 메인에 스낵바 `open_link_saved` + 동작 "재생 목록"(`action_open_playlist`, 기존 키). 메인 입력란은 비운다(받아 둔 링크로 다시 재생하지 않게) |
| 받아 두기 실패(동시 3개 초과는 실패가 아니라 대기) | 스낵바 `error_download_start`(기존 시안 키) |
| 취소·시트 밖 탭·아래로 끌기·뒤로 | 시트만 닫는다. 메인 입력란에는 링크가 남는다(planning 2-3). 아무것도 시작하지 않는다 |
| 시트가 떠 있을 때 또 링크가 들어옴 | 시트 내용을 새 링크로 바꾼다(시트를 두 개 쌓지 않는다) |
| 첫 실행(광고 동의 창이 먼저 뜸) | 동의 창이 닫힌 뒤 시트를 띄운다. 전면 광고는 이 흐름에서 띄우지 않는다(지금 규칙상 재생 종료 때만이라 해당 없음, program 확인) |
| 세로 | 360×640dp에서 제목·카드·버튼 셋이 한 화면에 들어간다(추정 약 400dp) |
| 가로 | 시트 폭 최대 600dp 가운데. 높이가 낮아(약 360dp) 내용이 스크롤된다. 버튼을 가로로 나란히 바꾸지 않는다(200%에서 다시 쌓여야 해서 한 가지로 둔다) |
| 글자 200% | 이름 2줄 말줄임, 버튼 글자는 한 줄(짧은 동사)이고 버튼 높이 52dp가 글자에 맞춰 늘어난다(`heightIn(min)`). 전체는 스크롤로 닿는다 |
| 키보드 | 시트에는 입력이 없다. 메인 입력란 키보드가 떠 있었다면 시트를 띄울 때 내린다 |
| 광고 | 배너는 시트 아래에 가려진다. 시트에 광고를 넣지 않는다 |
| 라이트·다크 | 다크 하나 |

## 3. 최종 문구 (strings.xml)

| 키 | 문구 | 비고 |
|---|---|---|
| `open_link_title` | 이 링크를 열까요? | 신규. 시트 제목 |
| `open_link_kind_magnet` | 마그넷 링크 | 신규. 종류 줄, `dn` 없을 때 이름 줄 앞부분 |
| `open_link_kind_torrent` | .torrent 파일 주소 | 신규 |
| `open_link_magnet_short` | 마그넷 링크 · %1$s… | 신규. %1$s = 정보 해시 앞 8자리 |
| `open_link_in_playlist` | 재생 목록에 있는 영상입니다. | 신규. PM 결정 문구 |
| `open_link_keep_playing` | 지금 재생은 그대로 이어집니다. | 신규. 2-3 |
| `open_link_saved` | 재생 목록에 추가했습니다. | 신규. 받아 두기 뒤 스낵바 |
| `open_link_error_title` | 링크를 열 수 없습니다 | 신규. 오류 시트 제목(문장이 아니라 제목이라 마침표 없음) |
| `action_stream_play` | 바로 재생 | 기존 |
| `action_resume_play` | 이어서 재생 | 신규. 이어보기 위치가 있을 때 주요 버튼 |
| `action_save_for_later` | 받아 두기 | 신규. PM 결정 문구 |
| `action_cancel` | 취소 | 기존 |
| `action_confirm` | 확인 | 기존 |
| `action_open_playlist` | 재생 목록 | 기존(스낵바 동작) |
| `playlist_resume` | 이어보기 %1$s | 기존(카드 안 위치) |
| `cd_save_for_later` | 받아 두기, 재생 목록에 추가하고 다운로드 | 신규. 버튼 읽기 이름(보이는 글자를 앞에, WCAG 2.5.3) |
| `info_waiting_for_wifi` | Wi-Fi에 연결되면 받기 시작합니다. | `settings-wifi.md`와 같은 키 |
| `error_unsupported_link` 외 3개 | `error-recovery.md` 2-1 | 시안 D 키 그대로 |

문구 점검:

- 제목은 질문 하나로 무엇을 정하는지 알린다("이 링크를 열까요?"). 버튼은 결과를 말하는 동사(바로 재생 / 받아 두기 / 취소)라 "확인" 같은 뜻 없는 글자를 쓰지 않는다(`design:ux-copy` 확인 창 원칙).
- "받아 두기"만으로는 어디에 쌓이는지 모를 수 있어 성공 스낵바에서 "재생 목록에 추가했습니다"로 알리고 바로 가는 동작을 준다. 버튼 글자는 PM 결정대로 짧게 둔다.
- 링크 원문은 시트·스낵바·로그 어디에도 통째로 보이지 않는다(보안 규칙: 마그넷 평문 로그 금지).

## 4. 컴포넌트·토큰

| 이름 | 종류 | 설명 |
|---|---|---|
| `AppBottomSheet` | 기존 공용 | 그대로 |
| `InlineNotice` | 예정 공용(#72 시안 D, 아직 코드 없음) | 이 시트가 두 번째 사용처. 준비 화면 피어 0명 안내와 같이 만든다. `surfaceContainer` + `Icons.Filled.Info` `primaryLight` + 문장 `bodySmall` `onSurface` |
| `StatusChip` | 기존 공용 | 재생 목록 상태 |
| `OpenLinkSheet` | 신규 화면 컴포저블 | `ui/screens/OpenLinkSheet.kt`. 메인과 재생 화면이 같이 띄우므로 콜백만 받는다(6절) |
| `LinkSummaryCard` | 시트 안 비공개 | 요약 카드. 다른 곳에서 쓰게 되면 `ui/components/`로 옮긴다 |

새 색·간격·크기 토큰 없음. 새 아이콘 없음(`ic_movie`, `ic_download`, `PlayArrow`·`Info`·`Warning` 코어).

## 5. 접근성

| 요소 | 읽기 | 기준 |
|---|---|---|
| 시트 열림 | 초점이 제목 "이 링크를 열까요?, 제목"으로 | 2.4.3, Material 기본 |
| 요약 카드 | 한 덩어리로 "Big.Buck.Bunny.2008.1080p.mkv, 마그넷 링크" (`mergeDescendants`), 아이콘은 장식 | 1.3.1 |
| 줄인 해시 | "마그넷 링크, 3F2A9C1D…" — 해시 글자를 하나씩 읽는 것은 막지 않는다(사용자가 대조할 수 있게) | |
| 재생 목록에 있음 | 안내 줄이 카드 바로 다음에 읽힌다. 상태 칩 "다운로드 중"은 글자로 읽힌다 | 1.4.1 |
| 받아 두기 | "받아 두기, 재생 목록에 추가하고 다운로드, 버튼" | 2.5.3 |
| 오류 시트 | 경고 아이콘은 장식(문장에 이미 오류 내용), 제목 "링크를 열 수 없습니다"가 먼저 읽힌다 | 3.3.1 |
| 순서 | 제목 → 카드 → 안내 → 주요 → 보조 → 취소 | 2.4.3 |

- 대비: 이름 `onSurface`/`surfaceContainer` 13.4 이상, 종류 줄 `onSurfaceVariant`/`surfaceContainer` 7.4, 아이콘 `primaryLight`/`surfaceContainer` 5.8, 주요 버튼 흰 글자 5.28 이상(renewal-plan 2절).
- 터치: 버튼 52dp, 취소 48dp.
- 사용자 확인 없이 재생·다운로드가 시작되지 않는다(planning 완료 조건, 3.2.1 예측 가능).

## 6. program에 넘길 것·확인 필요

- 메인에서 시트를 띄울 상태: 들어온 링크(검증 결과 포함), 요약 문자열, 재생 목록 일치 항목(상태·이어보기 위치). 콜백: `onPlay(link, resumeFromSaved: Boolean)`, `onSaveForLater(link)`, `onDismiss()`. 기존 `onStartStream` 시그니처는 그대로 두고 새 콜백을 더한다.
- 공유 글에서 링크 뽑기·줄이기·검증은 순수 객체로 빼고 단위 테스트(1-1의 예, 2-2의 오류 4종).
- 확인 필요(PM): ① 재생 중 진입(2-3)을 planning 2-4대로 할지 ② 공유 글에 링크가 여러 개일 때 첫 번째만 여는 것 ③ 받아 두기 성공 뒤 메인 입력란을 비우는 것.

## 7. 확인하지 못한 것

- 에뮬레이터 확인 없음(문서 시안). 높이·줄 수는 360dp·글자 100% 추정.
- 브라우저·메신저(크롬, 카카오톡)가 `magnet:` 링크를 앱 선택지로 넘기는지, 공유 글 형식(제목 + 링크 등)은 실기기에서 확인하지 않았다.
