# 스토어 자료

이 폴더의 자료는 marketing이 만들고 고친다(`marketing/CLAUDE.md`). launch는 만들지 않고, 올리기 전 확인(규격, 금지 표현, 실제 앱 화면과 일치)과 Play Console 업로드를 맡는다. 폴더 위치는 `launch/` 아래 그대로 둔다.

디자인 리뉴얼 5단계(#49)에서 다크 브랜드 테마(배경 `#0A0F1E`, 그라데이션 `#2F66F0 → #2EE6B0`)로 바꿨고, 한국 등록정보(#119)에서 캡션을 마케팅 안(`marketing/aso/ko-KR.md` 6절)으로 바꾸고 태블릿 스크린샷을 더했다.

## Play Console 슬롯별 업로드 파일

Play Console > 스토어 등록정보 > 기본 스토어 등록정보(또는 ko-KR 번역) > 그래픽.

| Play 슬롯 | 한국어(ko-KR) 등록정보 | 영어(en-US) 등록정보 | 크기 | 규격(공식) |
|---|---|---|---|---|
| 앱 아이콘 | `1_Play_Store_Assets/app_icon_512x512.png` | 같은 파일 | 512x512 PNG | 512x512, 32비트 PNG, 1MB 이하, 필수. 지금 Console 아이콘과 같아 그대로 둔다 |
| 그래픽 이미지 | `1_Play_Store_Assets/feature_graphic_1024x500_ko-KR.png` | `1_Play_Store_Assets/feature_graphic_1024x500_en-US.png` | 1024x500 PNG, 알파 없음 | 1024x500, JPEG 또는 24비트 PNG(알파 없음), 필수 |
| 동영상 | `7_Promo_Video/PiecePlay_promo_ko-KR.mp4`를 YouTube에 올린 URL | `7_Promo_Video/PiecePlay_promo_en-US.mp4`를 올린 URL | 1920x1080, 30초 | YouTube 동영상 URL만, 공개 또는 일부공개, 광고 끔. 업로드 방법은 `7_Promo_Video/README.md` |
| 휴대전화 스크린샷 | `4_Play_Store_Screenshots/ko-KR/01~07` | `4_Play_Store_Screenshots/en-US/01~07` | 1080x1920, 가로 재생만 1920x1080(`01`) | 2~8장, 변 320~3840px, 긴 변 ≤ 짧은 변 x2, 추천 노출은 1080px 이상 4장 이상 |
| 7인치 태블릿 | `5_Tablet_7in/ko-KR/01~07` | `5_Tablet_7in/en-US/01~07` | 1080x1920, 가로 재생만 1920x1080(`01`) | 대화면: 변 1080~7680px, 9:16 또는 16:9, 4장 이상, 선택 |
| 10인치 태블릿 | `6_Tablet_10in/ko-KR/01~07` | `6_Tablet_10in/en-US/01~07` | 2560x1440 | 위와 같음 |
| Chromebook | 비움 | 비움 | | 위와 같음, 선택 |

- 현재 Console 기본 등록정보(en-US)에는 로고만 있는 옛 그래픽 이미지와 옛 이름 "Torrent Stream Player"의 휴대전화 스크린샷 2장이 있다. 둘 다 위 en-US 파일로 바꾼다(옛 스크린샷 2장은 삭제).
- 영어(en-US)·한국어(ko-KR) 스크린샷은 2026-11-14에 다시 찍었다(#144, 아래 "영어 UI 재촬영", "2단계 재촬영").
- 두 언어 모두 같은 순서·같은 장면의 7장이다(재생 가로 → 메인 → 이동 → 재생 목록 → 링크 열기 → 자막 → 저장 파일. 근거: `marketing/reports/store-screenshots-en-2026-11.md`). 재생 장면을 첫 장으로 올리고 준비(preparing) 장면을 뺐다. 예전 한국어 6장의 원본 `raw/`와 영어 `raw-en-US/preparing.png`는 남겨 두었다.
- 메인 화면이 나오는 `02_main`, `05_open_link`는 로고 가운데 정렬(#187)이 들어간 v.2026.11.14.001 빌드로 다시 찍었다. 두 언어 모두 올려도 된다.

규격 출처: [Play Console 고객센터 "미리보기 애셋 추가"](https://support.google.com/googleplay/android-developer/answer/9866151?hl=ko) (2026-11-12 확인). 그 밖의 요구: 실제 앱 화면을 보여 줄 것, "지금 다운로드" 같은 행동 유도 문구 금지, 문구가 이미지의 20%를 넘지 않을 것, 각 애셋에 140자 이하 대체 텍스트 권장.

- Chromebook은 비워 둔다. 태블릿 이미지를 그대로 올려도 규격에는 맞지만, Chromebook에서 앱을 확인하지 않았다.
- 예전 휴대전화 스크린샷(#49 캡션)은 `4_Play_Store_Screenshots/_archive-49/`로 옮겼다. 올리지 않는다.

## 캡션

처음 한국어 캡션은 `marketing/aso/ko-KR.md` 6절 안이었고, 2026-11-14에 영어 7장과 같은 장면·뜻으로 바꿨다(아래 표). 보조 문구는 `marketing:content-creation`으로 쓰고 `marketing:brand-review`, `design:ux-copy`로 점검했다(금지 표현 없음, 앱에 없는 기능 없음).

한국어(ko-KR) 캡션. 영어 캡션과 같은 장면·같은 뜻으로 2026-11-14에 다시 정했다(앱 이름 `PiecePlay - 토렌트 플레이어`).

| 순서 | 파일(휴대전화·7인치 / 10인치) | 화면 | 제목 | 보조 |
|---|---|---|---|---|
| 1 | `01_player_landscape` / `01_player` | 재생(가로) | 받으면서 바로 보기 | 다 받을 때까지 기다리지 않고, 재생 중에는 광고 없이 |
| 2 | `02_main` | 메인 | 링크를 넣고 바로 재생 | 마그넷 링크와 .torrent 파일 주소를 지원해요 |
| 3 | `03_player` / `03_player_seek` | 재생(세로, 10인치는 가로) 이동 직후 | 원하는 곳으로 바로 이동 | 이동한 위치부터 받아요 · 10초 이동 |
| 4 | `04_playlist` | 재생 목록 | 보던 곳부터 이어보기 | 최근 50개를 기기 안에 암호화해 보관 |
| 5 | `05_open_link` | 링크 확인 시트 | 다른 앱의 링크도 바로 열기 | 바로 재생하거나 받아 두기 |
| 6 | `06_subtitles` | 자막 선택 | 자막 골라 보기 | 영상과 함께 받은 자막 파일을 선택 |
| 7 | `07_stored_files` | 저장 파일 관리 | 업로드 없이, 가입 없이 | 받은 파일은 기기 안에만. 언제든 삭제 |

- 예전 4번 보조 문구 "10초 이동 · 확대 · 축소"에서 "확대 · 축소"를 뺐다. 확대·축소는 재생 화면 더보기(⋮) 메뉴 안에 있어 스크린샷에 보이지 않는다.
- 7번 "업로드 없이"는 다른 사용자에게 올려 보내지 않는다(시딩 없음)는 뜻이고, "가입 없이"는 영어 "no account"와 같은 사실이다. 한국어 사용자에게 "시딩"보다 "업로드"가 익숙해 이렇게 썼다.
- 1번 "재생 중에는 광고 없이"는 예전 3번 제목을 옮긴 것이다. 광고 정책이 바뀌면 함께 고친다.

영어(en-US) 캡션(앱 이름 "PiecePlay: Torrent Player"). 제목은 30자 이내 2~5단어로 맞췄다. 결정과 근거는 `marketing/reports/store-screenshots-en-2026-11.md`.

| 순서 | 파일(휴대전화·7인치 / 10인치) | 화면 | 제목 | 보조 |
|---|---|---|---|---|
| 1 | `01_player_landscape` / `01_player` | 재생(가로) | Watch while it downloads | No waiting for the whole file · no ads during playback |
| 2 | `02_main` | 메인 | Paste a link, press play | Works with magnet links and .torrent file URLs |
| 3 | `03_player` / `03_player_seek` | 재생(세로, 10인치는 이동 직후) | Jump to any point | The download follows where you jump · 10-second skip |
| 4 | `04_playlist` | 재생 목록 | Pick up where you left off | Your last 50 links, encrypted on your device |
| 5 | `05_open_link` | 링크 확인 시트 | Open links from other apps | Play now or save for later |
| 6 | `06_subtitles` | 자막 선택 | Choose your subtitles | Pick subtitle files that come with the video |
| 7 | `07_stored_files` | 저장 파일 관리 | No seeding, no account | Saved files stay on your device. Delete them anytime |

- 3번 보조 문구에서 "zoom in · zoom out"을 뺐다. 확대·축소는 재생 화면 더보기(⋮) 메뉴 안에 있어 화면에 보이지 않는다. 지금 문구는 화면에 보이는 10초 이동 버튼과 조각 맵(이동한 위치부터 받아진 모습)에 맞췄다.
- 1번 "no ads during playback", 7번 "No seeding"(다른 사용자에게 업로드하지 않음), "no account"는 등록정보 문구(`launch/listing/en-US.md`)와 같은 사실이다. 광고 정책이나 업로드 동작이 바뀌면 함께 바꾼다.
- 예전 4번 보조 문구 "10초 이동 · 확대 · 배속 조절"의 배속 조절은 앱에 없는 기능이라 뺐다.
- 3번 "재생 중에는 광고 없이"는 사실이다(배너는 메인·재생 목록, 전면 광고는 재생 종료·완료 때만). 광고 정책이 바뀌면 이 캡션도 바꿔야 한다. 이 표현을 쓸지는 `marketing/aso/ko-KR.md` "결정 필요" 2번으로 PM이 정한다.
- 그래픽 이미지: 한국어 "받으면서 바로 재생" / "다 받기 전에 재생하는 토렌트 플레이어", 영어 "Play while it downloads" / "Paste a magnet link and play right away". 앱 이름 `PiecePlay - 토렌트 플레이어`와 같은 키워드를 쓰고, "바로"가 두 줄에 겹치던 것을 고쳤다.

## 원본 캡처

| 폴더 | 기기 | 빌드 | 비고 |
|---|---|---|---|
| `4_Play_Store_Screenshots/raw/` | emulator-5554, 1080x2400 420dpi | v.2026.11.12.002 디버그 (#49) | 이번에 다시 찍지 않았다 |
| `5_Tablet_7in/raw/` | AVD `Tablet_7in`(Nexus 7 2013 규격), 1200x1920 320dpi, Android 37 | v.2026.11.12.003 디버그 (main 5c8c305) | 세로, 재생만 가로 하나 |
| `6_Tablet_10in/raw/` | AVD `Tablet_10in`(Nexus 10 규격), 2560x1600(가로) 320dpi, Android 37 | 같음 | 모두 가로 |

### 영어 UI 재촬영 (2026-11-14, #144)

| 폴더 | 기기 | 빌드 |
|---|---|---|
| `4_Play_Store_Screenshots/raw-en-US/` | emulator-5554(AVD `Medium_Phone`), 1080x2400 420dpi, 시스템 언어 en-US | v.2026.11.13.001 디버그 (main 667c06e) |
| `5_Tablet_7in/raw-en-US/` | 같은 에뮬레이터에 `wm size 1200x1920`, `wm density 320` | 같음 |
| `6_Tablet_10in/raw-en-US/` | 같은 에뮬레이터에 `wm size 1600x2560`, `wm density 320`, `wm user-rotation lock 1`(가로 2560x1600) | 같음 |

- 원본 파일: `main`, `preparing`, `player-landscape`(10인치는 `player`), `player`(10인치는 `player-seek`), `playlist`, `stored-files`, `open-link`, `subtitles`.
- 영상: 준비·가로 재생·자막은 Sintel, 이동 장면은 Tears of Steel(10인치는 Sintel), 재생 목록·저장 파일에 Big Buck Bunny. 링크 확인 시트의 마그넷은 Tears of Steel(휴대전화)·Cosmos Laundromat(태블릿)이다. 모두 Blender Foundation 오픈 무비(CC BY).
- 테스트 광고 배너를 피하는 방법은 예전과 같다: 메인·재생 목록·저장 파일·링크 확인 시트는 와이파이·데이터를 끄고 앱을 새로 열어 찍었다. 재생·준비 화면에는 원래 광고가 없다.
- 7인치(sw600dp)는 앱의 가로 전환 버튼이 아니라 기기 회전(`wm user-rotation lock 1`)으로 가로 재생을 찍었다.
- 7인치 `preparing.png`는 데모 모드를 다시 걸기 전에 찍어 상태 표시줄에 와이파이 아이콘이 두 개였다. 상태 표시줄 오른쪽 아이콘 영역(240x48px)만 같은 빌드의 7인치 `main.png` 것으로 덮었다. 앱 화면 영역은 손대지 않았다.
- 이때 찍은 메인 화면은 로고가 왼쪽으로 치우친 예전 빌드였다. `main.png`, `open-link.png`(기기 3종)는 아래 "2단계 재촬영"에서 다시 찍었다.
- 찍은 뒤 에뮬레이터는 되돌렸다(`wm size reset`, `wm density reset`, 회전 자동, 데모 모드 종료, 앱 제거).

### 2단계 재촬영 (2026-11-14, #144, PR #181)

| 폴더 | 기기 | 빌드 |
|---|---|---|
| `*/raw-en-US/main.png`, `open-link.png` (3종) | emulator-5554(AVD `Medium_Phone`). 7인치·10인치는 위와 같은 `wm size`·`wm density` | v.2026.11.14.001 디버그 (main d37e3f7: 로고 가운데 정렬 #187, 받는 순서 #184, 재생 수정 #189 포함) |
| `*/raw-ko-KR/` (3종, 7장씩) | 같은 에뮬레이터, 앱별 언어 `cmd locale set-app-locales … ko-KR` | 같음 |

- 영상: 한국어 가로 재생·이동은 Big Buck Bunny(휴대전화, 10인치 가로 재생)와 Tears of Steel(7인치, 10인치 이동), 자막은 Sintel, 재생 목록·저장 파일은 Big Buck Bunny·Sintel·Tears of Steel. 링크 확인 시트의 마그넷은 기기 3종 모두 Tears of Steel이다(예전 태블릿의 Cosmos Laundromat은 이름이 보이게 되어 등록정보 출처 문장과 맞추려고 바꿨다). 태블릿 시트는 이미 재생 목록에 있는 영상이라 "재생 목록에 있는 영상입니다" 줄이 함께 보인다(실제 동작).
- 휴대전화·7인치는 가로 재생과 이동 장면이 같은 순간을 세로·가로로 찍은 것이다(받는 중에 이동한 구간이 조각 맵에 따로 보인다).
- 테스트 광고 배너를 피하는 방법은 위와 같다(메인·재생 목록·저장 파일·링크 확인 시트는 와이파이·데이터를 끄고 앱을 새로 열어 찍음). 상태 표시줄은 데모 모드다.
- 영어 휴대전화 `03_player`(어두운 장면)는 다시 찍지 못해 그대로다. 에뮬레이터 부하로 일시정지 프레임이 깨져 쓸 만한 장면을 얻지 못했다.
- 찍은 뒤 에뮬레이터는 되돌렸다(`wm size reset`, `wm density reset`, 회전 자동, 앱별 언어 해제, 데모 모드 종료, `show_ime_with_hard_keyboard`=0, 앱 제거).

### 한국어 원본(예전)

- 상태 표시줄은 데모 모드(12:00, 와이파이·배터리 가득, 알림 숨김)로 찍었다.
- 메인·재생 목록·저장 파일 관리는 와이파이·데이터를 끄고 앱을 다시 열어 찍었다. 디버그 빌드의 테스트 광고 배너가 들어가지 않게 하려는 것이다(릴리스 빌드에서는 메인·재생 목록 하단에 배너가 나온다).
- 저장 파일 관리는 `.mp4` 필터를 고른 상태다.
- 휴대전화 원본의 메인 화면은 #49 때 빌드라 지금 메인 하단의 "이용 안내 · 개인정보처리방침 · 오픈소스 라이선스" 줄이 없다. 저장 파일 관리의 `Sintel.2010.720p.mp4`는 그때 에뮬레이터에 남아 있던 테스트 파일이다.

## 아이콘

`app_icon_512x512.png`, 적응형 아이콘, mipmap은 바꾸지 않았다. 앱에 들어 있는 런처 아이콘(`res/mipmap-*/pieceplay_launcher.png`)과 같은 파일이고 이미 남색·파랑→민트 브랜드다. 스토어 아이콘만 바꾸면 설치 후 아이콘과 달라진다. `app_icon_1024x1024_master.png`는 이름과 달리 512x512다.

## 화면 속 영상

재생·목록 화면의 영상은 Blender Foundation의 오픈 무비다(Sintel, Big Buck Bunny, Tears of Steel, Cosmos Laundromat). 모두 CC BY 라이선스(© Blender Foundation, blender.org)다. 자세한 설명 끝에 출처 문구를 넣는다(`marketing/aso/ko-KR.md` 5절 안 A).

## 다시 만들기

`tools/`의 스크립트(Python + Pillow, Windows의 Noto Sans KR 가변 글꼴 `NotoSansKR-VF.ttf`. 맑은 고딕은 작은 크기에서 "받"이 깨져 쓰지 않는다)로 만든다. Pillow가 없으면 `uv run --no-project --with pillow python ...`로 실행한다.

- `screens.py <store-assets 폴더> <ko-KR|en-US|all> [phone|tab7|tab10]`: 언어별 원본(en-US는 `raw-en-US/`, ko-KR은 `raw-ko-KR/`, 스크립트의 `RAW_DIRS`)으로 휴대전화·7인치·10인치 스크린샷을 만든다. 캡션과 파일 대응은 스크립트 위쪽 `CAPTIONS`, `TARGETS`(기본 6장), `SCENES`(언어별 장면 순서. en-US·ko-KR 모두 7장)에 있다. 원본은 비율을 유지한 채 틀 안에 맞춘다.
- `feature.py <적응형 아이콘 전경 png> <출력> [ko-KR|en-US]`: 그래픽 이미지(`2_Android_Adaptive_Icon/ic_launcher_foreground.png` 사용)
