# 스토어 등록정보 영어(en-US) 초안

전 세계 출시용 번역 초안이다. 한국어 원문이 저장소에 없어서(`launch/listing/ko-KR.md` 없음) 앱 화면과 #49 스토어 그래픽 문구를 기준으로 썼다. Play Console에 넣기 전에 한국어 등록정보와 뜻이 맞는지 사용자가 확인한다.

## 결정

- **기본 언어는 한국어(ko-KR)로 둔다.** 앱 화면이 한국어뿐이고 개발자·주 사용자가 한국이다. 영어는 Play Console 기본 스토어 등록정보에 "번역 추가"로 넣는다(Grow users > Translations 또는 Store listing > Manage translations > English (United States) – en-US). 영어는 Play 무료 기계 번역 대상 언어가 아니므로 직접 넣는다.
- **앱 화면이 한국어만 된다는 사실을 영어 설명에 적는다.** 숨기면 설치 후 실망·환불·낮은 평점으로 이어지고, 등록정보가 앱과 다르면 메타데이터 정책 위험도 있다. 설명 첫 부분 가까이에 한 줄로 둔다.
- 저작권 안내 한 줄과 스크린샷 영상 출처(CC BY) 한 줄을 설명 끝에 둔다.
- 피하는 표현: 영화·드라마 제목, 토렌트 사이트 이름, "free movies", "latest releases", "unlimited downloads".

## 앱 이름 (30자 이내)

```
PiecePlay
```

(한국어 이름과 같게 둔다. 설명을 붙이려면 `PiecePlay: Stream While Loading` 29자.)

## 간단한 설명 (80자 이내)

```
Paste a magnet or .torrent link and start watching before the download ends.
```

(76자)

## 자세한 설명 (4000자 이내)

```
PiecePlay plays a video while it is still downloading. Paste a magnet link or the web address of a .torrent file, and playback starts as soon as the first part of the video is ready.

Note: the app interface is currently available in Korean only. English support is planned.

How it works
• Downloads the parts near your playback position first, so you can start watching in seconds.
• Seek anywhere: the download follows where you jump to.
• See what is ready at a glance with a piece map and a progress bar that shows played and downloaded parts.
• Only the main video file in a torrent is downloaded. PiecePlay does not upload to other peers and stops sharing as soon as a download is complete.

Playlist and storage
• Your recent links are kept in a playlist, and you can resume where you left off.
• Download up to three videos at once, with more waiting in a queue.
• Manage stored files by type and delete them when you need space.

Privacy
• Your playlist, playback positions and downloaded files stay on your device. The playlist is encrypted with the Android Keystore.
• PiecePlay has no account and no server of its own.
• The app shows ads through Google AdMob. Where required, you are asked for consent first, and you can change your ad privacy choices from the main screen.
• Peer-to-peer downloading means your IP address is visible to other peers in the same torrent.

Please use PiecePlay only for content you have the right to download and watch.

Screenshots show open movies by the Blender Foundation (Sintel, Big Buck Bunny, Tears of Steel), licensed under CC BY, © Blender Foundation | www.blender.org.
```

## 출시 노트 (500자 이내)

```
First public release of PiecePlay.
• Paste a magnet or .torrent link and start watching before the download finishes.
• The part just ahead of your playback position is downloaded first for smoother playback.
• Manage downloads from your playlist and resume where you left off.
• New dark design and a cleaner player screen.
• Your videos and history stay on your device, encrypted.
The app interface is in Korean only for now.
```
