# 화면: 재생 화면

코드: `ui/VlcPlayerScreen.kt`, `ui/player/PlayerOverlay.kt` / `AppScreen.Player(fileName)`

## 목적
받는 중이거나 받은 영상을 재생하고, 받은 구간과 진행을 함께 보여 준다.

## 진입
- 준비 화면에서 시작 구간이 확보되면 자동

## 보여 줄 정보
- 영상(검은 배경, 기본 맞춤 `SURFACE_BEST_FIT`)
- 조작 UI(탭으로 표시/숨김, 재생 중 3.5초 뒤 자동 숨김)
  - 위: ←, 파일 이름, 통계 칩 "↓ 속도 · N% · 피어 N명"(가로면 같은 줄), 회전, 축소, 배율(1.0×), 확대, 더보기
  - 가운데: 10초 뒤로, 재생/일시 정지, 10초 앞으로. 대기 중이면 위에 "이동한 구간을 준비하고 있습니다…" / "재생 데이터를 버퍼링하고 있습니다…"
  - 아래: "현재 00:00 / 전체 00:00", "다운로드 N%", 재생 막대(재생 구간·받은 구간), 조각 맵
- 조작 UI가 숨으면 오른쪽 아래 시간 표시만

## 동작
| 동작 | 결과 |
|---|---|
| 탭 | 조작 UI 토글 |
| 두 번 탭(왼쪽/오른쪽 절반) | 10초 뒤/앞 |
| 가로 스와이프(조작 UI 숨김 상태) | 탐색 미리보기, 손 떼면 이동. 화면 너비 = 영상 전체 길이 |
| 두 손가락 | 확대(최대 8배)·이동 |
| 재생 막대 드래그 | 탐색 |
| 회전 | 가로/세로 전환 |
| 더보기 → 링크 추가 | 재생 유지한 채 다운로드 추가 대화상자 |
| 더보기 → 재생 위치 초기화 · 처음부터 | 0으로 이동, 위치 0 저장 |
| ←/뒤로 | 시작한 곳으로, 전면 광고 판단 |

## 오류 상태
- 경고 아이콘 + 문구 + "돌아가기". 문구: 재생 실패, 플레이어 시작 실패(원문 포함), 영상 파일 없음, 영상 열기 실패(원문 포함).
- "다시 시도"가 없어 같은 위치에서 재개하려면 돌아가서 다시 재생해야 한다.

## 없는 것 (요구사항 후보)
- 자막·오디오 트랙 선택, 외부 자막 → [requests/subtitles-audio-tracks.md](../requests/subtitles-audio-tracks.md)
- 재생 속도, 화면 비율 → [requests/playback-speed-aspect.md](../requests/playback-speed-aspect.md)
- 정밀 탐색(긴 영상에서 스와이프가 너무 민감) → [requests/seek-precision.md](../requests/seek-precision.md)
- 화면 잠금, 밝기·음량 제스처, PiP → [requests/player-gestures-pip.md](../requests/player-gestures-pip.md)
- 오류 시 다시 시도 → [requests/error-recovery.md](../requests/error-recovery.md)
