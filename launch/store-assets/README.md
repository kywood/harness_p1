# 스토어 자료

디자인 리뉴얼 5단계(#49)에서 다크 브랜드 테마(배경 `#0A0F1E`, 그라데이션 `#2F66F0 → #2EE6B0`)로 바꿨고, 한국 등록정보(#119)에서 캡션을 마케팅 안(`marketing/aso/ko-KR.md` 6절)으로 바꾸고 태블릿 스크린샷을 더했다.

## Play Console 슬롯별 업로드 파일

Play Console > 스토어 등록정보 > 기본 스토어 등록정보(또는 ko-KR 번역) > 그래픽.

| Play 슬롯 | 한국어(ko-KR) 등록정보 | 영어(en-US) 등록정보 | 크기 | 규격(공식) |
|---|---|---|---|---|
| 앱 아이콘 | `1_Play_Store_Assets/app_icon_512x512.png` | 같은 파일 | 512x512 PNG | 512x512, 32비트 PNG, 1MB 이하, 필수. 지금 Console 아이콘과 같아 그대로 둔다 |
| 그래픽 이미지 | `1_Play_Store_Assets/feature_graphic_1024x500_ko-KR.png` | `1_Play_Store_Assets/feature_graphic_1024x500_en-US.png` | 1024x500 PNG, 알파 없음 | 1024x500, JPEG 또는 24비트 PNG(알파 없음), 필수 |
| 동영상 | `7_Promo_Video/PiecePlay_promo_ko-KR.mp4`를 YouTube에 올린 URL | `7_Promo_Video/PiecePlay_promo_en-US.mp4`를 올린 URL | 1920x1080, 30초 | YouTube 동영상 URL만, 공개 또는 일부공개, 광고 끔. 업로드 방법은 `7_Promo_Video/README.md` |
| 휴대전화 스크린샷 | `4_Play_Store_Screenshots/ko-KR/01~06` | `4_Play_Store_Screenshots/en-US/01~06` | 1080x1920, `03`만 1920x1080 | 2~8장, 변 320~3840px, 긴 변 ≤ 짧은 변 x2, 추천 노출은 1080px 이상 4장 이상 |
| 7인치 태블릿 | `5_Tablet_7in/ko-KR/01~06` | `5_Tablet_7in/en-US/01~06` | 1080x1920, `03`만 1920x1080 | 대화면: 변 1080~7680px, 9:16 또는 16:9, 4장 이상, 선택 |
| 10인치 태블릿 | `6_Tablet_10in/ko-KR/01~06` | `6_Tablet_10in/en-US/01~06` | 2560x1440 | 위와 같음 |
| Chromebook | 비움 | 비움 | | 위와 같음, 선택 |

- 현재 Console 기본 등록정보(en-US)에는 로고만 있는 옛 그래픽 이미지와 옛 이름 "Torrent Stream Player"의 휴대전화 스크린샷 2장이 있다. 둘 다 위 en-US 파일로 바꾼다(옛 스크린샷 2장은 삭제).
- 영어 스크린샷도 앱 화면은 한국어다(앱이 한국어만 지원). 영어 설명에 그 사실을 적는다(`launch/listing/en-US.md`).

규격 출처: [Play Console 고객센터 "미리보기 애셋 추가"](https://support.google.com/googleplay/android-developer/answer/9866151?hl=ko) (2026-11-12 확인). 그 밖의 요구: 실제 앱 화면을 보여 줄 것, "지금 다운로드" 같은 행동 유도 문구 금지, 문구가 이미지의 20%를 넘지 않을 것, 각 애셋에 140자 이하 대체 텍스트 권장.

- Chromebook은 비워 둔다. 태블릿 이미지를 그대로 올려도 규격에는 맞지만, Chromebook에서 앱을 확인하지 않았다.
- 예전 휴대전화 스크린샷(#49 캡션)은 `4_Play_Store_Screenshots/_archive-49/`로 옮겼다. 올리지 않는다.

## 캡션

`marketing/aso/ko-KR.md` 6절 안을 따랐다. 보조 문구는 `marketing:content-creation`으로 쓰고 `marketing:brand-review`, `design:ux-copy`로 점검했다(금지 표현 없음, 앱에 없는 기능 없음).

| 순서 | 화면 | 제목 | 보조 |
|---|---|---|---|
| 1 | 메인 | 링크를 넣으면 바로 재생 | 마그넷 링크나 토렌트 파일 주소만 붙여넣으세요 |
| 2 | 준비 | 앞부분부터 먼저 받아요 | 다 받을 때까지 기다리지 않아도 돼요 |
| 3 | 재생(가로) | 재생 중에는 광고 없이 | 받은 조각과 재생 위치를 한눈에 |
| 4 | 재생(세로, 10인치는 이동 직후) | 이동하면 그 위치부터 받아요 | 10초 이동 · 확대 · 축소 |
| 5 | 재생 목록 | 보던 곳부터 이어보기 | 최근 50개를 기기 안에 암호화해 보관 |
| 6 | 저장 파일 관리 | 업로드 없이, 기록은 기기 안에 | 받은 파일은 형식별로 보고 바로 삭제 |

영어(en-US) 캡션(앱 이름 "PiecePlay - Torrent Player", 등록정보 문구 톤):

| 순서 | 제목 | 보조 |
|---|---|---|
| 1 | Paste a magnet link, play right away | Works with magnet links and .torrent file URLs |
| 2 | The beginning downloads first | No need to wait for the whole file |
| 3 | No ads during playback | See downloaded pieces and your position at a glance |
| 4 | Seek, and it downloads from there | 10-second skip · zoom in · zoom out |
| 5 | Pick up where you left off | Your last 50 links, encrypted on your device |
| 6 | No uploads. History stays on device | Browse saved files by type and delete them |

- 예전 4번 보조 문구 "10초 이동 · 확대 · 배속 조절"의 배속 조절은 앱에 없는 기능이라 뺐다.
- 3번 "재생 중에는 광고 없이"는 사실이다(배너는 메인·재생 목록, 전면 광고는 재생 종료·완료 때만). 광고 정책이 바뀌면 이 캡션도 바꿔야 한다. 이 표현을 쓸지는 `marketing/aso/ko-KR.md` "결정 필요" 2번으로 PM이 정한다.
- 그래픽 이미지: 한국어 "받으면서 바로 재생" / "다 받기 전에 재생하는 토렌트 플레이어", 영어 "Play while it downloads" / "Paste a magnet link and play right away". 앱 이름 `PiecePlay - 토렌트 플레이어`와 같은 키워드를 쓰고, "바로"가 두 줄에 겹치던 것을 고쳤다.

## 원본 캡처

| 폴더 | 기기 | 빌드 | 비고 |
|---|---|---|---|
| `4_Play_Store_Screenshots/raw/` | emulator-5554, 1080x2400 420dpi | v.2026.11.12.002 디버그 (#49) | 이번에 다시 찍지 않았다 |
| `5_Tablet_7in/raw/` | AVD `Tablet_7in`(Nexus 7 2013 규격), 1200x1920 320dpi, Android 37 | v.2026.11.12.003 디버그 (main 5c8c305) | 세로, 재생만 가로 하나 |
| `6_Tablet_10in/raw/` | AVD `Tablet_10in`(Nexus 10 규격), 2560x1600(가로) 320dpi, Android 37 | 같음 | 모두 가로 |

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

- `screens.py <store-assets 폴더> <ko-KR|en-US|all> [phone|tab7|tab10]`: 각 `raw/` 원본으로 휴대전화·7인치·10인치 스크린샷을 만든다. 캡션과 파일 대응은 스크립트 위쪽 `CAPTIONS`, `TARGETS`에 있다. 원본은 비율을 유지한 채 틀 안에 맞춘다.
- `feature.py <적응형 아이콘 전경 png> <출력> [ko-KR|en-US]`: 그래픽 이미지(`2_Android_Adaptive_Icon/ic_launcher_foreground.png` 사용)
