# 1차 글로벌 출시 준비 체크리스트

- 작성: 2026-11-13, launch (#144). 갱신: 2026-11-13, 국가 계획 변경(#144 댓글)과 앱 영어화 병합(PR #163, #164, #170) 반영
- 기준: `main` 3c47cf4. 현재 프로덕션은 **대한민국만**, 마지막 출시 versionCode 28(main은 v.2026.11.13.001 / 29 이후).
- **1차 국가(사용자·PM 결정)**: 기존 대한민국 + **미국·캐나다·호주·뉴질랜드·인도·필리핀·싱가포르**
  - 2차(번역 후): 스페인어권 중남미(es-419)·브라질(pt-BR)·인도네시아·베트남. 말레이시아는 법률 확인 후 2차
  - 3차: **영국·아일랜드·EU**(AdMob 유럽 메시지 게시 후)
  - 보류: 일본·독일 / 제외: 러시아·중국
  - 근거: #144 댓글, [`marketing/market/global-torrent-markets-2026-11.md`](../../marketing/market/global-torrent-markets-2026-11.md)
- 이번 작업은 확인과 문서 작성만 했다. Play Console·AdMob은 열지 않았고(로그인 필요), 아무것도 바꾸거나 제출하지 않았다. 앱 코드는 읽기만 했다.

판정: **통과** / **사용자 확인 필요**(Console·AdMob에서 사용자가 확인) / **선행 조건**(다른 팀 작업이 끝나야 함) / **할 일**(이 문서대로 진행) / **권장**

## 요약

| # | 항목 | 판정 | 한 줄 |
|---|---|---|---|
| 1 | Play Console 국가 추가 | 할 일 | Production > Countries / regions에서 7개국 추가 → Publishing overview에서 검토 제출 |
| 2 | 영어 등록정보(en-US) | 통과(문구) / 선행 조건(스크린샷) | 문구는 영어 UI 기준으로 고쳤고 금지 표현 없음. **스크린샷 속 앱 화면이 아직 한국어**라 재촬영 필요(별도 요청 예정) |
| 3 | AdMob·UMP 동의 | 권장 | 1차 국가에는 필수 메시지가 없다. 미국 주 메시지 게시 권장. 유럽 메시지는 3차 전에 |
| 4 | 개인정보처리방침 영어판 | 할 일 | Notion에 영어 없음. 초안 [`listing/privacy-policy-en.md`](../listing/privacy-policy-en.md) |
| 5 | 데이터 보안·등급·타깃 연령 | 통과(변경 없음) | 셋 다 국가별 양식이 아니다. 등급은 국가별 기관 등급이 자동 부여 |
| 6 | 앱 쪽 선행 조건 | 영어화 통과 / 나머지 권장 | 앱 영어화는 main 병합됨(프로덕션 출시는 아직). LGPL LibVLC 교체는 권장 |
| 7 | 출시 후 확인 | 할 일 | 국가별 설치·크래시·eCPM을 1주·4주에 확인 |

---

## 1. Play Console 국가/지역 추가 절차

근거: [Prepare and roll out a release / 국가 설정](https://support.google.com/googleplay/android-developer/answer/7550024), [관리형 게시](https://support.google.com/googleplay/android-developer/answer/9859654), [앱 게시](https://support.google.com/googleplay/android-developer/answer/9859751) (2026-11-13 확인)

### 위치와 순서

1. **Test and release > Production** → **Countries / regions** 탭
2. **Add countries / regions** 를 누르고 7개국을 고른다: United States, Canada, Australia, New Zealand, India, Philippines, Singapore. 대한민국은 그대로 둔다. 영국·아일랜드·EU 국가는 **고르지 않는다**(3차).
3. **Save** (Google 문서: 선택한 국가는 현재와 이후의 모든 프로덕션 출시에 포함된다)
4. **Publishing overview** 로 가서 "검토를 위해 보낼 변경사항"에 국가 변경이 있는지 본다. 영어 등록정보·방침 등 함께 보낼 변경을 모은 뒤 **Send changes for review**.
5. 관리형 게시가 켜져 있으면 승인 후 사용자가 **Publish** 를 눌러야 반영된다. 꺼져 있으면 승인되는 대로 반영된다.

### 다시 심사를 받는가

- Google 관리형 게시 문서는 "프로덕션·테스트 트랙에 영향을 주는 **국가 타기팅 변경**"을 Publishing overview에 올라가는 변경으로 적는다. 즉 **국가 추가는 검토 흐름을 거친다고 보고 준비한다.**
- 국가 추가만으로 앱 전체를 처음부터 다시 심사하는지, 검토 시간이 얼마인지는 문서에 없다. 실제로 Console 화면에 "Save"만 나오는지 "Send for review"가 필요한지 **사용자 확인 필요**.
- 토렌트 앱은 "확장 검토" 대상이 될 수 있다(같은 문서). 영어 등록정보와 함께 보내면 심사관이 영어 문구 전체를 새로 읽으므로 2번 금지 표현 점검을 먼저 끝낸다(완료).
- 비공개 테스트(Alpha) 트랙 국가는 따로 설정된다. 영어 UI 빌드를 프로덕션 전에 Alpha에서 확인하려면 Alpha 국가도 필요하면 넓힌다(테스터가 해외에 없으면 불필요).

### 순서 주의

- **국가 추가는 맨 마지막**에 한다. 영어 UI 빌드가 프로덕션에 나가고, 영어 등록정보·방침이 준비된 뒤에 추가해야 영어권 사용자가 한국어 화면이나 실제와 다른 스크린샷을 보지 않는다.
- 국가를 추가해도 새 빌드는 필요 없다. 하지만 영어 UI 빌드가 먼저 나가 있어야 하므로 실제로는 "영어 UI 빌드 출시 → (안정 확인) → 국가 추가" 두 번의 제출이 된다. 한 번에 하려면 영어 UI 빌드·출시 노트·등록정보·국가 추가를 같은 묶음으로 보낸다(PM 결정).

---

## 2. 영어 스토어 등록정보(en-US) 점검

대상: [`launch/listing/en-US.md`](../listing/en-US.md), `launch/store-assets/*/en-US`

### 텍스트 (2026-11-13 영어 UI 기준으로 고침)

| 항목 | 판정 | 내용 |
|---|---|---|
| 앱 이름 `PiecePlay` | 통과 | 9자 |
| 간단한 설명 | 통과 | 76자. 기능 설명만 있다 |
| 자세한 설명 | 통과 | "Korean only" 줄 삭제. 다른 앱에서 링크 열기, 자막(트랙 선택·기기 언어 자동 표시), 설정의 Wi-Fi only를 추가. 화면 이름을 `strings.xml` 영어 문구와 맞춤: Download pieces, Playlist, Stored files, Home, Settings, Ad privacy settings, Play now, Save for later. "토렌트에서 영상 하나만" → "영상과 자막 파일만"(현재 동작). 2196자 |
| 출시 노트 | 통과 | 영어 UI 출시용으로 새로 씀(492자). 실제 출시 빌드의 PATCH_NOTES와 다시 대조 |
| 금지 표현 (`marketing/CLAUDE.md`) | 통과 | 영화·드라마 제목, 토렌트 사이트 이름, "free movies", "latest releases", "unlimited downloads" 없음. Sintel·Big Buck Bunny·Tears of Steel은 스크린샷 출처(CC BY) 표기라 문제없다. "Download up to three videos at once"는 수량 설명이라 괜찮다 |
| 저작권 안내 | 통과 | "PiecePlay does not provide or find content. Please use it only for content you have the right to download and watch." (앱 이용 안내 `guide_rights_body`와 같은 취지) |

### 그래픽·스크린샷

| 자료 | 판정 | 내용 |
|---|---|---|
| `1_Play_Store_Assets/feature_graphic_1024x500_en-US.png` | 통과(파일 존재) | 영어 그래픽 |
| `4_Play_Store_Screenshots/en-US/` 6장, `5_Tablet_7in/en-US`, `6_Tablet_10in/en-US` | **선행 조건** | 위 제목 글자는 영어지만 **휴대폰 안 앱 화면은 한국어**다(예: `01_main.png`의 "바로 재생", "재생 목록", 버전 v.2026.11.12.002). 영어 UI로 다시 찍어야 등록정보가 실제 앱과 맞는다(메타데이터 정책). 설정·자막·링크 확인 시트 장면 추가도 검토. **이번에는 하지 않음**(에뮬레이터 사용 중, PM이 별도 요청 예정) |
| `7_Promo_Video/PiecePlay_promo_en-US.mp4` | 사용자 확인 필요 | 영상 속 화면 언어를 확인하지 않았다. 한국어 화면이면 재촬영 때 같이 다시 만든다 |

### en-GB·en-IN 등 따로 둘 필요가 있나

- 판단: **지금은 en-US 하나로 충분하다. en-IN·en-AU·en-CA·en-SG는 만들지 않는다.** (en-GB는 영국이 3차라 지금은 대상이 아니다.)
- 근거: [번역 문서](https://support.google.com/googleplay/android-developer/answer/9844778)는 사용자 언어와 맞는 번역이 있으면 그것을, 없으면 기본 언어(또는 자동 번역 제안)를 보여 준다고만 적는다. en-IN 등 기기에서 en-US로 대체되는지는 문서에 명시가 없다(미확인).
- 위험: 인도·호주 등의 사용자가 en-US 대신 **기본 언어인 한국어 등록정보**를 볼 가능성이 문서상 배제되지 않는다. 그래서:
  - **방법 A(권장, 출시 후 확인)**: en-US만 넣고, 국가 추가 직후 Play 웹 페이지(`play.google.com/store/apps/details?id=com.gamenuri.pieceplay&hl=en_IN&gl=IN`, `hl=en_AU&gl=AU`, `hl=en_SG&gl=SG`)에서 영어가 보이는지 확인. 한국어가 보이면 해당 en-XX를 en-US와 같은 문구로 추가한다(문구 복사, 5분 작업).
  - 방법 B: 처음부터 en-IN 등에 en-US 문구를 복사해 넣는다. 확실하지만 이후 문구 수정 때 여러 곳을 고쳐야 한다.
- 기본 언어(ko-KR)를 en-US로 바꾸는 안: 1차 8개국 중 7개국이 영어 사용 국가이고 앱 기본 문구도 이제 영어(`values`)이므로 PM이 고려할 수 있다. 한국어 등록정보는 ko-KR 번역으로 그대로 남는다. Console에서 기본 언어를 바꾸면 기존 문구 이동을 다시 확인해야 한다. **PM 결정 사항**으로 둔다.
- 국가별 다른 문구가 필요하면(예: 인도만 다른 스크린샷) **맞춤 스토어 등록정보**(custom store listing, 국가 타기팅)를 쓴다. 1차에는 불필요.

---

## 3. AdMob·UMP 동의

### 어떤 메시지가 필요한가

| 지역 | 규정 | AdMob 메시지 | 1차 판단 | 근거 |
|---|---|---|---|---|
| **미국** | 20개 주 개인정보법(캘리포니아·텍사스·버지니아 등) | **US state regulations** | **게시 권장**. 문서상 "필수"라고는 적혀 있지 않지만 법적 책임은 게시자에게 있다고 적혀 있다 | [AdMob 미국 주 메시지](https://support.google.com/admob/answer/10860309), [미국 주 메시지 만들기](https://support.google.com/admob/answer/10862202) |
| 캐나다·호주·뉴질랜드·인도·필리핀·싱가포르 | 각국 개인정보법(PIPEDA, Privacy Act, DPDP, DPA, PDPA 등) | 해당 AdMob 메시지 없음 | 불필요(방침 고지로 대응, 법률 의견 아님) | — |
| 대한민국 | 개인정보 보호법 | 없음 | 불필요(기존과 같음) | — |
| 영국·아일랜드·EEA·스위스 | UK GDPR·EU GDPR | European regulations | **3차로 미룸.** 이 국가들을 추가하기 **전에 반드시** 게시한다(Google EU 사용자 동의 정책). 게시하지 않은 채 배포하면 UMP가 동의 불필요로 판단해 동의 없이 광고가 나간다 | [AdMob 유럽 규정 메시지](https://support.google.com/admob/answer/10113207), [EU 사용자 동의 정책](https://www.google.com/about/company/user-consent-policy/) |

### 앱 코드 확인 (읽기만, `program/android/app/src/main/java/com/example/torrentstreamplayer/`)

| 확인 | 결과 | 위치 |
|---|---|---|
| 앱 실행마다 동의 정보 갱신 | 통과. `onCreate`에서 `adConsent.gather(this)` | `MainActivity.kt` |
| 동의 창 표시 | 통과. `requestConsentInfoUpdate` 성공 후 `loadAndShowConsentFormIfRequired`. 디버그 지역 설정·테스트 기기 지정이 없어 **실제 기기 위치(IP) 기준**으로 UMP가 판단한다. 1차 국가에서는 동의 창이 뜨지 않는 것이 정상이다. 3차에 유럽 메시지를 게시하면 영국·EU 사용자에게 첫 실행 때 뜬다 | `ui/AdConsent.kt` `gather` |
| 동의 전 광고 요청 차단 | 통과. `MobileAds.initialize`와 배너·전면 광고 로드 모두 `canRequestAds` 확인 뒤 | `ui/TestBannerAd.kt` `TestAdsRuntime.initialize`, `MainActivity.kt` |
| 동의 변경 진입점 | 통과. `privacyOptionsRequirementStatus == REQUIRED`일 때만 "Ad privacy settings"(`action_ad_privacy`) 링크가 Home 하단·Settings "About"에 보이고 `showPrivacyOptionsForm`을 연다. 미국 주 메시지는 이 진입점으로 "Do Not Sell or Share" 창을 띄우는 방식이라 그대로 맞는다 | `ui/AdConsent.kt`, `ui/LegalLink.kt` `LegalLink.AD_PRIVACY` |
| 남은 확인 | 실제 미국 주 위치 동작은 확인하지 못했다. program이 `ConsentDebugSettings`(`DEBUG_GEOGRAPHY_REGULATED_US_STATE`, 3차 전에는 `..._EEA`)로 디버그 빌드에서 확인하는 것을 권장(테스트용 코드만) | — |

### AdMob "Privacy & messaging"에서 사용자가 할 일

AdMob 콘솔(영문 UI 기준 메뉴 이름). 로그인이 필요해 현재 상태는 **사용자 확인 필요**.

**1차(권장)**

1. **Privacy & messaging** 메뉴를 연다. 이미 만든 메시지가 있는지, 게시 상태(Published)를 본다.
2. **US state regulations** 카드 → **Create message**
   1. Apps: PiecePlay
   2. Language: English
   3. Targeting: **All current and future supported US states**
   4. 개인정보처리방침 URL: Notion 방침 주소(영어판을 붙인 뒤) → **Publish**
   5. 이 메시지는 앞에 뜨는 창이 아니라 앱의 "Ad privacy settings" 링크로 여는 "Do Not Sell or Share" 화면이다(앱 구조와 맞음)
3. **AdMob > Apps > app-ads.txt** 가 "Authorized"인지 확인(개발자 웹사이트 `https://kywood.github.io` 준비됨).

**3차(영국·아일랜드·EU 추가 전, 필수)**

4. **European regulations** 카드 → **Create message**
   1. Apps: PiecePlay / Languages: English(기본)·Korean, 3차에 번역한 언어 추가
   2. Targeting: **Countries subject to GDPR (EEA, UK, and Switzerland)**
   3. User choice: **Consent** / **Manage options**("Do not consent" 버튼 추가 여부 결정. 영국 ICO는 거부를 동의만큼 쉽게 하도록 권장)
   4. 방침 URL, 광고 파트너 기본값 → **Publish**
5. EU 배포 전 Play Console의 **DSA 판매자(trader) 신고** 요구 여부 확인(5절 표 참고)

---

## 4. 개인정보처리방침 영어판

- 확인: 2026-11-13, Notion 방침 페이지를 끝까지 스크롤해 읽었다. 제목 "PiecePlay: Torrent Player", 시행일 2026-11-12, 1~9절 **전부 한국어**이고 **영어 내용은 없다.**
- 초안: [`launch/listing/privacy-policy-en.md`](../listing/privacy-policy-en.md). 한국어 1~9절 번역 + 미국 주법 거부·국외 이전 절, 영국 권리 절(3차 대비로 남김, 이유는 초안 문서에 적음). 문의 이메일은 자리 표시자(`[CONTACT EMAIL]`)로 두었다.
- 게시 방식: 앱의 방침 링크는 언어와 상관없이 같은 주소 하나(`res/values/strings.xml` `url_privacy_policy`, `translatable="false"`)다. 그래서 **같은 Notion 페이지에 한국어 아래 영어를 붙이는 방식**을 권장한다. 페이지를 나누면 앱 코드가 바뀌어야 한다.
- 한국어 원문에 빠진 것: 설정(Wi-Fi 전용, 자막) 저장 항목. 영어판과 함께 한국어 2절도 한 줄 고치는 것을 권장.

---

## 5. 데이터 보안·콘텐츠 등급·타깃 연령

| 양식 | 국가 추가 시 바뀌는가 | 판단 | 근거 |
|---|---|---|---|
| 데이터 보안 | **아니오** | 패키지당 하나, 국가 구분 없음. 앱 동작이 그대로면 답도 그대로. 영어화·자막·설정 추가로 새로 수집하거나 기기 밖으로 보내는 데이터는 없다(설정은 기기 저장) | [데이터 보안](https://support.google.com/googleplay/android-developer/answer/10787469) |
| 콘텐츠 등급 | **설문을 다시 하지 않는다** | 설문 하나로 여러 등급 기관의 등급이 자동으로 나온다. 앱 내용이 바뀔 때만 다시 한다. 국가별로 보이는 등급: 미국·캐나다 **ESRB**, 호주(게임 아닌 앱)는 **IARC Generic**, 뉴질랜드·인도·필리핀·싱가포르는 참여 기관이 없는 국가로 **IARC Generic**이 붙는다고 추정, 한국은 지금과 같은 Google Play 등급 | [콘텐츠 등급](https://support.google.com/googleplay/android-developer/answer/9898843), [등급 기관](https://support.google.com/googleplay/android-developer/answer/188189) |
| 등급 거부 | 사용자 확인 필요 | 어떤 기관이 분류를 거부하면 그 지역에서만 앱이 내려간다. 국가 추가 후 Console의 **App content > Content ratings** 에서 국가별 등급이 모두 나왔는지 확인 | 같음 |
| 타깃 연령 | **아니오** | 18세 이상 그대로. 국가별 설정이 아니다 | — |
| 광고 포함·광고 ID | 아니오 | 그대로 | — |
| 인도·싱가포르 특이사항 | 없음(양식 기준) | 전용 등급 기관이나 Play 양식은 없다. 인도 DPDP 법(2023)·싱가포르 PDPA의 고지 의무는 방침으로 대응(법률 의견 아님) | — |
| EU DSA 판매자 신고 | **3차로 미룸** | 1차에 EU 국가가 없어 해당 없음. 3차(아일랜드·EU) 추가 전에 Console이 요구하는지 확인 | marketing 문서 3절 "미확인" 항목 |

---

## 6. 앱 쪽 선행 조건

| 항목 | 판정 | 담당 | 내용 |
|---|---|---|---|
| 앱 화면 영어화 | **main 병합됨**(PR #163, #164, #170) / 프로덕션 출시 전 | program | 기본 `values` = 영어, `values-ko` = 한국어. **이 빌드가 프로덕션에 나가기 전에는 1차 국가를 추가하지 않는다.** |
| 영어 스크린샷 재촬영 | 출시 전 필수 | launch(도구 `store-assets/tools/screens.py`) | 영어 UI 빌드로 휴대폰·7인치·10인치 각 6장, 그래픽·홍보 영상 화면 확인. 에뮬레이터가 비면 PM이 별도 요청 |
| UMP 디버그 지역 확인 | 출시 전 권장 | program | 디버그 빌드에서 미국 주 지역으로 "Ad privacy settings"가 보이는지 확인(EEA는 3차 전) |
| LGPL LibVLC 교체 | **권장(필수 아님)** | program·PM | 현재 오픈소스 라이선스 화면으로 고지 의무는 다루고 있다. 국가가 늘어 노출이 커지므로 LGPL 의무(라이선스 사본·재링크 가능성) 점검 또는 교체 검토를 권장 항목으로만 둔다 |

---

## 7. 출시 후 확인

| 시점 | 어디서 | 무엇을 | 기준 |
|---|---|---|---|
| 국가 추가 당일 | Play 웹 (`hl=en&gl=US`, `hl=en_IN&gl=IN`, `hl=en_AU&gl=AU`, `hl=en_SG&gl=SG`) | 등록정보가 영어로 보이는지, 국가에서 설치 가능한지 | 한국어가 보이면 해당 en-XX 추가(2절) |
| 당일 | AdMob > Privacy & messaging | 미국 주 메시지 상태 Published | — |
| 1주·4주 | Play Console > Statistics (국가별) | 국가별 신규 설치, 유지율, 제거 | 미국·인도·필리핀 비중 |
| 1주·4주 | Play Console > Android vitals (국가 필터) | 비정상 종료율·ANR 비율 | Play 기준치(크래시 1.09%, ANR 0.47%) 아래. 인도·필리핀 저사양 기기에서 따로 본다 |
| 1주·4주 | Play Console > Ratings and reviews (국가·언어 필터) | 영어 리뷰 경향 | 답변 문안은 launch, 게시는 사용자 |
| 1주·4주 | AdMob > Reports (Country 차원) | 국가별 노출수·eCPM·수익 | 미국·캐나다·호주·싱가포르 eCPM 대 인도·필리핀 |
| 매주 | AdMob 정책 센터, Play 정책 상태 | 정책 경고 | 국가가 늘면 권리자 신고 접점도 는다 |
| 4주 | marketing 보고 | 2차 국가(스페인어권·브라질·인도네시아·베트남) 진행 판단 자료 | PM 결정 |

---

## 사용자가 직접 할 일 (순서대로)

전제: 영어 UI 빌드가 준비된 뒤 국가를 추가한다. 1~3은 지금 해도 된다.

1. [ ] **Notion**: 방침 페이지 한국어 아래에 [`privacy-policy-en.md`](../listing/privacy-policy-en.md) 본문 붙여 넣기, `[CONTACT EMAIL]`·시행일 채우기, 맨 위 안내 줄 추가
2. [ ] **AdMob > Privacy & messaging**: US state regulations 메시지(모든 지원 주, 영어) 만들고 게시 (권장)
3. [ ] **AdMob > Apps > app-ads.txt**: 상태 확인
4. [ ] (program·PM) 영어 UI 빌드를 Alpha → 프로덕션(현재 국가 그대로)으로 출시
5. [ ] (launch) 영어 UI로 스크린샷 다시 찍기 — 별도 요청 후
6. [ ] **Play Console > Grow users > Store presence > Main store listing > (번역) English (United States)**: `launch/listing/en-US.md` 문구 입력, 새 영어 스크린샷·그래픽 업로드
7. [ ] **Play Console > Policy > App content**: 데이터 보안·타깃 연령·광고는 그대로인지만 보기
8. [ ] **Play Console > Test and release > Production > Countries / regions**: 7개국(미국·캐나다·호주·뉴질랜드·인도·필리핀·싱가포르) 추가 → Save
9. [ ] **Publishing overview**: 바뀐 내용(국가, 영어 등록정보) 확인 → Send changes for review → (관리형 게시면) 승인 후 Publish
10. [ ] 승인 후 **App content > Content ratings**: 국가별 등급(ESRB·Generic) 확인
11. [ ] 당일: 인도·호주·싱가포르 Play 페이지 언어 확인 → 결과를 launch에 알려 주기
12. [ ] 1주·4주: 7절 표 지표를 marketing·launch에 공유

3차(영국·아일랜드·EU) 전에 따로: AdMob European regulations 메시지 게시, DSA 판매자 신고 확인, 방침 영국·EU 절 점검, en-GB 표시 확인.

## 확인하지 못한 것

- Play Console·AdMob 현재 상태(로그인 필요): 기존 Privacy & messaging 메시지 유무, 관리형 게시 사용 여부, 국가 추가 시 실제 검토 버튼
- en-IN·en-AU·en-SG 기기에서 en-US 등록정보가 대체 표시되는지(문서에 명시 없음)
- 미국 주 위치에서 실제 "Ad privacy settings" 표시(실기기·디버그 지역 미확인)
- 홍보 영상 en-US의 화면 언어
- 각국 개인정보법 대응의 충분성(법률 검토 아님)
