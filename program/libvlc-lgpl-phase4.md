# LibVLC GPL 제거 4단계 — 앱 통합과 회귀 기록

작성: program, 2026-11-14. 계획: `program/libvlc-lgpl-plan.md` 8절 작업 7~8. 앞 단계: `program/libvlc-lgpl-phase3.md`. 이슈 #101.
PM 결정(2026-11-14): 3단계 (b)안 — `3.7.5-lgpl.2`를 새로 내서 쓴다. 내일 출시(빌드 32)에는 넣지 않고 다음 업데이트용이다.

## 1. 결과

- 앱 의존성 `org.videolan.android:libvlc-all:3.7.5` → `pieceplay.libvlc:libvlc-lgpl:3.7.5-lgpl.2`(자체 빌드, GPL 구성요소 없음).
- Release: https://github.com/kywood/pieceplay-libvlc/releases/tag/3.7.5-lgpl.2
  - Actions 실행 [43576909372](https://github.com/kywood/pieceplay-libvlc/actions/runs/43576909372)(태그 커밋 `faa80bc`)의 산출물을 그대로 올렸다. `.so` 12개는 3단계에서 같은 소스로 빌드한 것(Actions 2회, 로컬 WSL 1회)과 SHA-256이 같다.
  - 자산: `libvlc-lgpl-3.7.5-lgpl.2.aar`(82,043,842 B, SHA-256 `5ef801c030ead76871bf9cd4662137bd8ce1d5f038e024e99fa2ce84c34c2058`), `libvlc-lgpl-3.7.5-lgpl.2-vlc-sources.tar.xz`(패치 21개를 적용한 VLC 소스, 15 MB), `libvlc-lgpl-3.7.5-lgpl.2-contrib-sources.tar`(번들 라이브러리 원본 57개, 463 MB), `SHA256SUMS`.
  - 4 ABI가 받은 contrib 원본 목록·해시가 모두 같았다(ABI별 `contrib-tarballs-<abi>.sha256` 동일).
- 앱 코드(Kotlin) 변경은 오픈소스 고지 목록 데이터뿐이다. 재생 코드와 VLC 옵션은 바꾸지 않았다(공개 API 차이 없음, phase2 4절).

## 2. AAR 공급 방식

계획 4.1절 C안(GitHub Release 자산 + Gradle `ivy` 저장소 + 체크섬 고정)을 따랐다. 저장소에는 바이너리를 커밋하지 않는다.

| 파일 | 내용 |
|---|---|
| `settings.gradle.kts` | `exclusiveContent`로 그룹 `pieceplay.libvlc`만 `ivy` 저장소 `https://github.com/kywood/pieceplay-libvlc/releases/download/[revision]/[artifact]-[revision].[ext]`에서 받는다. 메타데이터 없이 파일만(`metadataSources { artifact() }`) |
| `gradle/libs.versions.toml` | `libvlc = "3.7.5-lgpl.2"`, 좌표 `pieceplay.libvlc:libvlc-lgpl`. Release에 POM이 없으므로 공식 `libvlc-all 3.7.5` POM의 의존성 `androidx.annotation:annotation:1.7.1`, `androidx.legacy:legacy-support-v4:1.0.0`을 따로 적었다(`kotlin-stdlib`은 앱에 이미 있다) |
| `app/build.gradle.kts` | `implementation(libs.libvlc) { artifact { type = "aar" } }`와 위 두 의존성 |
| `gradle/verification-metadata.xml`(새 파일) | Gradle dependency verification. 이 AAR의 SHA-256만 고정하고, 다른 모든 그룹은 정규식 `^(?!pieceplay[.]libvlc$).*$`으로 신뢰해 지금처럼 검사하지 않는다. 다른 라이브러리를 추가·변경할 때 이 파일을 고칠 필요가 없다 |

- 확인: 해시를 일부러 틀리게 바꾸면 빌드가 `Dependency verification failed ... libvlc-lgpl-3.7.5-lgpl.1.aar ... from repository PiecePlayLibVlcReleases`로 멈춘다(`lgpl.1`로 시험, 값은 되돌렸다).
- 첫 빌드 때 약 82 MB를 GitHub에서 받고 이후는 Gradle 캐시를 쓴다. 오프라인 첫 빌드는 불가하다.
- 의존성 해석 결과: `pieceplay.libvlc:libvlc-lgpl:3.7.5-lgpl.2`, `androidx.legacy:legacy-support-v4:1.0.0`, `androidx.annotation:annotation:1.7.1 -> 1.10.0`(다른 라이브러리가 이미 1.10.0을 쓴다). 기존과 같은 구성이다.
- 버전을 올릴 때: `libs.versions.toml`의 `libvlc`, `verification-metadata.xml`의 component 버전·파일 이름·`sha256`(Release `SHA256SUMS`의 AAR 값) 세 곳을 함께 바꾼다. `OpenSourceLicenses.kt`의 `LIBVLC_VERSION`도 같아야 한다(`OpenSourceLicensesTest`가 확인).

## 3. 오픈소스 고지 목록(데이터)

`OpenSourceLicensesTest.listedVersionsMatchTheBuild`가 카탈로그 버전과 고지 버전이 같은지 검사하므로, 의존성 교체와 같은 PR에 넣었다.

- `LIBVLC`: 버전 `3.7.5-lgpl.2`, 주소 `https://github.com/kywood/pieceplay-libvlc`(전문 보기의 "소스 코드" 링크가 여는 곳 = 이 빌드의 소스·패치·스크립트·AAR·소스 묶음이 있는 저장소).
- `LIBVLC_GPL_COMPONENTS`(x264, libdvdnav, libdvdread, libpostproc, GPL 2.0) 항목과 `res/raw/license_gpl_2_0.txt`를 뺐다.
- 테스트: GPL 항목·GPL 전문이 남지 않았는지(`noGplNoticeIsLeft`), LibVLC 링크가 공개 저장소이고 Gradle이 같은 저장소의 Release에서 받는지(`libVlcSourceLinkIsThePublishedBuild`)를 더했다.
- 화면 문구는 다음 PR(브랜치 `codex/101-libvlc-license-text`)에서 다룬다. `licenses_lgpl_notice`(LibVLC 전문 보기 위 안내)를 영어·한국어 모두 "VideoLAN 소스에서 GPL 구성요소를 빼고 빌드했다, 전체 소스·변경 내용·빌드 스크립트·함께 들어 있는 라이브러리 소스를 아래 주소(공개 저장소)에서 받을 수 있다, 수정·교체할 수 있다"로 고쳤다. 그 밖의 문구는 GPL을 언급하지 않아 그대로 둔다. **[확인 필요: 안내 문구가 LGPL 2.1 고지 요건에 맞는지, design 명세 103줄과 같이 launch 또는 사용자가 확인]**
- design 명세 `design/specs/open-source-licenses.md` 62줄은 아직 `libvlc-all`, `https://code.videolan.org/videolan/vlc-android`, "GPL 모듈이 들어 있는지"를 적고 있다. design 소유라 고치지 않았다(PM에 전달).

## 4. 크기(릴리스, 같은 main `c246ddd` 기준 공식 AAR 빌드와 비교)

| 산출물 | 공식 `libvlc-all 3.7.5` | `3.7.5-lgpl.2` | 차이 |
|---|---:|---:|---:|
| AAB | 129,857,553 B | 118,732,265 B | −11,125,288 B (−8.6%) |
| APK(4 ABI, 네이티브 비압축 저장) | 287,337,638 B | 263,745,538 B | −23,592,100 B |

AAB 안 ABI별 네이티브(`libvlc.so`+`libvlcjni.so`+`libc++_shared.so`+libtorrent4j 등 그 ABI 전체):

| ABI | 원본 크기 차이 | 압축 크기 차이(Play 기기별 다운로드 근사) |
|---|---:|---:|
| arm64-v8a | −5,756,592 B | −2,742,626 B |
| armeabi-v7a | −5,693,696 B | −2,746,402 B |
| x86 | −5,726,280 B | −2,581,962 B |
| x86_64 | −6,408,360 B | −2,992,946 B |

`assets/lua/**`(128,335 B)도 빠졌다. 계획 추정(다운로드 1~3 MB 감소)과 맞는다.

## 5. 검사

| 항목 | 결과 |
|---|---|
| `python3 -I program/tools/libvlc_gpl_scan.py` 새 릴리스 AAB·APK | 4 ABI 모두 ok(GPL 흔적 0), 64비트 LOAD 정렬 16384 |
| 같은 스캐너, 기존 main 릴리스 AAB(공식 AAR) | 4 ABI 모두 FAIL(`x264=1, libpostproc=1, libdvdnav=4, libdvdread=10, realrtsp=2, lua=1`, 32비트는 `gpl-module-license=1` 추가) |
| `zipalign -c -P 16 -v 4` 새 릴리스 APK | `libvlc.so`·`libvlcjni.so`·`libc++_shared.so` 모두 OK |
| APK `lib/*/libvlc.so` SHA-256 | Release `SHA256SUMS`와 같다 |
| `:app:testDebugUnitTest` | 통과(실패 0). `OpenSourceLicensesTest` 8개 통과 |
| `:app:lintDebug` | 오류 0, 경고 34. 이번 변경으로 생긴 것은 `GradleDependency`(annotation 1.7.1보다 새 버전 있음) 1건. 공식 POM과 같은 값을 적은 것이고 실제로는 1.10.0으로 해석된다 |
| `:app:assembleDebug`, `:app:assembleRelease`, `:app:bundleRelease` | 성공 |

## 6. VLC 옵션과 빠진 모듈의 영향

앱이 VLC에 넘기는 입력은 두 가지뿐이다: 미완료 파일 `http://127.0.0.1:<port>/stream`(NanoHTTPD), 완료 파일 `file://` URI(`VlcPlayerScreen`). 외부 자막은 `MediaPlayer.addSlave(Subtitle, file://…)`(`VlcTrackPlayer`).

| 앱이 쓰는 것 | 필요한 모듈 | 이번 빌드 정적 모듈 목록(252개) |
|---|---|---|
| `http://127.0.0.1` 스트림 | `http`, `adaptive` | 있음 |
| 파일 URI | `filesystem` | 있음 |
| mp4 / mkv·webm / ts·ps | `mp4`, `mkv`, `ts`, `ps`, `es`, `avformat` | 있음 |
| 영상 디코딩 | `mediacodec`, `avcodec`, `dav1d`, `vpx`, `packetizer_h264`·`hevc`·`av1` | 있음 |
| 음성 | `avcodec`, `opus`, `vorbis`, `flacsys`, `spdif`, `android_audiotrack`, `opensles_android`, `audio_format`, `scaletempo` | 있음 |
| 화면 출력 | `android_window`, `egl_android`, `gles2`, `swscale`, `chain`, `yuvp`, `blend` 등 | 있음 |
| 외부 자막 .srt/.smi/.ass, 자막 렌더링 | `subtitle`(SRT·SAMI·ASS 파싱), `subsdec`, `libass`, `freetype`, `substx3g`, `subsusf`, `webvtt`, `ttml`, `vobsub`, `cc` | 있음 |
| 계측 테스트의 PNG 정지 화상(`:image-duration`) | `image`, `png` | 있음 |
| 옵션 `--drop-late-frames`, `--skip-frames`, `--file-caching`, `--no-sub-autodetect-file`, `-vvv`(debug), `:network-caching`, `:file-caching` | VLC 코어·접근 모듈 옵션 | 해당 모듈 있음. 옵션 이름은 코어에 있으므로 영향 없음 |
| 오디오·자막 트랙 전환, 탐색 | `setAudioTrack`, `setSpuTrack`, `time =` | Java API 동일, 코어 기능 |

빠진 것과 영향:

| 빠진 것 | 영향 |
|---|---|
| TLS(gnutls), https | 앱은 VLC에 https를 넘기지 않는다. `.torrent` HTTPS 다운로드는 앱 코드(`TorrentInputResolver`)가 한다 |
| Lua(플레이리스트·메타 스크립트) | 쓰지 않는다 |
| RTSP(live555, realrtsp), DVD | 쓰지 않는다 |
| `rotate` 필터 | 회전 메타데이터는 Android vout이 처리한다고 본다. **[확인 필요: 회전 메타데이터가 있는 영상]** |
| `deinterlace`, `hqdn3d` | 인터레이스 영상(주로 방송 녹화 ts)에서 빗살 무늬가 보일 수 있다. 앱은 디인터레이스 옵션을 켜지 않으므로 기존에도 자동 적용 여부만 차이다 **[확인 필요: 인터레이스 샘플]** |
| `dolby_surround_decoder`, `headphone_channel_mixer`, `mono` | 5.1→스테레오 다운믹스는 `audio_format`·`trivial_channel_mixer`·`simple_channel_mixer` 등이 맡는다. 돌비 서라운드 디코딩·헤드폰 가상화는 빠진다 |
| `dummy` | 인터페이스·출력 더미 모듈. libvlc 재생에는 쓰이지 않는다 |

## 7. 계측 테스트(에뮬레이터, PM 차례 받아 실행)

재생·라이선스 경로를 지나는 것:

| 테스트 | 이유 |
|---|---|
| `SubtitleUiTest` | 실제 `LibVLC`·`MediaPlayer`로 PNG를 열고 SMI(CP949→UTF-8)·SRT를 `addSlave`로 붙여 트랙 인식과 전환을 확인. sintel 테스트는 앱 엔진→`127.0.0.1` 스트림 재생과 자막 |
| `CompletedPlaybackSwitchTest` | 완료된 mp4를 파일 URI로 재생 |
| `QuickDownloadResumeTest`, `IndependentDownloadTest` | 다운로드 중 재생 경로 |
| `OpenSourceLicensesFlowTest` | 라이선스 화면에서 LibVLC 항목과 LGPL 안내 |
| 전체 `connectedDebugAndroidTest` | 그 밖의 회귀 |

결과: **아직 실행하지 않았다**(PM에게 에뮬레이터 차례 요청). 결과가 나오면 이 절에 적는다.

## 8. 남은 위험과 [확인 필요]

- 계측 테스트·실재생 확인 전이다(7절).
- 코덱별 스모크 테스트(H.265·AV1·VP9 × AAC·AC3·Opus, mkv+ASS)는 아직 없다(계획 5.2). 지금 계측 테스트는 PNG와 sintel(H.264/mkv 계열)만 지난다.
- 빌드는 GitHub Release에 기대고 있다. Release 자산이 지워지면 새 PC·CI의 첫 빌드가 실패한다(캐시가 있으면 계속 된다). 해시는 고정되어 있으므로 바꿔치기는 막힌다.
- [확인 필요] LGPL 2.1 §6 요건(재링크 가능성, 서명된 APK에서의 교체, 소스 제공 방식)은 법률 확인이 필요하다(계획 6절). 이번 Release는 VLC 소스와 contrib 원본을 같은 자리에 두어 소스 제공 위치를 하나로 모았다.
- [확인 필요] 고지 목록에 LibVLC 안의 LGPL·BSD·MIT·FTL 구성요소(contrib 57개)를 각각 적어야 하는지. 지금은 LibVLC·FFmpeg만 따로 적는다(기존과 같은 범위).
- `libvlccore`·`libgpg-error`에 남은 "GNU General Public License" 안내 문자열 2건(phase1 3절)은 그대로다.
