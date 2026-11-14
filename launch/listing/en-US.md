# 스토어 등록정보 영어(en-US)

- 용도: **1차 글로벌 출시**(미국·캐나다·호주·뉴질랜드·인도·필리핀·싱가포르, 기존 한국 유지)용. #144
- 갱신: 2026-11-13, launch. 앱 화면 영어화(PR #163, #164, #170) 병합 뒤 기준으로 고쳤다. 화면 이름은 `program/android/app/src/main/res/values/strings.xml`의 영어 문구와 맞췄다.
- 한국어 등록정보 `launch/listing/ko-KR.md`와 뜻이 맞는지는 Play Console에 넣기 전에 사용자가 확인한다.

## 결정

- **기본 언어는 한국어(ko-KR) 그대로 둔다.** 영어는 Play Console 기본 스토어 등록정보에 번역(English (United States) – en-US)으로 넣는다. 기본 언어를 en-US로 바꿀지는 PM 결정 사항이다(`launch/releases/global-phase1-checklist.md` 2절).
- 앱 화면이 이제 영어로 나오므로(기기 언어가 한국어가 아니면 영어) "한국어만 지원" 문장은 지웠다.
- 저작권 안내 한 줄과 스크린샷 영상 출처(CC BY) 한 줄을 설명 끝에 둔다.
- 피하는 표현(`marketing/CLAUDE.md`): 영화·드라마 제목, 토렌트 사이트 이름, "free movies", "latest releases", "unlimited downloads". 아래 문안에 없다(2026-11-13 점검). Sintel·Big Buck Bunny·Tears of Steel은 스크린샷 출처 표기라 해당하지 않는다.
- 스크린샷: `launch/store-assets/*/en-US`는 2026-11-14에 **영어 UI**(v.2026.11.13.001 디버그, main 667c06e)로 다시 찍었다. 같은 날 marketing이 전환율 관점으로 순서·캡션을 고쳐 휴대전화·7인치·10인치 각 7장으로 다시 합성했다(재생 → 메인 → 이동 → 재생 목록 → 링크 열기 → 자막 → 저장 파일, 준비 장면은 뺐다. 근거: `marketing/reports/store-screenshots-en-2026-11.md`). 메인 화면이 나오는 `02_main`, `05_open_link`(기기 3종)는 2026-11-14에 로고 가운데 정렬(#187)이 들어간 v.2026.11.14.001 빌드로 다시 찍었다. 화면에 나오는 영상은 Sintel, Big Buck Bunny, Tears of Steel이라 아래 출처 문장과 맞는다(링크 확인 시트의 마그넷도 기기 3종 모두 Tears of Steel).
- 홍보 영상 `7_Promo_Video/PiecePlay_promo_en-US.mp4`는 자막만 영어이고 영상 속 앱 화면은 **한국어 UI**(v.2026.11.12.003)다(3·9·25초 프레임 확인). 영어 등록정보의 동영상 칸에 넣으려면 다시 만들어야 한다.

## 앱 이름 (30자 이내)

```
PiecePlay: Torrent Player
```

(25자. Google Play에 게시된 제목이다. 사용자 결정 2026-11-14: 브랜드 PiecePlay와 이 제목을 그대로 둔다.)

## 간단한 설명 (80자 이내)

```
Paste a magnet or .torrent link and start watching before the download ends.
```

(76자)

## 자세한 설명 (4000자 이내)

```
PiecePlay plays a video while it is still downloading. Paste a magnet link or the web address of a .torrent file, and playback starts as soon as the beginning of the video is ready.

How it works
• Downloads the parts near your playback position first, so you can start watching in seconds.
• Seek anywhere: the download follows where you jump to.
• See what is ready at a glance with the Download pieces map and a progress bar that shows played and downloaded parts.
• Only the video and its subtitle files are downloaded from a torrent. PiecePlay does not upload to other peers and disconnects as soon as a download finishes.

Open links from other apps
• Tap a magnet link in your browser or messenger, or share a link to PiecePlay, and choose Play now, Save for later, or Cancel.

Subtitles
• Choose subtitle and audio tracks during playback, including subtitle files that come with the video.
• With "Show subtitles automatically" on, subtitles in your device language turn on by themselves when available.

Playlist and storage
• Your recent links are kept in your Playlist, and you can resume where you left off.
• Download up to three videos at once, with more waiting in a queue.
• Manage Stored files by type and delete them when you need space.

Save mobile data
• Turn on "Wi-Fi only" in Settings to pause downloads on mobile data. They continue when you connect to Wi-Fi.
• If you start a video on mobile data, PiecePlay asks before playing.

Privacy
• Your Playlist, resume positions and downloaded files stay on your device. Your Playlist and resume data are encrypted.
• PiecePlay has no account and no server of its own.
• The app shows ads through Google AdMob. Where required, you are asked for consent first, and you can change your choices from "Ad privacy settings" on the Home screen or in Settings.
• Peer-to-peer downloading means your IP address is visible to other peers in the same torrent.

PiecePlay does not provide or find content. Please use it only for content you have the right to download and watch.

Screenshots show open movies by the Blender Foundation (Sintel, Big Buck Bunny, Tears of Steel), licensed under CC BY, © Blender Foundation | www.blender.org.
```

- 화면 문구 대응: `Download pieces`(`piece_map_title`), `Play now`·`Save for later`(`action_stream_play`·`action_save_for_later`), `Show subtitles automatically`(`settings_auto_subtitles`), `Wi-Fi only`(`settings_wifi_only`), `Playlist`·`Stored files`·`Home`·`Settings`·`Ad privacy settings`.
- "Ad privacy settings"는 동의가 필요한 지역에서만 보인다(UMP). 1차 국가 중에는 미국 주 메시지를 게시할 때 미국 일부 주에서 보인다. 그래서 "Where required" 문장 안에 두었다.

## 출시 노트 (500자 이내)

영어 UI가 처음 나가는 빌드용이다. 그 빌드의 PATCH_NOTES와 다시 대조한다.

```
PiecePlay is now available in English.
• The app shows English on devices that aren't set to Korean.
• Open magnet links from your browser or messenger, or share a link to PiecePlay.
• New Settings: "Wi-Fi only" pauses downloads on mobile data, and subtitles can turn on automatically in your device language.
• Pick subtitle and audio tracks during playback, including subtitle files that come with the video.
• Saved videos now download from the beginning, so you can start watching sooner.
```

(492자)
