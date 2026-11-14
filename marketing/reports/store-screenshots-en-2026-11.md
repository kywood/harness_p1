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

## 재촬영이 필요한 장면 (2단계, 에뮬레이터가 비면)

로고 가운데 정렬(main af9e877, #187)이 들어간 빌드로 찍는다.

| 구분 | 출력 파일 | 원본 파일 | 기기 | 이유 |
|---|---|---|---|---|
| 필수 | `02_main.png` | `main.png` | 휴대전화, 7인치, 10인치 | 로고·앱 이름·태그라인이 왼쪽으로 치우친 예전 빌드 화면 |
| 필수 | `05_open_link.png` | `open-link.png` | 휴대전화, 7인치, 10인치 | 시트 뒤 메인 화면에 같은 문제 |
| 권장 | `03_player.png` | `player.png` | 휴대전화만 | 영상 장면이 어둡고 화면 대부분이 검다. 7인치처럼 밝은 장면에서 다시 찍으면 좋다 |
| 선택 | `07_stored_files.png` | `stored-files.png` | 3종 | 파일 2개뿐이라 아래가 비어 보인다. `All` 필터로 찍으면 목록이 찬다. 급하지 않다 |

필수 6장을 다시 찍기 전에는 영어 스크린샷을 올리지 않는다.

## 남은 일

- 홍보 영상(`7_Promo_Video/make_video.py`, 영상 README)의 영어 마무리 문구가 `PiecePlay - Torrent Player`(하이픈)다. 게시된 제목은 `PiecePlay: Torrent Player`(콜론)다. 영어 영상은 앱 화면이 한국어라 어차피 다시 만들어야 하므로 그때 함께 고친다.
- 한국어(ko-KR) 스크린샷은 이번에 손대지 않았다(6장, 예전 순서). 같은 순서·원칙을 적용할지는 PM이 정한다. 한국어 캡션 4번에도 "확대 · 축소"가 있어 같은 문제가 있다.
- 영어 등록정보를 올린 뒤 스토어 등록정보 실험으로 첫 장(재생 vs 메인)을 비교할 수 있다.
