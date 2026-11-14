# 출시 전 점검: v.2026.11.14.002 (versionCode 31)

- 점검일: 2026-11-14, program (#192)
- 기준: `main` d37e3f7 + 버전 올림 커밋(브랜치 `codex/192-release-build`)
- 이번에 한 일: `:app:nextVersion`, 버전 검사, 단위 테스트, lint, `:app:packageReleaseFiles`, 산출물 확인, 같은 버전 디버그 빌드를 휴대폰(R59N300DBHH)에 설치. Play Console 업로드·제출은 하지 않았다.
- 직전 릴리스 산출물은 v.2026.11.14.001(30)이다.

판정: **통과** / **확인 필요**(사용자·Play Console에서 확인) / **미실행**

## 산출물

| 파일 | 크기(바이트) | SHA-256(파일) |
|---|---|---|
| `PiecePlay-v.2026.11.14.002-31.aab` | 129,851,088 | `0F663701…8366F034` |
| `PiecePlay-v.2026.11.14.002-31.apk` | 287,337,638 | `80EB0E36…EAC7` |

위치: `program/android/app/build/outputs/pieceplay-release/` (Git 무시 대상). Play Console에는 AAB를 올린다.

## 체크리스트

| # | 항목 | 판정 | 근거 |
|---|---|---|---|
| 1 | 버전 | 통과 | `version.properties` = 2026-11-14, revision 2, code 31. `:app:verifyVersioning` 통과. APK(`aapt2 dump badging`) `com.gamenuri.pieceplay`, versionCode 31, versionName `v.2026.11.14.002`, targetSdk 37. Play Console에 이미 올라간 versionCode가 31 미만인지는 Console에서 확인 |
| 2 | 서명 | 통과 | APK(`apksigner verify --print-certs`)와 AAB(`keytool -printcert -jarfile`, `jarsigner -verify`: jar verified) 서명 인증서 SHA-256이 모두 `1A:9C:49:12:1B:63:A1:7E:96:3C:CF:DD:39:D9:6F:09:34:E2:2A:49:D8:FD:DA:58:95:E4:39:02:C2:A6:1B:8F`로 업로드 키(`1A:9C:49:12…`)와 같다 |
| 3 | 빌드 | 통과 | `:app:packageReleaseFiles` 성공. APK 매니페스트에 `debuggable` 없음(릴리스) |
| 4 | 단위 테스트 | 통과 | `:app:testDebugUnitTest` 643개, 실패 0, 건너뜀 0 |
| 5 | lint | 통과 | `:app:lintDebug` 성공. 오류 0, 경고 12 |
| 6 | 계측 테스트 | 통과(PM 확인 값, 일부) | 받는 순서 관련 8건과 엔진 9개 클래스 통과(PM 확인, emulator-5554). 전체 51건 실행은 30 기준 값이며 이번 버전에서는 다시 돌리지 않았다. 이번 작업에서 에뮬레이터는 쓰지 않았다 |
| 7 | 언어 리소스 | 통과 | APK `aapt2 dump resources`에서 앱 문자열이 기본(영어)과 `ko` 두 벌로 들어 있다(예: `action_stream_play` = "Play now" / "바로 재생") |
| 8 | 광고·로그 | 통과(코드 기준) | 30과 같은 코드 경로(`BuildConfig.DEBUG`로 테스트/실제 단위 구분). 릴리스 빌드 실기기 광고 확인은 하지 않음 |
| 9 | 등록정보·양식 | 확인 필요 | 이번 작업 범위 밖. launch가 대조 |

## 30 대비 추가된 변경

자세한 내용은 `program/android/PATCH_NOTES.md`의 v.2026.11.14.002 절(이 PR이 "다음 배포 준비" 제목을 바꾼 것).

- 받는 순서 선택: 설정에서 "앞부분부터"(기본)와 "빠르게 받기" 중 고르고, 재생 목록 카드에서 영상마다 따로 정할 수 있다. 받는 중에도 바로 적용된다.
- 메인 화면 로고·이름·한 줄 설명 가운데 정렬.
- 재생 수정(PR #189, 지시서 기준 3건): 받던 항목을 멈춘 직후 다른 항목 재생 시 "Couldn't process the torrent" 오류 감소, 재생 막대가 영상 길이를 알기 전에도 받은 구간만 표시. 패치노트에는 이 2건이 적혀 있다. 나머지 1건은 PM이 항목을 확인해 달라.

## 알려진 위험

| 위험 | 내용 | 상태 |
|---|---|---|
| 간헐 네이티브 크래시 (#169) | libtorrent `TorrentInfo.bdecode`에서 SIGSEGV가 간헐적으로 남. 원인 미확정 | 열림. 출시 후 Play Console 비정상 종료율 확인 |
| #186 후속 낮음 항목 | 스크린샷 촬영 중 본 동작 중 우선순위 낮은 항목이 남아 있다 | 열림(낮음) |
| R8 최적화 꺼짐 | 축소·난독화 없음. APK 287 MB, AAB 130 MB(4개 ABI 포함) | 그대로 출시 |
| LibVLC 라이선스 (#101) | GPL 구성요소 제거·LGPL 빌드 교체 미완 | 열림 |
| 릴리스 빌드 실기기 미확인 | 이번 산출물(릴리스)은 기기에 설치해 보지 않았다. 휴대폰에는 같은 버전의 디버그 빌드만 설치했다 | 업로드 전 내부 테스트 트랙에서 확인 권장 |

## 출시 전 사용자 할 일

1. [ ] 노션 개인정보처리방침 영어판 게시
2. [ ] Play Console에 `PiecePlay-v.2026.11.14.002-31.aab` 업로드(이미 올라간 versionCode가 31 미만인지 확인), 출시 노트 입력
3. [ ] 등록정보·스크린샷 갱신
4. [ ] 국가 추가는 심사 통과 뒤(대상 국가는 PM 결정)
