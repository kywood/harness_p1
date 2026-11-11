# PiecePlay 디자인 리뉴얼 계획

이 문서가 리뉴얼의 기준이다. 토큰 값이 바뀌면 이 문서를 먼저 고친다. 규칙은 `design/CLAUDE.md`, 공통 규칙은 `CONVENTIONS.md`를 따른다.

## 1. 방향

- **브랜드**: 앱 아이콘의 짙은 남색 배경과 전기 파랑에서 민트로 이어지는 네온 그라데이션. "조각을 받으면서 바로 재생"을 빛이 흘러 들어오는 모습으로 표현한다.
- **다크 테마 하나**: 영상 앱이고 아이콘이 어두운 배경 기준이라 다크 테마만 제공한다. 기기 배경화면 색을 따르는 `dynamicColor`는 끈다(브랜드 색이 바뀌지 않게).
- **그라데이션은 아껴 쓴다**: 주요 버튼, 진행 막대의 받은 부분, 완료된 조각에만 쓴다. 나머지는 단색 표면. 흰 글자가 올라가는 주요 버튼은 대비를 위해 더 어두운 `brandButtonGradient`를 쓴다.
- **문구는 짧게, 한국어로**: 버튼은 동사 2~4글자(바로 재생, 다운로드, 중지, 삭제).

## 2. 색 토큰

대비는 WCAG 2.1 AA(본문 4.5:1) 이상으로 확인했다.

| 토큰 | 값 | 용도 | 대비 |
|---|---|---|---|
| `background` | `#0A0F1E` | 화면 배경 | |
| `surface` | `#111A2E` | 카드·시트·상단 바 | |
| `surfaceContainer` | `#18233D` | 입력창·칩·선택된 행 | |
| `outline` | `#2A3757` | 구분선·입력창 테두리 | |
| `onBackground` / `onSurface` | `#E8EEFF` | 본문 | 배경 16.5, 표면 15.0 |
| `onSurfaceVariant` | `#A7B3D1` | 보조 글자 | 표면 8.3, 컨테이너 7.4 |
| `primary` | `#2F66F0` | 주요 버튼 단색, 선택 상태 | 흰 글자 4.9 |
| `onPrimary` | `#FFFFFF` | 주요 버튼 글자 | |
| `primaryLight` | `#6E9BFF` | 어두운 표면 위 링크·아이콘 | 표면 6.4 |
| `accent`(secondary) | `#2EE6B0` | 완료·다운로드 완료·강조 | 배경 11.9 |
| `onAccent` | `#00281C` | 민트 위 글자 | 9.9 |
| `error` | `#FF6B7A` | 오류 | 표면 6.3 |
| `brandGradient` | `#2F66F0 → #2EE6B0` | 진행 막대의 받은 부분, 완료 조각 | 가운데가 밝아 흰 글자 1.6~2.8로 글자 배경에 쓰지 않음 |
| `brandButtonGradient` | `#2456D2 → #0E7A62` | 주요 버튼(`PrimaryButton`) 배경(#47) | 흰 글자 최저 5.28(11개 지점 sRGB 직선 혼합, `ThemeTokensTest`) |

재생 화면 전용 색(`PlaybackColors`)도 위 토큰에서 만든다: 재생한 구간 `brandGradient`, 받은 구간 `primaryLight` 40%, 트랙 `outline`, 패널 `surface` 94%.

조각 맵 색: 완료 `accent`, 받는 중 `primaryLight`(진행률에 따라 35·65·100%), 없음 `outline`.

## 3. 글자

시스템 글꼴(Roboto, 한글은 Noto Sans CJK)을 그대로 쓴다. 글꼴 파일을 넣으면 APK가 커지고 라이선스 검토가 필요해서 제외한다. Material 3 타입 스케일을 쓰고 굵기만 조정한다.

| 역할 | 스타일 |
|---|---|
| 앱 이름 | `headlineMedium`, Bold |
| 화면 제목 | `titleLarge`, SemiBold |
| 카드 제목·파일 이름 | `titleMedium`, Medium, 최대 2줄 |
| 본문 | `bodyMedium` |
| 보조·수치(속도, 피어, 크기) | `labelMedium`, 고정폭 숫자(`tnum`) |

## 4. 간격·모서리·크기

| 토큰 | 값 |
|---|---|
| 간격 `xs`·`sm`·`md`·`lg`·`xl`·`xxl` | 4·8·12·16·24·32dp |
| 화면 가로 여백 | 16dp |
| 모서리 `small`·`medium`·`large` | 8·16·24dp, 주요 버튼은 완전한 둥근 모서리 |
| 터치 영역 | 최소 48dp |
| 아이콘 | 기본 24dp, 재생 화면 큰 버튼 48dp |

`ui/theme/`에 `Spacing`, `Shapes`, `BrandColors` 객체로 정의하고 화면은 이 값만 쓴다.

코드 이름(#45에서 추가):

| 객체 | 항목 |
|---|---|
| `BrandColors` (`Color.kt`) | 위 2절 표의 색 + `brandGradient`, `brandButtonGradient`(`ButtonGradientStart`·`ButtonGradientEnd`) |
| `Spacing` (`Dimens.kt`) | `xs`·`sm`·`md`·`lg`·`xl`·`xxl`, `screenGutter` |
| `Shapes` (`Dimens.kt`) | `small`·`medium`·`large`·`pill` |
| `Sizes` (`Dimens.kt`) | `touchTarget` 48, `icon` 24, `iconSmall` 18(글자 버튼 안 아이콘, #47), `iconLarge` 48, `primaryButtonHeight` 52, `progressBarHeight` 8, `logo` 96, `contentMaxWidth` 600(가로·큰 화면 본문 최대 폭, #46), 재생 화면(#48): `iconMedium` 32(회전·10초 이동 아이콘), `playerControl` 56·`playerControlLarge` 72(가운데 버튼 지름), `seekTrackHeight` 6, `seekThumb` 16 |
| `PieceMapColors` | 조각 맵 색 규칙(`of(PieceCell)`) |
| `PlaybackColors` | 재생 화면 색. `Played`(그라데이션), `Downloaded`(primaryLight 40%), `Track`, `Panel`, `SeekPanel`, #48에서 `Video`(검정 바탕), `OnVideo`(흰색), `OnVideoMuted`, `ControlButton`(흰색 16%), `Chip`(surface 70%), `Error`, `ScrimTop`·`ScrimBottom`(background 85% ↔ 투명 그라데이션) |

## 5. 공용 컴포넌트 (`ui/components/`)

| 컴포넌트 | 설명 |
|---|---|
| `PrimaryButton` | `brandButtonGradient` 배경, 완전한 둥근 모서리, 높이 52dp |
| `SecondaryButton` | `surfaceContainer` 배경 |
| `AppTopBar` | 뒤로 버튼 + 제목 + 오른쪽 동작, `surface` 배경 |
| `SectionCard` | `surface`, 모서리 `medium`, 안쪽 여백 16dp |
| `StatusChip` | 다운로드 중·대기·완료·실패 상태 표시(색 + 글자) |
| `ProgressBar` | 받은 부분 그라데이션, 나머지 `outline` |
| `PieceMap` | 위 조각 색 규칙 |
| `EmptyState` | 아이콘 + 한 줄 설명 + 동작 버튼 |
| `ConfirmDialog` | 삭제 등 되돌릴 수 없는 동작 확인. 링크 추가처럼 입력이 있는 대화상자도 이 모양을 쓴다(`content` 칸, #47) |
| `TextAction` | 어두운 표면 위 글자 버튼. 기본 `primaryLight`, 삭제는 `error`, 덜 중요한 동작은 `onSurfaceVariant` (#47) |
| `LinkTextField` | 마그넷·.torrent 링크 입력창. 메인 입력 카드와 링크 추가 대화상자가 같이 쓴다 (#47) |

## 6. 화면별 변경

| 화면 | 지금 | 리뉴얼 |
|---|---|---|
| 메인 | 로고, 입력창, "바로 재생", 글자 버튼 두 개 | 로고 + 앱 이름 + 한 줄 소개("받으면서 바로 재생"). 입력 카드(붙여넣기 버튼 포함). 주요 버튼 "바로 재생". 아래에 재생 목록·저장 파일 카드 두 개. 버전과 광고 개인정보 설정은 맨 아래 작게 |
| 준비(정보 가져오기·버퍼링) | 원형 로딩, 텍스트, 버튼 | 파일 이름 + 단계 표시(정보 → 연결 → 버퍼링). 진행 막대와 속도·피어 칩. 조각 맵 카드. 세부 정보는 접기. 하단에 메인·재생 목록 버튼 |
| 재생 목록 | 카드 목록, 옵션, 일괄 선택 | `AppTopBar`. 상단에 "다운로드 중 n개 · 대기 n개" 요약과 정렬·필터 칩. 항목 카드: 제목, 크기, 진행 막대, 상태 칩, 동작 아이콘(재생·다운로드·중지). 일괄 선택 모드는 상단 바가 선택 동작 바로 바뀜. 빈 목록은 `EmptyState` |
| 저장 파일 관리 | 목록, 확장자 필터 | `AppTopBar`. 전체 용량 요약. 확장자 필터 칩. 파일 행: 형식 아이콘, 이름, 크기, 삭제. 삭제는 `ConfirmDialog` |
| 재생 | VLC 화면 위 컨트롤 | 상·하단 그라데이션 가림막. 재생 막대는 재생 구간 그라데이션 + 받은 구간 표시. 아이콘 크기 통일. 더보기 메뉴는 하단 시트 |
| 대화상자 | 기본 대화상자 | 링크 추가·삭제 확인을 `ConfirmDialog` 스타일로 통일 |

#47에서 정한 세부:

- 재생 목록 일괄 선택은 상단 바를 바꾸지 않고, 목록 위 선택 줄이 `surfaceContainer` 막대로 바뀌며 전체 선택 · 삭제 · 다운로드/중지를 한 줄에 둔다(화면 테스트가 세 동작이 같은 줄인지 확인). 아무것도 고르지 않으면 "전체 선택"만 보인다.
- 항목 카드: 제목(2줄) → 상태 칩 + 받은 양·속도 → 진행 막대 → (자세히) 조각 맵, 받은 날짜, 마그넷 주소 한 줄(말줄임) → 이어보기 → [재생] [다운로드/중지/대기 취소] … [파일과 함께 삭제 아이콘].
- 상단 바 오른쪽 동작(링크 추가·간단히)과 "닫기"는 화면 테스트가 글자로 찾으므로 글자 버튼(`TextAction`, `primaryLight`)으로 둔다.
- 요약·칩·선택 줄도 목록과 함께 스크롤된다(가로 화면에서 목록이 가려지지 않게).
- 저장 파일 행은 파일 이름을 제목으로, 폴더 경로를 보조 줄로 보여 준다.

#48에서 정한 재생 화면 세부:

- 상·하단 가림막은 `ScrimTop`·`ScrimBottom` 그라데이션이고, 화면 전체 어둡게 하는 `ControlsScrim`(15%)은 그대로 둔다.
- 재생 막대(`ui/player/PlayerSeekBar`): 트랙 `Track` 위에 받은 조각을 `Downloaded`로 칠하고, 재생한 구간을 `brandGradient`로 덮는다. 손잡이는 흰색 16dp. 숨은 상태에서 밀어 이동할 때 나오는 막대(`SwipeSeekBar`)도 같은 막대를 쓴다.
- 아이콘: 상단 동작 24dp, 회전·10초 이동 32dp, 재생/일시 정지 48dp. "-10s"/"+10s"/"❚❚"/"⋮" 글자 대신 Material 아이콘(replay_10, forward_10, pause, more_vert, zoom_in/out)을 쓴다.
- 더보기는 `ModalBottomSheet`(링크 추가, 재생 위치 초기화 · 처음부터).
- 오류는 경고 아이콘(`error`) + 문구 + "돌아가기"(`SecondaryButton`).
- 버퍼링·이동 안내는 가운데 버튼 바로 위 패널로 옮겼다.

화면 테스트가 글자로 버튼을 찾으므로 다음 문구는 유지한다: 재생 목록, 저장 파일 관리, 전체 선택, 삭제, 다운로드, 중지, 재생, 메인, 닫기. 바꾸면 테스트도 같이 고친다.

## 7. 단계

| 단계 | 담당 | 내용 | 이슈 |
|---|---|---|---|
| 0 | program | 화면 구조 정리: `sealed AppScreen`, 화면별 파일 분리, ViewModel 상태 노출 | #37 |
| 1 | design | 디자인 시스템: 색·글자·간격·모서리 토큰, 다크 테마, `dynamicColor` 끄기, 공용 컴포넌트 | 새 이슈 |
| 2 | design | 메인·준비 화면 | 새 이슈 |
| 3 | design | 재생 목록·저장 파일 관리·대화상자 | 새 이슈 |
| 4 | design | 재생 화면 오버레이 | 새 이슈 |
| 5 | launch | 스토어 스크린샷·그래픽 교체(지금 스크린샷은 옛 이름 "Torrent Stream Player"와 옛 화면) | 새 이슈 |

각 단계는 하나의 PR로 만들고, 에뮬레이터 화면 캡처를 PR에 남긴다. 단계마다 휴대폰 빌드는 버전을 올려 설치한다.

## 8. 범위 밖

- 밝은 테마, 태블릿 전용 배치, 새 글꼴, 애니메이션 라이브러리
- 기능 변경(화면 흐름과 버튼 동작은 그대로)
