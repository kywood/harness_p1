# 앱 이름 PiecePlay → PiecePlayer 변경 검토 (2026-11)

- 작성: 2026-11-14, marketing (#183). 사용자 질문: "앱 이름을 PiecePlay에서 PiecePlayer로 바꾸는 것을 마케팅은 어떻게 생각하는가."
- 조사만 했다. Play Console, 스토어 설정, 코드는 건드리지 않았다.
- **법률 검토가 아니다.** 상표 부분은 공개 검색으로 본 범위의 marketing 의견이며, 상표 등록·침해 여부 판단은 변리사 확인이 필요하다.
- 근거 표기:
  - **직접 확인**: 페이지나 API 응답을 직접 열어 읽었다(모두 2026-11-14).
  - **2차**: 검색 요약, 업계 블로그로만 확인했다.
  - **미확인**: 근거를 찾지 못했거나 열지 못했다.

---

## 요약

| 항목 | 결론 |
|---|---|
| 권장안 | **절충: 브랜드는 PiecePlay로 두고, 제목에 "Player"를 넣는다.** 한국어는 지금 제목(`PiecePlay - 토렌트 플레이어`) 그대로, 영어는 `PiecePlay: Torrent Player`(25자)로 정한다 |
| 가장 큰 이유 | "Player"를 브랜드에 붙여 한 단어(`PiecePlayer`)로 만들어도 "player" 검색어와 따로 맞는다는 근거가 없다. 부제에 띄어 쓴 "Torrent Player"가 같은 효과를 더 확실히 낸다. 그러면 바꿔서 얻는 것은 "이름만 봐도 플레이어"라는 직관성 하나인데, 이것도 부제가 이미 해 준다 |
| 충돌 | Play·App Store·GitHub·웹 검색에서 `PiecePlayer`, `Piece Player`, `PiecePlay`라는 이름의 다른 앱·서비스는 **찾지 못했다**(본 범위 안에서). `pieceplay.com`은 2020년부터 다른 사람이 등록해 두었고, `pieceplayer.com`은 등록돼 있지 않다. 상표 DB(USPTO, KIPRIS)는 **미확인** |
| 바꿔도 되는가 | 된다. 충돌이 보이지 않고, 설치·리뷰가 거의 없고, 영어 자료를 다시 만드는 지금이 비용이 가장 적다. 다만 "꼭 바꿔야 할 이유"도 찾지 못했다 |
| 바꾼다면 | 1차 글로벌 출시 빌드에 앱 아이콘 아래 이름을 함께 넣고, 스토어 제목·그래픽·영상·방침·웹사이트를 같은 날 맞춘다(5.3절 순서) |

---

## 1. 이름 자체

| 기준 | PiecePlay | PiecePlayer |
|---|---|---|
| 뜻 전달(영어) | "조각을 재생한다"로 읽힌다. 토렌트의 piece를 아는 사람에게는 뜻이 맞지만, 모르는 사람에게는 퍼즐·보드게임 이름처럼 들린다 | "조각 플레이어". 끝의 Player 덕에 **재생 앱**이라는 점이 바로 읽힌다 |
| 뜻 전달(한국) | "피스플레이". 무슨 앱인지는 이름만으로 알기 어렵다 | "피스플레이어". "플레이어"가 한국어에서도 재생 앱을 뜻해 더 쉽게 읽힌다 |
| 발음·길이 | 2음절(영어), 5음절(한국어). 짧다 | 3음절(영어), 6음절(한국어). 한 박자 길다 |
| 기억 | 짧고 운이 맞는다(P-P). 브랜드처럼 들린다 | 흔한 "○○Player" 꼴이라 기억에는 덜 남는다. MX Player, KMPlayer, PLAYit 같은 이름 사이에 섞인다 |
| 오타·혼동 | "Peace"와 소리가 같다(Peace Play). 사용자가 "PiecePlayer"로 잘못 부를 가능성이 있다. 이 저장소 작업 폴더 이름부터 `piecePlayer`다 | "Peace Player", "Piece Player"(띄어쓰기) 혼동은 똑같이 남는다 |
| "Player"의 다른 뜻 | 해당 없음 | 영어 "player"는 게이머·선수라는 뜻도 있다. Play에서 "piece player"를 검색하면 One Piece 게임, Court Piece 카드게임, 음악 플레이어가 나온다(3.1절) |
| 패키지 이름과의 관계 | `com.gamenuri.pieceplay`와 같다 | 패키지 이름과 다르지만 사용자에게는 거의 보이지 않아 문제는 아니다 |

판단:
- **직관성은 PiecePlayer가 조금 낫다.** 이름만 따로 떨어져 보일 때(홈 화면 아이콘 아래, 입소문)가 그렇다.
- **브랜드로서는 PiecePlay가 조금 낫다.** 짧고, 다른 플레이어 앱 이름과 덜 겹친다.
- 스토어에서는 제목이 항상 부제와 함께 보인다. `PiecePlay - 토렌트 플레이어`, `PiecePlay: Torrent Player`라고 쓰면 직관성 차이는 대부분 사라진다.
- 차이가 나는 곳은 **홈 화면 아이콘 아래 이름**과 **말로 전할 때**뿐이다. 두 이름의 차이는 작다.

---

## 2. 검색(ASO)

### 2.1 "Player"를 브랜드에 붙이는 것과 부제에 넣는 것

- Play 제목은 30자까지다. 정책은 제목이 앱 기능을 정확히 나타내야 하고, 반복되거나 무관한 키워드, 이모지, 순위·가격 문구를 금지한다([Play 메타데이터 정책](https://support.google.com/googleplay/android-developer/answer/9898842?hl=en), 직접 확인).
- 제목의 키워드가 설명보다 검색에 더 크게 반영된다는 것은 업계 통설이다. Google이 가중치를 공개한 적은 없고 측정 자료도 찾지 못했다(2차: [AppFollow](https://appfollow.io/blog/app-store-optimization-title), [App Radar](https://appradar.com/academy/app-title)).
- **`PiecePlayer`처럼 붙여 쓴 한 단어 안의 "player"가 검색어 "player"와 맞는지는 미확인이다.** 반대 방향의 관찰은 하나 있다. 지금 제목 `PiecePlay - 토렌트 플레이어`는 한국 스토어에서 "PiecePlay" 검색에는 단독으로 나오지만 "PiecePlayer" 검색 결과(보이는 19개)에는 없다(3.1절, 직접 확인). 붙여 쓴 단어는 통째로 한 단어처럼 다뤄지는 것으로 보인다.
- 부제에 띄어 쓴 `Torrent Player`는 "torrent player"라는 검색 문구와 그대로 맞는다. 미국 스토어 "torrent player" 검색 1위가 `Amnis - Torrent Player`로, 같은 "브랜드 - Torrent Player" 꼴이다([검색 결과](https://play.google.com/store/search?q=torrent%20player&c=apps&hl=en&gl=us), 직접 확인).

검색어별 정리:

| 검색어 | `PiecePlayer`(브랜드에 포함) | `PiecePlay: Torrent Player`(부제) |
|---|---|---|
| torrent player | "torrent"가 제목에 따로 없으면 약하다. 부제를 또 붙여야 한다 | 문구가 그대로 들어 있다 |
| video player | "video"가 없다. 경쟁도 매우 세다(MX Player 등). 어느 쪽이든 제목으로 노릴 검색어가 아니다 | 같다 |
| magnet player | "magnet"이 없다 | "magnet"은 간단한 설명에서 받는다 |
| 브랜드 검색 | 새 이름이라 처음부터 다시 쌓는다. "PiecePlay"로 찾던 사람이 계속 찾을 수 있는지는 미확인 | 한국에서는 이미 "PiecePlay" 검색이 우리 앱으로 연결된다 |

- `PiecePlayer`로 바꿔도 "torrent"를 넣으려면 부제가 필요하다. 그런데 `PiecePlayer: Torrent Player`(27자)는 "Player"가 두 번 나와 어색하고, 정책의 "반복 키워드"로 보일 소지가 있다. 그래서 부제를 `Torrent Streaming` 같은 다른 말로 바꿔야 하고, 그러면 정작 주력 검색 문구인 "torrent player"가 제목에서 빠진다.
- 한국어도 같다. 주력 키워드는 자동완성에 뜨는 "토렌트 플레이어"다([`../aso/ko-KR.md`](../aso/ko-KR.md)). `PiecePlayer - 토렌트 플레이어`는 "플레이어"가 영어·한국어로 겹친다.

### 2.2 제목 후보 (30자 이내, 공백 포함)

글자 수는 스크립트로 셌다. Play Console 입력란 카운터로 다시 확인한다. ([`../aso/ko-KR.md`](../aso/ko-KR.md) 3절은 `PiecePlay - 토렌트 플레이어`를 21자로 적었는데 20자가 맞다.)

**브랜드를 PiecePlay로 둘 때**

| 언어 | 제목 | 글자 수 | 비고 |
|---|---|---|---|
| 한국어 | `PiecePlay - 토렌트 플레이어` | 20 | **현재 게시된 제목.** 그대로 둔다 |
| 영어 (권장) | `PiecePlay: Torrent Player` | 25 | 주력 검색 문구가 그대로 들어간다 |
| 영어 | `PiecePlay Torrent Video Player` | 30 | "video player"까지 넣지만 30자를 꽉 채워 읽기 답답하다 |
| 영어 | `PiecePlay: Magnet Player` | 24 | "torrent"를 제목에서 빼 정책 노출을 줄인다. 검색 효과는 작다 |

**브랜드를 PiecePlayer로 바꿀 때**

| 언어 | 제목 | 글자 수 | 비고 |
|---|---|---|---|
| 한국어 | `PiecePlayer - 토렌트 스트리밍` | 22 | "플레이어" 중복을 피한다. 대신 자동완성 키워드 "토렌트 플레이어"가 한글로는 빠진다 |
| 한국어 | `PiecePlayer - 토렌트 바로 재생` | 23 | 차별점 문구. 검색 키워드는 "토렌트"뿐이다 |
| 영어 | `PiecePlayer - Torrent Stream` | 28 | 무난하다 |
| 영어 | `PiecePlayer: Magnet & Torrent` | 29 | 키워드 두 개. `&` 기호 하나는 정책상 문제없어 보이나 launch가 확인한다 |
| 영어 | `PiecePlayer: Torrent Streaming` | 30 | 30자를 꽉 채운다 |

"torrent"를 제목에 넣는 데 따른 정책 노출 판단은 [`../aso/ko-KR.md`](../aso/ko-KR.md) 3절과 같다. 경쟁 앱 다수가 같은 꼴로 등록돼 있다.

---

## 3. 충돌 확인

### 3.1 스토어

| 확인한 곳 | 검색어 | 결과 | 근거 |
|---|---|---|---|
| Google Play 미국 | PiecePlayer | 같은 이름 없음. 보이는 18개는 One Piece 카드게임·TCG 앱, "Piece by Piece" 등 | [검색](https://play.google.com/store/search?q=PiecePlayer&c=apps&hl=en&gl=us), 직접 확인 |
| Google Play 미국 | PiecePlay | 같은 이름 없음. 보이는 20개는 직소 퍼즐 게임(Piece Up!, JigsawPlay, PuzzlePlay, PieceBloom, PiecePic 등). 우리 앱은 미국에 아직 없다 | [검색](https://play.google.com/store/search?q=PiecePlay&c=apps&hl=en&gl=us), 직접 확인 |
| Google Play 미국 | piece player | 같은 이름 없음. One Piece 게임, Court Piece 카드게임, 음악 플레이어(Pi Music Player, Salt Player 등) | [검색](https://play.google.com/store/search?q=piece%20player&c=apps&hl=en&gl=us), 직접 확인 |
| Google Play 한국 | PiecePlay | **우리 앱 `PiecePlay - 토렌트 플레이어` 하나만 나온다** | [검색](https://play.google.com/store/search?q=PiecePlay&c=apps&hl=ko&gl=kr), 직접 확인 |
| Google Play 한국 | PiecePlayer | 같은 이름 없음. 보이는 19개 안에 우리 앱도 없다(One Piece 계열, corgiplayer 등) | [검색](https://play.google.com/store/search?q=PiecePlayer&c=apps&hl=ko&gl=kr), 직접 확인 |
| App Store 미국(iTunes Search API) | pieceplayer / pieceplay / piece player | 각 25·21·25건 중 앞 10건씩만 읽었다. 같은 이름 없음(Fileplay, PLAYit, 퍼즐 게임 등). **나머지 41건은 미확인** | [pieceplayer](https://itunes.apple.com/search?term=pieceplayer&entity=software&limit=25&country=us), [pieceplay](https://itunes.apple.com/search?term=pieceplay&entity=software&limit=25&country=us), [piece player](https://itunes.apple.com/search?term=piece+player&entity=software&limit=25&country=us) |

- Play 검색 페이지는 뒷부분이 잘려 앞 18~20개만 읽었다. 정확히 같은 이름의 앱이 있으면 보통 맨 위에 나오므로 없을 가능성이 높지만, "없다"고 단정하지는 않는다.
- 해석: "Piece"가 들어간 스토어 검색은 **퍼즐 게임과 One Piece**가 차지한다. 어느 이름이든 "Piece"만으로는 동영상 앱 검색과 이어지지 않는다. 그래서 부제의 "Torrent Player"가 필요하다.
- 비슷한 철자로 혼동될 만한 유명 동영상 플레이어 앱은 찾지 못했다. 가장 가까운 것은 이름 꼴이 같은 "○○Player"류(MX Player, PLAYit, corgiplayer)와 퍼즐 앱 PiecePic·PieceBloom인데, 둘 다 업종이나 철자가 달라 혼동 위험은 낮다고 본다.

### 3.2 웹·도메인·GitHub

| 확인한 곳 | 결과 | 근거 |
|---|---|---|
| 웹 검색 "PiecePlayer" | 같은 이름의 앱·서비스 없음. 비슷한 것은 음악 연습 앱 Practice Player, PracticePlay, PieceKeeper | 2차(검색 요약) |
| 웹 검색 "Piece Player" | 같은 이름 없음 | 2차 |
| 웹 검색 "PiecePlay" | 같은 이름의 브랜드 없음. 비슷한 상표 출원으로 게임 소프트웨어 분야의 PUZZLEPLAY가 있다(다른 이름) | 2차([Justia](https://trademark.justia.com/991/48/puzzleplay-99148336.html)) |
| 웹 검색 "피스플레이어"/"피스플레이" | 관련 앱·상표 없음 | 2차 |
| `pieceplay.com` | **등록돼 있다.** 2020-11-05 등록, 2027-11-05 만료. 페이지 내용은 비어 있어 무엇에 쓰는지 알 수 없다(미확인) | [RDAP](https://rdap.verisign.com/com/v1/domain/pieceplay.com), 직접 확인 |
| `pieceplayer.com` | **등록돼 있지 않다**(RDAP 404, DNS 조회 실패) | [RDAP](https://rdap.verisign.com/com/v1/domain/pieceplayer.com), 직접 확인 |
| GitHub 저장소 "pieceplayer" | 0건 | [API](https://api.github.com/search/repositories?q=pieceplayer), 직접 확인 |
| GitHub 저장소 "pieceplay" | 1건. 우리 것(`pieceplay-libvlc`)뿐이다 | [API](https://api.github.com/search/repositories?q=pieceplay), 직접 확인 |

- `.com` 도메인을 쓸 계획이 생긴다면 PiecePlayer 쪽이 유리하다. 지금은 GitHub Pages를 쓰고 있어 당장의 차이는 아니다.

### 3.3 상표

| 데이터베이스 | 결과 |
|---|---|
| USPTO(미국) | **미확인.** 검색 사이트가 스크립트로 동작해 결과를 읽지 못했다. 웹 검색으로 본 제3자 상표 목록 사이트에는 PIECEPLAYER, PIECE PLAYER, PIECEPLAY가 나오지 않았다(2차, 목록이 완전하지 않음) |
| KIPRIS(한국) | **미확인.** 열지 못했다 |
| 그 밖(EUIPO, 인도, 호주 등 1차 출시국) | **미확인** |

- 사용자가 직접 확인할 곳: [USPTO 상표 검색](https://tmsearch.uspto.gov)에서 `pieceplayer`, `piece player`, `pieceplay`, `piece play`, 그리고 [KIPRIS](https://www.kipris.or.kr)에서 같은 영문과 `피스플레이어`, `피스플레이`. 분류는 9류(소프트웨어)와 41·42류를 본다.
- 이름을 바꾸기로 한다면 바꾸기 **전에** 이 확인을 한다. 지금 이름 PiecePlay도 같은 확인을 한 기록이 저장소에 없다.

---

## 4. 바꾸는 비용과 위험

### 4.1 고칠 곳

| 대상 | 내용 | 담당 | 비고 |
|---|---|---|---|
| 스토어 제목(ko-KR, en-US) | Play Console 기본 스토어 등록정보 | launch + 사용자 | 메타데이터 변경이라 심사를 거친다 |
| 간단한 설명·자세한 설명·출시 노트 | `launch/listing/en-US.md`에 "PiecePlay" 10곳, `ko-KR.md`에 3곳 | marketing | |
| 앱 아이콘 아래 이름 | `res/values/strings.xml`의 `app_name` | program | **새 빌드가 필요하다.** 앱 이름을 확인하는 에뮬레이터 테스트(`ExampleInstrumentedTest`)도 함께 고친다 |
| 앱 안 문구 | 영어 `strings.xml` 7곳, 한국어 3곳(이용 안내, 오픈소스 라이선스 안내, 로고 설명) | design·program | |
| 로고·아이콘 | 로고 이미지에 글자가 들어 있는지는 **미확인**. 들어 있으면 다시 만든다 | design | |
| 그래픽 이미지(1024x500) | `launch/store-assets/tools/feature.py`에 이름이 적혀 있다 | marketing | 지금 다시 만드는 중이라 추가 비용이 작다 |
| 스크린샷 | 메인 화면에 로고·제목이 보인다. 영어판은 어차피 다시 찍는다. **한국어판도 다시 찍어야 한다** | marketing | 한국어판 재촬영이 추가 비용이다 |
| 홍보 영상 | `7_Promo_Video/make_video.py`에 4곳 | marketing | 다시 만드는 중 |
| 개인정보처리방침(노션) | 제목과 본문. 앱에 넣어 둔 주소에 `PiecePlay-Torrent-Player`가 들어 있다 | launch + 사용자 | 노션 제목을 바꿨을 때 예전 주소가 계속 열리는지 **미확인**. 열리지 않으면 앱 안 링크와 Play Console의 방침 주소가 깨진다. 바꾸기 전에 반드시 시험한다 |
| 영어 방침 문안 | `launch/listing/privacy-policy-en.md` 2곳 | launch | |
| 개발자 웹사이트(GitHub Pages) | 앱 이름 표기 | 사용자 | 내용은 이번에 열어 보지 않았다(미확인) |
| AdMob 앱 이름 | AdMob에 표시되는 이름 | 사용자 | 스토어에 연결된 앱은 스토어 이름을 따라가는지 **미확인**. 광고 단위 ID는 이름과 무관해 수익에는 영향이 없다 |
| GitHub 저장소 `pieceplay-libvlc` | LGPL 소스 공개용 저장소 | 사용자 | 이름을 바꾸면 앱·문서의 링크도 고쳐야 한다. **바꾸지 않는 편을 권한다** |
| 내부 문서 | 저장소 전체에 "PiecePlay"가 46개 파일 167곳 | 각 팀 | 한꺼번에 바꾸지 않아도 된다. 과거 기록(패치 노트, 지난 점검표)은 그대로 둔다 |
| 바꾸지 않는 것 | `applicationId`(`com.gamenuri.pieceplay`), 코드 식별자(`PiecePlayColorScheme` 등), Gradle 프로젝트 이름, 릴리스 파일 이름 | — | 사용자에게 보이지 않는다 |

### 4.2 위험

| 위험 | 수준 | 설명 |
|---|---|---|
| 기존 사용자 혼동 | 매우 낮음 | 한국 출시 직후이고 설치·리뷰가 거의 없다(정확한 수치는 Play Console에서 확인, 미확인) |
| Play 심사 | 낮음 | 제목 변경은 등록정보 심사를 거친다. 걸리는 시간은 미확인. 이름 자체에는 정책에 걸릴 요소가 없다. 다만 글로벌 출시 제출과 같은 때 넣으면 심사가 한 번으로 끝난다 |
| 검색 순위 | 낮음 | 제목을 바꿔도 등록정보나 설치 기록이 초기화되지는 않는다. 키워드 색인은 새 제목 기준으로 다시 잡힌다(2차, 업계 통설). 쌓인 순위가 거의 없어 잃을 것도 거의 없다. 유일하게 확인된 자산은 한국에서 "PiecePlay" 검색이 우리 앱 하나로 연결된다는 점이고, 이름을 바꾼 뒤에도 유지되는지는 미확인이다 |
| 이름이 섞여 남는 것 | 중간 | 가장 현실적인 위험이다. 스토어는 PiecePlayer인데 앱 아이콘 아래·방침·웹사이트·영상 어딘가에 PiecePlay가 남으면, 스토어 등록정보와 앱이 다르다는 지적을 받을 수 있다. 4.1 표를 빠짐없이 같은 날 맞춰야 한다 |
| 방침 주소 깨짐 | 중간 | 4.1의 노션 항목. 깨지면 Play 정책 위반(방침 링크 접근 불가)이 된다 |
| 상표 | 미확인 | 3.3절. 어느 이름이든 확인이 필요하다 |
| 나중에 바꾸는 비용 | — | 글로벌 출시 뒤에는 번역 등록정보(2차 4개 언어), 리뷰, 외부 글에 이름이 퍼진다. **바꿀 거라면 지금이 가장 싸다**는 전제는 맞다 |

---

## 5. 권장안

### 5.1 결론: 절충 (브랜드는 PiecePlay, 제목에 Player 포함)

- 한국어 제목: `PiecePlay - 토렌트 플레이어` (지금 그대로)
- 영어 제목: `PiecePlay: Torrent Player` (지금 문안 `PiecePlay`에서 변경)
- 앱 아이콘 아래 이름: `PiecePlay` (그대로)

이유:
1. **검색 효과가 같거나 더 낫다.** "Player"를 부제에 띄어 쓰면 "torrent player"와 그대로 맞는다. 브랜드에 붙이면 맞는지 알 수 없고, 부제에서는 "Player"를 다시 쓸 수 없어 주력 문구가 제목에서 빠진다(2.1절).
2. **직관성 문제도 부제가 해결한다.** 스토어에서 이름은 늘 부제와 함께 보인다.
3. **비용이 거의 없다.** 한국어 스토어·스크린샷·방침·웹사이트를 건드리지 않는다. 영어 제목 한 줄만 바뀐다. 새 빌드도 필요 없다.
4. **이미 가진 것을 지킨다.** 한국에서 "PiecePlay" 검색이 우리 앱 하나로 연결된다.
5. PiecePlay가 브랜드로 더 짧고, "○○Player" 이름들 사이에서 덜 묻힌다.

이 권장의 약한 점도 적는다. 두 이름의 차이는 작다. 홈 화면 아이콘 아래와 입소문에서는 PiecePlayer가 조금 더 알기 쉽다. 사용자가 PiecePlayer라는 이름을 더 좋아한다면 **바꿔도 마케팅상 손해는 없다.** 충돌도 보이지 않았다. 그 경우에는 5.3절대로 한다.

### 5.2 권장안대로 할 때 할 일

1. `launch/listing/en-US.md`의 앱 이름을 `PiecePlay: Torrent Player`로 고친다(marketing, PM 승인 뒤).
2. `launch/listing/ko-KR.md`의 앱 이름이 아직 `PiecePlay`로 적혀 있다. 게시된 제목 `PiecePlay - 토렌트 플레이어`와 맞춘다(marketing).
3. launch가 1차 글로벌 제출 때 Play Console en-US 등록정보에 넣는다.
4. 사용자가 USPTO·KIPRIS에서 PiecePlay를 한 번 검색해 본다(3.3절).

### 5.3 그래도 PiecePlayer로 바꾼다면: 순서와 시점

시점은 **1차 글로벌 출시 빌드와 함께**다. 그 뒤로 미루면 안 바꾸는 편이 낫다.

1. **먼저 확인**: USPTO·KIPRIS 상표 검색(사용자). 노션 방침 제목을 바꿔도 예전 주소가 열리는지 시험(사용자·launch).
2. **결정 기록**: PM이 이슈로 남기고 `CONVENTIONS.md`에 표기 규칙을 적는다(붙여 쓰기 `PiecePlayer`, 한글 표기 `피스플레이어`).
3. **빌드**: program이 `app_name`과 앱 안 문구, 테스트를 고친다. design이 로고를 확인한다. 패치 노트에 "앱 이름이 PiecePlayer로 바뀌었습니다"를 적는다.
4. **자료**: marketing이 등록정보 문안(한·영), 그래픽 이미지, 홍보 영상, 영어 스크린샷을 새 이름으로 만들고 **한국어 스크린샷도 다시 찍는다.**
5. **같은 날 게시**: 새 빌드 제출, 스토어 제목(한·영), 방침 문서, 웹사이트, AdMob 표시 이름. launch가 사용자와 함께 한다.
6. **출시 후 확인**: Play에서 "PiecePlayer"와 "PiecePlay" 검색에 각각 우리 앱이 나오는지 1~2주 뒤 확인한다(marketing).
7. 저장소 이름 `pieceplay-libvlc`, 패키지 이름, 과거 문서는 바꾸지 않는다.

제목은 2.2절의 `PiecePlayer - Torrent Stream`(영어), `PiecePlayer - 토렌트 스트리밍`(한국어)을 권한다.

---

## 결정 필요 (PM·사용자)

1. 이름: 절충(권장) / PiecePlayer로 변경 / 지금 그대로
2. 영어 제목을 `PiecePlay: Torrent Player`로 할지(절충안일 때)
3. 상표 검색을 사용자가 직접 할지, 변리사에게 맡길지

## 확인하지 못한 것

- USPTO, KIPRIS, 그 밖의 상표 데이터베이스(3.3절). 웹 검색에 안 나왔다는 것 말고는 근거가 없다
- Play 검색 결과의 20번째 이후, App Store 검색 결과의 11번째 이후
- App Store는 미국만 봤다. Play는 미국·한국만 봤다(인도·필리핀 등 다른 1차 국가는 보지 않았다)
- `pieceplay.com`을 누가 무엇에 쓰는지
- 붙여 쓴 `PiecePlayer`가 Play 검색에서 "player" 검색어와 맞는지, 이름을 바꾼 뒤에도 "PiecePlay" 검색에 나오는지
- 제목 변경 심사에 걸리는 시간
- 현재 설치 수·리뷰 수(Play Console 값)
- 노션 방침 문서 제목을 바꿨을 때 예전 주소 동작, AdMob 표시 이름이 스토어 이름을 따라가는지
- 개발자 웹사이트의 현재 표기, 로고 이미지에 글자가 들어 있는지
- Play Console에 en-US 등록정보가 이미 입력돼 있는지와 그 제목(저장소 문안은 `PiecePlay`)
