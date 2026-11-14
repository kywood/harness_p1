# 출시 전 점검: v.2026.11.14.001 (versionCode 30)

- 점검일: 2026-11-14, program (#176)
- 기준: `main` 667c06e + 버전 올림 커밋(브랜치 `codex/176-release-build`)
- 이번에 한 일: `:app:nextVersion`, 버전 검사, 단위 테스트, lint, `:app:packageReleaseFiles`, 산출물 확인. Play Console 업로드·제출과 실기기 설치는 하지 않았다.
- 직전 릴리스 산출물은 v.2026.11.12.003(28)이다. v.2026.11.13.001(29)은 휴대폰 확인용 빌드였고 릴리스 산출물 폴더에는 없다.

판정: **통과** / **확인 필요**(사용자·Play Console에서 확인) / **미실행**

## 산출물

| 파일 | 크기(바이트) | SHA-256(파일) |
|---|---|---|
| `PiecePlay-v.2026.11.14.001-30.aab` | 129,840,271 | `fdfc4a61…0336b0f` |
| `PiecePlay-v.2026.11.14.001-30.apk` | 287,318,322 | `6cb6e58d…2ca6f6c` |

위치: `program/android/app/build/outputs/pieceplay-release/` (Git 무시 대상). Play Console에는 AAB를 올린다.

## 체크리스트

| # | 항목 | 판정 | 근거 |
|---|---|---|---|
| 1 | 버전 | 통과 | `version.properties` = 2026-11-14, revision 1, code 30. `:app:verifyVersioning` 통과. APK(`aapt2 dump badging`)와 AAB 매니페스트 모두 `com.gamenuri.pieceplay`, versionCode 30, versionName `v.2026.11.14.001`, targetSdk 37. Play Console에 이미 올라간 versionCode가 30 미만인지는 Console에서 확인 |
| 2 | 서명 | 통과 | APK(`apksigner verify --print-certs`)와 AAB(`keytool -printcert -jarfile`, `jarsigner -verify`) 서명 인증서 SHA-256이 모두 `1A:9C:49:12:1B:63:A1:7E:96:3C:CF:DD:39:D9:6F:09:34:E2:2A:49:D8:FD:DA:58:95:E4:39:02:C2:A6:1B:8F`로 업로드 키(`1A:9C:49:12…`)와 같다 |
| 3 | 빌드 | 통과 | `:app:packageReleaseFiles` 성공. 매니페스트에 `debuggable` 없음(릴리스). AAB에 arm64-v8a·armeabi-v7a·x86·x86_64 네이티브 라이브러리 포함 |
| 4 | 단위 테스트 | 통과 | `:app:testDebugUnitTest` 594개, 실패 0, 건너뜀 0 |
| 5 | lint | 통과 | `:app:lintDebug` 성공. 오류 0, 경고 12 |
| 6 | 계측 테스트 | 통과(PM 확인 값) | 51/51 통과. PM이 main 667c06e 직전 커밋 기준, emulator-5554 en-US에서 확인한 값이다. 이번 작업에서는 에뮬레이터를 쓰지 않아 다시 돌리지 않았다 |
| 7 | 광고 | 통과(코드 기준) | `meta/AdSpec.kt` `AdUnit.id(debug)`: 릴리스는 `ca-app-pub-2418522550325553/…` 실제 단위, Google 테스트 단위(`ca-app-pub-3940256099942544/…`)는 `BuildConfig.DEBUG`일 때만. 매니페스트 앱 ID도 실제 값. 릴리스 빌드 실기기 광고 확인은 하지 않음 |
| 8 | 로그 | 통과(코드 기준) | 개발용 `Log.d`와 `PlaybackStartTrace`, `StrictMode`는 `BuildConfig.DEBUG`로 막혀 있다. 릴리스에 남는 로그는 오류(`EngineFailureLog.describe`가 릴리스에서는 내용을 줄임)와 `MetadataFetch`의 경과 시간·조각 수 `Log.i` 두 줄이며 마그넷·파일 이름은 없다 |
| 9 | 언어 리소스 | 통과 | APK `aapt2 dump resources`에서 앱 문자열이 기본(영어)과 `ko` 두 벌로 들어 있다(예: `action_stream_play` = "Play now" / "바로 재생"). 그 밖의 로캘은 라이브러리(Google Play 서비스 등) 문자열이다 |
| 10 | 등록정보·양식 | 확인 필요 | 이번 작업 범위 밖. 등록정보 문구·스크린샷·데이터 보안 양식 대조는 launch가 한다 |

## 포함된 변경 요약

직전 릴리스 산출물(28) 이후. 자세한 내용은 `program/android/PATCH_NOTES.md`의 v.2026.11.14.001, v.2026.11.13.001 절.

- 영어 UI: 기기 언어가 한국어가 아니면 앱 화면이 영어로 나온다. "자막 자동 표시"는 고른 적이 없으면 기기 언어 자막을 먼저 켠다.
- 자막: 재생 화면 자막 버튼과 시트(내장 자막·음성 트랙, 토렌트 안 자막 파일), 자동 표시, EUC-KR 자막 자동 변환, 시즌 팩 회차별 자막.
- 링크 열기: 다른 앱에서 마그넷·.torrent 링크를 누르거나 공유하면 확인 시트([바로 재생] [받아 두기] [취소])가 뜬다. 열 수 없는 링크는 이유를 안내한다.
- 설정 화면과 Wi-Fi 전용: 모바일 데이터에서는 받기를 멈추고 "Wi-Fi 대기"로 표시, 재생은 "이번만 모바일 데이터로" 확인 후 허용.
- 재생 목록·입력: 대기열 항목 바로 표시, 링크 추가 창 [붙여넣기], 키보드 위로 올라오는 [바로 재생].
- 받는 순서: 받아 두는 영상을 앞부분부터 받고, 인덱스가 끝에 있는 영상은 끝부분도 미리 받는다.
- 재생 화면: 화면 확대를 더보기 메뉴로 옮김, 속도·피어 칩 글자색.

## 알려진 위험

| 위험 | 내용 | 상태 |
|---|---|---|
| 간헐 네이티브 크래시 (#169) | 계측 전체 실행 중 libtorrent `TorrentInfo.bdecode`에서 SIGSEGV가 간헐적으로 났다. 원인 미확정, 미해결 | 열림. 출시 후 Play Console 비정상 종료율을 본다 |
| R8 최적화 꺼짐 | 릴리스가 `optimization { enable = false }`라 축소·난독화가 없다. APK 287 MB, AAB 130 MB(4개 ABI 포함, 기기에는 ABI별로 나뉘어 내려간다) | 그대로 출시 |
| LibVLC 라이선스 (#101) | LibVLC의 GPL 구성요소 제거·LGPL 빌드 교체가 끝나지 않았다. 앱 안 오픈소스 라이선스 화면은 있다 | 열림 |
| 릴리스 빌드 실기기 미확인 | 이번 산출물은 기기에 설치해 보지 않았다(지시상 실기기·에뮬레이터 미사용). 계측 51/51은 디버그 빌드 기준이다 | 업로드 전 내부 테스트 트랙에서 확인 권장 |

## 출시 전 사용자 할 일

1. [ ] 노션 개인정보처리방침 영어판 게시(영어 UI·해외 국가 추가에 맞춤)
2. [ ] Play Console에 `PiecePlay-v.2026.11.14.001-30.aab` 업로드(이미 올라간 versionCode가 30 미만인지 확인), 출시 노트 입력
3. [ ] Play Console 국가 추가(대상 국가는 PM 결정)
