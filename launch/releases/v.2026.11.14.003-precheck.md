# 출시 전 점검: v.2026.11.14.003 (versionCode 32)

- 점검일: 2026-11-14, program (#192)
- 기준: `main` d37e3f7 + 버전 올림 커밋(브랜치 `codex/192-release-build`)
- 이번에 한 일: `:app:nextVersion`, 버전 검사, 단위 테스트, lint, `:app:packageReleaseFiles`, 산출물 확인, 같은 버전 디버그 빌드를 휴대폰(R59N300DBHH)에 설치. Play Console 업로드·제출은 하지 않았다.
- 직전 릴리스 산출물은 v.2026.11.14.002(31)이다.

판정: **통과** / **확인 필요**(사용자·Play Console에서 확인) / **미실행**

## 산출물

| 파일 | 크기(바이트) | SHA-256(파일) |
|---|---|---|
| `PiecePlay-v.2026.11.14.003-32.aab` | 129,857,768 | `15843EC0…270309` |
| `PiecePlay-v.2026.11.14.003-32.apk` | 287,337,638 | `DAFCF10B…37AF25` |

위치: `program/android/app/build/outputs/pieceplay-release/` (Git 무시 대상). Play Console에는 AAB를 올린다.

## 체크리스트

| # | 항목 | 판정 | 근거 |
|---|---|---|---|
| 1 | 버전 | 통과 | `version.properties` = 2026-11-14, revision 3, code 32. `:app:verifyVersioning` 통과. APK(`aapt2 dump badging`) `com.gamenuri.pieceplay`, versionCode 32, versionName `v.2026.11.14.003`, targetSdk 37. Play Console에 이미 올라간 versionCode가 32 미만인지는 Console에서 확인 |
| 2 | 서명 | 통과 | APK(`apksigner verify --print-certs`)와 AAB(`keytool -printcert -jarfile`, `jarsigner -verify`: jar verified) 서명 인증서 SHA-256이 모두 `1A:9C:49:12:1B:63:A1:7E:96:3C:CF:DD:39:D9:6F:09:34:E2:2A:49:D8:FD:DA:58:95:E4:39:02:C2:A6:1B:8F`로 업로드 키(`1A:9C:49:12…`)와 같다 |
| 3 | 빌드 | 통과 | `:app:packageReleaseFiles` 성공. APK 매니페스트에 `debuggable` 없음(릴리스) |
| 4 | 단위 테스트 | 통과 | `:app:testDebugUnitTest` 653개, 실패 0, 건너뜀 0 |
| 5 | lint | 통과 | `:app:lintDebug` 성공. 오류 0, 경고 12 |
| 6 | 계측 테스트 | 통과(PM 확인 값, 일부) | 31 기준 PM 확인 값(받는 순서 관련 8건과 엔진 9개 클래스 통과, emulator-5554). 전체 51건 실행은 30 기준이며 32에서는 계측을 다시 돌리지 않았다. 이번 작업에서 에뮬레이터는 쓰지 않았다 |
| 7 | 언어 리소스 | 통과 | APK `aapt2 dump resources`에서 앱 문자열이 기본(영어)과 `ko` 두 벌로 들어 있다(예: `action_stream_play` = "Play now" / "바로 재생") |
| 8 | 광고·로그 | 통과(코드 기준) | 30과 같은 코드 경로(`BuildConfig.DEBUG`로 테스트/실제 단위 구분). 릴리스 빌드 실기기 광고 확인은 하지 않음 |
| 9 | 등록정보·양식 | 확인 필요 | 이번 작업 범위 밖. launch가 대조 |

## 31 대비 추가된 변경

main c7b85e2(PR #196, 전체 코드 리뷰 수정) 포함. 자세한 내용은 `program/android/PATCH_NOTES.md`의 v.2026.11.14.003 절.

- 재생 목록 읽기 실패 보호: 파일을 잠시 읽지 못한 상태에서 재생·위치 저장이 일어나도 저장된 목록을 덮어쓰지 않는다. 다시 읽을 수 없는 파일은 지우지 않고 따로 보관한 뒤 새 목록을 시작한다.
- 여러 편 영화(`Movie.1`·`Movie.2`, `CD1`·`CD2`)에서 다른 편의 자막이 자동으로 켜지던 문제: 이름이 맞는 자막만 고른다.

31에 들어 있던 변경(받는 순서 선택, 메인 로고 가운데 정렬, 재생 수정 3건)은 31 점검 문서에 있다. 재생 수정 3건은 ① 멈춘 직후 다른 항목 재생 오류 ② 영상 길이를 알기 전 재생 막대 비활성 ③ 재생 막대는 다 받은 구간만 받은 색이다.

## 코드 리뷰 결과

`program/reviews/main-review-2026-11-14.md`: 출시 가능, 높음 0건. 남은 항목은 이슈 #194 댓글에 있다(아래 알려진 위험).
## 알려진 위험

| 위험 | 내용 | 상태 |
|---|---|---|
| 간헐 네이티브 크래시 (#169) | libtorrent `TorrentInfo.bdecode`에서 SIGSEGV가 간헐적으로 남. 원인 미확정 | 열림. 출시 후 Play Console 비정상 종료율 확인 |
| #186 후속 낮음 항목 | 스크린샷 촬영 중 본 동작 중 우선순위 낮은 항목이 남아 있다 | 열림(낮음) |
| 리뷰 후속 [중간] M-1 (#194) | 키는 정상인데 파일만 계속 못 읽는 경우(EIO, 일부 제조사 KeyMint 오류 — 미확인) 일시 실패로 분류돼 재생 목록 쓰기가 계속 막힐 수 있다 | 열림. 출시 필수 아님 |
| 리뷰 후속 [낮음] (#194) | `Movie.2019` + `Movie.2019.3D`/`v2`를 다른 편으로 오판, CD2 크기·Part 뒤 연도 놓침 등 자막 매칭 경계 사례, 예외 분류 순서·주석 정리 | 열림(낮음) |
| 리뷰 후속 이슈 후보 (#194) | Android 8~11에서 키 조회가 일시 null일 때 키를 덮어쓸 수 있음(미확인), 설정 저장소의 같은 덮어쓰기 패턴, 일시 실패 중 삭제한 항목이 다시 보임 | 열림 |
| R8 최적화 꺼짐 | 축소·난독화 없음. APK 287 MB, AAB 130 MB(4개 ABI 포함) | 그대로 출시 |
| LibVLC 라이선스 (#101) | GPL 구성요소 제거·LGPL 빌드 교체 미완 | 열림 |
| 릴리스 빌드 실기기 미확인 | 이번 산출물(릴리스)은 기기에 설치해 보지 않았다. 휴대폰에는 같은 버전의 디버그 빌드만 설치했다 | 업로드 전 내부 테스트 트랙에서 확인 권장 |

## 출시 전 사용자 할 일

1. [ ] 노션 개인정보처리방침 영어판 게시
2. [ ] Play Console에 `PiecePlay-v.2026.11.14.003-32.aab` 업로드(이미 올라간 versionCode가 32 미만인지 확인), 출시 노트 입력
3. [ ] 등록정보·스크린샷 갱신
4. [ ] 국가 추가는 심사 통과 뒤(대상 국가는 PM 결정)
