# 영어(en-US) 스토어 스크린샷 검토: 순서와 캡션

- 작성: 2026-11-14, marketing (#144, PR #181)
- 대상: `launch/store-assets/{4_Play_Store_Screenshots,5_Tablet_7in,6_Tablet_10in}/en-US/`
- 방법: 기존 8장(기기 3종)을 직접 열어 보고, 미국 Play 스토어 "torrent player" 검색 상위 앱의 스크린샷과 비교했다. 캡션·순서는 `tools/screens.py`에서 고쳐 기존 원본(`raw-en-US/`)으로 다시 합성했다. 에뮬레이터는 쓰지 않았다.
- 전환율 수치(스토어 등록정보 실적)는 영어 등록정보를 아직 올리지 않아 없다. 아래는 관례와 화면 내용에 근거한 판단이다.

## 결론

1. **첫 장은 가로 재생 장면**으로 한다. 영상이 재생되는 화면에 "Downloaded 77%"와 조각 맵이 함께 보여, 앱의 핵심(받으면서 본다)을 한 장으로 설명한다. 예전 첫 장(메인 화면)은 입력란만 있어 무엇을 하는 앱인지 그림으로 보이지 않는다.
2. **8장 → 7장.** 준비(preparing) 장면을 뺐다. "Buffering: 4%"와 빈 화면 절반이 보여 "기다리지 않는다"는 메시지와 반대 인상을 준다. 그 메시지는 1번 보조 문구로 옮겼다.
3. **제목은 30자 이내 2~5단어.** 예전 제목 3개가 33~36자라 글꼴이 줄어들었고, 스토어 목록의 작은 미리보기에서 읽기 어렵다.
4. 순서는 핵심 가치 → 시작 방법 → 차별 기능 → 편의 기능 → 신뢰(업로드·계정 없음) 순이다.

## 장면과 캡션 (before → after)

| 새 순서 | 장면 | 예전 순서 | 제목 before | 제목 after | 보조 after |
|---|---|---|---|---|---|
| 1 | 재생(가로) | 3 | No ads during playback | Watch while it downloads (24자) | No waiting for the whole file · no ads during playback |
| 2 | 메인 | 1 | Paste a magnet link, play right away (36자) | Paste a link, press play (24자) | Works with magnet links and .torrent file URLs (그대로) |
| 3 | 재생(세로, 10인치는 이동 직후) | 4 | Seek, and it downloads from there (33자) | Jump to any point (17자) | The download follows where you jump · 10-second skip |
| 4 | 재생 목록 | 5 | Pick up where you left off | 그대로 (26자) | Your last 50 links, encrypted on your device (그대로) |
| 5 | 링크 확인 시트 | 7 | Open links from other apps | 그대로 (26자) | Play now or save for later (그대로) |
| 6 | 자막 선택 | 8 | Choose your subtitles | 그대로 (21자) | Pick subtitle files that come with the video (그대로) |
| 7 | 저장 파일 관리 | 6 | No uploads. History stays on device (35자) | No seeding, no account (22자) | Saved files stay on your device. Delete them anytime |
| 뺌 | 준비 | 2 | The beginning downloads first | | |

캡션별 이유:

- **3번 "zoom in · zoom out" 삭제.** 확대·축소는 재생 화면 더보기(⋮) 메뉴 안에 있어 스크린샷에 보이지 않는다. 화면에 보이는 것(10초 이동 버튼, 이동한 위치부터 받아진 조각 맵)에 맞췄다. 예전 보조 문구 before: `10-second skip · zoom in · zoom out`.
- **"Seek" → "Jump".** seek은 개발자 용어다. 등록정보 문구도 "the download follows where you jump to"라고 쓴다.
- **"No uploads" → "No seeding".** 토렌트 사용자가 실제로 쓰는 말은 seeding이다. "No uploads"는 "파일을 올릴 수 없다"로도 읽힌다. "History stays on device"는 저장 파일 화면과 맞지 않아, 화면에 보이는 것(파일 목록과 삭제 버튼)을 보조 문구로 적었다. "no account"는 등록정보의 "PiecePlay has no account"와 같은 사실이다.
- **1번 보조의 "no ads during playback"**은 예전 3번 제목을 옮긴 것이다. 사실이며(재생·준비 화면에 광고 없음) 광고 정책이 바뀌면 고쳐야 한다.
- 금지 표현(영화·드라마 제목, 토렌트 사이트 이름, free movies, latest releases, unlimited downloads) 없음. 최상급·속도 수치 없음. 앱에 없는 기능 없음.

## 경쟁 앱 관례 (미국 Play 스토어, 2026-11-14 확인)

[검색 "torrent player"](https://play.google.com/store/search?q=torrent%20player&c=apps&hl=en_US&gl=US) 상위 3개 앱의 상세 페이지 스크린샷을 봤다.

| 앱 | 첫 장 | 구성 | 캡션 |
|---|---|---|---|
| [Amnis - Torrent Player](https://play.google.com/store/apps/details?id=com.amnis&hl=en_US&gl=US) (검색 1위) | **가로, 영상 재생 화면** + "Watch torrents instantly!" | 앞 4장 가로 | 3~5단어, 한 장에 혜택 하나 |
| [µTorrent](https://play.google.com/store/apps/details?id=com.utorrent.client&hl=en_US&gl=US) | 세로, 목록 화면 + "Lightning fast downloads" | 세로 5장 | 2~4단어("Save data using wifi", "Watch while downloading") |
| [Torrent Play: Video Streaming](https://play.google.com/store/apps/details?id=com.bsoft.torrent_play&hl=en_US&gl=US) | 세로, 파일 선택 화면 + "Choose what to download" | 세로 8장 | 2~3단어 제목 + 작은 보조 한 줄 |

- 재생 전용 앱(Amnis)은 재생 화면을 첫 장에 두고, 다운로더(µTorrent)도 "Watch while downloading"을 따로 한 장 쓴다. PiecePlay는 재생이 본업이라 Amnis 방식을 따랐다.
- 세 앱 모두 제목이 2~5단어다. 예전 PiecePlay 제목(6~7단어)이 가장 길었다.
- 경쟁 앱이 쓰는 "Lightning fast", "No speed or size limits" 같은 표현은 근거 없는 최상급·"무제한" 금지 표현에 가까워 쓰지 않았다.
- 한계: 상위 3개만 봤다. 앱별 전환율은 알 수 없다(미확인). 스토어 검색 결과에 스크린샷이 실제로 어떻게 잘려 보이는지는 기기에서 확인하지 않았다.

## 2단계 재촬영 결과 (2026-11-14)

v.2026.11.14.001 디버그(main d37e3f7: 로고 가운데 정렬 #187, 받는 순서 #184, 재생 수정 #189)를 emulator-5554에 설치해 찍었다. 자세한 촬영 조건은 `launch/store-assets/README.md` "2단계 재촬영".

| 구분 | 출력 파일 | 기기 | 결과 |
|---|---|---|---|
| 필수 | `02_main.png` | 휴대전화, 7인치, 10인치 | 다시 찍음. 로고·앱 이름·태그라인이 가로 가운데 |
| 필수 | `05_open_link.png` | 휴대전화, 7인치, 10인치 | 다시 찍음. 태블릿 시트의 마그넷을 Cosmos Laundromat → Tears of Steel로 바꿨다(이름이 화면에 보여 등록정보 출처 문장과 맞춤) |
| 권장 | `03_player.png` | 휴대전화 | **못 함.** 밝은 장면(Big Buck Bunny)에서 시도했지만 에뮬레이터 부하(load 20~70)로 일시정지 프레임이 깨졌다. 지금 파일은 예전 어두운 장면 그대로 |
| 선택 | `07_stored_files.png` | 3종 | 하지 않음(`.mp4` 필터 2개 그대로) |

### 한국어(ko-KR)도 같은 7장으로

영어와 같은 순서·같은 장면으로 한국어 스크린샷을 새로 찍고 합성했다(휴대전화·7인치·10인치 각 7장, 원본 `raw-ko-KR/`). 예전 6장(`01_main`~`06_stored_files`)은 지웠다. 원본 `raw/`는 남겨 두었다.

| 순서 | 제목 before → after | 보조 before → after |
|---|---|---|
| 1 재생(가로) | 재생 중에는 광고 없이 → **받으면서 바로 보기** | 받은 조각과 재생 위치를 한눈에 → 다 받을 때까지 기다리지 않고, 재생 중에는 광고 없이 |
| 2 메인 | 링크를 넣으면 바로 재생 → **링크를 넣고 바로 재생** | 마그넷 링크나 토렌트 파일 주소만 붙여넣으세요 → 마그넷 링크와 .torrent 파일 주소를 지원해요 |
| 3 이동 | 이동하면 그 위치부터 받아요 → **원하는 곳으로 바로 이동** | 10초 이동 · 확대 · 축소 → 이동한 위치부터 받아요 · 10초 이동 |
| 4 재생 목록 | 보던 곳부터 이어보기 (그대로) | 최근 50개를 기기 안에 암호화해 보관 (그대로) |
| 5 링크 열기 | (새 장면) **다른 앱의 링크도 바로 열기** | 바로 재생하거나 받아 두기 |
| 6 자막 | (새 장면) **자막 골라 보기** | 영상과 함께 받은 자막 파일을 선택 |
| 7 저장 파일 | 업로드 없이, 기록은 기기 안에 → **업로드 없이, 가입 없이** | 받은 파일은 형식별로 보고 바로 삭제 → 받은 파일은 기기 안에만. 언제든 삭제 |
| 뺌 | 준비: 앞부분부터 먼저 받아요 | |

- "확대 · 축소"는 화면에 보이지 않아 뺐다(영어와 같은 이유).
- "업로드 없이"는 "No seeding"의 뜻이다. 한국어 사용자에게 "시딩"보다 익숙한 말로 썼다. "가입 없이"는 "no account"와 같은 사실.
- 금지 표현 없음, 최상급·수치 과장 없음, 앱에 없는 기능 없음.

### 확인 결과 (모든 이미지를 직접 열어 봄)

- 언어: 한국어판은 앱 화면도 한국어다. 파일 이름(Sintel.mp4 등)과 10인치 이동 장면의 영화 자막 한 줄("Nooooo!", Tears of Steel 영어 자막)은 콘텐츠라 영어로 남는다.
- 잘림·겹침 없음. 테스트 광고 배너 없음(네트워크를 끄고 찍은 화면). 개인정보 없음(마그넷은 공개 오픈 무비 해시).
- 로고·앱 이름: 메인·링크 열기 장면 모두 가로 가운데.
- 휴대전화·7인치의 가로 재생과 이동 장면은 같은 순간을 세로·가로로 찍은 것이라 영상 프레임이 같다.

### 촬영 중 발견한 앱 문제 (PM 전달)

- **재생 중 네이티브 비정상 종료 1회**: Big Buck Bunny를 받으면서 재생하다가 가로 화면에서 일시정지 후 "10초 앞으로"를 누르자 앱이 SIGSEGV로 종료됐다(07:25, 디버그 v.2026.11.14.001, x86_64 에뮬레이터, 부하 높음). 스택은 `base.apk` 안 네이티브 라이브러리(기호 없음). 로그 발췌는 공유 노트 폴더 `marketing-crash-2026-11-14-0725.txt`. 재현은 시도하지 않았다.
- 재생 목록에서 Tears of Steel 항목이 한동안 안 보였는데 링크를 다시 열면 "재생 목록에 있는 영상"으로 나왔다(목록 아래쪽에 있었을 가능성도 있어 확인 필요).

## 남은 일

- 영어 휴대전화 `03_player`를 밝은 장면으로 다시 찍기(권장, 에뮬레이터가 한가할 때).
- 홍보 영상(`7_Promo_Video/make_video.py`, 영상 README)의 영어 마무리 문구가 `PiecePlay - Torrent Player`(하이픈)다. 게시된 제목은 `PiecePlay: Torrent Player`(콜론)다. 영어 영상은 앱 화면이 한국어라 어차피 다시 만들어야 하므로 그때 함께 고친다.
- 영어 등록정보를 올린 뒤 스토어 등록정보 실험으로 첫 장(재생 vs 메인)을 비교할 수 있다.
