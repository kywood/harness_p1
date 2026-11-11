# 시장·경쟁 분석: Android 토렌트 스트리밍 앱

조사일: 2026-11-12. Google Play 미국 스토어(`hl=en&gl=us`) 등록정보 페이지를 브라우저로 열어 확인한 값만 적었다. 평점·리뷰 수·설치 수는 조사 시점 값이며 바뀔 수 있다. 리뷰 내용은 등록정보 첫 화면에 보인 "도움이 됨" 상위 리뷰 몇 개를 요약한 것이라 전체 여론을 대표하지 않는다(표본 작음). 한국 스토어는 확인하지 않았다.

## 1. 비교 대상

| 앱 | 개발사 | 평점(리뷰 수) | 설치 | 광고·결제 | 성격 |
|---|---|---|---|---|---|
| [µTorrent](https://play.google.com/store/apps/details?id=com.utorrent.client&hl=en&gl=us) | bittorrent.com | 4.7 (5.42M) | 100M+ | 광고, 인앱 결제, Pro $2.99 | 범용 클라이언트 + 내장 플레이어 |
| [BitTorrent](https://play.google.com/store/apps/details?id=com.bittorrent.client&hl=en&gl=us) | bittorrent.com | 4.5 (953K) | 50M+ | 광고, 인앱 결제, Pro $2.99 | 범용 클라이언트 + 내장 플레이어 |
| [Flud](https://play.google.com/store/apps/details?id=com.delphicoder.flud&hl=en&gl=us) | Delphi Softwares | 4.6 (455K) | 10M+ | 광고, 인앱 결제 | 범용 클라이언트, 순차 다운로드 |
| [LibreTorrent](https://play.google.com/store/apps/details?id=org.proninyaroslav.libretorrent&hl=en&gl=us) | proninyaroslav | 3.8 (8.34K) | 1M+ | 없음(오픈소스) | 범용 클라이언트, 순차 다운로드 스트리밍, libtorrent4j 기반 |
| [Amnis - Torrent Player](https://play.google.com/store/apps/details?id=com.amnis&hl=en&gl=us) | Nir Halay | 3.6 (677) | 100K+ | 표시 없음 | **스트리밍 전용** 플레이어 |
| [TVS - Torrent Video Streaming](https://play.google.com/store/apps/details?id=org.itsasoftware.tvs&hl=en&gl=us) | Itsasoftware | 4.2 (28) | 1K+ | 광고, 인앱 결제(광고 제거) | **스트리밍 전용** |

참고: [Play 검색 "torrent stream player"](https://play.google.com/store/search?q=torrent%20stream%20player&c=apps&hl=en&gl=us)에 스트리밍 전용 앱이 20개 넘게 보이지만 대부분 평점 3점대이거나 평점이 없다. 범용 클라이언트(µTorrent·BitTorrent·Flud)가 설치 수를 압도하고, 스트리밍 전용 앱은 작은 시장이다.

## 2. 주요 기능 비교

등록정보 설명에 적힌 것만 표시했다(○ 있음, – 설명에 없음).

| 기능 | µTorrent | BitTorrent | Flud | LibreTorrent | Amnis | TVS | **PiecePlay** |
|---|---|---|---|---|---|---|---|
| 받으면서 재생 | ○ | ○ | 순차 다운로드 | ○ | ○ | ○ | ○ (재생 위치 중심 우선 다운로드) |
| 탐색 시 그 구간 먼저 받기 | – | – | – | – | ○ | – | ○ |
| 내장 플레이어 | ○ | ○ | – | – | ○ | ○ | ○ (LibVLC) |
| 자막 | – | – | – | – | ○ 원클릭 자막 다운로드·파일 | ○ 다국어 | **없음** |
| Chromecast | – | – | – | – | ○ | ○ | **없음** |
| Wi-Fi 전용 | ○ | ○ | ○ | 네트워크 설정 | – | – | **없음** |
| 브라우저 마그넷 링크 인식 | ○ | – | ○ | – | – | – | **없음** |
| `.torrent` 파일 열기 | ○ | ○ | ○ | ○ | – | ○ (기기 파일) | HTTP(S) 링크만 |
| 파일 선택 | ○ | ○ | ○ | ○ | – | – | 자동(가장 큰 영상 하나) |
| 저장 위치 선택·내보내기 | ○ | ○ | ○ (이동) | ○ (자동 이동) | – | ○ (저장 옵션) | **없음**(앱 전용 저장소) |
| 백그라운드·알림 | – | – | – | ○ | – | ○ 스트림 제어 알림 | **없음** |
| 업로드·시딩 | 함 | 함 | 함 | 함 | – | – | **하지 않음** |
| 다국어 | 4개 언어 | 4개 언어 | – | 35+ | – | – | 한국어만 |
| Android TV | – | – | – | ○ | ○ | – | 없음 |
| 광고 제거 결제 | Pro | Pro | 유료판 | – | – | ○ | 없음 |

## 3. 리뷰에서 반복되는 불만 (표본 요약)

- **광고가 너무 많고 끼어든다** (µTorrent·BitTorrent 상위 리뷰 다수): 전체 화면 광고가 수십 초 이어지거나, 다운로드 도중에 뜨거나, 광고 때문에 앱이 죽어 다운로드를 다시 시작해야 했다는 내용. 개발사 답변은 "Pro로 광고 제거".
- **배터리 소모** (Flud 리뷰): 배터리를 많이 쓴다는 지적. µTorrent는 Pro 기능으로 배터리 절약을 내세운다.
- **모바일 데이터 시딩 걱정** (BitTorrent 리뷰): 모바일 데이터에서 시딩되지 않도록 사용자가 직접 토렌트를 지워야 한다는 조언.
- **스트리밍 품질** (Amnis 리뷰): 영상이 끊기고 회색 깨짐이 생긴다, 화면 확대 시 자막이 화면 밖으로 나간다.
- **받은 파일을 밖으로 못 꺼냄** (Amnis 리뷰): 다 받은 파일을 다운로드 폴더에 저장하거나 외부 플레이어로 열고 싶다는 요청.
- **앱이 꺼져 있을 때 마그넷 링크를 열면 정보를 못 받음** (LibreTorrent 리뷰): 외부 링크 진입 시 첫 실행 처리 버그.
- **권한 요구에 대한 거부감** (µTorrent 리뷰): 위치 정보를 주지 않으면 다운로드가 안 되는 것 같다는 불만.

## 4. 광고 방식

| 앱 | 방식(등록정보·리뷰 기준) |
|---|---|
| µTorrent·BitTorrent | 배너 + 전면(동영상 포함), 유료 Pro로 제거. 리뷰 불만의 1순위 |
| Flud | 광고 있음, 유료판 제거. 리뷰는 "광고 위치가 덜 거슬린다"는 평 |
| TVS | 광고 + 인앱 결제로 제거 |
| LibreTorrent | 광고 없음(오픈소스) |
| **PiecePlay** | 배너는 메인·재생 목록만, 전면은 재생 종료·완료 시 5분 이상 시청 또는 5회 완료 + 10분 쿨다운. 재생·준비 중에는 광고 없음 |

→ 경쟁 앱의 가장 큰 불만이 "작업 도중 끼어드는 광고"다. PiecePlay의 광고 규칙은 이 불만을 정면으로 피한다. 스토어 설명에서 "재생 중 광고 없음"을 강점으로 내세울 만하다(launch 검토). 광고 제거 결제는 경쟁 앱 대부분이 제공한다 → [requests/remove-ads-purchase.md](../requests/remove-ads-purchase.md).

## 5. 데이터 보안 신고 비교 (등록정보의 Data safety 요약)

| 앱 | 공유 | 수집 | 암호화 | 삭제 요청 |
|---|---|---|---|---|
| µTorrent·BitTorrent | 위치, 기기 ID | 없음으로 신고 | 전송 암호화 안 함 | 불가 |
| Flud | 없음 | 앱 활동, 앱 정보·성능, 기기 ID | 전송 암호화 | 불가 |
| LibreTorrent | 없음 | 없음 | — | — |
| Amnis | 위치, 앱 정보·성능, 기기 ID | 없음으로 신고 | 전송 암호화 | 불가 |
| TVS | 위치, 앱 활동 외 2개 | 없음으로 신고 | 전송 암호화 | — |

→ PiecePlay는 앱 자체 수집이 없고 기록을 기기 안에 암호화한다. 광고 SDK 항목만 신고하면 되므로 "위치 공유"가 없는 짧은 신고가 가능할 수 있다(실제 AdMob 신고 항목은 launch가 확인). 이것도 차별점이다.

## 6. Play 정책 위험

### 정책 원문 요지
[Google Play 지식재산권 정책](https://support.google.com/googleplay/android-developer/answer/9888072?hl=en)
- 지식재산권을 침해하거나, 침해를 **부추기거나 유도하는** 앱을 허용하지 않는다.
- "저작권 침해 조장"의 흔한 위반 사례로 다음을 든다.
  - 저작권 있는 콘텐츠를 허가 없이 로컬에 내려받게 하는 스트리밍 앱
  - 저작권 있는 음악·영상을 스트리밍·다운로드하도록 부추기는 앱. 특히 **등록정보 설명과 스크린샷**이 무단 다운로드를 부추기는 경우를 예시로 든다.
- 권장 사항 표에는 "무단 스트리밍처럼 침해를 부추기거나 돕는 앱을 만들지 말라"가 있다.

### 심사 사례
- 2014년 12월 Google은 Pirate Bay 관련 앱(The Pirate Bay Proxy, Premium, Mirror, PirateApp)을 지식재산권·사칭 조항 위반으로 삭제했고, 개발자는 정책 위반 경고(strike)를 받았다. DMCA 신고 없이 선제 조치였고 이의 신청도 거절됐다고 보도됐다([9to5Google](https://9to5google.com/2014/12/05/google-pirate-bay-android-removal/), [Digital Trends](https://digitaltrends.com/mobile/google-removes-pirate-bay-apps-from-play-store)).
- 반면 범용 토렌트 클라이언트(µTorrent 100M+, BitTorrent 50M+, Flud 10M+, 오픈소스 LibreTorrent)는 지금도 정상 등록되어 있다. **검색·사이트 접근 기능 없이 사용자가 가져온 링크만 처리하는 클라이언트는 허용, 특정 불법 사이트 접근·무단 콘텐츠 홍보는 제재**로 정리할 수 있다(planning 해석, 법률 검토 아님).
- BitTorrent·µTorrent는 설명에 "라이선스를 받은 파트너 콘텐츠"를 함께 내세우고, 이용약관·개인정보처리방침 링크를 설명 본문에 둔다.

### PiecePlay에 대한 위험과 대응

| 위험 | 지금 상태 | 대응 |
|---|---|---|
| 스크린샷·설명이 저작권 콘텐츠를 보여 줌 | 스크린샷 교체 진행 중(#49). 코드 미리보기 파일명은 공개 영상(Big Buck Bunny)을 쓴다 | 스크린샷·그래픽에 영화·드라마 제목·포스터·장면을 쓰지 않는다. 공개 라이선스 영상만 쓴다(launch #49에 전달) |
| "무료 영화" 같은 설명 문구 | 미확인(등록정보 문안은 launch 소관) | 설명에서 "무료 영화·드라마" 표현 금지, "내가 가진 권리의 영상" 표현 사용 |
| 앱 안 저작권 안내 없음 | 없음 | [requests/policy-notice-and-privacy.md](../requests/policy-notice-and-privacy.md) |
| 개인정보처리방침 없음 | 없음 | 같은 문서 |
| 검색·사이트 연결 기능 | 없음(좋음) | 앞으로도 토렌트 검색·사이트 바로가기를 넣지 않는다 |
| 다운로드한 파일을 기기에 저장(로컬 사본) | 앱 전용 저장소에만 있음 | 내보내기 기능([requests/export-to-device.md](../requests/export-to-device.md))은 정책 문구 "로컬 사본"과 맞닿으므로 출시 후, 이용 안내와 함께 검토 |

## 7. PiecePlay의 차별점

1. **재생 위치 중심 다운로드**: 탐색하면 그 구간을 먼저 받는다. 범용 클라이언트의 "순차 다운로드"는 앞에서부터만 받아 중간 탐색에 약하다. 스트리밍 전용 앱 중 이 점을 내세우는 것은 Amnis뿐이었다.
2. **업로드·시딩을 하지 않음, 완료 즉시 공유 중단**: 모바일 데이터 걱정(BitTorrent 리뷰)과 법적 부담을 줄인다. 경쟁 앱 중 이를 내세운 곳은 없었다.
3. **재생 중 광고 없음**: 경쟁 앱 최대 불만을 피한다.
4. **기기 안 암호화 저장, 앱 자체 수집 없음**: 데이터 보안 신고가 짧다.
5. **조각 맵·받은 구간 표시**: 어디까지 볼 수 있는지 보인다.
6. **한 화면에 집중한 단순한 흐름**: 링크 → 바로 재생. 파일 선택·설정 없이 시작한다.

## 8. PiecePlay의 부족한 점 (경쟁 대비)

| 부족한 점 | 갖춘 경쟁 앱 | 연결된 요구사항 |
|---|---|---|
| 자막 | Amnis, TVS | [subtitles-audio-tracks.md](../requests/subtitles-audio-tracks.md) (외부 자막 검색은 범위 밖으로 둠) |
| Chromecast·TV | Amnis, TVS, LibreTorrent(TV) | [cast-to-tv.md](../requests/cast-to-tv.md) **신규** |
| 브라우저 마그넷 링크 인식, `.torrent` 파일 열기 | µTorrent, Flud, TVS | [open-links-from-other-apps.md](../requests/open-links-from-other-apps.md) |
| Wi-Fi 전용 | µTorrent, BitTorrent, Flud | [mobile-data-guard.md](../requests/mobile-data-guard.md) |
| 백그라운드 다운로드·알림 | LibreTorrent, TVS | [background-download.md](../requests/background-download.md) |
| 받은 파일 내보내기·외부 플레이어 | µTorrent, BitTorrent, Flud, TVS | [export-to-device.md](../requests/export-to-device.md) **신규** |
| 광고 제거 결제 | µTorrent, BitTorrent, Flud, TVS | [remove-ads-purchase.md](../requests/remove-ads-purchase.md) **신규** |
| 다국어 | LibreTorrent(35+), µTorrent(4) | [english-localization.md](../requests/english-localization.md) |
| 배터리 절약 | µTorrent Pro | [background-download.md](../requests/background-download.md)에 함께 검토 |
| 토렌트 안 파일 직접 선택 | 범용 클라이언트 전부 | 제안하지 않음: "링크 → 바로 재생" 단순함이 차별점이라, 자막 파일 자동 포함(자막 요구사항 2단계)까지만 권한다 |

## 9. 결론 (planning 의견)

- 시장은 범용 클라이언트가 장악하고, 스트리밍 전용은 평점 낮은 작은 앱이 흩어져 있다. 스트리밍 전용으로 **평점 4점대 + 깔끔한 광고**를 유지하면 이 분야 상위권에 들 여지가 있다.
- 출시 전에는 정책 위험(개인정보처리방침·이용 안내·스크린샷)과 첫 실패 복구를 먼저 막는다.
- 출시 후 경쟁 격차가 큰 순서: 자막 → 외부 링크 열기 → Wi-Fi 전용 → Chromecast → 백그라운드 다운로드.

## 출처

- Google Play 등록정보(2026-11-12 확인): [µTorrent](https://play.google.com/store/apps/details?id=com.utorrent.client&hl=en&gl=us), [BitTorrent](https://play.google.com/store/apps/details?id=com.bittorrent.client&hl=en&gl=us), [Flud](https://play.google.com/store/apps/details?id=com.delphicoder.flud&hl=en&gl=us), [LibreTorrent](https://play.google.com/store/apps/details?id=org.proninyaroslav.libretorrent&hl=en&gl=us), [Amnis](https://play.google.com/store/apps/details?id=com.amnis&hl=en&gl=us), [TVS](https://play.google.com/store/apps/details?id=org.itsasoftware.tvs&hl=en&gl=us), [Play 검색 결과](https://play.google.com/store/search?q=torrent%20stream%20player&c=apps&hl=en&gl=us)
- 정책: [Google Play 지식재산권 정책](https://support.google.com/googleplay/android-developer/answer/9888072?hl=en)
- 심사 사례: [9to5Google (2014-12-05)](https://9to5google.com/2014/12/05/google-pirate-bay-android-removal/), [Digital Trends](https://digitaltrends.com/mobile/google-removes-pirate-bay-apps-from-play-store)

## 결정 필요

- 스토어 설명에 "재생 중 광고 없음", "업로드하지 않음"을 강점으로 쓸지(launch·PM)
- 한국 스토어 기준 경쟁 조사를 추가할지(이번 조사는 미국 스토어)
- 광고 제거 결제를 도입할지(수익 모델 결정)
