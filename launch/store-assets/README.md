# 스토어 자료

디자인 리뉴얼 5단계(#49)에서 새 다크 브랜드 테마(배경 `#0A0F1E`, 그라데이션 `#2F66F0 → #2EE6B0`)로 교체했다.

## Play Console에 올리는 파일

| Play Console 항목 | 파일 | 크기 |
|---|---|---|
| 앱 아이콘 | `1_Play_Store_Assets/app_icon_512x512.png` | 512x512 |
| 그래픽 이미지 | `1_Play_Store_Assets/feature_graphic_1024x500.png` | 1024x500, 알파 없음 |
| 휴대전화 스크린샷 | `4_Play_Store_Screenshots/01_main.png` ~ `06_stored_files.png` (6장, 번호 순서) | 1080x1920, `03`만 1920x1080 |

- 스크린샷은 Play 규칙(긴 변이 짧은 변의 2배 이하)에 맞춰 1080x1920 틀 안에 실제 화면을 넣은 이미지다. 기기 원본 캡처(1080x2400)는 비율이 2.22:1이라 그대로는 올릴 수 없다.
- 원본 캡처는 `4_Play_Store_Screenshots/raw/`에 있다. 2026-11-12 main 빌드(v.2026.11.12.002 디버그)를 에뮬레이터(1080x2400, 420dpi)에서 상태 표시줄 데모 모드로 찍었다.
- 메인·재생 목록·저장 파일 관리는 네트워크를 끈 상태로 앱을 다시 열어 찍었다. 디버그 빌드의 테스트 광고 배너가 들어가지 않게 하려는 것이다(릴리스 빌드에서는 이 두 화면 하단에 배너가 나온다).
- 준비 화면 캡처(`raw/preparing.png`)는 상태 표시줄 영역(위 63px)만 같은 세션의 다른 캡처로 바꿨다. 앱 화면은 손대지 않았다.

## 아이콘

`app_icon_512x512.png`, 적응형 아이콘, mipmap은 바꾸지 않았다. 앱에 들어 있는 런처 아이콘(`res/mipmap-*/pieceplay_launcher.png`)과 같은 파일이고 이미 남색·파랑→민트 브랜드다. 스토어 아이콘만 바꾸면 설치 후 아이콘과 달라진다. `app_icon_1024x1024_master.png`는 이름과 달리 512x512다.

## 화면 속 영상

재생·목록 화면의 영상은 Blender Foundation의 오픈 무비다(Sintel, Big Buck Bunny, Tears of Steel, Cosmos Laundromat). 모두 CC BY 라이선스(© Blender Foundation, blender.org)이며, 스토어 설명에 출처 문구를 넣을지는 등록정보 작업 때 정한다.

## 다시 만들기

`tools/`의 스크립트(Python + Pillow, Windows의 Noto Sans KR 가변 글꼴 `NotoSansKR-VF.ttf`. 맑은 고딕은 작은 크기에서 "받"이 깨져 쓰지 않는다)로 만들었다.

- `feature.py <적응형 아이콘 전경 png> <출력>`: 그래픽 이미지
- `compose.py <작업 폴더> <출력 폴더>`: 작업 폴더의 `final/` 아래 원본 캡처(`01_main.png`, `02_preparing.png`, `player_landscape.png`, `player_portrait.png`, `02_playlist.png`, `03_stored_files.png`)로 스크린샷 6장을 만든다. 문구는 스크립트 안에 있다.
