#!/usr/bin/env bash
# 홍보 영상 원본 녹화 (Git Bash). 10인치 태블릿 에뮬레이터(가로 2560x1600)에서
# 링크 입력 -> 준비 -> 재생 -> 탐색 -> 재생 목록 흐름을 녹화하고, 장면 시각을 events.txt 에 남긴다.
# screenrecord 는 한 번에 최대 3분이고 메타데이터 받기가 길어질 수 있어 두 구간으로 나눠 찍는다.
#   a: 메인 -> 링크 입력 -> 준비 화면 25초   b: 재생 시작 -> 탐색 -> 재생 목록
# 사용: bash record.sh <adb 시리얼> <출력 폴더> [마그넷 info-hash]
# 전제: 앱(디버그) 설치, 재생 목록에 다른 오픈 무비 항목이 있음, 상태 표시줄 데모 모드, 화면 가로 고정(wm user-rotation lock 1).
# 테스트 광고 배너를 피하려고 메인·재생 목록은 네트워크를 끈 상태로 보여 준다.
set -u
export MSYS_NO_PATHCONV=1 PYTHONIOENCODING=utf-8
SERIAL=$1; OUT=$2; mkdir -p "$OUT"
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
S="$ADB -s $SERIAL shell"
PKG=com.gamenuri.pieceplay
HASH=${3:-08ada5a7a6183aae1e09d831df6748d566095a10}   # 기본: Sintel (Blender Foundation, CC BY). 기기에 아직 없는 영상이어야 준비 화면이 나온다
EV="$OUT/events.txt"; : > "$EV"
T0=0
mark() { echo "$(( $(date +%s%3N) - T0 )) $1" >> "$EV"; echo "mark $1"; }
ui_xy() {  # 글자로 요소를 찾아 가운데 좌표
  $S uiautomator dump /sdcard/u.xml >/dev/null 2>&1
  $S cat /sdcard/u.xml | python -c "
import sys,re
x=sys.stdin.buffer.read().decode('utf-8','replace')
for n in re.findall(r'<node [^>]*>',x):
  t=re.search(r' text=\"([^\"]*)\"',n).group(1)
  if t==sys.argv[1]:
    a=list(map(int,re.findall(r'\d+',re.search(r'bounds=\"([^\"]*)\"',n).group(1)))); print((a[0]+a[2])//2,(a[1]+a[3])//2); break" "$1"
}

# 0. 앱을 네트워크 없이 새로 연다(메인 배너 없음)
$S svc wifi disable; $S svc data disable; $S am force-stop $PKG; sleep 1
$S monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 20
$S wm user-rotation lock 1; sleep 3   # 앱을 열면 회전 고정이 풀려 다시 건다
rec_start() {  # $1 = 구간 이름
  $S rm -f /sdcard/promo_$1.mp4
  $S screenrecord --size 1920x1200 --bit-rate 16000000 --time-limit 170 /sdcard/promo_$1.mp4 &
  REC=$!; sleep 1; T0=$(date +%s%3N); mark "start_$1"
}
rec_stop() {
  mark "stop_$1"; $S pkill -INT screenrecord; sleep 3; wait $REC 2>/dev/null
  "$ADB" -s $SERIAL pull /sdcard/promo_$1.mp4 "$OUT/promo_$1.mp4"
}
rec_start a
sleep 2
# 1. 링크 붙여넣기(입력)
set -- $(ui_xy '마그넷 또는 .torrent 다운로드 링크'); $S input tap $1 $2; sleep 0.8; mark type
for c in 'magnet:?xt=' 'urn:btih:' ${HASH:0:10} ${HASH:10:10} ${HASH:20:10} ${HASH:30:10}; do $S "input text '$c'"; sleep 0.25; done
$S input keyevent 4; sleep 0.8
$S svc wifi enable; sleep 2.5
set -- $(ui_xy '바로 재생'); mark tap_play; $S input tap $1 $2
# 2. 준비 화면 -> 재생 시작까지 기다림
sleep 25; rec_stop a
for i in $(seq 1 300); do sleep 1; $S uiautomator dump /sdcard/u.xml >/dev/null 2>&1; $S cat /sdcard/u.xml | grep -q '현재 ' && break; done
rec_start b; mark playing
sleep 10
# 3. 탐색: 컨트롤을 띄우고 재생 막대 60% 지점을 누른다
$S input tap 1280 500; sleep 0.8; mark seek; $S input tap 1523 1403; sleep 7
$S input tap 1280 500; sleep 2.5; mark seek_shown; sleep 1
# 4. 네트워크를 끄고 메인 -> 재생 목록
$S svc wifi disable; sleep 1; $S input keyevent 4; sleep 2.5
set -- $(ui_xy '재생 목록'); mark playlist; $S input tap $1 $2
for i in $(seq 1 15); do sleep 1; $S uiautomator dump /sdcard/u.xml >/dev/null 2>&1; $S cat /sdcard/u.xml | grep -q '이어보기' && { mark playlist_loaded; break; }; done
sleep 5
mark end
rec_stop b
$S svc wifi enable
cat "$EV"
