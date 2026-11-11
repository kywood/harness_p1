# 시안 C: 이용 안내 시트

- 요구사항: `planning/requests/policy-notice-and-privacy.md` 2-3, 완료 조건 "이용 안내에 '권리가 있는 콘텐츠만' 취지 문장"
- 진입: 메인 하단 "이용 안내"(시안 A). 첫 사용 안내(새 기능, 승인 전)가 생기면 마지막 장에서 같은 문장(`main_rights_notice`)을 재사용한다
- 형식 결정: **앱 안 하단 시트**(웹 문서 아님). 오프라인에서도 읽히고, 심사관이 앱 안에서 바로 확인할 수 있으며, 내비게이션(`AppScreen`)을 늘리지 않는다(리뷰 Q2)
- 상태: 시안

## 1. 배치

```
┌ ModalBottomSheet (surface, 위 모서리 Shapes.large) ┐
│                ───  (끌기 손잡이)                   │
│ 이용 안내                                titleLarge │
│                                                     │
│ ┌ 강조 카드: surfaceContainer, Shapes.medium ─────┐ │
│ │ (i) 이용할 권리가 있는 콘텐츠만 재생하세요.      │ │ titleSmall onSurface, 아이콘 primaryLight
│ │     PiecePlay는 콘텐츠를 제공하거나 찾아 주지   │ │ bodyMedium onSurfaceVariant
│ │     않습니다. …                                 │ │
│ └─────────────────────────────────────────────────┘ │
│                                                     │
│ 영상 하나만 받습니다                      titleSmall │
│ 마그넷 링크나 .torrent 파일 주소를 넣으면 …  bodyMedium│
│                                                     │
│ 올려 보내지 않습니다                                 │
│ …                                                   │
│ 기기 안에만 저장합니다                               │
│ …                                                   │
│ 데이터를 많이 씁니다                                 │
│ …                                                   │
│ 광고                                                │
│ …                                                   │
│ ─────────────── (outline 구분선) ───────────────── │
│ 개인정보처리방침 ↗                     TextAction    │
│                                                     │
│ [            확인            ]   SecondaryButton 폭 전체 │
└─────────────────────────────────────────────────────┘
```

| 요소 | 컴포넌트·토큰 |
|---|---|
| 시트 | `AppBottomSheet`(신규 공용, 아래 4절). `skipPartiallyExpanded = true`(바로 전체로 펼침), 바탕 `surface`, 안쪽 가로 여백 `Spacing.screenGutter`, 아래 `navigationBarsPadding` + `Spacing.lg` |
| 내용 | `Column` + `verticalScroll`. 절 사이 `Spacing.lg`, 제목과 본문 사이 `Spacing.xs` |
| 제목 | `titleLarge`(renewal-plan "화면 제목") |
| 강조 카드 | `Surface(color = surfaceContainer, shape = Shapes.medium)`, 안쪽 `Spacing.lg`. 아이콘 `Icons.Filled.Info`(코어) `Sizes.icon` `primaryLight` |
| 절 제목 | `titleSmall`, `onSurface` |
| 절 본문 | `bodyMedium`, `onSurfaceVariant` |
| 구분선 | `HorizontalDivider(color = outline)` |
| 개인정보처리방침 | `TextAction(DEFAULT, trailingIcon = ic_open_in_new)` — 시안 A와 같은 동작·키 |
| 확인 | `SecondaryButton`, `fillMaxWidth`. 화면의 주요 동작이 아니라 닫기라 `PrimaryButton`을 쓰지 않는다 |

- 저작권 문장을 맨 위 강조 카드에 둔다. 심사관과 사용자가 스크롤하지 않고 먼저 본다.
- 첫 사용 안내·의견 보내기가 승인되면 구분선 아래 `TextAction` 줄에 "사용 방법 다시 보기", "의견 보내기"를 더한다(리뷰 1(a)). 지금은 넣지 않는다.

## 2. 최종 문구

사실 확인 근거: `CLAUDE.md` 아키텍처, precheck "데이터 보안"(업로드 없음 `DownloadOnlyPolicy`, 완료 후 정지, `noBackupFilesDir`, 재생 목록·상태 암호화, P2P IP 노출), 광고 원칙.

| 키 | 문구 |
|---|---|
| `action_usage_guide` | 이용 안내 (시트 제목, 시안 A와 같은 키) |
| `main_rights_notice` | 이용할 권리가 있는 콘텐츠만 재생하세요. (강조 카드 제목, 시안 A와 같은 키) |
| `guide_rights_body` | PiecePlay는 콘텐츠를 제공하거나 찾아 주지 않습니다. 재생할 링크는 직접 고르며, 저작권을 침해하는 이용에 대한 책임은 사용자에게 있습니다. |
| `guide_what_title` | 영상 하나만 받습니다 |
| `guide_what_body` | 마그넷 링크나 .torrent 파일 주소를 넣으면, 다 받기 전에 앞부분부터 받아 바로 재생합니다. 토렌트 안에서 가장 큰 영상 파일 하나만 받고 나머지 파일은 받지 않습니다. |
| `guide_upload_title` | 올려 보내지 않습니다 |
| `guide_upload_body` | 받기만 하고 다른 사람에게 올려 보내지 않습니다. 다 받으면 바로 연결을 멈춥니다. 다만 토렌트 방식의 특성상, 연결된 사용자에게 이 기기의 IP 주소가 보입니다. |
| `guide_storage_title` | 기기 안에만 저장합니다 |
| `guide_storage_body` | 받은 영상과 재생 목록은 이 기기에만 저장되고 어디로도 보내지 않습니다. 재생 목록과 이어받기 정보는 암호화해 보관합니다. 저장 파일 관리에서 지우거나 앱을 삭제하면 모두 지워집니다. |
| `guide_data_title` | 데이터를 많이 씁니다 |
| `guide_data_body` | 영상 크기만큼 데이터를 씁니다. 모바일 데이터에서는 요금이 나올 수 있으니 Wi-Fi에서 쓰는 것을 권합니다. |
| `guide_ads_title` | 광고 |
| `guide_ads_body` | 메인과 재생 목록에 배너 광고가 나오고, 재생을 마친 뒤 가끔 전면 광고가 나옵니다. 재생하는 동안에는 광고가 나오지 않습니다. |
| `action_privacy_policy` | 개인정보처리방침 (시안 A와 같은 키) |
| `action_confirm` | 확인 (신규, 공용) |

문구 점검:
- 용어: "재생 목록", "저장 파일 관리"는 CONVENTIONS 용어 표 그대로. "피어" 대신 "연결된 사용자"(본문 문장 기준, 리뷰 1(c)-5).
- "업로드하지 않음"과 "IP 주소 노출"을 같은 절에 둔다. 장점만 쓰면 사실보다 안전해 보이는 문구가 된다.
- `guide_ads_body`는 `CompletionAdPolicy` 규칙(5분 시청 또는 5회 완료, 10분 간격)을 "가끔"으로 줄였다. 규칙이 바뀌어도 문장은 맞다. 광고 규칙이 "재생 중 광고"를 허용하는 쪽으로 바뀌면 이 문장을 같이 고친다(program 체크 항목).
- 정책 위험 단어(무료 영화·드라마, 사이트 이름)를 쓰지 않았다.

## 3. 상태별 모습

| 상태 | 모습 |
|---|---|
| 열림 | 전체 펼침, 맨 위부터. 세로 360×640dp에서 강조 카드와 첫 절이 첫 화면에 보이고 나머지는 스크롤 |
| 닫기 | "확인", 시트 밖 탭, 아래로 끌기, 시스템 뒤로 — 모두 메인으로 |
| 가로 | 시트 최대 폭 `Sizes.contentMaxWidth`(600dp) 가운데. 높이가 낮아 내용 대부분이 스크롤 |
| 글자 200% | 전부 줄바꿈, 스크롤로 끝까지 닿는다. "확인"은 스크롤 내용의 맨 끝(고정 버튼으로 두지 않는다 — 작은 가로 화면에서 본문을 가린다) |
| 브라우저 열기 실패 | 시트를 닫지 않고 스낵바 `error_browser_unavailable`. 스낵바가 시트 뒤에 가리면 시트 안 `Text`(error 색)로 대신한다(구현 때 확인) |
| 광고 | 시트가 메인 위에 덮이므로 배너는 시트 아래에 가려진다. 시트에 광고를 넣지 않는다 |

## 4. 컴포넌트

- **`AppBottomSheet`(신규, `ui/components/`)**: 지금 재생 화면 `PlayerMoreSheet`가 `ModalBottomSheet`를 직접 쓴다. 이용 안내가 두 번째 사용처이므로 공용으로 뽑는다(풀링 규칙). 기본값: 바탕 `surface`, `sheetMaxWidth = Sizes.contentMaxWidth`, 아래 `navigationBarsPadding()` + `Spacing.lg`, 제목 슬롯(선택). `PlayerMoreSheet`는 이 컴포넌트로 옮긴다(시각 변화 없음).
- 새 토큰 없음.

## 5. 접근성

- 시트 제목과 절 제목은 `heading()`. TalkBack 제목 탐색으로 절을 건너뛸 수 있다.
- 강조 카드 아이콘은 장식(`contentDescription = null`).
- 시트가 열리면 초점이 제목으로 간다(Material 기본). 닫히면 "이용 안내" 링크로 돌아간다.
- 대비: `onSurfaceVariant` / `surface` 8.3:1, `/ surfaceContainer` 7.4:1(renewal-plan 2절).
