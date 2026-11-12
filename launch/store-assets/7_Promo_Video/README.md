# 스토어 홍보 동영상

Play 등록정보의 "동영상" 칸(YouTube URL)에 넣을 영상이다. YouTube 업로드와 Play Console 입력은 사용자가 직접 한다.

## 결과물

| 파일 | 자막 | 길이 | 해상도 | 크기 |
|---|---|---|---|---|
| `PiecePlay_promo_ko-KR.mp4` | 한국어 | 30.1초 | 1920x1080, 30fps, H.264, 소리 없음 | 2.8MB |
| `PiecePlay_promo_en-US.mp4` | 영어 | 30.1초 | 같음 | 2.9MB |

대표 장면: `stills/<언어>_1_link.png`(링크 입력), `_2_seek.png`(탐색), `_3_playlist.png`(재생 목록).

## 구성 (30초)

| 시간 | 화면(실제 앱 녹화) | 한국어 자막 | 영어 자막 |
|---|---|---|---|
| 0.0–6.2 | 메인 → 마그넷 링크 입력 → 바로 재생 | 링크를 붙여넣고 바로 재생 | Paste a magnet link, play right away |
| 6.2–12.6 | 준비 화면(정보 → 연결 → 버퍼링, 4배속) | 앞부분부터 먼저 받아요 | The beginning downloads first |
| 12.6–20.1 | 재생(Sintel 첫 장면) | 받으면서 보고, 재생 중에는 광고 없이 | Watch while it downloads, no ads during playback |
| 20.1–24.1 | 재생 막대 60% 지점으로 이동 → 그 위치 버퍼링 | 이동하면 그 위치부터 받아요 | Seek, and it downloads from there |
| 24.1–26.5 | 재생 목록(이어보기) | 보던 곳부터 이어보기 | Pick up where you left off |
| 26.5–30.0 | 마무리: 로고 + 앱 이름 + 출처 | PiecePlay - 토렌트 플레이어 / 받으면서 바로 재생 | PiecePlay - Torrent Player / Play while it downloads |

- 실제 앱 화면이 0초부터 나오고, 전체의 88%(26.5/30초)가 실제 앱 화면이다.
- 마무리 화면 아래에 영상 출처를 넣었다: "Sintel · Cosmos Laundromat © Blender Foundation | blender.org (CC BY)".
- "Best", "#1", "New", "Download now" 같은 문구와 배경음악은 넣지 않았다(소리 없음).
- 테스트 광고 배너가 나오지 않도록 메인·재생 목록은 네트워크를 끈 상태로 보여 줬다.

## Play 규격 (공식)

[Play Console 고객센터 "미리보기 애셋 추가"](https://support.google.com/googleplay/android-developer/answer/9866151?hl=ko) (2026-11-13 확인):

- YouTube **동영상 URL** 하나만 넣는다. 재생목록·채널 URL, 시간 지정 같은 추가 파라미터는 안 된다.
- 공개 범위는 **공개 또는 일부공개**(비공개 금지). **광고·수익 창출을 끈다.** 연령 제한 영상은 안 된다. 퍼가기(삽입)를 허용한다.
- 앞 30초만 소리 없이 자동 재생된다. 가로·세로 모두 된다(세로는 양옆 검은 띠 금지).
- 처음 10초 안에 실제 앱을 보여 주고, 80% 이상을 실제 사용 화면으로 채운다. 자막 권장.

## YouTube 업로드 방법 (사용자가 직접)

1. YouTube Studio > 만들기 > 동영상 업로드에서 `PiecePlay_promo_ko-KR.mp4`를 올린다(영어 등록정보용은 `en-US`를 따로 올린다).
2. 제목·설명
   - 한국어 제목: `PiecePlay - 토렌트 플레이어 | 받으면서 바로 재생`
   - 한국어 설명: `마그넷 링크를 붙여넣으면 다 받기 전에 바로 재생합니다. 이동한 위치부터 먼저 받고, 재생 중에는 광고가 없습니다. 영상: Sintel, Cosmos Laundromat © Blender Foundation | www.blender.org (CC BY)`
   - 영어 제목: `PiecePlay - Torrent Player | Play while it downloads`
   - 영어 설명: `Paste a magnet link and start watching before the download finishes. Seeking downloads from the new position first, and there are no ads during playback. Footage: Sintel, Cosmos Laundromat © Blender Foundation | www.blender.org (CC BY)`
3. 시청자층: "아니요, 아동용이 아닙니다". 연령 제한 없음.
4. 공개 범위: **일부공개**를 추천한다(채널 목록에 안 보이고 Play에서는 재생된다).
5. 수익 창출: 채널이 수익 창출 중이면 이 영상의 광고를 **끈다**.
6. 고급 설정: 퍼가기 허용 확인.
7. 영상 URL(`https://www.youtube.com/watch?v=...`, 추가 파라미터 없이)을 Play Console > 스토어 등록정보 > 동영상 칸에 넣는다. ko-KR 등록정보에는 한국어판, en-US에는 영어판 URL.

## 다시 만들기

준비: 10인치 태블릿 에뮬레이터(AVD `Tablet_10in`, 가로 고정 `wm user-rotation lock 1`, 상태 표시줄 데모 모드), 디버그 빌드 설치, 재생 목록에 다른 오픈 무비 1~2개, 녹화에 쓸 영상(기본 Sintel)은 기기에 없는 상태.

1. 녹화(Git Bash): `bash record.sh emulator-5558 <작업 폴더> [info-hash]` → `promo_a.mp4`(메인·준비), `promo_b.mp4`(재생·탐색·재생 목록), `events.txt`
2. 자를 구간 고르기: 녹화 시각과 `events.txt` 시각이 screenrecord 시작 지연으로 몇 초씩 어긋나므로, 프레임을 뽑아 보고 `cuts.txt`를 고친다. `events_sample.txt`는 이번 녹화의 기록이다.
3. 편집 준비(Windows Python + Pillow): `python make_video.py <작업 폴더>` → 자막·배경·마무리 PNG와 `build.sh`
4. ffmpeg 이미지(한 번): WSL에서 이 폴더로 이동해 `docker build -t pieceplay-ffmpeg .` (공식 `debian:bookworm-slim` + Debian 저장소 ffmpeg)
5. 인코딩: WSL에서 작업 폴더로 이동해 `bash build.sh` (일회용 `--rm` 컨테이너)
6. 끝나면 `docker rmi pieceplay-ffmpeg debian:bookworm-slim`
