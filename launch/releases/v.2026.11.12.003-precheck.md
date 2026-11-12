# 프로덕션 출시 전 점검: v.2026.11.12.003 (예정)

- 점검일: 2026-11-12, launch 세션 (#67)
- 기준: `main` d92a8ab (현재 버전 v.2026.11.12.002, versionCode 27)
- 다음 버전: 오늘 `:app:nextVersion`을 실행하면 v.2026.11.12.003 / 28이 된다. 날짜가 바뀌면 이름도 바뀌므로 실제 실행 후 이 파일 이름을 맞춘다.
- 이번 점검에서는 버전 올리기, 릴리스 빌드, Play Console 제출을 하지 않았다. 코드는 읽기만 했다. 디버그 APK는 #49 스크린샷 작업 때 같은 main 기준으로 만든 것을 썼다.

- 갱신 2026-11-12 (#104, 아래 "대한민국 단독 출시" 절): 첫 프로덕션 출시 국가는 **대한민국만**이다(전 세계는 후속). Play Console 기준 프로덕션 액세스는 열린 것으로 보이고, 비공개 테스트 Alpha에 versionCode 2(1.0.1)가 국가 1개로 올라가 있다. 기준 커밋은 `main` b561339.
- d92a8ab 이후 바뀐 점(b561339 기준 확인): 오픈소스 라이선스 화면(`AppScreen.OpenSourceLicenses`)이 생겼고, 파일 이름·요청 URI 로그(`TorrentEngine.kt`, `TorrentStreamServer.kt`)가 빠졌다. 아래 해당 행은 그대로 두되 이 줄이 우선한다.

판정: **통과** / **확인 필요**(사용자·Play Console에서 확인) / **막힘**(자료나 결정이 없어 진행 불가) / **수정 필요**(코드·디자인 변경 필요)

## 체크리스트

| # | 항목 | 판정 | 근거 |
|---|---|---|---|
| 1 | 버전 | 확인 필요 | `version.properties` = 2026-11-12, revision 2, code 27. 출시 직전에 `:app:nextVersion` 단독 실행 필요(이번에는 지시대로 실행하지 않음). 마지막 Play 업로드 versionCode가 27 미만인지 Play Console에서 확인 |
| 2 | 서명 | 통과(파일 존재만) | 원본 체크아웃의 `program/android/.signing/`에 `upload.properties`와 `pieceplay-upload.jks`가 있다. `build.gradle.kts` release가 `upload` 서명 설정을 쓴다. 주의: `upload.properties`가 없으면 빌드가 실패하지 않고 서명 없이 만들어진다. 출시 빌드 후 `apksigner verify` 결과를 확인할 것 |
| 3 | 빌드 | 막힘(지시상 미실행) | `:app:packageReleaseFiles`를 실행하지 않았다. 기존 산출물은 v.2026.11.10.003(25)까지만 있다 |
| 4 | 테스트 | 확인 필요 | PATCH_NOTES 기준 v.2026.11.12.001에서 단위 260개, 에뮬레이터 31개, 디버그 빌드, lint 통과. v.2026.11.12.002(디자인 리뉴얼)에는 검증 기록이 없다. 출시 빌드 전에 program이 단위·에뮬레이터 테스트를 다시 돌려야 한다 |
| 5 | 광고 | 통과(코드 기준) | `meta/AdSpec.kt` `AdUnit`: 디버그는 Google 테스트 단위, 릴리스는 `ca-app-pub-2418522550325553/…` 실제 단위. 배너는 `TestAdPolicy.visible`에서 메인·재생 목록만 true이고 저장 파일·준비·재생은 false. 전면 광고는 재생 종료·완료 시 `CompletionAdPolicy.eligible`(5분 시청 또는 5회 완료, 10분 간격)일 때만. SDK 초기화와 광고 요청은 UMP `canRequestAds` 뒤에만 한다. 릴리스 빌드 실기기 확인은 하지 않음 |
| 6 | 정책·데이터 보안 | 막힘 | 양식 답변 기록(`launch/forms/`)이 저장소에 없다. 개인정보처리방침 URL도 저장소·앱 어디에도 없다. 실제 동작은 아래 "데이터 보안" 절에 정리했다 |
| 7 | 등록정보 | 확인 필요 | 한국어 초안은 `launch/listing/ko-KR.md`. Console 현재 문구와 대조해야 한다. 새 스크린샷·그래픽은 PR #64 |
| 8 | 출시 노트 | 초안 | 한국어 출시 노트는 `launch/listing/ko-KR.md`(아래 초안과 같음). Alpha의 versionCode 2(1.0.1)는 이 저장소 버전 체계(v.YYYY.MM.DD.NNN) 이전 빌드이므로 프로덕션은 첫 출시로 보고 쓴다 |
| 9 | 트랙 | 확인 필요 | Alpha(비공개 테스트)에 versionCode 2, 국가 1개. 프로덕션 국가는 대한민국만(전 세계는 후속). 새 빌드(28 예정)를 Alpha에서 먼저 확인한 뒤 프로덕션으로 올리고 단계적 출시 비율은 PM이 정한다 |

## 데이터 보안 (6번 상세)

### 앱 권한 (디버그 빌드 병합 매니페스트 기준)

| 권한 | 출처 | 비고 |
|---|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | 앱 | 토렌트·광고 통신 |
| `com.google.android.gms.permission.AD_ID` | play-services-ads | Play Console "광고 ID" 선언에서 **사용함**으로 답해야 한다 |
| `ACCESS_ADSERVICES_AD_ID`, `_ATTRIBUTION`, `_TOPICS` | play-services-ads | Privacy Sandbox |
| `WAKE_LOCK`, `FOREGROUND_SERVICE` | 라이브러리(WorkManager·광고 SDK) | 앱이 직접 쓰는 포그라운드 서비스는 없다. 유형별 FGS 권한이 없어 FGS 선언 대상은 아닐 것으로 본다(Console에서 확인) |

위치·연락처·저장소·카메라 같은 민감 권한은 없다.

### 앱이 직접 다루는 데이터

| 데이터 | 저장 위치 | 외부 전송 | 데이터 보안 양식 |
|---|---|---|---|
| 재생 목록(마그넷, 제목, 이어보기 위치, 최대 50개) | `noBackupFilesDir/magnet_history`, Keystore AES-GCM 암호화(`TorrentCache`/`TorrentCacheCipher`) | 없음 | 기기 밖으로 보내지 않으므로 "수집" 아님 |
| 받은 영상, 이어받기 상태 | `noBackupFilesDir/torrent_downloads`, `torrent_state`(상태는 암호화) | 없음 | 수집 아님. 삭제는 앱 안 "저장 파일 관리"와 앱 삭제로 가능 |
| 전면 광고 횟수·시각 | SharedPreferences `completion_ads` | 없음 | 수집 아님 |
| `.torrent` 링크 | 사용자가 넣은 주소로 HTTPS 요청(http는 https로 바꿈) | 사용자가 고른 서버 | 개발자에게 오지 않으므로 수집 아님 |
| IP 주소 | BitTorrent 피어·트래커·DHT에 노출 | P2P 특성상 다른 사용자에게 보임 | 개발자 수집은 아니지만 **개인정보처리방침에 적어야 한다** |

- 업로드는 하지 않는다(`DownloadOnlyPolicy`: unchoke slot 0). 다 받은 토렌트는 바로 멈춰 시드로 남지 않는다.
- `allowBackup=true`이고 백업 규칙 파일은 템플릿 그대로다. 마그넷·영상은 `noBackupFilesDir`라 백업에서 빠진다. SharedPreferences(광고 횟수, UMP 동의 문자열)는 Android 백업에 들어갈 수 있다. Google 권장상 이것만으로 "수집"이 되지는 않는다.

### 광고 SDK가 수집하는 데이터 (AdMob 25.5.0 + UMP 4.0.0)

Google이 공개한 Google Mobile Ads SDK 데이터 공개 문서를 기준으로 아래처럼 신고하는 것을 권장한다. 문서가 바뀔 수 있으니 **제출 직전에 최신 문서와 대조**해야 한다.

| 데이터 유형 | 수집 | 공유 | 목적 | 비고 |
|---|---|---|---|---|
| 위치 > 대략적인 위치 | 예 | 예 | 광고, 분석, 사기 방지·보안 | IP 주소에서 추정 |
| 앱 활동 > 앱 상호작용 | 예 | 예 | 광고, 분석, 사기 방지 | 광고 노출·탭, 앱 실행 |
| 앱 정보 및 성능 > 비정상 종료 로그, 진단 | 예 | 예 | 분석, 사기 방지 | SDK 자체 진단 |
| 기기 또는 기타 ID | 예 | 예 | 광고, 분석, 사기 방지 | 광고 ID, 앱 세트 ID |

- 전송 중 암호화: 예. 사용자 데이터 삭제 요청 수단: 광고 SDK 데이터는 Google 정책(광고 ID 재설정)으로 안내.
- 필수 여부: 광고는 앱 사용에 필수가 아니지만 SDK 데이터 처리는 끌 수 없으므로 "필수"로 표시하는 경우가 많다. UMP 동의 거부 지역에서는 맞춤형 광고가 꺼진다.
- UMP는 동의 상태(TCF 문자열)를 기기에 저장하고 Google로 보낸다. 별도 데이터 유형은 위 항목에 포함된다.

### 그 밖의 Play Console 양식

| 양식 | 권장 답변 | 비고 |
|---|---|---|
| 광고 포함 | 예 | 배너·전면 |
| 광고 ID | 사용함, 목적: 광고·분석 | AD_ID 권한이 병합됨 |
| 타겟층 | 18세 이상 권장 | 13세 미만을 포함하면 가족 정책·아동용 광고 설정이 필요하다. 코드에 `tagForChildDirectedTreatment` 설정이 없다 |
| 콘텐츠 등급 | 사용자가 출처를 고르는 영상 재생 앱으로 답변 | 앱이 콘텐츠를 제공하지 않지만 임의의 인터넷 콘텐츠를 재생할 수 있다는 점을 숨기지 않는다. 설문 문항은 사용자가 Console에서 확인 |
| 앱 액세스 | 로그인 없음, 모든 기능 바로 사용 | |
| 개인정보처리방침 | **URL 필요** | 광고 SDK가 있으면 필수. 저장소에 문서·URL이 없다 |

## 토렌트 앱 정책 위험

| 위험 | 현재 상태 | 판정 |
|---|---|---|
| 저작권 콘텐츠 유도 문구(앱) | `strings.xml` 124개 문자열과 Kotlin 하드코딩 문구에 "무료 영화·드라마" 같은 표현이 없다. 기능 설명("마그넷 또는 .torrent 다운로드 링크")만 있다. 앱에 내장된 마그넷·사이트 링크도 없다 | 통과 |
| 저작권 콘텐츠 유도 문구(스토어 설명) | 설명 문안이 저장소에 없어 확인 불가. Play Console의 현재 설명에 영화·드라마 이름, 토렌트 사이트 이름, "무료로 최신 영화" 같은 표현이 없는지 확인 필요 | 막힘 |
| 스크린샷·그래픽의 저작물 노출 | PR #64의 새 스크린샷은 Blender Foundation 오픈 무비(CC BY)만 보인다. 그래픽 이미지에는 "torrent" 단어도 넣지 않았다. main의 옛 스크린샷 2장은 영상 노출이 없지만 옛 이름이다 | PR #64 병합 후 통과 |
| CC BY 출처 표시 | 스크린샷에 Sintel 등이 보이므로 스토어 설명 끝에 출처 한 줄을 넣는 것이 안전하다 | 확인 필요 |
| 저작권 안내·면책 | 앱과 스토어에 "권리가 있는 콘텐츠만 재생하라"는 안내가 없다. 심사관이 불법 다운로드 도구로 볼 위험을 줄이려면 앱(메인 하단 또는 첫 실행)과 설명에 한 줄 넣는 것을 권장 | 수정 필요(디자인·문구) |
| 기기 및 네트워크 악용: 업로드·시드 | 업로드하지 않고, 완료 후 바로 멈춘다. 다른 사용자 기기·네트워크에 부담을 주는 동작이 없다 | 통과 |
| 기기 및 네트워크 악용: 데이터 사용량 | Wi-Fi 전용 설정이나 모바일 데이터 경고가 없다. 영상 하나에 수백 MB를 받는다. 정책 위반은 아니지만 사용자 불만·리뷰 위험 | 권장(기획) |
| 백그라운드 동작 | 포그라운드 서비스 없이 raw `Thread`로 받는다. 앱이 백그라운드로 가면 OS가 언제든 멈출 수 있고, 알림 없이 받는 중일 수 있다. 숨겨진 백그라운드 통신으로 보일 소지는 작다(사용자가 시작한 작업) | 확인 필요 |
| 로그 | 릴리스에서도 `Log.d`가 남는다(R8 최적화 꺼짐). `engine/TorrentEngine.kt:239`가 받은 파일 경로·이름을 로그에 남긴다. 마그넷 자체는 남기지 않지만 시청 기록이 logcat에 보인다 | 수정 필요(program) |
| 오픈소스 라이선스 고지 | LibVLC(LGPL 2.1), libtorrent4j, NanoHTTPD를 포함하는데 앱 안에 라이선스 화면이 없다. LGPL은 고지와 라이선스 사본 제공이 필요하다 | 수정 필요(program·design) |
| 16 KB 페이지 크기 | targetSdk 37. 디버그 APK의 `.so` 전부 ELF LOAD 정렬 0x4000, zip 16KB 정렬 검증 통과 | 통과 |
| 평문 통신 | `network_security_config`가 127.0.0.1·localhost만 허용 | 통과 |

## 따로 보고

### 막힌 항목

| 항목 | 막힌 이유 | 필요한 것 |
|---|---|---|
| 데이터 보안 양식 대조 | `launch/forms/` 없음 | 사용자가 Play Console에 이미 넣은 답변을 알려 주거나, 위 권장 답변으로 새로 작성할지 결정 |
| 스토어 설명 점검 | `launch/listing/` 없음 | Console의 현재 앱 이름·간단한 설명·자세한 설명 텍스트 |
| 개인정보처리방침 | URL·문서 없음 | 호스팅할 곳 결정. 내용: 기기 내 암호화 저장, 개발자 서버 없음, AdMob 수집 항목, P2P IP 노출 |
| 릴리스 빌드·서명 검증 | 이번 지시상 실행 안 함 | PM이 출시 결정 후 `nextVersion` → `packageReleaseFiles` → `apksigner verify` |
| 새 스크린샷 | PR #64 미병합 | PM 병합 |

### 사용자가 Play Console에서 직접 확인할 항목

| 항목 | 확인할 것 |
|---|---|
| 앱 콘텐츠 > 데이터 보안 | 위 광고 SDK 표와 일치하는지, 앱 자체 데이터는 "수집 안 함"인지 |
| 앱 콘텐츠 > 광고 ID | "사용함"으로 되어 있는지 |
| 앱 콘텐츠 > 광고 | "광고 포함" |
| 앱 콘텐츠 > 타겟층 | 13세 미만이 포함되지 않았는지 |
| 앱 콘텐츠 > 콘텐츠 등급 | 설문 답변과 받은 등급 |
| 앱 콘텐츠 > 개인정보처리방침 | URL이 있고 열리는지 |
| 기본 스토어 등록정보 | 설명에 저작물 이름·사이트 이름·"무료 영화" 표현이 없는지, 출처 한 줄 추가 여부 |
| 릴리스 > 트랙 | 각 트랙의 현재 versionCode, 비공개 테스트 진행 상태(프로덕션 신청 조건 충족 여부) |
| 정책 상태 | 받은 경고·거절이 있는지 |
| AdMob 콘솔 | 앱이 Play 스토어와 연결됐는지, app-ads.txt(개발자 웹사이트) 설정 여부, 광고 단위 2개 활성 |

### 코드·디자인 수정이 필요한 항목

| 우선 | 담당 | 내용 | 근거 |
|---|---|---|---|
| 높음 | program·design | 오픈소스 라이선스 화면 추가(LibVLC LGPL 2.1 등). 메인 하단 버전 옆 링크 정도 | LGPL 고지 의무 |
| 높음 | design | 저작권 안내 한 줄("권리를 가진 콘텐츠만 재생하세요") 앱과 스토어 설명에 추가 | 지식재산권 정책 심사 위험 완화 |
| 중간 | program·design | 앱 안에 개인정보처리방침 링크 | 광고 SDK 포함 앱 권장 사항, 데이터 보안 양식과 연결 |
| 중간 | program | `TorrentEngine.kt:239` 파일 이름 로그 제거 또는 디버그 빌드에서만 출력. `TorrentStreamServer.kt:17` 요청 로그도 같은 처리 | 시청 기록이 logcat에 남음(CONVENTIONS 보안 규칙 취지) |
| 낮음 | program | `upload.properties`가 없으면 릴리스 빌드를 실패시키기 | 서명 없는 AAB가 조용히 만들어질 수 있음 |
| 낮음 | program | `backup_rules.xml`·`data_extraction_rules.xml` 템플릿 정리(백업 대상 명시) | 백업 범위 명확화 |
| 낮음 | planning | 모바일 데이터 사용 경고 또는 Wi-Fi 전용 설정 검토 | 리뷰·데이터 요금 불만 위험 |

## 출시 노트 초안 (500자 이내, ko-KR)

```
PiecePlay 첫 정식 출시입니다.
- 마그넷 또는 .torrent 링크를 넣으면 다 받기 전에 바로 재생합니다.
- 재생 위치 앞부분을 먼저 받아 끊김을 줄였습니다.
- 재생 목록에서 다운로드를 관리하고 보던 곳부터 이어봅니다.
- 새 다크 테마와 정리된 재생 화면을 적용했습니다.
- 받은 영상과 재생 기록은 기기 안에만 암호화해 보관합니다.
```

이전 트랙에 이미 나간 내용이 있으면 v.2026.11.12.001~002 항목(30초 버퍼, 다크 테마 리뉴얼)만 남긴다.

## 대한민국 단독 출시 (2026-11-12 갱신)

사용자 결정: 첫 프로덕션은 **대한민국만**. 전 세계는 다음 단계다(문서 끝 "후속: 전 세계 확대").

### 1. 한국어 스토어 등록정보

- 초안: `launch/listing/ko-KR.md` (앱 이름, 간단한 설명, 자세한 설명, 출시 노트). Play Console의 현재 문구가 저장소에 없어서 앱 화면과 #49 그래픽 문구를 기준으로 썼다. 입력 전에 Console 현재 문구와 대조한다.
- 기본 언어는 한국어(ko-KR) 그대로. 영어 번역은 이번 출시에 넣지 않는다.
- 피하는 표현: 영화·드라마 제목, 토렌트 사이트 이름, "무료 영화", "최신 개봉작", "무제한 다운로드". 설명 끝에 저작권 안내 한 줄과 스크린샷 영상 출처(Blender Foundation, CC BY) 한 줄을 둔다.
- 스크린샷·그래픽: PR #64 자료(`launch/store-assets/4_Play_Store_Screenshots/`, `feature_graphic_1024x500.png`).

### 2. 개인정보처리방침 (개인정보 보호법 기준)

앱은 회원가입·서버가 없고 앱 데이터는 기기 안에만 있다. 개인정보로 볼 수 있는 처리는 사실상 **광고(Google AdMob)가 자동 수집하는 정보**와 **P2P 통신에서 IP가 다른 사용자에게 보이는 것**이다. 방침은 이 사실에 맞춰 짧게 쓰되, 개인정보 보호법 제30조(개인정보 처리방침의 수립 및 공개)가 요구하는 항목을 빠짐없이 둔다. 근거: [개인정보 보호법](https://www.law.go.kr/법령/개인정보보호법) 제30조, 세부 서식은 개인정보보호위원회 「개인정보 처리방침 작성지침」(최신판 원문과 대조 권장).

| 항목 | PiecePlay 내용 |
|---|---|
| 처리 목적 | 광고 제공·광고 성과 측정·부정 이용 방지(AdMob). 앱 기능(재생 목록, 이어보기)은 기기 안에서만 처리 |
| 처리 항목 | 자동 수집: 광고 ID, 기기 정보, IP 주소에서 추정한 대략적 위치, 광고 노출·클릭 기록, 오류 진단 정보(AdMob). 개발자가 직접 수집하는 항목 없음 |
| 보유·이용 기간, 파기 | 개발자 보유 없음. 기기 안 재생 목록·영상은 앱 안 삭제 또는 앱 삭제 시 즉시 파기. AdMob 정보는 Google 정책에 따름 |
| 제3자 제공·처리 위탁 | Google LLC(AdMob)에 광고 목적 처리. 제공인지 위탁인지 표기 방식은 작성지침 기준으로 정한다 |
| 국외 이전 | Google(미국 등)로 전송됨: 이전 항목, 국가, 시기·방법(앱 사용 시 네트워크로 전송), 받는 자, 목적, 보유 기간, 거부 방법(광고 ID 재설정·맞춤 광고 끄기)과 거부 시 영향 |
| 자동 수집 장치 설치·운영·거부 | 광고 ID 등 식별자 사용, Android 설정에서 광고 ID 재설정·삭제로 거부 가능 |
| 정보주체 권리와 행사 방법 | 열람·정정·삭제·처리정지 요구 방법. 개발자가 보유한 정보가 없다는 점과 문의처 |
| 안전성 확보 조치 | 기기 안 재생 목록·상태는 Android Keystore 기반 AES-GCM 암호화, 앱 전용 저장 공간 사용 |
| P2P 안내 | 토렌트 특성상 받는 동안 IP 주소가 같은 토렌트의 다른 사용자에게 보임. 업로드는 하지 않음 |
| 개인정보 보호책임자·문의처 | 이름·연락처는 방침 본문에만 적고 이 저장소에는 적지 않는다(Play Console 개발자 연락처와 일치시킴) |
| 만 14세 미만 | 대상으로 하지 않음(타겟층 18세 이상과 일치) |
| 시행일·변경 이력 | 게시일 표기 |

- 게시 위치: 5번 개발자 웹사이트와 같은 도메인에 두면 app-ads.txt와 함께 해결된다. Play Console > App content > Privacy policy에 URL 입력.
- 앱 안 링크: 지금 앱에는 방침 링크가 없다(메인 하단은 버전·"광고 개인정보 설정"·"오픈소스 라이선스"). 메인 하단 링크 추가를 program·design에 권장(필수는 아님, Play Console URL은 필수).

### 3. 콘텐츠 등급

- 등급 설문은 한 번 작성하면 모든 지역에 적용된다. **한국에서 앱(게임 아님)에는 GRAC가 아니라 "Google Play 등급"**이 표시된다. 근거: [콘텐츠 등급](https://support.google.com/googleplay/android-developer/answer/9898843), [콘텐츠 등급 요건](https://support.google.com/googleplay/android-developer/answer/9859655)
- 앱이 콘텐츠를 직접 제공하지 않지만 사용자가 고른 인터넷 영상을 재생한다는 점을 설문에서 숨기지 않는다. 등급 기관이 분류를 거부하면 그 지역에서만 내려간다(같은 문서).

### 4. 데이터 보안

- 양식은 패키지당 하나이고 국가별로 다르지 않다. 위 "데이터 보안" 절의 권장 답변을 그대로 쓴다. 한국 단독 출시로 바뀌는 답은 없다. 근거: [데이터 보안 섹션](https://support.google.com/googleplay/android-developer/answer/10787469)
- 방침(2번)의 처리 항목과 양식의 AdMob 항목(대략적 위치, 앱 상호작용, 진단, 기기 ID)이 서로 맞아야 한다.
- 광고 동의(UMP): 한국은 AdMob 유럽·미국 주 메시지 대상 지역이 아니다. 앱의 UMP 흐름은 그대로 두면 되고 한국 출시를 위해 AdMob에서 메시지를 게시할 필요는 없다.

### 5. app-ads.txt (광고 수익)

Google은 app-ads.txt가 없으면 "큰 광고 수익 손실"이 생길 수 있다고 안내한다. AdMob이 파일을 확인하려면 Play 등록정보에 개발자 웹사이트가 있어야 하는데 지금 비어 있다. 근거: [About app-ads.txt](https://support.google.com/admob/answer/9787936), [app-ads.txt 설정](https://support.google.com/admob/answer/9363762)

1. 파일을 **도메인 루트**에 둘 수 있는 웹사이트를 정한다. 자기 도메인이 없으면 GitHub Pages 사용자 사이트(`<계정>.github.io`)처럼 루트에 둘 수 있는 곳. 저장소 하위 경로(`<계정>.github.io/<저장소>/`)는 안 된다.
2. AdMob > Apps > app-ads.txt > "How to set up app-ads.txt"에서 게시자 한 줄을 복사해 `https://<도메인>/app-ads.txt`로 올린다.
3. Play Console > Grow users > Store presence > Store settings > Store listing contact details에 웹사이트 URL 입력.
4. 최대 24시간 뒤 AdMob > Apps > View all apps > app-ads.txt에서 앱 행을 펼쳐 "Check for updates".

### 6. 사용자가 Play Console·AdMob에서 할 일 (순서대로)

1. [ ] 웹사이트 정하기, `app-ads.txt`와 개인정보처리방침(한국어) 게시 (2번, 5번)
2. [ ] Play Console > Store settings > Store listing contact details: 웹사이트 URL
3. [ ] Play Console > App content > Privacy policy: 방침 URL
4. [ ] App content > Data safety: 위 권장 답변과 대조(앱 자체 수집 없음, AdMob 항목만)
5. [ ] App content > Ads: 광고 포함 / Advertising ID: 사용함
6. [ ] App content > Target audience: 18세 이상
7. [ ] App content > Content ratings: 설문 확인, 한국 "Google Play 등급" 확인
8. [ ] Main store listing(ko-KR): `launch/listing/ko-KR.md` 문구, PR #64 스크린샷 6장·그래픽 이미지
9. [ ] 새 빌드(versionCode 28 예정)를 Alpha(비공개 테스트)에 올려 확인
10. [ ] Production > Countries / regions: **대한민국만** 선택 후 저장
11. [ ] 프로덕션 출시 만들기, 출시 노트(ko-KR) 입력, 단계적 출시 비율(PM 결정) → 검토 제출
12. [ ] 24시간 뒤 AdMob app-ads.txt 상태, Play 정책 상태 확인

## 후속: 전 세계 확대

한국 출시 뒤 진행한다. 이번에는 깊게 조사하지 않았다.

- 영어 등록정보: 초안 `launch/listing/en-US.md`(기본 언어 한국어 유지, 영어 설명에 "앱 화면은 한국어만" 명시). 영어 UI 번역 여부는 planning 판단.
- EEA·UK·스위스: AdMob > Privacy & messaging > European regulations 메시지를 게시해야 한다(Google [EU 사용자 동의 정책](https://www.google.com/about/company/user-consent-policy/)). 앱의 UMP 흐름과 "광고 개인정보 설정" 진입점은 이미 있다.
- 미국 주 개인정보법: AdMob US state regulations 메시지(대상 20개 주) 게시 권장. 문서상 필수라고는 적혀 있지 않다([설정](https://support.google.com/admob/answer/10860309)).
- 국가별 토렌트 제한: 공식 근거가 있는 국가 단위 금지는 아직 찾지 못했다. 확대 전에 다시 본다.
- 개인정보처리방침: 영어본과 GDPR·미국 주 항목 추가.
- 확대 시 program이 디버그 지역(EEA, 미국 주)으로 동의 창이 뜨는지 확인.
