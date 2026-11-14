# Play Console 진행 순서: 빌드 32 + 1차 글로벌 출시 (2026-11-15 오전)

- 작성: 2026-11-14, launch (#144). 사장님이 위에서부터 그대로 따라 하는 문서다. 메뉴 이름은 Play Console 영문 화면 기준이고, 괄호 안은 한국어 화면 이름이다.
- 올릴 빌드: `program/android/app/build/outputs/pieceplay-release/PiecePlay-v.2026.11.14.003-32.aab` (versionCode 32, 129,857,768바이트, 2026-11-14 16:14 생성. 파일 있음 확인)
- 근거 문서: [`v.2026.11.14.003-precheck.md`](v.2026.11.14.003-precheck.md)(빌드 점검, program), [`global-phase1-checklist.md`](global-phase1-checklist.md)(국가·동의·양식)
- launch는 Play Console·AdMob·Notion에서 아무것도 올리거나 바꾸지 않았다. 아래는 모두 사장님이 직접 할 일이다.

---

## ⚑ 사장님 몫: 시작 전에 끝낼 것

| # | 할 일 | 막는가 | 어디서 | 참고 |
|---|---|---|---|---|
| A | **개인정보처리방침 영어판 게시** | **막음**(7개국 추가 전에 필수) | Notion 방침 페이지 | 한국어 아래에 [`launch/listing/privacy-policy-en.md`](../listing/privacy-policy-en.md)의 "본문" 블록을 붙여 넣고 `[CONTACT EMAIL]`(한국어 9절과 같은 이메일)·시행일을 채운다. 맨 위에 `English version below / 영어 원문은 아래에 있습니다.` 한 줄 |
| B | Play Console 최고 versionCode 확인 | 막음(32 미만이어야 업로드됨) | Test and release > App bundle explorer(App bundle 탐색기) | 지금까지 올린 가장 큰 versionCode가 31 이하인지 본다. 프로덕션은 28로 알고 있다 |
| C | 기본 스토어 등록정보 언어 확인 | 순서가 바뀜 | Grow users > Store presence > Store listings(스토어 등록정보) | 맨 위 "Default store listing"의 언어가 **English (United States)**인지 **Korean**인지 본다. `store-assets/README.md`에는 "현재 기본 등록정보(en-US)"라고 적혀 있어 en-US일 가능성이 높다. 4단계에서 어느 칸에 무엇을 넣을지가 이것으로 정해진다 |
| D | AdMob 미국 주 메시지 게시 | 권장(막지 않음) | AdMob > Privacy & messaging > US state regulations | Apps: PiecePlay / Language: English / Targeting: All current and future supported US states / 방침 URL → Publish. 자세한 절차는 체크리스트 3절 |
| E | 홍보 영상 | 막지 않음 | — | en-US 영상 속 앱 화면이 한국어 UI다. **이번에는 en-US 동영상 칸을 비워 둔다.** ko-KR 칸은 지금 값 그대로 |

---

## 1단계. 비공개 테스트 트랙에 AAB 32 올리기

1. **Test and release > Testing > Closed testing**(비공개 테스트) → 지금 쓰는 트랙(**Alpha**) → **Manage track**(트랙 관리) → **Create new release**(새 버전 만들기)
2. **App bundles** 칸에 `PiecePlay-v.2026.11.14.003-32.aab` 업로드
   - 업로드 후 버전 이름이 `v.2026.11.14.003`, versionCode `32`로 보이는지 확인
   - 경고가 나오면 내용을 캡처해 launch에 알린다(서명 키 불일치 오류는 나오면 안 된다. program이 업로드 키 SHA-256 `1A:9C:49:12…` 일치를 확인함)
3. **Release name**(버전 이름): 자동으로 들어간 `32 (v.2026.11.14.003)` 그대로
4. **Release notes**(출시 노트): 아래 블록 전체를 그대로 붙여 넣는다(언어 태그 포함)

```
<en-US>
PiecePlay is now available in English.
• Open magnet links from your browser or messenger, or share a link to PiecePlay.
• Pick subtitle and audio tracks during playback. Subtitles can turn on automatically in your device language.
• New Settings: Wi-Fi only, and Download order (From the start or Fastest).
• Your Playlist is better protected, with fewer playback errors.
</en-US>
<ko-KR>
- 앱 화면을 영어로도 볼 수 있습니다(기기 언어가 한국어가 아니면 영어).
- 브라우저·메신저의 마그넷 링크를 누르거나 공유하면 바로 열 수 있습니다.
- 재생 중 자막·음성 트랙을 고르고, 기기 언어 자막을 자동으로 켭니다.
- 설정을 추가했습니다: Wi-Fi 전용, 받는 순서(앞부분부터·빠르게 받기).
- 재생 목록을 더 안전하게 보관하고 재생 오류를 줄였습니다.
</ko-KR>
```

   - 각 언어 500자 이내(영어 372자, 한국어 207자). 28 이후 바뀐 내용(PATCH_NOTES 29~32)을 묶었다. 영어 문구 이름(Wi-Fi only, Download order, From the start, Fastest)은 앱 화면과 같다.
5. **Next**(다음) → 경고·오류 확인 → **Save**(저장)
6. **Countries / regions**(국가/지역) 탭: 비공개 테스트는 대한민국 그대로 둔다(해외 테스터가 없으면 바꾸지 않음)
7. 아직 **검토 제출은 하지 않는다.** 2~4단계 변경을 모아 5단계에서 한 번에 보낸다.

> 비공개 테스트 앱이 승인되면 테스터 휴대폰에서 **릴리스 빌드**를 한 번 열어 본다(program 점검에서 릴리스 빌드는 기기 설치 확인을 하지 않았다). 메인 영어/한국어 화면, 마그넷 하나 재생, 메인 배너 광고가 보이는지.

## 2단계. 앱 콘텐츠 양식 확인 (바꾸지 않음, 보기만)

**Policy > App content**(앱 콘텐츠)에서 아래가 그대로인지 본다. 국가를 추가해도 바뀌는 양식은 없다(체크리스트 5절).

| 항목 | 그대로여야 하는 값 |
|---|---|
| Privacy policy(개인정보처리방침) | `https://shine-antler-dd8.notion.site/PiecePlay-Torrent-Player-403c3f3da78780ec88cbdce8e89d314d` (앱 안 링크와 같은 주소. A에서 영어를 붙인 같은 페이지) |
| Ads(광고) | Yes, my app contains ads |
| Advertising ID(광고 ID) | 사용함, 목적: 광고·분석 |
| Target audience(타겟층) | 18세 이상만 |
| Data safety(데이터 보안) | 앱 자체 수집 없음, AdMob 항목(대략적 위치, 앱 상호작용, 진단, 기기 ID)만. 32에서 권한·수집 변경 없음 |
| Content ratings(콘텐츠 등급) | 다시 하지 않음 |

## 3단계. 스토어 설정 연락처 확인

**Grow users > Store presence > Store settings**(스토어 설정) > Store listing contact details

| 칸 | 값 |
|---|---|
| Website | `https://kywood.github.io` (app-ads.txt가 있는 사이트) |
| Email | 지금 값 그대로(이 문서에 적지 않음) |

## 4단계. 스토어 등록정보 (en-US·ko-KR)

**Grow users > Store presence > Store listings > Default store listing**(기본 스토어 등록정보) → 위 C에서 본 기본 언어에 따라:

- 기본이 **en-US**이면: 기본 등록정보 칸에 아래 **영어**를 넣고, **Manage translations**(번역 관리)의 Korean – ko-KR에 아래 **한국어**를 넣는다.
- 기본이 **ko-KR**이면: 기본 칸에 **한국어**, 번역 English (United States) – en-US에 **영어**.

### 4-1. 영어 (English (United States) – en-US)

**App name**(앱 이름, 30자 이내):

```
PiecePlay: Torrent Player
```

**Short description**(간단한 설명, 80자 이내):

```
Paste a magnet or .torrent link and start watching before the download ends.
```

**Full description**(자세한 설명, 4000자 이내): [`launch/listing/en-US.md`](../listing/en-US.md) "자세한 설명" 코드 블록 전체(2196자). 첫 줄 `PiecePlay plays a video while it is still downloading.`부터 마지막 줄 `… © Blender Foundation | www.blender.org.`까지.

**Graphics**(그래픽):

| 칸 | 파일 (`launch/store-assets/` 아래) |
|---|---|
| App icon | `1_Play_Store_Assets/app_icon_512x512.png` (지금 아이콘과 같으면 그대로) |
| Feature graphic | `1_Play_Store_Assets/feature_graphic_1024x500_en-US.png` |
| Video | **비움**(E 참고) |
| Phone screenshots | `4_Play_Store_Screenshots/en-US/01_player_landscape.png` ~ `07_stored_files.png` 7장, 파일 이름 순서대로. **옛 "Torrent Stream Player" 스크린샷 2장은 삭제** |
| 7-inch tablet screenshots | `5_Tablet_7in/en-US/01~07` 7장 |
| 10-inch tablet screenshots | `6_Tablet_10in/en-US/01~07` 7장 |
| Chromebook | 비움 |

### 4-2. 한국어 (Korean – ko-KR)

**앱 이름**:

```
PiecePlay - 토렌트 플레이어
```

**간단한 설명**·**자세한 설명**: [`launch/listing/ko-KR.md`](../listing/ko-KR.md)의 코드 블록. 단, 아래 "막지 않는 문제" 1번을 보고 지금 Console 문구를 그대로 둘지 정한다.

| 칸 | 파일 |
|---|---|
| 그래픽 이미지 | `1_Play_Store_Assets/feature_graphic_1024x500_ko-KR.png` |
| 동영상 | 지금 값 그대로 |
| 휴대전화 스크린샷 | `4_Play_Store_Screenshots/ko-KR/01~07` 7장 |
| 7인치 / 10인치 | `5_Tablet_7in/ko-KR/01~07`, `6_Tablet_10in/ko-KR/01~07` 각 7장 |

4-1·4-2 모두 **Save**(저장).

## 5단계. 검토 제출

1. **Publishing overview**(게시 개요) → "Changes ready to send for review"(검토를 위해 보낼 변경사항)에 ① 비공개 테스트 32 ② 스토어 등록정보(en-US·ko-KR)가 보이는지 확인
2. **Send changes for review**(변경사항 검토 요청)
3. 관리형 게시가 켜져 있으면 승인 뒤 **Publish**(게시)를 눌러야 반영된다.
4. 승인 메일·정책 메일이 오면 캡처해 launch에 알린다. 거절이면 받은 문구 그대로 전달(launch가 `launch/reviews/`에 정리).

## 6단계. 프로덕션에 32 올리기 (한국 그대로)

> **PM 지시 순서에 없던 단계다.** 지금 프로덕션은 versionCode 28(한국어 UI만)이다. 32를 프로덕션에 올리지 않고 국가만 추가하면 영어권 사용자가 **한국어 화면인 28**을 받는다. 그래서 국가 추가 전에 32를 프로덕션으로 올린다. PM 확인 필요.

1. 비공개 테스트에서 32를 확인한 뒤 **Test and release > Production** → **Create new release** → **Add from library**(라이브러리에서 추가)로 versionCode 32 선택(또는 Alpha 출시 화면의 **Promote release > Production**)
2. 출시 노트: 1단계 4번 블록 그대로
3. 단계적 출시 비율: PM 결정(예: 20%)
4. **Next** → **Save** → Publishing overview에서 **Send changes for review** (관리형 게시면 승인 후 Publish)

## 7단계. 7개국 추가 (5·6단계 승인 뒤)

전제: A(방침 영어판) 완료, 프로덕션에 32 반영.

1. **Test and release > Production** → **Countries / regions**(국가/지역) 탭 → **Add countries / regions**
2. 아래 7개를 체크한다. 대한민국은 그대로 두고, 영국·아일랜드·EU 국가는 고르지 않는다(3차).

```
United States
Canada
Australia
New Zealand
India
Philippines
Singapore
```

3. **Save** → **Publishing overview** → **Send changes for review** (관리형 게시면 승인 후 Publish)
4. 결과적으로 프로덕션 국가는 **8개**(대한민국 + 7)다.

## 8단계. 국가 추가 당일 확인

| 확인 | 방법 | 문제일 때 |
|---|---|---|
| 각 국가 스토어에 영어로 보이는지 | 브라우저에서 `https://play.google.com/store/apps/details?id=com.gamenuri.pieceplay&hl=en&gl=US`, `…&hl=en_IN&gl=IN`, `…&hl=en_AU&gl=AU`, `…&hl=en_SG&gl=SG` | 한국어가 보이면 launch에 알림(해당 en-XX 번역 추가) |
| 콘텐츠 등급 | Policy > App content > Content ratings | 국가별 등급(미국·캐나다 ESRB, 나머지 IARC Generic)이 모두 나왔는지 |
| AdMob | AdMob > Apps > app-ads.txt 상태, Privacy & messaging 메시지 상태 | — |
| 비정상 종료 | Android vitals(1~2일 뒤) | 간헐 네이티브 크래시 #169를 지켜본다 |

---

## 이번 최종 점검 결과

### 막는 것

| # | 내용 | 해결 |
|---|---|---|
| 1 | 노션 방침에 영어 없음 | 사장님 A |
| 2 | 프로덕션 32 출시가 PM 순서에 없음. 빠지면 해외 사용자가 한국어 UI 28을 받는다 | 6단계 추가, PM 확인 |
| 3 | Console 최고 versionCode 미확인 | 사장님 B |

### 통과

| 항목 | 결과 |
|---|---|
| AAB 파일 | `PiecePlay-v.2026.11.14.003-32.aab` 있음, 129,857,768바이트(precheck와 같음). 서명·버전·테스트는 program 점검 통과 |
| 휴대전화 스크린샷 | en-US·ko-KR 각 7장(규격 2~8장). `01`만 1920x1080, 나머지 1080x1920. 16:9, 긴 변 ≤ 짧은 변 x2, 모두 RGB PNG(알파 없음), 장당 1.2MB 이하 |
| 7인치 태블릿 | en-US·ko-KR 각 7장, 1080x1920(`01`만 1920x1080). 대화면 규격(변 1080~7680px, 16:9) 통과 |
| 10인치 태블릿 | en-US·ko-KR 각 7장, 모두 2560x1440(16:9) |
| 그래픽 이미지 | en-US·ko-KR 1024x500, RGB(알파 없음) |
| 앱 아이콘 | `app_icon_512x512.png` 512x512 |
| 스크린샷 정책 | `en-US/01_player_landscape`, `05_open_link`를 열어 봤다: 실제 영어 UI, 화면 속 영상은 Blender 오픈 무비(Sintel, Tears of Steel), "지금 다운로드" 같은 행동 유도 문구 없음, 캡션은 위쪽 한 줄+보조 한 줄. 나머지 장면·영상 출처는 `store-assets/README.md`(marketing) 기록과 맞음 |
| 앱 이름 | `PiecePlay: Torrent Player` 25자, `PiecePlay - 토렌트 플레이어` 20자(사용자 결정 유지) |
| en-US 문구 | 간단한 설명 76자, 자세한 설명 2196자. 금지 표현(영화·드라마 제목, 사이트 이름, "free movies", "latest releases", "unlimited downloads") 없음. 32 기능과 어긋나는 문장 없음 |
| 앱 콘텐츠 양식 | 국가 추가·32로 바뀌는 것 없음 |

### 막지 않는 문제 (marketing에 넘길 것, PM 경유)

| # | 내용 | 제안 |
|---|---|---|
| 1 | `ko-KR.md` 자세한 설명이 28 기준이다. "토렌트 안에서 가장 큰 영상 파일 하나만 받습니다"는 지금 동작(영상+자막 파일)과 다르고, 영어 en-US에 있는 링크 열기·자막·설정(Wi-Fi 전용·받는 순서) 내용이 없다. 출시 노트 블록도 "첫 정식 출시" 그대로 | marketing이 en-US와 같은 범위로 고친다. 고치기 전이라면 **지금 Console의 한국어 설명을 그대로 두고** 스크린샷만 바꿔도 출시에는 지장 없다(사실과 크게 어긋나는 문장은 "가장 큰 영상 파일 하나만" 한 줄) |
| 2 | en-US 홍보 영상 속 앱 화면이 한국어 UI | 다시 만들기 전까지 en-US 동영상 칸 비움 |
| 3 | `1_Play_Store_Assets/app_icon_1024x1024_master.png`가 실제로는 512x512 | 올리지 않는 파일이라 영향 없음. 이름만 정리 |
| 4 | 영어 휴대전화 `03_player`는 v.2026.11.13.001 화면(README 기록) | 32와 화면 차이 없음(재생 화면 변경은 막대 색 정도). 그대로 올려도 됨 |
| 5 | 32의 "받는 순서"(Download order) 기능이 en-US 자세한 설명에 없다 | 다음 등록정보 갱신 때 추가 검토 |

### 확인하지 못한 것

- Play Console 현재 상태(로그인 필요): 기본 등록정보 언어, 최고 versionCode, 관리형 게시 여부, 비공개 테스트 트랙 이름·테스터 상태
- 릴리스 빌드(32)를 실제 기기에서 연 결과(program 점검에서도 미실행)
- 스크린샷 42장(3기기 x 2언어 x 7장) 중 2장만 직접 열어 봤다. 나머지는 marketing 기록을 믿었다
