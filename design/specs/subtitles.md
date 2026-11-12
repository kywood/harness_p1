# 시안 E: 자막·음성 선택 시트

- 요구사항: `planning/requests/subtitles-audio-tracks.md`, 화면 정의 `planning/screens/player.md`
- 이슈: #125(1단계 내장 트랙), #126(2단계 토렌트 안 자막 파일)
- 리뷰: `design/reviews/planning-60.md` 1(b) 재생 화면 상단 버튼
- PM 결정: 상단 "자막" 버튼 → `AppBottomSheet`(자막 목록 + 음성 목록은 2개 이상일 때만). 외부 자막은 파일 이름 + "파일" 표시. 한국어 자막 자동 선택, 처음 켜질 때 2~3초 알림. 이름 없는 트랙은 "트랙 1"
- 상태: 시안. 트랙 조회·선택·기억, 자막 파일 받기, 언어 판별은 program

## 1. 재생 화면 상단 바

1(b) 결론대로 상단 오른쪽을 고정 순서로 바꾸고, 배율 3버튼(축소 · 1.0× · 확대)을 더보기 시트로 옮긴다.

```
지금:  ←  파일 이름…  [회전] [축소] [1.0×] [확대] [⋮]        오른쪽 5개(240dp)
바뀜:  ←  파일 이름…………  [자막] [회전] [⋮]                오른쪽 3개(144dp)
       [↓ 1.20 MB/s · 35% · 피어 3명] [1.5×]                세로: 통계 칩 줄. 배율 칩은 1.0×가 아닐 때만
```

가로 화면은 지금처럼 통계 칩이 같은 줄에 들어가고, 배율 칩은 통계 칩 바로 오른쪽이다.

| 요소 | 컴포넌트·토큰 | 비고 |
|---|---|---|
| 자막 버튼 | 기존 `OverlayIconButton`, 아이콘 `ic_subtitles`(신규 벡터) `Sizes.icon` 24dp, `PlaybackColors.OnVideo` | 터치 48dp(IconButton 기본) |
| 자막이 켜져 있을 때 | 같은 아이콘을 `PlaybackColors.Accent`(primaryLight)로. 아이콘 아래 2dp 밑줄은 쓰지 않는다 | 색만으로 알리지 않도록 읽기 문구가 "자막, 켜짐: 한국어"로 바뀐다(6절) |
| 순서 | (TV로 보내기, 승인 시) · 자막 · 회전 · 더보기 | 1(b) 고정 순서. TV 버튼이 생겨도 4개 |
| 배율 칩 | `PlaybackColors.Panel` 바탕 + `OnVideo` 글자 `labelMedium` tnum, `Shapes.pill`, 안쪽 `Spacing.md`×`Spacing.xs`. 터치 영역은 `minimumInteractiveComponentSize()`로 48dp | 1.0×일 때 숨김. 누르면 1.0×로(기존 `onZoomReset`) |
| 통계 칩 | 그대로 | |

배율 칩 바탕은 통계 칩의 `PlaybackColors.Chip`(surface 70%)이 아니라 `Panel`(94%)을 쓴다. 흰 영상 위에서 `Chip` 바탕의 `primaryLight` 글자는 2.4:1로 AA에 못 미친다(아래 7절). 기존 통계 칩도 같은 문제가 있어 같은 작업에서 `Panel`로 바꾸기를 권한다.

### 더보기 시트(바뀜)

```
┌ AppBottomSheet ──────────────────────────────────┐
│ [🔍] 화면 확대            [−]  1.5×  [+]          │  ListItem, 끝에 IconButton 2개 + 값
│ [+]  링크 추가                                    │  (재생 목록에서 연 경우 없음, 지금과 같음)
│ [↻]  재생 위치 초기화 · 처음부터                   │
└───────────────────────────────────────────────────┘
```

- "화면 확대" 줄: 앞 아이콘 `ic_zoom_in`(기존) `tertiary`, 끝에 `IconButton(ic_zoom_out)` · 값 `labelLarge` tnum · `IconButton(ic_zoom_in)`. 값을 누르면 1.0×(지금 `SecondaryLabelButton`과 같음). 줄 자체는 누를 수 없다(버튼만).
- 확대 버튼을 눌러도 시트는 닫지 않는다(여러 번 누르기). 두 손가락 확대는 그대로다.
- 버튼 설명은 기존 키(`cd_zoom_out`, `cd_zoom_in`, `cd_zoom_reset`)를 그대로 쓴다. androidTest에 이 설명으로 찾는 테스트는 지금 없다(확인함).

## 2. 자막·음성 시트

```
┌ AppBottomSheet (반 펼침으로 열림 → 위로 끌면 전체) ─┐
│                ───                                │
│ 자막                                 titleSmall heading │
│ (●) 끄기                                          │  RadioListItem
│ ( ) 한국어                                        │
│ ( ) 영어                                          │
│ ( ) Movie.Name.2024.ko.srt                [파일]  │  외부 자막: 파일 이름 + 태그
│ ( ) Movie.Name.2024.en.smi             [받는 중]  │  아직 받는 중: 흐리게, 고를 수 없음
│ ─────────────── (outline 구분선) ──────────────── │
│ 음성                                 titleSmall heading │  음성 트랙이 2개 이상일 때만
│ (●) 일본어                                        │
│ ( ) 한국어                                        │
└───────────────────────────────────────────────────┘
```

| 요소 | 컴포넌트·토큰 |
|---|---|
| 시트 | `AppBottomSheet(title = null, skipPartiallyExpanded = false)`. 반 펼침으로 열어 위쪽에 영상이 보이게 한다(고르면서 자막을 바로 확인). 내용은 `Column(verticalScroll)` |
| 묶음 제목 | `SectionHeader`(신규 공용, 5절): `titleSmall`, `onSurfaceVariant`, 가로 `Spacing.screenGutter`, 위 `Spacing.sm` 아래 `Spacing.xs`, `heading()` |
| 행 | `RadioListItem`(신규 공용, 5절): 최소 높이 56dp, 앞에 `RadioButton`, 글자 `bodyLarge` `onSurface` 최대 2줄 말줄임(가운데가 아니라 끝), 끝에 선택 태그 |
| 라디오 색 | 선택 `primaryLight`(표면 대비 6.4), 선택 안 함 `onSurfaceVariant`(8.3) |
| 태그 "파일"·"받는 중"·"받지 못함" | `labelSmall`, 글자 `onSurfaceVariant`, 바탕 `surfaceContainer`, `Shapes.small`, 안쪽 가로 `Spacing.sm`, 세로 0(글자 줄 높이로 맞춘다). 새 크기 토큰을 만들지 않는다 |
| 구분선 | `HorizontalDivider(color = outline)`, 위아래 `Spacing.sm` |
| 시트 아래 | `AppBottomSheet` 기본(내비게이션 바 + `Spacing.lg`) |

동작:

- 행을 누르면 바로 바뀌고 시트는 열린 채로 둔다(자막과 음성을 이어서 바꿀 수 있고, 영상에서 결과가 보인다). 닫기는 시트 밖 탭·아래로 끌기·뒤로.
- 시트가 열려 있는 동안 재생 조작 UI 자동 숨김(3.5초)을 멈춘다. 닫히면 다시 센다.
- 영상은 계속 재생한다(일시 정지하지 않음).

### 목록 순서와 이름

| 순서 | 항목 | 이름 규칙 |
|---|---|---|
| 1 | 끄기 | 항상 맨 위 |
| 2 | 내장 자막 트랙 | VLC가 주는 순서. 이름은 언어(아래) → 트랙 설명 → 둘 다 비면 "트랙 N"(N은 이 목록 안 1부터) |
| 3 | 외부 자막 파일 | 파일 이름(경로 없이, 확장자 포함) + 태그 "파일". 파일 이름 순 |
| 음성 | 오디오 트랙 | 내장 자막과 같은 이름 규칙. "끄기"는 두지 않는다 |

- 언어 이름: 언어 코드가 있으면 한국어 이름으로(`ko`/`kor` → 한국어, `en`/`eng` → 영어, `ja`/`jpn` → 일본어, `zh`/`chi`/`zho` → 중국어). 그 밖의 코드는 VLC 설명 그대로. 같은 언어가 둘 이상이면 뒤에 설명을 괄호로 붙인다: "영어 (SDH)", 설명도 같으면 "영어 2".
- VLC 설명 원문에 "Track 1 - [English]"처럼 영어 틀이 섞이면, 언어를 뽑을 수 있을 때 언어 이름만 쓴다. 뽑지 못하면 원문 그대로(트랙 이름은 영상 제작자가 정한 자료라 예외 원문 금지 규칙과 다르다).

## 3. 자동 선택 알림

한국어 자막이 자동으로 켜질 때(설정 "자막 자동 표시"가 켜져 있을 때만, `settings-wifi.md`) 한 번 보인다.

```
┌ 영상 ─────────────────────────────────────────────┐
│                                                   │
│                                                   │
│ ┌──────────────────────┐                          │
│ │ [CC] 자막: 한국어(파일) │  ← 2.5초 뒤 사라짐        │  PlayerNoticeChip
│ └──────────────────────┘                          │
│ ── (하단 패널: 시간·재생 막대·조각 맵) ──             │
└───────────────────────────────────────────────────┘
```

| 요소 | 토큰 |
|---|---|
| 위치 | 왼쪽 아래. 조작 UI가 보이면 하단 패널 바로 위(`Spacing.sm` 띄움), 숨어 있으면 화면 아래에서 `Spacing.lg`(오른쪽 아래 시간 표시와 겹치지 않는 왼쪽). 가운데 대기 패널 자리는 쓰지 않는다(버퍼링 안내와 겹침, 리뷰 resume 항목과 같은 판단) |
| 모양 | `PlayerNoticeChip`(신규, 재생 화면 전용, 5절): `PlaybackColors.Panel` 바탕, `Shapes.pill`, 안쪽 `Spacing.md`×`Spacing.sm`, 앞에 `ic_subtitles` `Sizes.iconSmall` `OnVideo` |
| 글자 | `labelLarge`, `OnVideo`, 1줄, 넘치면 끝 말줄임. 최대 폭 화면 폭의 70% |
| 시간 | 2.5초 보이고 사라진다. 나타남·사라짐은 기본 `AnimatedVisibility(fadeIn/fadeOut)` |
| 다시 나오는 조건 | 이 영상을 이번에 열었을 때 자동 선택이 일어난 경우 한 번. 사용자가 직접 고른 경우에는 안 나온다(이미 시트에서 봤다) |

- 알림 이름은 목록 이름 규칙을 따르되, 외부 파일은 언어를 알면 "한국어(파일)", 모르면 파일 이름 + "(파일)".
- 이어보기 칩(resume, 승인 시)과 같은 자리라 둘이 겹치면 이어보기 칩 → 자막 알림 순서로 이어서 보인다(동시에 두 개를 쌓지 않는다).
- 알림을 눌러도 아무 일도 없다(누를 수 있는 것처럼 보이지 않게 그림자·물결 없음).

## 4. 최종 문구 (strings.xml)

| 키 | 문구 | 비고 |
|---|---|---|
| `label_subtitles` | 자막 | 신규. 상단 버튼 읽기 이름, 시트 묶음 제목 |
| `label_audio` | 음성 | 신규. 시트 묶음 제목 |
| `subtitle_off` | 끄기 | 신규 |
| `subtitle_none` | 자막 없음 | 신규. 내장·외부 자막이 하나도 없을 때 "끄기" 대신 한 줄(누를 수 없음) |
| `track_untitled` | 트랙 %1$d | 신규 |
| `track_duplicate` | %1$s %2$d | 신규, `translatable="false"`. "영어 2" |
| `track_with_detail` | %1$s (%2$s) | 신규, `translatable="false"`. "영어 (SDH)" |
| `tag_subtitle_file` | 파일 | 신규. 외부 자막 태그 |
| `tag_downloading` | 받는 중 | 신규. 외부 자막을 아직 받는 중 |
| `tag_download_failed` | 받지 못함 | 신규. 외부 자막 받기 실패(고를 수 없음) |
| `tracks_loading` | 트랙을 확인하는 중… | 신규. 재생기가 아직 트랙을 알려 주지 않았을 때 시트 안 한 줄 |
| `subtitle_auto_notice` | 자막: %1$s | 신규. 알림 칩 |
| `subtitle_file_suffix` | %1$s(파일) | 신규. 알림 칩·읽기 문구에서 외부 파일 표시 |
| `lang_ko` | 한국어 | 신규 |
| `lang_en` | 영어 | 신규 |
| `lang_ja` | 일본어 | 신규 |
| `lang_zh` | 중국어 | 신규 |
| `error_track_select` | 트랙을 바꾸지 못했습니다. 다시 골라 주세요. | 신규. 선택 실패 시 시트 아래 스낵바(시트를 닫지 않음) |
| `cd_subtitles_on` | 자막, 켜짐: %1$s | 신규. 자막이 켜져 있을 때 상단 버튼 읽기 문구 |
| `cd_subtitles_off` | 자막, 꺼짐 | 신규 |
| `cd_subtitle_file` | %1$s, 자막 파일 | 신규. 외부 자막 행 읽기 이름(보이는 글자를 앞에 둔다, WCAG 2.5.3) |
| `player_zoom` | 화면 확대 | 신규. 더보기 시트 줄 이름 |

함께 고칠 기존 문구(2단계 #126 출시 때):

| 키 | 지금 | 바뀜 |
|---|---|---|
| `guide_what_title` | 영상 하나만 받습니다 | 영상과 그 자막만 받습니다 |
| `guide_what_body` | …토렌트 안에서 가장 큰 영상 파일 하나만 받고 나머지 파일은 받지 않습니다. | …토렌트 안에서 가장 큰 영상 파일 하나와 그 영상의 자막 파일만 받고, 나머지 파일은 받지 않습니다. |

planning 문서 5절대로 패치 노트·스토어 설명의 "영상 하나만"도 같이 고쳐야 한다(launch·PM).

문구 점검(`design:ux-copy` 기준): 버튼·행은 명사 하나(끄기, 트랙 이름), 알림은 결과만("자막: 한국어"), 오류는 두 문장(무슨 일 + 할 일, 할 일 "다시 고르기"는 시트에서 바로 가능). 용어는 "받다"(error-recovery 1-6). "오디오" 대신 "음성"(PM 결정 용어).

## 5. 컴포넌트·토큰

| 이름 | 종류 | 위치 | 설명 |
|---|---|---|---|
| `RadioListItem` | 신규 공용 | `ui/components/` | `selectable(role = Role.RadioButton)` 행 하나 전체가 터치 영역. 인자: `label`, `selected`, `enabled`, `tag: String?`, `onClick`. 묶음은 호출 쪽이 `Modifier.selectableGroup()`. 자막 시트에서 처음 쓰고, 재생 속도·화면 비율(승인 시)이 칩 줄로 가면 이 행은 자막 전용이 되지만 링크 시트 등 이후 선택 목록에 재사용한다 |
| `SectionHeader` | 신규 공용 | `ui/components/` | 시트·설정 화면의 묶음 제목. `titleSmall` `onSurfaceVariant` + `heading()`. 설정 화면(`settings-wifi.md`)과 같이 쓴다 |
| `PlayerNoticeChip` | 신규, 재생 화면 전용 | `ui/player/PlayerOverlay.kt` | 3절. 이어보기 칩(승인 시)과 같이 쓴다 |
| `ic_subtitles.xml` | 신규 벡터 | `res/drawable/` | Material Symbols "subtitles" 24dp 하나. 코어 아이콘 묶음에 없다(`material-icons-core`만 의존) |
| `PlayerTopBar` 인자 | 변경 | | `onZoomOut`·`onZoomIn` 제거(더보기 시트로), `onSubtitles`, `subtitleLabel: String?`(켜진 트랙 이름, 꺼짐 `null`) 추가, `zoomLabel`은 1.0×가 아닐 때만 칩. 콜백 시그니처 변경이라 program |
| `PlayerMoreSheet` 인자 | 변경 | | `zoomLabel`, `onZoomOut`, `onZoomReset`, `onZoomIn` 추가 |

- 새 색·간격·크기 토큰 없음. 쓰는 토큰: `PlaybackColors.OnVideo`·`Accent`·`Panel`, `Spacing.xs`·`sm`·`md`·`lg`·`screenGutter`, `Sizes.icon`·`iconSmall`, `Shapes.small`·`pill`, 표면 색 `surface`·`surfaceContainer`·`outline`·`onSurface`·`onSurfaceVariant`·`primaryLight`.

## 6. 상태별 모습

| 상태 | 모습 |
|---|---|
| 트랙 확인 전(재생기 준비 중) | 자막 버튼은 보이고 누를 수 있다. 시트에는 "트랙을 확인하는 중…"(`bodyMedium` `onSurfaceVariant`, 작은 `CircularProgressIndicator` `Sizes.iconSmall`) 한 줄. 트랙이 오면 그 자리에서 목록으로 바뀐다 |
| 자막이 하나도 없음 | 자막 묶음에 "자막 없음" 한 줄(누를 수 없음, 라디오 없음). 음성이 1개면 시트 내용은 이 한 줄뿐 — 그래도 버튼은 숨기지 않는다(버튼 위치가 영상마다 바뀌지 않게) |
| 음성 트랙 1개 | 음성 묶음과 구분선을 숨긴다 |
| 외부 자막 받는 중 | 행 `enabled = false`(글자 `onSurface` 38%가 아니라 `onSurfaceVariant` — 흐린 글자도 AA를 지킨다), 태그 "받는 중". 다 받으면 바로 고를 수 있게 바뀐다 |
| 외부 자막 받기 실패·읽기 실패 | 행 `enabled = false`, 태그 "받지 못함" |
| 선택 실패 | 고른 표시를 이전 값으로 되돌리고 스낵바 `error_track_select` |
| 자막 자동 표시 끔 | 처음 열 때 "끄기"가 선택. 알림 칩 없음 |
| 마지막 선택 기억 | 같은 영상을 다시 열면 지난 선택이 선택된 채로 열린다(기억 범위는 planning "결정 필요" — 6-1) |
| 세로 | 시트 반 펼침에서 자막 4~5줄이 보이고 나머지는 스크롤. 위쪽에 영상 일부가 보인다 |
| 가로 | 시트 폭 최대 600dp 가운데. 높이가 낮아 반 펼침이 거의 전체가 된다 — 자막이 위쪽 영상에 보이게 `skipPartiallyExpanded = false` 유지, 내용은 스크롤 |
| 글자 200% | 행 글자가 2줄까지 감기고 넘치면 말줄임. 태그는 글자 오른쪽에 붙어 줄어들지 않는다(`weight(1f)` 글자 + 태그). 상단 바: 아이콘 크기는 그대로라 파일 이름 칸만 좁아진다(1줄 말줄임). 배율 칩은 통계 칩 줄에서 `FlowRow`로 다음 줄로 내려간다 |
| 라이트·다크 | 다크 하나(renewal-plan 1절). 재생 화면은 원래 영상 위 고정 색 |

### 6-1. PM·planning 확인 필요

1. **외부 자막이 하나뿐이고 언어를 모를 때**(예: `movie.srt`): planning 2-5는 "하나뿐이면 자동으로 켠다", PM 결정은 "한국어면 자동". design 권장: 파일 이름에서 언어를 못 찾은 외부 자막이 하나뿐이면 한국어로 보고 켠다(국내 토렌트의 단일 `.smi`·`.srt`는 대부분 한국어). 알림은 "자막: movie.srt(파일)".
2. **선택 기억 범위**: 영상별(재생 목록 항목에 저장, `MagnetHistory` 버전 증가) vs 선호 언어 하나. 시안은 영상별을 전제로 그렸다. 어느 쪽이든 화면은 같다.

## 7. 접근성

| 요소 | 읽기(TalkBack) | 기준 |
|---|---|---|
| 상단 자막 버튼 | "자막, 꺼짐, 버튼" / "자막, 켜짐: 한국어, 버튼" | 4.1.2 상태를 이름에 넣는다. 색(Accent)만으로 켜짐을 알리지 않는다(1.4.1) |
| 묶음 제목 | "자막, 제목" / "음성, 제목" | 1.3.1 `heading()` |
| 자막 행 | "한국어, 라디오 버튼, 선택됨, 3개 중 2번째" | `selectable` + `selectableGroup`이 역할·상태·위치를 준다 |
| 외부 자막 행 | "Movie.ko.srt, 자막 파일, 라디오 버튼, 선택 안 됨" | 태그 "파일"은 따로 읽지 않고(`clearAndSetSemantics`) 행 이름을 `cd_subtitle_file`로 준다 |
| 받는 중 행 | "Movie.en.smi, 받는 중, 사용 중지됨" | |
| 알림 칩 | 나타날 때 한 번 "자막: 한국어(파일)" | `liveRegion = Polite`. 2.5초 뒤 사라져도 내용은 상단 버튼 이름으로 다시 확인 가능(2.2.1 — 정보가 시간 제한으로만 주어지지 않음) |
| 배율 칩 | "확대 배율 되돌리기, 버튼"(기존 `cd_zoom_reset`) + 값 "1.5×" | |

대비(계산값, sRGB):

| 앞 | 바탕 | 대비 | 기준 | 결과 |
|---|---|---|---|---|
| `onSurface` 행 글자 | `surface` | 15.0 | 4.5 | 통과 |
| `onSurfaceVariant` 태그·묶음 제목 | `surfaceContainer` / `surface` | 7.4 / 8.3 | 4.5 | 통과 |
| `primaryLight` 선택 라디오 | `surface` | 6.4 | 3(비텍스트) | 통과 |
| `onSurfaceVariant` 선택 안 한 라디오 | `surface` | 8.3 | 3 | 통과 |
| `OnVideo` 알림 글자 | `Panel` 위 흰 영상(최악) | 14.7 | 4.5 | 통과 |
| `primaryLight` 통계 칩 글자(지금) | `Chip`(70%) 위 흰 영상(최악) | 2.4 | 4.5 | **미달** — 배율 칩은 `Panel`+`OnVideo`로. 통계 칩도 `Panel`로 바꾸기를 권한다(검은 영상 위에서는 6.9로 통과) |
| `Accent` 켜진 자막 아이콘 | `ScrimTop`(background 85%) 위 흰 영상(최악) | 4.7 | 3(비텍스트) | 통과(가림막 맨 위 기준) |

- 터치 영역: 행 56dp, 아이콘 버튼 48dp, 배율 칩 48dp(보이는 크기보다 넓게).
- 키보드·D-pad: 행은 위아래 이동, 선택은 Enter. 시트는 뒤로로 닫힌다.

## 8. 확인하지 못한 것

- 에뮬레이터 확인 없음(문서 시안). 줄 수·잘림은 360dp·글자 100% 추정.
- VLC가 실제로 주는 트랙 설명 형식(언어 코드가 오는지, "Track 1 - [Korean]" 같은 틀인지)은 MKV 표본으로 확인하지 않았다. 이름 규칙은 program이 표본으로 맞춰 본 뒤 6절 문구가 어긋나면 design에 되돌린다.
- `.smi` 자막을 LibVLC가 그대로 읽는지(인코딩 EUC-KR 포함)는 program 확인 사항.
- `ScrimTop` 위 `Accent` 아이콘 대비는 가림막 맨 위 지점만 계산했다.
