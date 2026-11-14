# LibVLC GPL 제거 4단계 — 앱 통합과 회귀 기록

작성: program, 2026-11-14. 계획: `program/libvlc-lgpl-plan.md` 8절 작업 7~8. 앞 단계: `program/libvlc-lgpl-phase3.md`. 이슈 #101.
PM 결정(2026-11-14): 3단계 (b)안 — 새 Release를 내서 쓴다. 내일 출시(빌드 32)에는 넣지 않고 다음 업데이트용이다.
**현재 앱이 쓰는 것은 `3.7.5-lgpl.3`이다.** 처음 넣은 `3.7.5-lgpl.2`는 reviewer 검토에서 GPL-2 코드(zvbi, arm64는 FFmpeg aarch64 파일 3개)가 확인되어 대체했다(9절). 1·2·4·5절은 `lgpl.3` 기준으로 고쳤고, `lgpl.2` 때 수치는 각 절의 괄호와 9절에 남겼다.

## 1. 결과

- 앱 의존성 `org.videolan.android:libvlc-all:3.7.5` → `pieceplay.libvlc:libvlc-lgpl:3.7.5-lgpl.3`(자체 빌드, 링크된 GPL 소스 0).
- Release: https://github.com/kywood/pieceplay-libvlc/releases/tag/3.7.5-lgpl.3
  - Actions 실행 [43586876344](https://github.com/kywood/pieceplay-libvlc/actions/runs/43586876344)(태그 커밋 `41e7e12`, PR #2 병합)의 산출물을 그대로 올렸다. `.so` 12개는 PR 실행 [43584765658](https://github.com/kywood/pieceplay-libvlc/actions/runs/43584765658)과 SHA-256이 같고, arm64·armeabi-v7a `libvlc.so`는 로컬 WSL 빌드와도 같다.
  - 자산: `libvlc-lgpl-3.7.5-lgpl.3.aar`(80,910,311 B, SHA-256 `aafbb3ca121b69f8e87e381fd4c8cb286cb0e64010a10d6d7407af5c4f62f854`), `libvlc-lgpl-3.7.5-lgpl.3-vlc-sources.tar.xz`(패치 22개를 적용한 VLC 소스, 15 MB), `libvlc-lgpl-3.7.5-lgpl.3-contrib-sources.tar`(번들 라이브러리 소스 아카이브 54개 + fluidlite `.githash` 1개 = 파일 55개, 458 MB), `SHA256SUMS`.
  - (`lgpl.2`: 82,043,842 B, SHA-256 `5ef801c0…`, contrib 소스 아카이브 56개 + `.githash` 1개 = 파일 57개. GPL 코드 포함으로 대체됨. Release 본문에 경고를 달았고 지우지 않았다.)
  - 4 ABI가 받은 contrib 원본 목록·해시가 모두 같았다(ABI별 `contrib-tarballs-<abi>.sha256` 동일).
- 앱 코드(Kotlin) 변경은 오픈소스 고지 목록 데이터뿐이다. 재생 코드와 VLC 옵션은 바꾸지 않았다(공개 API 차이 없음, phase2 4절).

## 2. AAR 공급 방식

계획 4.1절 C안(GitHub Release 자산 + Gradle `ivy` 저장소 + 체크섬 고정)을 따랐다. 저장소에는 바이너리를 커밋하지 않는다.

| 파일 | 내용 |
|---|---|
| `settings.gradle.kts` | `exclusiveContent`로 그룹 `pieceplay.libvlc`만 `ivy` 저장소 `https://github.com/kywood/pieceplay-libvlc/releases/download/[revision]/[artifact]-[revision].[ext]`에서 받는다. 메타데이터 없이 파일만(`metadataSources { artifact() }`) |
| `gradle/libs.versions.toml` | `libvlc = "3.7.5-lgpl.3"`, 좌표 `pieceplay.libvlc:libvlc-lgpl`. Release에 POM이 없으므로 공식 `libvlc-all 3.7.5` POM의 의존성 `androidx.annotation:annotation:1.7.1`, `androidx.legacy:legacy-support-v4:1.0.0`을 따로 적었다(`kotlin-stdlib`은 앱에 이미 있다) |
| `app/build.gradle.kts` | `implementation(libs.libvlc) { artifact { type = "aar" } }`와 위 두 의존성 |
| `gradle/verification-metadata.xml`(새 파일) | Gradle dependency verification. 이 AAR의 SHA-256만 고정하고, 다른 모든 그룹은 정규식 `^(?!pieceplay[.]libvlc$).*$`으로 신뢰해 지금처럼 검사하지 않는다. 다른 라이브러리를 추가·변경할 때 이 파일을 고칠 필요가 없다 |

- 확인: `verification-metadata.xml`의 버전·파일 이름은 그대로 두고 `sha256` 값만 앞 16자리를 `0`으로 바꾸면, 받은 AAR의 해시가 맞지 않아 빌드가 `Dependency verification failed ... One artifact failed verification: libvlc-lgpl-3.7.5-lgpl.1.aar ... from repository PiecePlayLibVlcReleases`로 멈춘다(`lgpl.1`로 시험, 값은 되돌렸다). `lgpl.3`에서 다시 한 결과는 9.7절.
- 첫 빌드 때 약 81 MB를 GitHub에서 받고 이후는 Gradle 캐시를 쓴다. 오프라인 첫 빌드는 불가하다.
- 의존성 해석 결과(`lgpl.2` 때 확인, `lgpl.3`은 버전만 다름): `pieceplay.libvlc:libvlc-lgpl:3.7.5-lgpl.2`, `androidx.legacy:legacy-support-v4:1.0.0`, `androidx.annotation:annotation:1.7.1 -> 1.10.0`(다른 라이브러리가 이미 1.10.0을 쓴다). 기존과 같은 구성이다.
- 버전을 올릴 때: `libs.versions.toml`의 `libvlc`, `verification-metadata.xml`의 component 버전·파일 이름·`sha256`(Release `SHA256SUMS`의 AAR 값) 세 곳을 함께 바꾼다. `OpenSourceLicenses.kt`의 `LIBVLC_VERSION`도 같아야 한다(`OpenSourceLicensesTest`가 확인).

## 3. 오픈소스 고지 목록(데이터)

`OpenSourceLicensesTest.listedVersionsMatchTheBuild`가 카탈로그 버전과 고지 버전이 같은지 검사하므로, 의존성 교체와 같은 PR에 넣었다.

- `LIBVLC`: 버전 `3.7.5-lgpl.3`, 주소 `https://github.com/kywood/pieceplay-libvlc`(전문 보기의 "소스 코드" 링크가 여는 곳 = 이 빌드의 소스·패치·스크립트·AAR·소스 묶음이 있는 저장소).
- `LIBVLC_GPL_COMPONENTS`(x264, libdvdnav, libdvdread, libpostproc, GPL 2.0) 항목과 `res/raw/license_gpl_2_0.txt`를 뺐다.
- 테스트: GPL 항목·GPL 전문이 남지 않았는지(`noGplNoticeIsLeft`), LibVLC 링크가 공개 저장소이고 Gradle이 같은 저장소의 Release에서 받는지(`libVlcSourceLinkIsThePublishedBuild`)를 더했다.
- 화면 문구(`licenses_lgpl_notice` 등, `values`·`values-ko`)는 다음 PR에서 다룬다.
- design 명세 `design/specs/open-source-licenses.md` 62줄은 아직 `libvlc-all`, `https://code.videolan.org/videolan/vlc-android`, "GPL 모듈이 들어 있는지"를 적고 있다. design 소유라 고치지 않았다(PM에 전달).

## 4. 크기(릴리스, 같은 main `c246ddd` 기준 공식 AAR 빌드와 비교)

| 산출물 | 공식 `libvlc-all 3.7.5` | `3.7.5-lgpl.2` | `3.7.5-lgpl.3` | 차이(공식→lgpl.3) |
|---|---:|---:|---:|---:|
| AAB | 129,857,553 B | 118,732,265 B | 117,598,723 B | −12,258,830 B (−9.4%) |
| APK(4 ABI, 네이티브 비압축 저장) | 287,337,638 B | 263,745,538 B | 261,320,706 B | −26,016,932 B |

AAB 안 ABI별 네이티브(`libvlc.so`+`libvlcjni.so`+`libc++_shared.so`+libtorrent4j 등 그 ABI 전체):

| ABI | 원본 크기 차이(lgpl.3) | 압축 크기 차이(lgpl.3, Play 기기별 다운로드 근사) | (lgpl.2 압축 차이) |
|---|---:|---:|---:|
| arm64-v8a | −6,351,608 B | −3,020,636 B | −2,742,626 B |
| armeabi-v7a | −6,241,640 B | −3,021,903 B | −2,746,402 B |
| x86 | −6,339,744 B | −2,868,988 B | −2,581,962 B |
| x86_64 | −7,048,280 B | −3,285,939 B | −2,992,946 B |

`assets/lua/**`(128,335 B)도 빠졌다. 계획 추정(다운로드 1~3 MB 감소)과 맞는다.

## 5. 검사

| 항목 | 결과 |
|---|---|
| `python3 -I program/tools/libvlc_gpl_scan.py`(zvbi·librist 표식 추가판) `lgpl.3` 릴리스 AAB·APK | 4 ABI 모두 ok(GPL 흔적 0), 64비트 LOAD 정렬 16384. 같은 스캐너로 `lgpl.2` AAR은 4 ABI 모두 FAIL(`zvbi=2, librist=2`) |
| 같은 스캐너, 기존 main 릴리스 AAB(공식 AAR) | 4 ABI 모두 FAIL(`x264=1, libpostproc=1, libdvdnav=4, libdvdread=10, realrtsp=2, lua=1`, 32비트는 `gpl-module-license=1` 추가) |
| `zipalign -c -P 16 -v 4` 새 릴리스 APK | `libvlc.so`·`libvlcjni.so`·`libc++_shared.so` 모두 OK |
| APK `lib/*/libvlc.so` SHA-256 | Release `SHA256SUMS`와 같다 |
| `:app:testDebugUnitTest` | `lgpl.3`에서 `--rerun`으로 다시 실행해 통과(테스트 655개, 실패 0). `OpenSourceLicensesTest` 8개 통과 |
| `:app:lintDebug` | 오류 0, 경고 34. 이번 변경으로 생긴 것은 `GradleDependency`(annotation 1.7.1보다 새 버전 있음) 1건. 공식 POM과 같은 값을 적은 것이고 실제로는 1.10.0으로 해석된다 |
| `:app:assembleDebug`, `:app:assembleRelease`, `:app:bundleRelease`, `:app:assembleDebugAndroidTest` | `lgpl.3`에서 성공 |

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

### 결과(2026-11-14, emulator-5554 `sdk_gphone64_x86_64`, PM 차례 받아 실행)

기본 adb 서버에 실기기도 연결되어 있어 Gradle `connected…` 작업은 쓰지 않았다(연결된 기기 전체에 설치한다). `app-debug.apk`·`app-debug-androidTest.apk`를 `adb -s emulator-5554 install -r -t`로 넣고 `am instrument -w -r`로 돌렸다. 빌드는 #201+#202(`codex/101-libvlc-license-text`).

| 실행 | 결과 | 시간 |
|---|---|---|
| 다섯 클래스(`SubtitleUiTest` 3개, `CompletedPlaybackSwitchTest`, `QuickDownloadResumeTest`, `IndependentDownloadTest`, `OpenSourceLicensesFlowTest`) | `OK (7 tests)` | 120초 |
| 전체(25개 클래스), 앱 데이터 초기화 후 | `OK (55 tests)`, 크래시(`FATAL EXCEPTION`·`Fatal signal`) 0 | 229초 |
| 비교: 공식 `libvlc-all 3.7.5` main(`e39b1bc`) 전체, 같은 조건 | `OK (55 tests)`, 크래시 0 | 395초 |

- VLC는 `revision 3.0.23-2-851-gb793163796`(= 이 빌드의 VLC HEAD)으로 떴다. `http` 접근, `mp4`·`subtitle` demux, `avcodec` 디코더, `filesystem`(자막 slave), `android_audiotrack`, `scaletempo`가 쓰였다.
- VLC 오류 로그(`E/VLC`)를 두 빌드에서 세었다. `video output creation failed` 21건, `window: request … not implemented` 48건, `buffer deadlock prevented` 1건으로 **두 빌드가 같다.** 테스트가 끝나 화면(Surface)이 사라진 뒤 남은 재생기가 vout을 만들려다 실패하는 정리 단계 로그다. 새 빌드에만 있는 것은 `option sout-deinterlace-mode does not exist` 22건(`deinterlace` 모듈을 뺀 데서 나오는 안내, 기본값으로 계속 진행)뿐이다.
- 참고: sintel 재생에서 자막 렌더러(freetype+fontconfig)가 처음에 "Building font databases"로 약 13초를 쓰고, 그동안 첫 화면이 늦게 뜬다. **공식 빌드도 같다**(앱 데이터 초기화 뒤 sintel 테스트 하나로 비교: 공식 12.98초). 이번 교체와는 관계없는 기존 동작이다. 개선할 가치가 있으면 별도 이슈로 다룬다.
- 같은 비교에서 공식 빌드의 sintel 테스트가 한 번 실패했다(토렌트로 자막 파일을 받는 단계 60초 초과, 네트워크·스웜 시간 문제). VLC와 관계없고 네이티브 크래시(#169)도 아니다. 새 빌드는 같은 테스트를 통과했다.

## 8. 남은 위험과 [확인 필요]

- 계측 테스트는 x86_64 에뮬레이터에서만 돌렸다(소프트웨어 디코딩). arm64·armeabi-v7a 실기기의 `mediacodec` 하드웨어 디코딩 경로는 확인하지 않았다(실기기 사용 금지 지침).
- 코덱별 스모크 테스트(H.265·AV1·VP9 × AAC·AC3·Opus, mkv+ASS)는 아직 없다(계획 5.2). 지금 계측 테스트는 PNG, sintel(H.264 mp4 + SRT), 테스트용 mp4만 지난다.
- 빌드는 GitHub Release에 기대고 있다. Release 자산이 지워지면 새 PC·CI의 첫 빌드가 실패한다(캐시가 있으면 계속 된다). 해시는 고정되어 있으므로 바꿔치기는 막힌다.
- [확인 필요] LGPL 2.1 §6 요건(재링크 가능성, 서명된 APK에서의 교체, 소스 제공 방식)은 법률 확인이 필요하다(계획 6절). 이번 Release는 VLC 소스와 contrib 원본을 같은 자리에 두어 소스 제공 위치를 하나로 모았다.
- [확인 필요] 고지 목록에 LibVLC 안의 LGPL·BSD·MIT·FTL 구성요소(`lgpl.3` 기준 contrib 소스 아카이브 54개)를 각각 적어야 하는지. 지금은 LibVLC·FFmpeg만 따로 적는다(기존과 같은 범위).
- `libvlccore`·`libgpg-error`에 남은 "GNU General Public License" 안내 문자열 2건(phase1 3절)은 그대로다.

## 9. reviewer 지적과 `3.7.5-lgpl.3`(2026-11-14)

reviewer 검토에서 PR #201·#202가 보류됐다. 높음 1(zvbi)을 직접 확인했고 사실이었다. PM 결정: 링크 여부를 따지지 않고 GPL 파일을 빼서 `3.7.5-lgpl.3`을 낸다. `lgpl.1`·`lgpl.2` Release는 지우지 않고 본문 맨 위에 "GPL-2 코드 포함, `lgpl.3`으로 대체" 경고를 달았다.

### 9.1 확인 방법

- 대상: 3단계 로컬 빌드 트리(`c845e23`, `lgpl.2`와 같은 소스·패치·contrib). strip 전 `libvlc/jni/obj/local/<abi>/libvlc.so`에 DWARF가 남아 있다.
- `buildsystem/pieceplay-gpl-sources.py`(이번에 추가, 공개 저장소): 링크된 컴파일 단위(DW_AT_comp_dir + DW_AT_name)의 소스 파일 앞 6000바이트를 읽어 GPL 전용(LGPL·Library GPL 문구 없음, 다른 라이선스 선택지 없음)인지 가린다. arm64는 컴파일 단위 4,182개를 모두 찾아 읽었다(디스크에 없음 0). 32비트는 NDK r21 런타임(libgcc는 GCC Runtime Library Exception, bionic·libunwind는 BSD/MIT) 16개·8개만 없었다.
- 보조: contrib tarball 디렉터리의 파일 57개(소스 아카이브 56개 + fluidlite `.githash` 1개, `lgpl.2` 소스 기준)에서 GPL 헤더 소스를 모두 뽑아 위 결과와 대조했고, 이름이 겹친 경우(`iconv.o`, `plural-exp.o` 등)는 심볼로 확인했다.

### 9.2 링크된 GPL 코드

| 구성요소 | 파일 | 근거(원문) | 링크 확인 | ABI |
|---|---|---|---|---|
| zvbi 0.2.44 (`zvbi-0.2.44.tar.gz`, SHA-256 `bca620ab…5ac5e`) | `src/packet-830.c` | `COPYING.md`: "The files src/packet-830.\* and src/pdc.\* have the following copyright and license: … License: GPL-2". 파일 헤더: "it under the terms of the GNU General Public License version 2 as" | strip 전 `libvlc.so`에 `vbi_decode_teletext_8301_cni`, `…_8301_local_time`, `…_8302_cni`, `…_8302_pdc`가 정의되어 있다. LGPL인 `src/packet.c`가 `vbi_decode_teletext_8301_local_time`, `…_8302_pdc`를 부른다(`nm -u packet.o`). VLC `modules/codec/zvbi.c`가 `vbi_decoder_new`·`vbi_decode`를 쓴다 | 4개 모두 |
| zvbi 0.2.44 | `src/pdc.c` | 위와 같다 | `libzvbi.a`에는 있지만 `libvlc.so`에는 심볼이 없다(링크되지 않음). 같은 방침으로 zvbi 전체를 뺀다 | (링크 안 됨) |
| FFmpeg 8.1.2 (`ffmpeg-8.1.2.tar.xz`, SHA-256 `464beb5e…7b524c`, 태그 `n8.1.2` = 커밋 `38b88335f99e76ed89ff3c93f877fdefce736c13`) | `libavcodec/aarch64/ac3dsp_init_aarch64.c` | 7–8줄: " * it under the terms of the GNU General Public License as published by / * the Free Software Foundation; either version 2 of the License, or" | `ff_ac3dsp_init_aarch64` 정의됨(`T`) | arm64 |
| 〃 | `libavcodec/aarch64/ac3dsp_neon.S` | 8–9줄: 위와 같은 문구(Copyright 2011 Mans Rullgard, 2024 Geoff Hill) | `ff_ac3_exponent_min_neon`, `ff_ac3_extract_exponents_neon`, `ff_ac3_sum_square_butterfly_{float,int32}_neon` 정의됨 | arm64 |
| 〃 | `libavutil/aarch64/crc.S` | 7–8줄: 위와 같은 문구(Copyright 2025 Zhao Zhili) | `ff_crc32_aarch64`, `ff_crc_get_table_aarch64.crc32_ieee_le_ctx` 정의됨 | arm64 |

- FFmpeg 세 파일은 `LICENSE.md`의 "the GPL parts of FFmpeg" 목록에 없고, Makefile에서 `CONFIG_GPL` 조건 없이 빌드된다(`OBJS-$(CONFIG_AC3DSP) += aarch64/ac3dsp_init_aarch64.o`, `NEON-OBJS-$(CONFIG_AC3DSP) += aarch64/ac3dsp_neon.o`, `ARMV8-OBJS += aarch64/crc.o`). 상류 master(2026-11-14 조회)에서도 세 파일의 헤더는 GPL 그대로다. 32비트 `libavcodec/arm/ac3dsp_neon.S`는 LGPL이다.
- 공식 `libvlc-all 3.7.5`의 arm64에도 이 세 파일이 들어 있을 것이라는 점은 **추정이다**. 같은 FFmpeg 8.1.2·같은 contrib 규칙이지만 공식 `.so`는 strip되어 있고 심볼이 없어 확인하지 못했다.
- `lgpl.1`의 FFmpeg arm64 부분도 같은 이유(같은 소스·규칙)로 같다고 보지만, 그 빌드 트리는 지워져 바이너리로 확인하지 못했다. zvbi는 `lgpl.1`·`lgpl.2` 4 ABI 모두에 `libzvbi:%s: %s` 등 zvbi 문자열이 있어 확인했다(스캐너 새 표식).

### 9.3 GPL은 아니지만 남기거나 뺀 것

| 구성요소 | 파일 | 판단 |
|---|---|---|
| librist 0.2.18에 묶인 Mbed TLS | `contrib/mbedtls/library/*.c` 56개 | 헤더 `SPDX-License-Identifier: Apache-2.0 OR GPL-2.0-or-later`(이중 라이선스). GPL을 고를 필요는 없다. 다만 Apache-2.0과 LGPL-2.1을 한 바이너리에 함께 두는 것은 FFmpeg `LICENSE.md`도 맞지 않는다고 적는 조합이다 **[확인 필요]**. RIST는 쓰지 않으므로 PM 승인으로 librist를 끈다 |
| libgcrypt 1.12.0 jitterentropy | `random/rndjent.c` | BSD 3조항 또는 GPL 중 선택("…the GNU General Public License, in which case the provisions of the GPL are required INSTEAD OF the above restrictions"). BSD로 본다 |
| VLC webvtt | `modules/codec/webvtt/CSSGrammar.c`(bison 3.8.2 생성) | GPL 본문 + Bison 예외("As a special exception, you may create a larger work that contains part or all of the Bison parser skeleton and distribute that work under terms of your choice") |
| NDK r21 런타임(32비트) | libgcc `libgcc2.c` 등 | GCC Runtime Library Exception. 모든 NDK 앱에 같은 것 |

### 9.4 contrib 표(`lgpl.2` 소스 기준, 소스 아카이브 56개)

**세는 기준**: VLC contrib가 `contrib/tarballs/`에 받는 파일 수다. 대부분은 소스 아카이브(`.tar.*`)이고, git으로 받는 fluidlite는 받은 커밋을 적은 `fluidlite-<커밋>.githash`도 함께 남긴다(소스는 같은 이름의 `.tar.xz`). 그래서 `lgpl.2`는 파일 57개 = 소스 아카이브 56개 + `.githash` 1개, `lgpl.3`은 zvbi·librist가 빠져 파일 55개 = 소스 아카이브 54개 + `.githash` 1개다. Release의 contrib 소스 묶음에는 이 파일들이 그대로 들어 있다.

`GPL 헤더` = tarball 안 GPL 전용 헤더 소스 수(테스트·예제·도구 포함). `링크` = 그중 `libvlc.so`에 링크된 것(9.1 방법, arm64·armeabi-v7a·x86·x86_64).

| contrib | GPL 헤더 | 링크 | 비고 |
|---|---:|---|---|
| ffmpeg 8.1.2 | 157 | arm64: aarch64 3개 | 9.2. 나머지는 `--enable-gpl` 전용 필터·x86 asm·도구 |
| zvbi 0.2.44 | 38 | 4 ABI: `src/packet-830.c` | 9.2 |
| librist 0.2.18 | 270 | 4 ABI: Mbed TLS 56개(이중 라이선스) | 9.3, 끔 |
| flac 1.4.2 | 83 | - | 도구(`src/flac`, `metaflac` 등). libFLAC은 BSD |
| gettext 0.26 | 1097 | - | gettext-tools. `libintl`(LGPL)만 쓴다 |
| libiconv 1.17 | 14 | - | `src/iconv.c`(프로그램)·`srclib`. 라이브러리 `lib/iconv.c`는 LGPL(`libiconv.a`의 `iconv.o`는 `libiconv_open` 등을 정의) |
| libnfs 6.0.2 | 56 | - | 예제·유틸 |
| libtasn1 4.21.0 | 33 | - | `src/` 도구 |
| libpng 1.6.58 | 12 | - | `contrib/gregbook` 예제 |
| libgcrypt 1.12.0 | 6 | (`rndjent.c` 이중) | 9.3. 나머지는 `doc/`, `tests/` |
| fribidi 1.0.12 | 4 | - | `bin/getopt*` |
| libtheora 1.1.1 | 2 | - | `win32/experimental` |
| libsmb2 6.1 | 2 | - | `tests/`, `utils/` |
| libbluray 1.4.0 | 1 | - | `src/devtools` |
| libcddb 1.3.2 | 1 | - | `examples/` |
| libdvbpsi 1.3.3 | 1 | - | `misc/` 테스트 |
| libgpg-error 1.56 | 1 | - | `doc/yat2m.c` |
| dav1d, fluidlite, fontconfig, freetype, game-music-emu, harfbuzz, jpeg(v10), lame, libaom, libarchive, libass, libdsm, libebml, libgsm, libmatroska, libmodplug, libmysofa, libogg, libplacebo, libshout, libssh2, libvorbis, libvpx, libxml2, lua(빌드 도구), microdns, mpg123, openapv, openjpeg, opus, protobuf, pupnp, soxr, spatialaudio, speex, speexdsp, taglib, twolame, zlib | 0 | - | GPL 헤더 소스 없음 |

### 9.5 `lgpl.3` 변경(공개 저장소)

PR: https://github.com/kywood/pieceplay-libvlc/pull/2 (브랜치 `pieceplay/no-zvbi`).

| 커밋 | 내용 |
|---|---|
| `873d697` VLC 패치 `0022` | `contrib/src/ffmpeg/pieceplay-no-gpl-aarch64.patch`를 더하고 `rules.mak`의 `USE_FFMPEG` 패치 목록 끝에 적용한다. FFmpeg aarch64 Makefile에서 세 파일의 객체 줄을 지우고, `libavcodec/ac3dsp.c`(1곳)·`libavutil/crc.c`(3곳)의 `#if ARCH_AARCH64`를 `#if 0`으로 바꾼다. 이어지는 `#elif ARCH_ARM`/`#elif ARCH_X86`은 그대로라 다른 ABI는 바뀌지 않는다. `crc.c`의 일반 표는 `ctx[0] == 0`이라 C 경로로 간다 |
| `873ff71`·`97c2a79` compile-libvlc | `--license l`·`a`일 때 contrib `--disable-zvbi --disable-librist`, configure `--disable-zvbi --disable-librist`. 원래 인자 목록에 `--enable-zvbi`가 있어 contrib bootstrap이 "Same package(s) disabled and enabled at the same time"으로 멈췄으므로(첫 로컬 시도) 그 항목을 먼저 지운다. contrib 라이브러리 게이트에 `libzvbi.a`·`librist.a`, 모듈 블랙리스트에 `zvbi`·`rist`·`access_output_rist` |
| `782de00`·`cdb73f8` 검사 도구 | `pieceplay-gpl-sources.py`를 `ndk-build` 뒤에 실행(strip 전 `libvlc.so`). 9.2의 다섯 파일(zvbi `packet-830.c`·`pdc.c`, FFmpeg aarch64 3개)은 헤더와 관계없이 항상 실패로 본다. `libvlc_gpl_scan.py`(앱 `program/tools/`와 같은 파일)에 `zvbi`(`VBI and Teletext decoder`, `libzvbi:%s: %s`)·`librist`(`librist_api_version`, `RIST Receiver Library version`) 표식. 단어 `zvbi`·`librist`는 끈 뒤에도 configure 문자열(`--disable-zvbi`)에 남아 표식으로 쓰지 않는다. FFmpeg 세 파일은 strip된 `.so`에 문자열이 남지 않아 스캐너 표식이 없고, DWARF 검사가 맡는다 |
| `b010091` README | "`--license a`이면 GPL 패키지는 빌드되지 않는다" 문장을 사실대로 고침(패키지 단위 표시라 GPL 파일이 섞인 패키지는 못 막았다), 패치 목록 |

로컬 확인(WSL, `97c2a79`, arm64·armeabi-v7a, 각 ABI 새 clone):

| 항목 | arm64-v8a | armeabi-v7a |
|---|---|---|
| 빌드 | 성공 | 성공 |
| contrib 게이트 / 스캐너 / GPL 소스 검사 | ok / ok / 컴파일 단위 4,072개, **GPL 0** | ok / ok / 4,086개, **GPL 0**(NDK 런타임 16개 디스크에 없음) |
| 이중 라이선스·예외 | `rndjent.c` 1, bison `CSSGrammar.c` 1 | 같음 |
| `libzvbi.a`·`librist.a` | 없음 | 없음 |
| 정적 모듈 | `telx` 있음, `zvbi`·`rist` 없음 | 같음 |
| strip 전 `.so` 심볼 | `ff_ac3dsp_init` T, `av_crc` T, `ff_ac3dsp_init_aarch64`·`ff_ac3_exponent_min_neon`·`ff_ac3_extract_exponents_neon`·`ff_crc32_aarch64`·`ff_crc_get_table_aarch64` 없음, 정의 안 된 `ff_*`·`av_*` 0 | 확인하지 않음(패치는 aarch64 분기만 바꾼다. 32비트 `arm/ac3dsp_*`는 LGPL이라 그대로 쓴다) |
| `mbedtls_*`·`rist_*`·`vbi_decode*` 심볼 | 0 | 0 |
| `libvlc.so` SHA-256 | `686c089637c2f49a…` | `6d3180258c7ccce2…` |

- PM 조건 1: arm64에서 aarch64 init 심볼 자체가 없으므로 `ff_ac3dsp_init`이 그것을 부를 수 없다. 링크는 `--no-undefined`로 성공했고 정의되지 않은 FFmpeg 심볼이 없다. 그래서 AC3 디코더(다운믹스 포함)는 `ac3dsp.c`의 C 구현을 쓴다. **AC3/E-AC3 음성 영상 재생은 arm64 실기기 수동 확인 항목이다**(x86_64 에뮬레이터는 이 경로를 지나지 않는다).
- PM 조건 3: librist를 끈 뒤 두 ABI의 strip 전 `.so`에 `mbedtls_` 심볼이 0이다. 다른 모듈이 Mbed TLS를 끌어오지 않는다.

### 9.6 그 밖의 지적

- **중간 2 — 빌드 플래그.** 공식 `libvlc-all 3.7.5`의 `libvlc.so`에 남은 configure 줄(4 ABI 같음)과 우리 빌드를 비교했다.

  | 항목 | 공식 | 우리 |
  |---|---|---|
  | `compile-libvlc.sh` | `--release` 없이(디버그 구성) | `--release` |
  | VLC configure | `--enable-debug --disable-branch-protection` | 둘 다 없음 |
  | CFLAGS | `-g -O2 … -fstack-protector-strong`(NDEBUG 없음) | 같고 `-DNDEBUG` 추가(contrib에도 적용) |
  | ndk-build | `NDK_DEBUG=1` | `NDK_DEBUG=0` |

  - 이유: 우리는 배포용이라 계획 단계부터 `--release`로 빌드했다. 공식 Maven AAR이 디버그 구성인 것은 이번에 처음 알았다.
  - VLC 3.0.x `configure.ac`에는 branch protection 옵션이 없다(`git grep` 결과 없음). 그래서 `--disable-branch-protection`은 configure가 무시하는 인자이고, 두 빌드의 실제 차이가 아니다. arm64 컴파일러 기본값에도 `-mbranch-protection`은 없다.
  - 실제 차이는 `--enable-debug`/`NDEBUG`다. 공식 빌드는 VLC와 contrib의 `assert()`가 켜져 있어 조건이 깨지면 그 자리에서 abort한다. 우리 빌드는 assert가 꺼져 진행한다. 성능은 우리 쪽이 같거나 낫다. 반대로 공식에서 assert로 드러나던 문제가 조용히 지나갈 수 있다. **[확인 필요: 간헐 네이티브 크래시 #169가 공식 빌드의 assert abort인지. 맞다면 교체 후 증상이 바뀐다]**
  - x86_64 에뮬레이터에서만 계측 테스트를 했으므로 **arm64 `.so`는 실행 검증을 하지 못했다**(7절, 8절). arm64 수동 확인은 PM이 사장님께 요청한다.
- **낮음 4 — 해시 불일치 시험.** 2절의 시험은 버전 `lgpl.1`의 `verification-metadata.xml`에서 `sha256` 값만 앞 16자리를 `0`으로 바꾼 것이다(버전·파일 이름은 그대로). 받은 파일의 해시가 고정 값과 달라 `Dependency verification failed … One artifact failed verification: libvlc-lgpl-3.7.5-lgpl.1.aar … from repository PiecePlayLibVlcReleases`로 실패했다. 목록에 없는 산출물 때문이 아니라 해시 불일치로 실패한 것이다. `lgpl.3`에서 같은 시험을 다시 해 9.7에 적는다.
- **낮음 5 — FFmpeg 고지 링크(의견, 이번에 바꾸지 않음).** 지금 `https://ffmpeg.org`는 프로젝트 홈페이지다. 앱에 들어간 FFmpeg는 8.1.2에 VLC contrib 패치와 우리 패치 0022를 적용한 것이고, 그 정확한 소스는 우리 Release의 contrib 소스 묶음과 VLC 소스(`contrib/src/ffmpeg/*.patch`)에 있다. 고지의 링크는 LibVLC 항목처럼 `https://github.com/kywood/pieceplay-libvlc`(또는 해당 Release)로 맞추고, 버전 칸에 "8.1.2 (patched)"를 적는 것이 사실에 더 가깝다고 본다. 바꿀지는 문구와 함께 design·PM이 정한다 **[확인 필요: 소스 제공 요건상 어느 쪽이 맞는지]**.
- **계측 결과 미기재 지적.** 결과 커밋 `4538b6c`는 2026-11-14T09:41:42Z(서울 18:41)에 푸시됐고 PR #201 본문에도 있다. reviewer 검토가 그 전이었던 것으로 보인다.

### 9.7 `lgpl.3` 결과

- Release: https://github.com/kywood/pieceplay-libvlc/releases/tag/3.7.5-lgpl.3 (태그 = `41e7e12161ab8bcb93d946048892b67e9e8d99c8`, PR #2 병합 커밋). Actions 실행 [43586876344](https://github.com/kywood/pieceplay-libvlc/actions/runs/43586876344)(main, `workflow_dispatch`)의 산출물을 그대로 올렸다.

| 자산 | 크기 | SHA-256 |
|---|---:|---|
| `libvlc-lgpl-3.7.5-lgpl.3.aar` | 80,910,311 B | `aafbb3ca121b69f8e87e381fd4c8cb286cb0e64010a10d6d7407af5c4f62f854` |
| `libvlc-lgpl-3.7.5-lgpl.3-vlc-sources.tar.xz`(VLC `453bcced8f` = `ac6c2a40` + 패치 22개) | 15,092,100 B | `92f5e3801888cbc789039b6db42cfcfdb53e32e14218016f8fa018ae64d42786` |
| `libvlc-lgpl-3.7.5-lgpl.3-contrib-sources.tar`(파일 55개 = 소스 아카이브 54개 + `.githash` 1개, zvbi·librist 빠짐, 4 ABI 같은 세트) | 458,424,320 B | `fbbddcacd0ccce5ba1704d65a6cf17df533f3b1dc327aef54eca9ccdc18dec20` |

GitHub가 계산한 자산 digest가 위 값과 같다.

| 확인 | 결과 |
|---|---|
| 재현 | `.so` 12개가 PR 실행 43584765658과 SHA-256이 같다. arm64 `libvlc.so` `686c0896…`, armeabi-v7a `6d318025…`는 로컬 WSL 빌드와도 같다. VLC HEAD 4 ABI 모두 `453bcced8f` |
| GPL 소스 검사 | arm64-v8a 4,072 / armeabi-v7a 4,086 / x86 3,973 / x86_64 4,124 컴파일 단위, **GPL 0**. 이중 라이선스 1(`gcrypt/random/rndjent.c`), 예외 1(`webvtt/CSSGrammar.c`) |
| 스캐너(`libvlc_gpl_scan.py`) | AAR·앱 릴리스 AAB·APK 4 ABI 모두 ok, 64비트 LOAD 정렬 16384 |
| `lgpl.1` AAR과 구조 비교 | 파일 22/22, 다른 파일 8/8·`classes.jar` 바이트 같음 |
| 앱 APK `lib/*/libvlc.so` | Release `SHA256SUMS`와 4 ABI 모두 같음 |
| `zipalign -c -P 16 -v 4` 릴리스 APK | Verification successful |
| 해시 불일치 시험(낮음 4) | `verification-metadata.xml`의 버전·파일 이름은 그대로, `sha256`만 앞 16자리를 `0`으로 바꿔 `:app:assembleDebug` → `Dependency verification failed for configuration ':app:androidApis'` / `One artifact failed verification: libvlc-lgpl-3.7.5-lgpl.3.aar (pieceplay.libvlc:libvlc-lgpl:3.7.5-lgpl.3) from repository PiecePlayLibVlcReleases`. 보고서: "Expected a sha256 checksum of 0000000000000000e87e…f854 but was aafbb3ca121b69f8e87e…f854". 값은 되돌렸다 |
| 단위 테스트·lint·빌드 | 5절 |
| 크기 | 4절. `lgpl.2`보다 AAB 1,133,542 B, ABI당 다운로드 약 0.28 MB 더 줄었다(zvbi·librist·Mbed TLS) |

**검사 범위의 한계(`pieceplay-gpl-sources.py`)**

- 이 검사는 `libvlc.so`의 DWARF 컴파일 단위만 본다. DWARF가 없는 코드는 단위 목록에 나오지 않아 "디스크에 없음"으로도 세지지 않는다.
- `lgpl.3` 실행의 "디스크에 없음": arm64-v8a 0, armeabi-v7a 16, x86 8, x86_64 0. 모두 NDK r21 런타임이다(libgcc `libgcc2.c`·`emutls.c`·`unwind-dw2*.c`·ARM `bpabi`·`ieee754-*`·`lib1funcs.S`, bionic `locale.cpp`·libm `e_log2*.c`, libunwind, compiler-rt `mulodi4.c`). 빌드 서버(`/buildbot/src/android/…`) 경로라 이 빌드 트리에 소스가 없다. libgcc는 GCC Runtime Library Exception, bionic·libunwind·compiler-rt는 BSD/MIT 계열이며 모든 NDK 앱에 같이 들어간다.
- DWARF 없는 contrib 코드 확인(로컬 `97c2a79` 빌드, arm64-v8a·armeabi-v7a): 설치된 contrib `lib*.a`를 모두 풀어 객체마다 `.debug_info` 유무를 보고, DWARF 없는 객체가 정의한 전역 함수가 strip 전 `libvlc.so`에 있는지 대조했다.
  - DWARF가 전혀 없는 라이브러리 중 `libvlc.so`에 링크된 것: **없음**(DWARF 없는 라이브러리는 `libcddb.a`, `libspeexdsp.a`뿐이고 둘 다 링크된 함수 0).
  - 일부 객체만 DWARF가 없는 라이브러리(FLAC, avcodec, gcrypt, intl, opus, protobuf 등 약 20개): 그 객체들이 정의한 함수 중 `libvlc.so`에 들어간 것 **0**.
  - 한계: `libavcodec.a`(1,004개 중 같은 이름 15개), `libavutil.a`, `libdav1d.a`(13개), `libplacebo.a`, `libswscale.a`는 같은 이름 객체가 있어 `ar x`로 일부를 따로 꺼내지 못했다. 이들은 C·어셈블리 모두 DWARF가 있어(FFmpeg aarch64 `.S`도 단위로 잡혔다) `.so` 쪽 검사에 포함된다고 보지만, 객체 단위 대조는 하지 못했다.
  - x86·x86_64 contrib는 이 객체 단위 대조를 하지 않았다(`.so` 쪽 검사에서 디스크에 없음 8·0개).

**계측 테스트(`lgpl.3`, 2026-11-14 서울 20:4x, emulator-5554 `sdk_gphone64_x86_64`)**

- 빌드: #201+#202(`codex/101-libvlc-license-text`)의 debug APK. APK 안 `libvlc.so`가 `lgpl.3`(arm64 `686c0896…`, x86_64 `7f4f720f…`)이고, #202의 LGPL 안내 문구(영어·한국어)가 `resources.arsc`에 있음을 확인했다. VLC는 `revision 3.0.23-2-852-g453bcced8f`(= `lgpl.3` VLC HEAD)로 떴다.
- 방식은 7절과 같다(`adb -s emulator-5554` 설치, `am instrument`, 실행마다 `pm clear`). 휴대폰·포트 5038 adb 서버는 쓰지 않았다.
- 첫 시도는 에뮬레이터 쪽 문제로 실행되지 않았다. 서울 20:20에 zygote64·system_server가 다시 시작된 뒤 `Can't find service: package`가 나왔고, adb shell도 응답하지 않았다. 이 세션의 첫 에뮬레이터 명령은 20:25였다. PM이 콜드 부트한 뒤 다시 돌렸다.

| 실행 | 결과 | 시간 |
|---|---|---|
| 다섯 클래스(`SubtitleUiTest` 3개, `CompletedPlaybackSwitchTest`, `QuickDownloadResumeTest`, `IndependentDownloadTest`, `OpenSourceLicensesFlowTest`) | `OK (7 tests)`, 크래시 0 | 153초 |
| 전체 25개 클래스 | 55개 중 54개 통과, **1개 실패**, 크래시(`FATAL EXCEPTION`·`Fatal signal`) 0 | 180초 |
| 실패한 클래스만 다시(`MainThreadResponsivenessTest`) | `OK (2 tests)` | 7초 |

- 실패: `MainThreadResponsivenessTest.buttonFlowsKeepEngineWorkOffMainThread` — `blocking engine calls on the main thread expected:<[]> but was:<[gcore.dynamite]>`. 이 테스트는 메인 스레드의 StrictMode 사용자 정의 느린 호출을 모두 모은다. 잡힌 것은 앱 엔진 호출이 아니라 Google Play 서비스가 동적 모듈(광고 SDK·UMP)을 처음 불러올 때 `StrictMode.noteSlowCall`로 남기는 `gcore.dynamite`다. 콜드 부트 직후 GMS 모듈이 캐시에 없어 처음 한 번 메인 스레드에서 불렸다고 본다. 같은 테스트는 `lgpl.2` 전체 실행·공식 빌드 전체 실행에서 통과했고, 다시 돌리니 통과했다. LibVLC와 관계없고 간헐 네이티브 크래시(#169)도 아니다. 다만 이 테스트는 GMS 상태에 따라 깨질 수 있으므로, `gcore.dynamite` 같은 외부 라이브러리 알림을 걸러야 할지는 별도 판단이다(이 PR에서는 고치지 않음).
- VLC 오류 로그(`E/VLC`) 종류와 수는 `lgpl.2`·공식 빌드 때와 같다: `window: request … not implemented` 48, `video output creation failed` 21, `buffer deadlock prevented` 1, `deinterlace` 안내 22. 모듈을 찾지 못했다는 로그(zvbi·rist 등)는 없다.
- 하지 못한 것(그대로): arm64 실기기 실행, AC3/E-AC3 음성 재생(arm64 C 경로), 코덱별 스모크 테스트.

### 9.8 GPL 소스 검사 판정 강화(reviewer 재검토 낮음 1·2)

reviewer 재검토에서 #201·#202는 병합 가능(높음 없음) 판정이었다. 낮음 지적에 따라 공개 저장소 PR https://github.com/kywood/pieceplay-libvlc/pull/3 으로 `buildsystem/pieceplay-gpl-sources.py`의 판정을 고쳤다. 빌드 자체는 바꾸지 않았다.

- 문제: 앞 6000바이트에 단어 "LGPL"만 있어도 LGPL로 보고 `other`로 통과시켰다(예: FFmpeg `libavfilter/vf_owdenoise.c`, 이번 산출물에는 없음). `dual`은 검토 없이 통과했다.
- 바꾼 것:
  - 헤더의 주석 기호와 줄바꿈을 공백 하나로 접은 뒤 판정한다. 여러 줄에 걸친 허가문도 맞는다.
  - GPL과 LGPL 모두 허가 문장("terms of the GNU General Public License", "terms of the GNU Lesser/Library General Public License", SPDX 식별자)으로 판정한다.
  - GPL 허가문과 LGPL 허가문이 함께 있으면 `mixed`다. LGPL 허가문 없이 GPL 이름만 있으면 `gpl`(또는 `dual`)이다.
  - `dual`·`mixed`는 이유를 적은 허용 목록(`ALLOWED`)에 있을 때만 통과하고, 없으면 빌드가 실패한다. `exception`(Bison·GCC 예외)은 통과하되 목록에 표시한다.
  - 36줄 주석을 사실대로 고쳤다(`lgpl.1`·`lgpl.2`에는 zvbi `packet-830.c`와 FFmpeg aarch64 3개가 링크됐고, zvbi `pdc.c`는 `libzvbi.a`에만 있었다).
- 허용 목록(파일을 읽고 넣음):

  | 종류 | 파일 | 이유 | ABI |
  |---|---|---|---|
  | dual | libgcrypt `random/rndjent.c` | BSD-3-Clause, 또는 사용자가 고르면 GPL | 4개 |
  | mixed | libgpg-error `src/estream.c`, `src/estream-printf.c` | libestream: LGPL-2.1-or-later, ALTERNATIVELY BSD 계열. GPL은 BSD 선택 조항 문장에 이름만 나옴 | 4개 |
  | mixed | libgcrypt `cipher/keccak-amd64-avx512.S` | libgcrypt 부분 LGPL-2.1-or-later, KeccakF1600 핵심은 CRYPTOGAMS BSD-3-Clause("ALTERNATIVELY … may be distributed under the terms of the GNU General Public License"). libgcrypt `LICENSES`도 이 파일을 CRYPTOGAMS로 적음 | x86_64만 |

- 판정 시험(알려진 파일 15개, 모두 기대대로): FFmpeg `vf_owdenoise.c`·`aarch64/ac3dsp_neon.S`·`aarch64/crc.S` gpl, `crc.c`·`h264dec.c` other / zvbi `packet-830.c` gpl, `packet.c` other / Mbed TLS `aes.c` dual / libgcrypt `rndjent.c` dual / libgpg-error `estream.c` mixed, `argparse.c` other / VLC `ts_pid.c` other, `rotate.c` gpl, `CSSGrammar.c` exception / "LGPL" 단어가 든 합성 GPL 헤더 gpl.
- 처음에는 GPL도 단어(이름)로만 찾았더니 `mixed`가 21개 나왔다. VLC 모듈 14개(`ts_*.c`, `atsc_a65.c`, `scte18.c`)와 libgpg-error `argparse.c`·`b64enc.c`는 LGPL 허가문인데 안내 문장("See/You should have received a copy of the GNU General Public License")에 GPL 이름을 잘못 적은 것이었다. `name-value.c`·`strlist.c`·libestream 2개는 LGPL 또는 BSD였고, FFmpeg `crc.c`는 우리 패치 0022의 주석 때문이었다. GPL도 허가문으로 판정하도록 바꾸자 libestream 2개만 남았다.
- `lgpl.3` 빌드에 새 규칙 적용: 로컬 arm64-v8a·armeabi-v7a 빌드 트리는 GPL 0, 종료 코드 0. Actions 첫 실행(43593648075)은 x86_64에서 `keccak-amd64-avx512.S`(위 표)를 `mixed, not allowed`로 멈췄다(로컬에서 x86 계열을 돌리지 않아 놓쳤던 것). 읽어 보고 허용 목록에 넣어 다시 돌렸다.

- Actions 두 번째 실행 [43595417243](https://github.com/kywood/pieceplay-libvlc/actions/runs/43595417243)(`6d42d7c`): 4 ABI 모두 성공.

  | ABI | 컴파일 단위 | GPL | dual(허용) | mixed(허용) | exception | 디스크에 없음 |
  |---|---:|---:|---:|---:|---:|---:|
  | arm64-v8a | 4,072 | 0 | 1 | 2 | 1 | 0 |
  | armeabi-v7a | 4,086 | 0 | 1 | 2 | 1 | 16 |
  | x86 | 3,973 | 0 | 1 | 2 | 1 | 8 |
  | x86_64 | 4,124 | 0 | 1 | 3 | 1 | 0 |

- 산출물: `.so` 12개와 `classes.jar`가 `3.7.5-lgpl.3`과 SHA-256까지 같다. AAR 안에서 다른 것은 `res/values/values.xml`의 `build_libvlc_revision`(이 저장소 커밋 문자열)뿐이다. **라이브러리 내용이 같으므로 새 Release는 내지 않았다**(PM 결정). 앱은 계속 `3.7.5-lgpl.3`을 쓴다.
- 3번 지적(contrib 개수)은 9.4의 "세는 기준"과 1·9.7절 숫자로 맞췄다. 4번(PR #202 본문)은 결과로 고쳤다. 5번(GMS `gcore.dynamite` 간헐 실패)은 PM이 별도 이슈로 다룬다.
