# 시안 G: 설정 화면과 Wi-Fi 전용

- 요구사항: `planning/requests/mobile-data-guard.md`, 화면 정의 `planning/screens/main.md`·`playlist.md`
- 이슈: #128
- 리뷰: `design/reviews/planning-60.md` 1(a) 설정 화면 결론, 2절 mobile-data-guard 행
- PM 결정: 새 "설정" 화면(진입점은 1(a)에 맞게). 항목 ① Wi-Fi 전용 토글(기본 끔, 설명 "모바일 데이터에서는 다운로드를 멈춥니다") ② 자막 자동 표시 토글(기본 켬) ③ 버전·오픈소스 라이선스·개인정보처리방침·이용 안내는 메인 하단에 그대로 두고 설정에는 넣지 않거나 중복으로만(design 판단). Wi-Fi 전용 + 모바일 데이터 → 진행 중 다운로드는 "Wi-Fi 대기"로 멈추고 Wi-Fi로 돌아오면 자동 재개. 재생 목록 카드에 상태 칩. Wi-Fi 전용 + 모바일 데이터에서 "바로 재생" → 확인 창 [이번만 재생] [취소]
- 상태: 시안. 연결 종류 감시, 설정 저장, 대기 상태, 자동 재개는 program

## 1. 진입점: 메인 오른쪽 위 톱니

1(a)-4 규칙대로 메인 오른쪽 위에 톱니 아이콘 하나를 둔다. 메인 하단 링크 묶음은 4개 제한(1(a)-2)이 있고 이미 꽉 차 있어 하단에는 넣지 않는다.

```
┌ 메인 (본문 최대 600dp, 가운데) ───────────────────┐
│                                         [⚙]      │  IconButton 48dp, 스크롤과 함께 움직임
│                  [로고]                           │
│                PiecePlay                          │
│            받으면서 바로 재생                       │
│ ┌ 입력 카드 ─────────────────────────────────────┐ │
│ ...                                               │
```

| 요소 | 컴포넌트·토큰 |
|---|---|
| 자리 | 본문 `Column`(최대 `Sizes.contentMaxWidth`) 맨 위 `Row(fillMaxWidth, End)`. 고정 위치(화면 모서리에 떠 있는 버튼)로 두지 않는다 — 가로 화면에서 스크롤하면 로고·입력란 위에 겹치기 때문 |
| 아이콘 | `Icons.Filled.Settings`(코어) `Sizes.icon`, `onSurfaceVariant` |
| 터치 | `IconButton` 48dp |
| 간격 | 로고와의 간격은 본문 `spacedBy(Spacing.lg)`에서 빼서 `Spacing.xs`로(톱니 줄이 로고를 아래로 너무 밀지 않게). 세로 360×640dp에서 메인 첫 화면 높이가 약 40dp 늘어난다 |

## 2. 설정 화면

```
┌ AppTopBar: [←] 설정 ───────────────────────────────┐
│                                                     │
│ 다운로드                       SectionHeader          │
│ ┌ SettingsSwitchItem ─────────────────────────────┐ │
│ │ Wi-Fi 전용                              [  ●]   │ │  bodyLarge / 스위치
│ │ 모바일 데이터에서는 다운로드를 멈춥니다.           │ │  bodyMedium onSurfaceVariant
│ └─────────────────────────────────────────────────┘ │
│ ┌ InlineNotice (켬 + 지금 모바일 데이터일 때만) ──┐ │
│ │ (i) 모바일 데이터에 연결되어 다운로드를 멈췄습니다. │ │
│ │     Wi-Fi에 연결되면 이어서 받습니다.            │ │
│ └─────────────────────────────────────────────────┘ │
│                                                     │
│ 재생                                                 │
│ ┌ SettingsSwitchItem ─────────────────────────────┐ │
│ │ 자막 자동 표시                          [●  ]    │ │
│ │ 한국어 자막이 있으면 재생할 때 바로 켭니다.        │ │
│ └─────────────────────────────────────────────────┘ │
│                                                     │
│ 정보                                                 │
│ 이용 안내                                       ›   │  SettingsLinkItem
│ 개인정보처리방침                                 ↗   │
│ 오픈소스 라이선스                               ›   │
│ 광고 개인정보 설정 (동의 필요 지역만)            ›   │
│ 버전                     v.2026.11.13.001           │  누를 수 없음
└─────────────────────────────────────────────────────┘
```

| 요소 | 컴포넌트·토큰 |
|---|---|
| 화면 | `Scaffold` + `AppTopBar(title = "설정", onBack)`. 본문 `Column(verticalScroll)`, 최대 폭 `Sizes.contentMaxWidth` 가운데, 아래 `navigationBarsPadding` + `Spacing.lg` |
| 묶음 제목 | `SectionHeader`(신규 공용, `subtitles.md` 5절과 같은 것): `titleSmall` `onSurfaceVariant`, 가로 `Spacing.screenGutter`, 위 `Spacing.xl`(첫 묶음은 `Spacing.lg`) 아래 `Spacing.xs` |
| 스위치 행 | `SettingsSwitchItem`(신규 공용, 3절): 최소 높이 72dp(제목 + 설명 2줄 기준), 가로 `Spacing.screenGutter`, 세로 `Spacing.md`, 글자와 스위치 사이 `Spacing.lg`. 행 전체가 터치 영역(`toggleable(role = Switch)`) |
| 제목 | `bodyLarge`, `onSurface` |
| 설명 | `bodyMedium`, `onSurfaceVariant`, 줄 수 제한 없음 |
| 링크 행 | `SettingsLinkItem`(신규 공용, 3절): 최소 높이 56dp, 제목 `bodyLarge` `onSurface`, 끝 아이콘 `Icons.AutoMirrored.Filled.KeyboardArrowRight`(코어) 또는 외부 링크면 `ic_open_in_new`(기존), `Sizes.iconSmall`, `onSurfaceVariant` |
| 버전 행 | `SettingsLinkItem(onClick = null)`: 제목 "버전", 끝에 값 `bodyMedium` tnum `onSurfaceVariant`. 화살표 없음 |
| 안내 줄 | `InlineNotice`(예정 공용, `error-recovery.md` 시안 D), 행 아래 가로 `Spacing.screenGutter` |
| 구분 | 행 사이 구분선 없음(묶음 제목과 간격으로 나눈다). 바탕은 화면 `background` 그대로, 행에 카드를 두르지 않는다(설정은 목록 하나 한 종류로 — 리뷰 2절) |
| 광고 | 설정 화면에는 배너를 넣지 않는다(CONVENTIONS 광고 원칙: 메인·재생 목록만) |

### 2-1. "정보" 묶음을 두는 판단(PM 결정 ③)

**중복으로 둔다.** 메인 하단 링크 묶음(`main-legal-footer.md`)은 그대로 두고, 설정 맨 아래 "정보" 묶음에 같은 동작을 한 번 더 둔다.

- 메인 하단에 두는 이유: 개인정보처리방침은 "메인에서 두 번 이하 탭" 조건이 있고(1(a)-4), 저작권 안내와 이용 안내는 심사관·사용자가 첫 화면에서 보는 것이 목적이다. 이미 구현되어 있어 옮기면 화면 테스트만 바뀐다.
- 설정에도 두는 이유: 버전·라이선스를 찾는 사용자는 설정부터 연다(안드로이드 앱 관례). 같은 문자열 키·같은 콜백을 쓰므로 비용이 작다.
- 1(a)-4에서 "설정이 생기면 버전·라이선스·광고 개인정보 설정을 설정으로 옮긴다"고 했던 것은 이 PM 결정으로 **옮기지 않고 중복**으로 바꾼다. 메인 하단은 4개 링크 그대로.

## 3. 컴포넌트·토큰

| 이름 | 종류 | 설명 |
|---|---|---|
| `SettingsSwitchItem` | 신규 공용 `ui/components/` | 인자: `title`, `description: String?`, `checked`, `onCheckedChange`, `enabled`. 스위치는 M3 `Switch`, 행이 `toggleable`이라 스위치 자체의 `onCheckedChange = null`(두 번 읽힘 방지) |
| `SettingsLinkItem` | 신규 공용 | 인자: `title`, `value: String?`, `external: Boolean`, `onClick: (() -> Unit)?` |
| `SectionHeader` | 신규 공용 | `subtitles.md`와 같이 쓴다 |
| `InlineNotice` | 예정 공용(#72) | 이 화면, 링크 시트, 준비 화면 |
| `StatusTone.WAITING_WIFI` | `StatusChip` 변경 | 4절 |
| `StatusChip(icon: ImageVector? = null)` | `StatusChip` 변경 | 점 대신 아이콘(`Sizes.iconSmall`, tone 색). 기존 호출 영향 없음 |
| `ic_wifi.xml` | 신규 벡터 `res/drawable/` | Material Symbols "wifi" 24dp. 코어 아이콘에 없다 |
| 스위치 색 | `BrandColors`로 `SwitchDefaults.colors(...)` | 켬: 트랙 `primary`, 손잡이 `onPrimary`. 끔: 트랙 `surfaceContainer`, 테두리·손잡이 `onSurfaceVariant`. **끔 상태 테두리를 `outline`으로 두지 않는다**(표면 대비 1.5로 1.4.11 미달). 사용 안 함: 각 색 38% |

새 색·간격·크기 토큰 없음(스위치 색은 기존 토큰 조합). `SwitchDefaults.colors`는 `SettingsSwitchItem` 안에 한 번만 둔다.

## 4. Wi-Fi 대기 상태 칩과 재생 목록

```
┌ 재생 목록 카드 ───────────────────────────────────┐
│ Big.Buck.Bunny.2008.1080p.mkv                     │
│ [📶 Wi-Fi 대기]  1.2 GB / 3.4 GB                  │  StatusChip(WAITING_WIFI, icon = ic_wifi)
│ ▓▓▓▓▓▓░░░░░░░░░░                                  │  ProgressBar 그대로(받은 부분 그라데이션)
│ [재생] [중지]                         [🗑]         │
└───────────────────────────────────────────────────┘
요약 줄: 다운로드 중 0개 · Wi-Fi 대기 2개
```

| 요소 | 모습 |
|---|---|
| 칩 | `StatusTone.WAITING_WIFI`: 아이콘 `ic_wifi` `onSurfaceVariant`, 글자 "Wi-Fi 대기" `onSurface`. 대기(`QUEUED`)와 같은 회색 계열이지만 아이콘과 글자로 구분한다(리뷰 2절) |
| 칩 우선순위 | Wi-Fi 대기 > 다운로드 중 > 대기 > 완료 > 실패 > 미완료. Wi-Fi 대기에 걸린 항목은 원래 "다운로드 중"이었든 "대기"였든 칩 하나만 "Wi-Fi 대기" |
| 속도 | 숨긴다(0 B/s를 보이지 않는다). 받은 양만 |
| 동작 버튼 | "중지"(`action_stop`, 기존). 누르면 대기에서 빠지고 미완료가 된다. Wi-Fi로 돌아와도 다시 시작하지 않는다 |
| 요약 줄 | 지금 "다운로드 중 %d개 · 대기 %d개"에 " · Wi-Fi 대기 %d개"를 붙인다. 0개인 조각은 빼고 이어 붙인다(program이 조각을 `separator_dot`으로 잇는다) |
| 재개 | Wi-Fi에 다시 연결되면 칩이 바로 "다운로드 중"/"대기"로 바뀐다. 따로 알림을 띄우지 않는다 |
| 다운로드 버튼(Wi-Fi 전용 + 모바일) | 누르면 바로 "Wi-Fi 대기"로 들어가고 스낵바 `info_waiting_for_wifi` |

준비 화면·재생 화면에서 받던 영상(바로 재생 중)이 모바일로 바뀐 경우: 사용자가 "이번만 재생"을 고른 재생이면 그 재생이 끝날 때까지 받는다. 확인 없이 Wi-Fi에서 시작한 재생 중에 모바일로 바뀌면 재생 버퍼가 멈추므로 확인 창(5절)을 그 자리(재생 화면 위)에 띄운다 — 6절 확인 필요.

## 5. 모바일 데이터 재생 확인 창

```
┌ ConfirmDialog ───────────────────────────────────┐
│ 이번만 모바일 데이터로 재생할까요?        titleLarge │
│                                                   │
│ Wi-Fi 전용이 켜져 있습니다. 영상 크기만큼           │  bodyMedium onSurfaceVariant
│ 모바일 데이터를 쓸 수 있습니다.                     │
│                                                   │
│                          취소      이번만 재생     │  TextAction MUTED / DEFAULT
└───────────────────────────────────────────────────┘
```

- 기존 `ConfirmDialog(destructive = false)` 그대로. 확인 버튼 `primaryLight`, 취소 `onSurfaceVariant`.
- PM이 정한 두 문장("Wi-Fi 전용이 켜져 있습니다." / "이번만 모바일 데이터로 재생할까요?")을 그대로 쓰되, 질문을 제목으로 올리고 설명에 결과(데이터 사용)를 한 문장 붙였다. 확인 창은 제목만 읽어도 무엇을 고르는지 알아야 한다(`design:ux-copy` 확인 창 원칙).
- 뜨는 곳: 메인 "바로 재생", 링크 시트 "바로 재생"·"이어서 재생", 재생 목록 "재생"(완료되지 않은 항목). **다 받은 영상 재생은 데이터를 쓰지 않으므로 묻지 않는다.**
- "이번만 재생": 이 재생 한 번에만 모바일 데이터를 허용한다. 설정은 바꾸지 않는다. 다음 재생은 다시 묻는다.
- "취소"·바깥 탭·뒤로: 아무것도 시작하지 않는다. 메인 입력란은 그대로.
- 연결이 아예 없으면 이 창이 아니라 `error_offline`(시안 D).

## 6. 최종 문구 (strings.xml)

| 키 | 문구 | 비고 |
|---|---|---|
| `title_settings` | 설정 | 신규. 상단 바 제목, 메인 톱니 읽기 이름 |
| `settings_section_download` | 다운로드 | 신규 |
| `settings_section_playback` | 재생 | 신규(`action_play` "재생"과 글자는 같지만 묶음 제목이라 키를 나눈다 — 영어 번역 때 "Playback"/"Play"로 갈린다) |
| `settings_section_about` | 정보 | 신규 |
| `settings_wifi_only` | Wi-Fi 전용 | 신규. 확인 창 문장과 같은 이름 |
| `settings_wifi_only_desc` | 모바일 데이터에서는 다운로드를 멈춥니다. | 신규. PM 결정 문구 + 마침표 |
| `settings_wifi_paused_notice` | 모바일 데이터에 연결되어 다운로드를 멈췄습니다. Wi-Fi에 연결되면 이어서 받습니다. | 신규. 켬 + 지금 모바일일 때 안내 줄 |
| `settings_auto_subtitles` | 자막 자동 표시 | 신규. PM 결정 이름 |
| `settings_auto_subtitles_desc` | 한국어 자막이 있으면 재생할 때 바로 켭니다. | 신규 |
| `settings_version` | 버전 | 신규. 값은 버전명 그대로 |
| `settings_save_failed` | 설정을 저장하지 못했습니다. 다시 눌러 주세요. | 신규. 저장 실패 스낵바 |
| `status_wifi_waiting` | Wi-Fi 대기 | 신규. 상태 칩, 용어 표 추가 요청(planning) |
| `playlist_summary_wifi_waiting` | Wi-Fi 대기 %1$d개 | 신규. 요약 줄 조각 |
| `info_waiting_for_wifi` | Wi-Fi에 연결되면 받기 시작합니다. | 신규. 스낵바 |
| `mobile_play_confirm_title` | 이번만 모바일 데이터로 재생할까요? | 신규 |
| `mobile_play_confirm_message` | Wi-Fi 전용이 켜져 있습니다. 영상 크기만큼 모바일 데이터를 쓸 수 있습니다. | 신규 |
| `action_play_once` | 이번만 재생 | 신규 |
| `action_cancel` | 취소 | 기존 |
| `action_usage_guide`, `action_privacy_policy`, `action_open_source_licenses`, `action_ad_privacy` | (기존) | 정보 묶음에서 재사용 |
| `cd_opens_in_browser` | 브라우저에서 열림 | 기존. 개인정보처리방침 행 |
| `cd_settings` | — | 만들지 않는다. 톱니 읽기 이름은 `title_settings` 재사용 |

함께 고칠 기존 문구:

| 키 | 지금 | 바뀜 |
|---|---|---|
| `guide_data_body` | 영상 크기만큼 데이터를 씁니다. 모바일 데이터에서는 요금이 나올 수 있으니 Wi-Fi에서 쓰는 것을 권합니다. | 영상 크기만큼 데이터를 씁니다. 모바일 데이터에서는 요금이 나올 수 있으니, 설정에서 Wi-Fi 전용을 켜 두는 것을 권합니다. |

용어: "Wi-Fi"는 영문 표기 그대로(국내 표준 표기, 한글 "와이파이"로 쓰지 않는다). "모바일 데이터"는 안드로이드 설정 용어와 같다. planning에 용어 표 추가 요청: 설정, Wi-Fi 전용, Wi-Fi 대기.

## 7. 상태별 모습

| 상태 | 모습 |
|---|---|
| 처음 | Wi-Fi 전용 끔, 자막 자동 표시 켬. 안내 줄 없음 |
| Wi-Fi 전용 켬 + Wi-Fi | 안내 줄 없음 |
| Wi-Fi 전용 켬 + 모바일 데이터 | 안내 줄 보임. 재생 목록 받던 항목은 Wi-Fi 대기 |
| 켬 → 끔(모바일 데이터에서) | Wi-Fi 대기 항목이 바로 다시 받기 시작한다. 확인을 묻지 않는다(사용자가 스스로 끈 것) |
| 연결 없음 | 안내 줄 없음(멈춘 이유가 Wi-Fi 전용이 아니다). 재생 목록은 지금 동작 그대로 |
| 설정 저장 실패 | 스위치를 원래대로 되돌리고 스낵바 `settings_save_failed` |
| 세로 | 360×640dp에서 정보 묶음 일부까지 첫 화면. 나머지 스크롤 |
| 가로 | 본문 600dp 가운데, 스크롤 |
| 글자 200% | 제목·설명이 여러 줄로 감기고 스위치는 행 세로 가운데에 붙어 있다. 스위치 폭(52dp)은 줄지 않는다. 버전 행은 값이 길면 제목 아래 줄로 내려간다(`FlowRow`) |
| 메인 톱니 + 글자 200% | 아이콘 크기는 글자 크기와 무관(그대로 24dp) |
| 라이트·다크 | 다크 하나 |

## 8. 접근성

| 요소 | 읽기 | 기준 |
|---|---|---|
| 메인 톱니 | "설정, 버튼" | 1.1.1, 4.1.2 |
| 스위치 행 | "Wi-Fi 전용, 모바일 데이터에서는 다운로드를 멈춥니다, 스위치, 꺼짐" — 행 하나로 합쳐 읽고 두 번 탭으로 바뀐다 | 4.1.2, `toggleable` + `mergeDescendants` |
| 묶음 제목 | "다운로드, 제목" | 1.3.1 |
| 안내 줄 | 나타날 때 `liveRegion = Polite`로 한 번 읽힘 | 4.1.3 상태 메시지 |
| 개인정보처리방침 행 | "개인정보처리방침, 브라우저에서 열림, 버튼" | 메인 하단과 같다 |
| 버전 행 | "버전, v.2026.11.13.001" — 버튼 역할 없음 | |
| Wi-Fi 대기 칩 | "Wi-Fi 대기"(아이콘은 장식) | 1.4.1 색이 아니라 글자로 |
| 확인 창 | 제목 → 설명 → 취소 → 이번만 재생 | 2.4.3 |

대비(계산값):

| 앞 | 바탕 | 대비 | 기준 | 결과 |
|---|---|---|---|---|
| `onSurface` 행 제목 | `background` | 16.5 | 4.5 | 통과 |
| `onSurfaceVariant` 설명·묶음 제목 | `background` | 9.1 | 4.5 | 통과 |
| 켬 트랙 `primary` | `background` | 3.9 | 3(비텍스트) | 통과 |
| 끔 테두리 `onSurfaceVariant` | `background` | 9.1 | 3 | 통과 |
| 끔 테두리를 `outline`으로 둘 경우 | `background` | 약 1.6 | 3 | **미달 — 쓰지 않는다** |
| `onSurfaceVariant` 칩 아이콘 | `surfaceContainer` | 7.4 | 3 | 통과 |

- 터치: 톱니 48dp, 스위치 행 72dp, 링크 행 56dp.

## 9. PM·planning 확인 필요

1. **첫 모바일 데이터 사용 확인(planning 2-1)**: 요구사항에는 "모바일 데이터에서 처음 재생할 때 한 번 묻고, Wi-Fi에서만 받기를 고르면 설정이 켜진다"가 있으나 PM 결정에는 없다. 이번 시안에 넣지 않았다. 넣는다면 같은 `ConfirmDialog`로 제목 "모바일 데이터로 받을까요?", 설명 "영상 크기만큼 데이터가 쓰일 수 있습니다.", 버튼 [계속] [Wi-Fi에서만 받기]를 제안한다(문구 미확정, 키 미등록).
2. **준비 화면 세부 정보의 연결 종류(planning 2-4)**: PM 결정에 없어 넣지 않았다.
3. **Wi-Fi에서 시작한 재생 중 모바일로 바뀜**: 4절 끝. 시안은 재생 화면 위에 같은 확인 창을 띄우는 쪽이다. 묻지 않고 계속 받는 쪽도 가능하다.
4. **"모바일 데이터"의 범위**: 셀룰러 연결만인지, 데이터 제한 Wi-Fi(휴대폰 핫스팟)도 포함인지. planning은 "데이터 제한 연결 포함". 문구는 어느 쪽이든 그대로 맞다.
5. **이어보기 카드 영구 끄기(planning-60 Q8)**: "설정 화면이 생기면 설정 항목으로 옮긴다"고 되어 있다. 이어보기(resume)가 승인되면 "재생" 묶음에 항목을 더한다. 이번에는 넣지 않았다.

## 10. 확인하지 못한 것

- 에뮬레이터 확인 없음(문서 시안). 줄 수·높이는 360dp·글자 100% 추정.
- 메인에 톱니를 더한 뒤 첫 화면에서 "바로 재생" 버튼이 접히는지(작은 화면 360×640dp, 키보드 없음)는 구현 후 캡처로 확인해야 한다.
