# 전체 컨벤션

모든 팀에 적용한다. 팀별 지침은 각 팀 디렉터리의 `CLAUDE.md`(PM은 루트 `PM.md`)에 있다.

## 팀 구성

| 팀 | 형태 | 디렉터리 | 소유 | 고치지 않는 것 |
|---|---|---|---|---|
| PM | 메인 세션 (루트에서 연다) | 루트 | `PM.md`, GitHub 이슈, 우선순위·출시 결정, `program/android/PATCH_NOTES.md` | 코드, 리소스 |
| launch | 별도 세션 (`launch/`에서 연다) | `launch/` | Play Console 양식, 출시 체크리스트, 스토어 자료 정책·규격 확인과 업로드, 심사·출시 후 대응 | 코드, 리소스, 스토어 자료 제작 |
| marketing | 별도 세션 (`marketing/`에서 연다) | `marketing/`, 스토어 자료는 `launch/store-assets/`·`launch/listing/` | 스토어 스크린샷·그래픽 이미지·홍보 영상·등록정보 문구 제작, ASO, 시장·경쟁 분석, 리뷰·평점 관리, 지표 보고, 홍보안 | 코드, 리소스, 승인 없는 외부 게시·유료 집행 |
| planning | 별도 세션 (`planning/`에서 연다) | `planning/` | 요구사항, 화면 정의, 사용자 흐름, 스펙 | 코드, 리소스 |
| design | 서브에이전트 `design` | `design/` | `ui/theme/`, `res/`, 컴포저블의 시각 부분, `design/`의 토큰·시안 문서 | `engine/`, ViewModel, `StreamUiState`, `*Policy`·`*Plan` 로직 |
| program | 서브에이전트 `program` | `program/` | `engine/`, ViewModel, 상태·정책 객체, 테스트, Gradle·릴리스 | 색·간격·문구를 임의로 새로 만드는 일 |
| reviewer | 서브에이전트 `reviewer` | `.claude/agents/reviewer.md` | 병합 전 PR 검토, 판정과 발견 사항 보고 | 코드 수정, PR 승인·병합 |

팀 디렉터리에는 지침과 그 팀의 문서 산출물을 둔다. 앱 코드는 팀 디렉터리가 아니라 `program/android/`에 있다.

화면 파일(`ui/*Screen.kt`, `MainActivity.kt`)은 design과 program이 함께 고친다. 시각 변경은 design 지침, 상태·콜백 변경은 program 지침을 따르고, 한 변경이 둘 다 건드리면 program이 테스트로 최종 확인한다.

## 작업 순서

planning(무엇을·왜, 화면 정의) → PM(우선순위 결정, 이슈로 배정) → design(토큰·시안·화면) → program(구현·검증) → PM(확인, 출시 결정) → launch(출시 준비·진행·출시 후 대응). 심사 거절이나 출시 후 문제는 launch → PM → program/design 순서로 되돌아간다. 앞 단계 산출물 없이 다음 단계를 시작하지 않는다. 넘길 때는 산출물 경로와 완료 조건을 적는다.

## 절차

- GitHub 이슈·브랜치·커밋·푸시 절차는 `program/android/AGENTS.md`를 따른다.
- 사용자에게 보이는 변경은 `program/android/PATCH_NOTES.md`에 버전별로 기록한다.
- 버전은 `:app:nextVersion`으로만 올린다. `version.properties`를 손으로 고치지 않는다.
- 검증하지 못한 항목은 숨기지 않고 완료 보고에 적는다.

## 보안·정책

- 마그넷과 재생 기록은 평문으로 저장하거나 로그에 남기지 않는다.
- 외부 의존성은 사용자 승인 없이 추가하지 않는다.
- 테스터 이메일 등 개인정보는 `launch/private/`에만 두고 커밋·문서·이슈에 옮겨 적지 않는다.
- 광고는 메인과 재생 목록에만 배너, 재생 종료·완료 시에만 전면 광고. 재생·준비·버퍼링·저장 파일 관리 화면에는 넣지 않는다.

## 용어

| 사용자에게 보이는 이름 | 영어 화면 이름 | 코드 |
|---|---|---|
| 메인 | Home | `MagnetLinkInputScreen` (`ui/screens/MainScreen.kt`) |
| 재생 목록 | Playlist | `MagnetHistoryScreen` |
| 저장 파일 관리 | Stored files | `StoredFilesScreen` |
| 준비 화면 | (화면 이름 없음) | `PreparingScreen` (`LoadingScreen`, `BufferingScreen`, `ui/screens/PreparingScreen.kt`) |
| 재생 화면 | (화면 이름 없음) | `VlcPlayerScreen` |
| 피스 맵 | Download pieces | `PieceMap` |
| 설정 | Settings | `SettingsScreen` (`ui/screens/SettingsScreen.kt`, `AppScreen.Settings`) |
| Wi-Fi 전용 | Wi-Fi only | `AppSettings.wifiOnly` (설정 항목) |
| Wi-Fi 대기 | Waiting for Wi-Fi | `DownloadPhase.WAITING_FOR_WIFI`, `PlaylistItemStatus.WAITING_FOR_WIFI` (재생 목록 상태 칩) |
| 받는 순서 | Download order | `download_order_title`, `DownloadOrderPreference` (`AppSettings.downloadOrder`, `SavedMagnet.downloadOrder`) |
| 앞부분부터 | From the start | `DownloadOrderPreference.FROM_START` (`download_order_from_start`) |
| 빠르게 받기 | Fastest | `DownloadOrderPreference.FASTEST` (`download_order_fastest`) |
| 바로 재생 | Play now | `action_stream_play` |
| 받아 두기 | Save for later | `action_save_for_later` |
| 이어서 재생 | Resume | `action_resume_play` |
| 광고 개인정보 설정 | Ad privacy settings | `action_ad_privacy` |
| 이용 안내 | Usage guide | `action_usage_guide` |
| 오픈소스 라이선스 | Open source licenses | `action_open_source_licenses` |
| 개인정보처리방침 | Privacy policy | `action_privacy_policy` |

문서는 한국어로 쓴다. 앱 화면 문구는 기본 `res/values/strings.xml`이 영어, `res/values-ko/strings.xml`이 한국어다(기기 언어가 한국어면 한국어). 새 문구는 두 파일에 같은 키로 넣고, 영어 화면 이름은 위 표를 따른다.
