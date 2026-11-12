# LibVLC에서 GPL 구성요소 빼기 — 조사와 계획

작성: program, 2026-11-12. 범위: 조사와 계획까지. 앱 의존성(`libvlc-all 3.7.5`)은 이번에 바꾸지 않았다.
법률 판단은 하지 않는다. 법적 해석이 필요한 곳은 **[확인 필요]**로 표시했다.
1단계(패치 + arm64 시험 빌드) 결과는 `program/libvlc-lgpl-phase1.md`에 있다.

## 0. 요약

- **미리 빌드된 LGPL 전용 LibVLC는 공식 배포처에 없다.** Maven Central의 `org.videolan.android`에는 `libvlc-all`, `medialibrary-all`, `remote-access` 세 가지뿐이다. 그래서 직접 빌드해야 한다.
- **libvlcjni 3.x 빌드 스크립트에 이미 라이선스 옵션이 있다.** `buildsystem/compile-libvlc.sh --license <g|l|a>` (2025-07-29 커밋 `882e540`에서 추가). 기본값은 `g`(GPLv3)이고, 3.7.5 시점 스크립트에도 들어 있다.
- **이 옵션만으로는 부족하다.** 옵션은 contrib(외부 라이브러리)에만 `--disable-gpl`을 건다. VLC 자체 모듈 가운데 소스가 GPL인 것은 그대로 빌드된다. 특히 libvlcjni가 켜 두는 **Lua 모듈과 Lua 스크립트(AAR assets)가 GPL**이다. 그래서 모듈을 끄거나 블랙리스트에 넣는 패치를 따로 해야 한다(2.3절).
- **추천 경로:** 공개 저장소 하나에 libvlcjni 3.7.5 커밋, 우리 패치, 빌드 스크립트를 둔다. VideoLAN 공식 Docker 이미지로 `--license a`(LGPLv2.1)에 GPL 모듈 비활성화 패치를 더해 빌드한다. 빌드한 `.so`는 자동 스캔으로 검사한 뒤 앱에 넣는다. 첫 빌드는 이 PC의 WSL Docker에서 하고, 재현 빌드와 소스 공개는 같은 공개 저장소의 GitHub Actions로 한다.
- **sudo는 필요 없다.** WSL 사용자가 이미 `docker` 그룹에 속해 있고 Docker 29.7.2가 동작한다(확인함). NDK도 Docker 이미지 안에 있다.
- **예상 일정:** 작업일 4~6일(기계 빌드 시간 포함). 법률 확인은 이와 별도로 진행한다.

## 1. 미리 빌드된 LGPL 전용 배포판

| 출처 | 결과 |
|---|---|
| Maven Central `org/videolan/android/` | `libvlc-all`, `medialibrary-all`, `remote-access`만 있다. LGPL 전용 변형은 없다. https://repo1.maven.org/maven2/org/videolan/android/ |
| `libvlc-all` POM | POM 라이선스는 LGPL-2.1로 적혀 있다(`buildsystem/publish.gradle`). 하지만 실제 산출물은 기본 `--license g`로 빌드된다. 3.7.5 AAR의 `libvlc.so`에서 x264·dvdnav·dvdread·postproc·realrtsp·Lua 흔적을 확인했다(5.1절 기준선). |
| VideoLAN prebuilt contrib (`artifacts.videolan.org/vlc-3.0/android-*`) | CI용 contrib 묶음이다. 기본 라이선스(`g`)로 만든 것이라 쓸 수 없다. contrib는 소스부터 빌드해야 한다. |
| 서드파티 `masterwok/libvlc-android-sdk` (JitPack) | 마지막 푸시가 2018년이다. 관리되지 않아 제외한다. https://awesome.ecosyste.ms/projects/github.com%2Fmasterwok%2Flibvlc-android-sdk |

결론: 신뢰할 만한 LGPL 전용 prebuilt는 없다. 직접 빌드한다.

## 2. 직접 빌드하는 방법

### 2.1 3.7.5에 대응하는 소스(WSL에서 확인)

- libvlcjni에는 `3.7.x` 태그가 없다. 브랜치 `libvlcjni-3.x`에서 `libvlcVersion`이 바뀐 커밋으로 찾았다.
  - `81bb02b` (2026-07-08) "Bump vlc 3 version": 3.7.5로 올린 커밋이다.
  - `c0cc8ce` (2026-09-10): 3.7.6으로 올린 커밋이다. 현재 브랜치 끝은 3.7.7(`0b8dc65`)이다.
- **확정 근거:** 우리가 쓰는 `libvlc-all-3.7.5.aar`의 `res/values/values.xml`에 `build_libvlc_revision=81bb02b`, `build_vlc_revision=1e3502a298`가 들어 있다.
  - `1e3502a298`은 VLC `3.0.x` 브랜치에 없는 해시다. `get-vlc.sh`가 VLC `ac6c2a405d652b5576128ceb9fec2c342f0e83ec`(2026-07-04, 3.0.x)에 `libvlc/patches/0001~0020`을 `git am`으로 적용한 뒤의 로컬 커밋으로 보인다. **[확인 필요: 첫 빌드에서 같은 해시가 나오는지]**
- 따라서 빌드 기준은 **libvlcjni `81bb02b` + VLC 3.0.x `ac6c2a40` + 동봉 패치 20개**다. Java API가 같으므로 앱 코드는 바꾸지 않아도 된다.
- 참고: 3.7.5 시점 스크립트에는 `--enable-realrtsp`가 강제로 들어 있다. 이후 커밋 `ddde54f`(2026-09-14, "do not force build realtrtsp")에서 빠졌으므로 우리 패치에서도 같은 줄을 뺀다.

### 2.2 `compile-libvlc.sh --license` 옵션(3.x 브랜치, 3.7.5 시점에도 있음)

```sh
case $AVLC_CONTRIB_LICENSE in
    l)  # LGPL v3 + ad-clauses
        VLC_CONTRIB_ARGS="$VLC_CONTRIB_ARGS --disable-gpl --enable-ad-clauses"
        VLC_CONFIGURE_ARGS="$VLC_CONFIGURE_ARGS --enable-live555" ;;
    a)  # LGPL v2.1 + ad-clauses
        VLC_CONTRIB_ARGS="$VLC_CONTRIB_ARGS --disable-gpl --disable-gnuv3 --enable-ad-clauses" ;;
    g|*) # GPL v3 (기본)
        VLC_CONFIGURE_ARGS="$VLC_CONFIGURE_ARGS --enable-live555 --enable-dvdread" ;;
esac
```

VLC `contrib/bootstrap`(ac6c2a40)의 옵션은 세 가지다. `--disable-gpl`("viral GPL code를 빌드하지 않음"), `--disable-gnuv3`(GPLv3·LGPLv3 코드 제외), `--enable-ad-clauses`(광고 조항이 있는 패키지 허용)다. 각 contrib의 `rules.mak`가 `ifdef GPL` / `$(REQUIRE_GPL)`로 빠진다.

| contrib | GPL 아님(`a`)일 때 |
|---|---|
| x264, x262, x265, postproc, dvdnav, dvdread, dvdcss, a52, faad2, libmpeg2, mad, dca, samplerate, aribb24, sidplay2, vncclient | 빠진다(`REQUIRE_GPL`) |
| live555 | `--disable-gnuv3`이면 빠진다(LGPLv3). RTSP는 필요 없다. |
| gnutls, nettle, gmp, srt | `a`에서는 빠진다(GMP가 GPLv2 또는 LGPLv3 조건). 그러면 VLC 안의 TLS가 사라진다. 앱은 VLC에 `http://127.0.0.1`과 파일 URI만 주므로 영향이 없다. `.torrent` HTTPS 다운로드는 앱 코드가 따로 한다. |
| freetype2 | `--enable-ad-clauses`가 있어야 빌드된다(FreeType License의 광고·고지 조항). 자막 렌더링에 필요하다. |
| ffmpeg | contrib 규칙에 `--enable-gpl`이 없다. postproc는 별도 contrib다. |

### 2.3 contrib 옵션만으로 남는 GPL: VLC 자체 모듈

VLC 3.0 모듈 중 `#define VLC_MODULE_LICENSE VLC_LICENSE_GPL_2_PLUS`를 선언한 소스는 44개다(ac6c2a40, `git grep`). Android 빌드에서 이미 꺼져 있거나 블랙리스트(`VLC_MODULE_BLACKLIST`)에 든 것을 빼면, **우리가 추가로 막아야 할 것**은 다음과 같다.

| 모듈(소스) | 지금 상태 | 조치 |
|---|---|---|
| `lua` (`modules/lua/*`, `modules/lua/libs/*` 28개 파일 GPL) + AAR `assets/lua/**` 스크립트 | libvlcjni가 `--enable-lua`로 켜고, Gradle `luaPlaylistCopy`·`luaMetaCopy`·`luaModuleCopy`로 스크립트를 넣는다 | contrib과 configure에 `--disable-lua`를 주고, 세 Copy 작업을 제거한다. 유튜브 같은 플레이리스트 파싱이 사라지지만 앱은 쓰지 않는다. |
| `access_realrtsp` (`modules/access/rtsp/*`) | 3.7.5 스크립트가 `--enable-realrtsp`로 강제한다 | 그 줄을 지운다(상위 `ddde54f`와 같은 수정). |
| `x264`, `x26410b`, `x265`, `postproc`, `dvdnav`, `dvdread` | contrib이 없으면 configure가 자동으로 끈다 | 방어적으로 `--disable-x264 --disable-x26410b --disable-x265 --disable-postproc --disable-dvdnav --disable-dvdread`를 명시한다. |
| `dolby_surround_decoder`, `headphone_channel_mixer`, `mono` (`audio_filter/channel_mixer`) | 빌드된다 | 블랙리스트에 넣는다. 일반 재생에는 필요 없다. |
| `rotate` (`video_filter/rotate.c`) | 빌드된다 | 블랙리스트에 넣는다. 화면 회전은 Android vout이 메타데이터로 처리한다. **[확인 필요: 회전 메타데이터가 있는 영상의 회귀]** |
| `file_logger`, `syslog` (`logger/*`) | 블랙리스트 `logger`는 `liblogger_plugin.a`만 걸러서 이 둘은 남는 것으로 보인다 | 블랙리스트에 넣는다. |
| `stream_out_rtp`, `stream_out_cycle`, `vod_rtsp` | sout/vlm 설정에 따라 다르다 | 블랙리스트에 넣는다(인코딩·송출은 필요 없음). |
| `deinterlace` (모듈 선언은 LGPL이지만 GPL 헤더 `yadif.h`·`yadif_template.h`를 포함한다) | 빌드된다 | **[확인 필요]** 보수적으로 블랙리스트에 넣는다. 인터레이스 영상에서 빗살 무늬가 보일 수 있다. |
| `hqdn3d` (GPL 헤더 `hqdn3d.h`) | 빌드된다 | 블랙리스트에 넣는다. |

이 목록은 소스 헤더를 기준으로 한 휴리스틱이다. 최종 기준은 빌드 결과다. 링크 직전의 `lib*_plugin.a` 각각에 GPL 라이선스 문자열이 있는지 검사하고, 하나라도 있으면 빌드를 실패시킨다(5.1절).

### 2.4 왜 `a`(LGPLv2.1)인가: `l`(LGPLv3)과 비교

- `l`은 gnutls·nettle·gmp(LGPLv3)와 live555를 남긴다. (L)GPLv3에는 사용자 제품(휴대폰 포함)에 "설치 정보(Installation Information)"를 제공하라는 조건이 붙는다. **[확인 필요]**
- 앱은 VLC 안에서 TLS와 RTSP를 쓰지 않는다. 그래서 `a`가 의무는 가장 작고 기능 손실은 없다.
- 위험: VideoLAN CI는 `g`만 빌드하므로 `a` 조합은 검증되지 않은 경로다. 빌드가 깨지면 configure에 `--disable-gnutls`를 명시하는 패치로 먼저 대응하고, 그래도 안 되면 `l`로 물러나서 법률 확인을 받는다.

## 3. 빌드 환경

| 항목 | 내용 |
|---|---|
| OS | Linux 필수. libvlcjni README는 "recent Linux distribution"이라 하고 Windows/macOS는 공식 지원하지 않는다. Windows 스마트 앱 컨트롤 때문에 Windows 네이티브 도구는 쓰지 않는다. |
| 컨테이너(공식) | libvlcjni CI(`buildsystem/gitlab/.gitlab-ci.yml`)가 쓰는 이미지를 그대로 쓴다. 64비트(arm64, x86_64)는 `registry.videolan.org/vlc-debian-android:20260611225331`(레이어 약 2.1 GB 압축), 32비트(arm, x86)는 `registry.videolan.org/vlc-debian-android-3.0:20260701183656`이다. 두 매니페스트 모두 이 PC에서 조회됐다. |
| NDK | 스크립트가 강제한다. 64비트는 NDK r27~r29, 32비트는 NDK r21이며 API는 각각 21과 17이다. 이미지 안에 들어 있다. |
| 이 PC(WSL Ubuntu-26.04) | 12코어, RAM 31 GB, 디스크 여유 843 GB. Docker 29.7.2가 있고 사용자가 `docker` 그룹에 있어 **sudo가 필요 없다**. |
| 디스크 | 이미지 2개 압축 해제 약 6~8 GB, ABI당 빌드 트리 약 10~20 GB(추정). 4 ABI 합계 50~90 GB(추정). |
| 시간(추정) | prebuilt contrib를 못 쓰므로 contrib를 소스부터 빌드해야 한다. 12코어에서 ABI당 1.5~3시간, 4 ABI면 6~12시간. 첫 시도의 실패와 재시도는 따로 잡는다. |
| Docker 없이 직접 설치 | 쓰지 않는다. 쓴다면 `sudo apt install automake ant autopoint cmake build-essential libtool-bin patch pkg-config protobuf-compiler ragel subversion unzip git flex python3 wget`과 NDK r21·r27 수동 설치가 필요하다(README). |

### GitHub Actions

- 공개 저장소는 표준 러너 사용이 무료다. `container:`로 위 VideoLAN 이미지를 그대로 쓸 수 있다.
- 위험: 표준 러너는 4코어에 디스크 여유가 약 14 GB이고, 작업 하나는 6시간으로 제한된다. ABI마다 job을 나누는 matrix로 구성하고, 시작할 때 러너의 기본 도구를 지워 디스크를 확보해야 한다. contrib 단계가 6시간을 넘으면 job을 "contrib → 아티팩트 → VLC" 둘로 나눈다.
- 장점: 빌드 로그와 스크립트, 소스 묶음이 공개 저장소에 그대로 남아 LGPL 소스 제공(6절)과 재현성에 같이 쓰인다.

## 4. 앱에 넣는 방법

### 4.1 형태

| 방법 | 장점 | 단점 |
|---|---|---|
| A. 앱 저장소 `app/libs/*.aar` + `files()` | 간단하다 | AAR이 약 93 MB(현재 3.7.5, 4 ABI)라 GitHub 경고 50 MB와 한도 100 MB에 걸린다. POM이 없어 의존성(`androidx.annotation`, `legacy-support-v4`)을 직접 적어야 한다. |
| B. 앱 저장소 안 파일 Maven 저장소 | POM과 버전 카탈로그를 유지한다 | A와 같은 크기 문제가 있다. |
| **C. 공개 소스 저장소의 GitHub Release 자산 + Gradle `ivy` 저장소(인증 없음) + 체크섬 고정** | 저장소 비대화가 없다. 소스 공개 위치와 바이너리 위치가 같다. 자산 한도는 2 GB다. | `ivy`에 메타데이터가 없어 전이 의존성 두 개를 앱에 명시해야 한다. 체크섬 고정은 Gradle dependency verification으로 한다. |
| D. 사설 Maven(GitHub Packages) | 표준 방식이다 | 읽기에도 토큰이 필요해 CI·PC마다 비밀값을 관리해야 한다. |

**추천: C.** 좌표는 예를 들어 `pieceplay.libvlc:libvlc-lgpl:3.7.5-lgpl.1`로 하고, 기존 `org.videolan.android:libvlc-all`과 섞이지 않게 `libs.versions.toml`에서 교체한다. 패키지명 `org.videolan.libvlc.*`은 그대로다. **[확인 필요: PM이 저장소 이름과 소유 계정 결정]**

### 4.2 ABI와 16 KB 정렬

- 현재 앱에는 `abiFilters`가 없어 AAR의 4 ABI(arm64-v8a, armeabi-v7a, x86, x86_64)가 모두 들어간다. 요청 범위는 3 ABI(x86 제외)다. x86을 빼면 x86 에뮬레이터·기기에서는 재생할 수 없다. **[결정 필요: x86 포함 여부]** 빌드는 4개를 모두 만들고 앱 쪽 `abiFilters`로 고르는 것을 추천한다.
- 16 KB: 스크립트가 `VLC_LDFLAGS="-z max-page-size=16384"`를 모든 ABI에 건다. 현재 3.7.5 산출물은 4 ABI 모두 LOAD 정렬이 16384다(스캐너로 확인). 같은 스크립트를 쓰므로 유지될 것으로 보고, 스캐너가 64비트 `.so`의 정렬이 16 KB보다 작으면 실패시킨다. `libc++_shared.so`와 `libvlcjni.so`도 같이 검사한다. 최종 APK는 `zipalign -c -P 16 -v 4`로 확인한다.

### 4.3 예상 크기 변화(추정, 빌드 후 실측으로 바꾼다)

- 현재 `libvlc.so`(압축 전): arm64 46.1 MB, armeabi-v7a 40.1 MB, x86 50.2 MB, x86_64 54.6 MB. AAR은 93.2 MB다.
- 빠지는 것: x264(수 MB), gnutls·nettle·gmp(약 2~3 MB), Lua와 스크립트, dvdnav·dvdread·postproc(1 MB 미만), 일부 필터.
- 추정: ABI당 `libvlc.so` 3~6 MB 감소, Play 배포(AAB 기기별 분할) 다운로드는 1~3 MB 감소.

## 5. 검증

### 5.1 GPL 흔적 자동 검사: `program/tools/libvlc_gpl_scan.py`

- 입력: AAR·APK·AAB(안의 `libvlc.so`), `.so` 파일, 또는 빌드 디렉터리(`lib*_plugin.a`). GPL 흔적이나 64비트 정렬 위반이 있으면 종료 코드 1로 끝난다.
- 표식: x264(`x264_encoder_open`, 인코더 배너 `H.264/MPEG-4 AVC codec - Copy`. `x264 - core`는 LGPL FFmpeg의 H.264 SEI 해석기에도 있어 1단계에서 뺐다), x265, postproc(`pp_postprocess`, `postproc_version`), `dvdnav_open`, `DVDOpen`, realrtsp(`access_realrtsp`, `Real RTSP`), Lua(`luaL_newstate`). 여기에 VLC GPL 모듈 라이선스 문자열 `"Licensed under the terms of the GNU General Public License, version 2 or later."`을 더한다.
- 링크 전 `.a` 단계 검사가 핵심이다. 링크된 `.so`에서는 같은 문자열이 합쳐지고, 64비트 산출물에서는 라이선스 문자열이 보이지 않았다. 그래서 `.so` 단계 검사는 보조로 쓴다. 빌드 스크립트의 정적 모듈 목록을 만든 직후 `python3 -I libvlc_gpl_scan.py $VLC_BUILD_DIR/install/lib/vlc/plugins`를 돌려 실패하면 빌드를 멈춘다.
- 추가 검사: `contrib/<triplet>/lib`에 `libx264.a`, `libpostproc.a`, `libdvdnav.a`, `libdvdread.a`, `liblua*.a`, `libgnutls.a`가 없는지 확인한다. AAR `assets/`에 `lua/`가 없는지도 확인한다.
- **기준선(현재 libvlc-all 3.7.5, 실행 결과):** 4 ABI 모두 FAIL이다. arm64는 `x264=1`(1단계 표식 수정 전에는 3), `libpostproc=1, libdvdnav=4, libdvdread=10, realrtsp=2, lua=1`이고, 32비트 두 ABI는 GPL 모듈 라이선스 문자열도 1개씩 나왔다. LOAD 정렬은 모두 16384다.

### 5.2 기능 회귀

- 단위 테스트: `:app:testDebugUnitTest`. `OpenSourceLicensesTest`는 버전과 GPL 항목 문자열을 확인하므로 고지 목록을 바꾸면 같이 고친다.
- 에뮬레이터: 재생 경로를 지나는 기존 테스트는 `CompletedPlaybackSwitchTest`, `QuickDownloadResumeTest`, `IndependentDownloadTest`다. 셋 다 `connectedDebugAndroidTest`로 arm64·x86_64 에뮬레이터에서 실행한다.
- 부족한 점: 코덱별 재생을 확인하는 계측 테스트가 없다. `LibVlcCodecSmokeTest`(가칭)를 추가할 것을 제안한다. 테스트 자산으로 짧은 샘플(H.264/H.265/VP9/AV1 영상 × AAC/AC3/Opus 음성, MKV+ASS 자막, MP4)을 `127.0.0.1` 스트림과 파일 두 경로로 열어, `Playing` 이벤트와 vout 1개 이상, 자막 트랙 인식을 확인한다. 같은 테스트를 기존 AAR과 새 AAR에서 돌려 결과를 비교한다.
- 실기기: arm64 휴대폰 1대와 armeabi-v7a(가능하면)에서 수동으로 탐색(seek)과 자막을 확인한다.

## 6. LGPL 준수에 필요한 것(모두 [확인 필요])

1. **앱 안 고지**: `meta/OpenSourceLicenses.kt`에서 `LIBVLC_GPL_COMPONENTS`(x264, libdvdnav, libdvdread, libpostproc, GPL 2.0)를 뺀다. 빌드에 실제로 들어간 contrib 목록(`make list` 출력)으로 LGPL·BSD·MIT·FTL 항목을 다시 만든다. FreeType은 `--enable-ad-clauses` 조건이라 FTL 고지 문구가 필요하다. 화면 문구가 새로 필요하면 design에 요청한다.
2. **소스 제공**: 공개 저장소에 다음을 올린다.
   - libvlcjni `81bb02b`를 기준으로 한 우리 패치 커밋
   - VLC `ac6c2a40`과 패치 20개
   - 빌드 스크립트와 워크플로
   - 각 Release에 contrib 소스 tarball(`make fetch` 결과)
   - 앱 고지 화면에서 이 저장소로 연결한다. 기존 `sourceOffer` 안내와 같은 방식이다.
3. **재링크 가능 요건**(LGPL 2.1 §6): `libvlc.so`는 APK 안의 공유 라이브러리로 동적 로드된다. §6(b)의 "suitable shared library mechanism"에 해당하는지, 서명된 APK·Play 배포에서 사용자가 수정본을 넣을 수 있다고 볼 수 있는지 확인이 필요하다. 이용약관이 수정을 위한 리버스 엔지니어링을 막지 않는지도 확인한다.
4. **libvlcjni 자체**: LGPLv2.1이다(`libvlc/COPYING.LIB`). Java 바인딩 수정본도 같은 저장소에 공개한다.
5. **광고 수익과 비공개 앱**: LGPL 자체는 상업적 사용을 막지 않는다고 알려져 있지만, 위 1~3을 법률 검토로 확인받는다.

## 7. 대안(비교용)

- Media3/ExoPlayer(Apache 2.0)를 쓰면 GPL·LGPL 의무가 사라지지만, 프로젝트 규칙상 다시 넣지 않는다.
- LibVLC를 그대로 두고 GPL 의무(앱 전체 소스 공개)를 지키는 방법도 있지만, 비공개 앱 방침과 맞지 않는다.

## 8. 단계별 작업 목록

| # | 작업 | 담당 | 산출물 / 완료 조건 |
|---|---|---|---|
| 1 | 공개 저장소 결정·생성(예: `pieceplay-libvlc`) | 사용자/PM | 저장소 URL |
| 2 | libvlcjni `81bb02b` 가져오기, 패치 작성: `--enable-realrtsp` 제거, `--disable-lua` 및 Lua Copy 작업 제거, 명시적 `--disable-*`, 블랙리스트 추가, 스캐너 게이트 | program | 패치 커밋 |
| 3 | WSL Docker에서 arm64 1개 시험 빌드(`--license a --release`) | program | arm64 `libvlc.so`, 스캐너 PASS, 소요 시간 실측 |
| 4 | 실패 시 2.4절 대응(`--disable-gnutls` 등) | program | 빌드 통과 |
| 5 | 나머지 ABI 빌드, 한 AAR로 합치기(Gradle `GRADLE_ABI` 경로 확인) | program | `libvlc-lgpl-3.7.5-lgpl.1.aar`, 스캐너 PASS(4 ABI) |
| 6 | GitHub Actions 워크플로로 같은 빌드 재현, Release에 AAR, 체크섬, contrib 소스 업로드 | program | Release 링크 |
| 7 | 앱 브랜치: 의존성 교체(ivy+verification), `abiFilters`, 고지 목록 수정 | program(+design 문구) | `assembleDebug`, `testDebugUnitTest` 통과 |
| 8 | 회귀: 기존 계측 테스트 3개 + 코덱 스모크 테스트, 실기기 확인 | program | 검증 기록 `program/` |
| 9 | 법률 확인(6절) 후 PM이 병합·출시 결정 | 사용자/PM | 승인 |

## 9. 사용자가 해야 할 일

1. **승인**: 앱 의존성을 `libvlc-all`에서 자체 빌드 AAR로 바꾸는 것(외부 의존성 변경). 공개 저장소를 새로 만드는 것.
2. **결정**: x86 ABI 포함 여부, 공개 저장소의 이름과 소유 계정, `a`(LGPLv2.1)를 기본으로 하는 것.
3. **법률 확인**: 6절의 [확인 필요] 항목.
4. **WSL**: sudo 명령은 필요 없다. 빌드할 때 PC가 6~12시간 켜져 있어야 하고 디스크 약 100 GB를 쓴다. Docker가 동작하지 않는 경우에만 `sudo service docker start`(또는 Docker Desktop 실행)가 필요하다.

## 10. 예상 일정(작업일)

| 단계 | 기간 |
|---|---|
| 패치 작성 + arm64 시험 빌드(기계 2~3시간) | 1~1.5일 |
| 빌드 문제 대응 + 4 ABI·AAR 합치기 | 1~2일 |
| GitHub Actions 재현 + Release | 0.5~1일 |
| 앱 통합 + 회귀 테스트 | 1~1.5일 |
| **합계** | **4~6일**(법률 확인 기간은 별도) |

## 11. 위험

| 위험 | 대응 |
|---|---|
| `--license a`는 VideoLAN CI에서 빌드하지 않는 조합이라 깨질 수 있다 | 2.4절 대응. 최후에는 `l`로 물러나고 법률 확인을 받는다. |
| 블랙리스트에 넣은 모듈 때문에 기능이 회귀한다(회전, 디인터레이스, 채널 다운믹스) | 코덱 스모크 테스트와 실기기 확인. 문제가 있으면 LGPL 대체 모듈을 찾거나 PM이 수용 여부를 결정한다. |
| 헤더 기반 GPL 판정이 빠뜨린 것이 있다 | `.a` 단계 라이선스 문자열 게이트, contrib 산출물 목록 검사, 소스 헤더 재검사를 빌드마다 한다. |
| GitHub Actions의 디스크와 6시간 제한 | ABI별 job, 디스크 정리, contrib job 분리. 안 되면 WSL Docker 빌드 결과를 Release에 올리고 워크플로는 재현 검증용으로 둔다. |
| 상위 보안 수정(3.7.6, 3.7.7)을 따라가는 부담 | 패치를 작게 유지해 `libvlcjni-3.x`의 새 커밋에 다시 적용한다. 업데이트마다 스캐너와 스모크 테스트를 돌린다. |
| `build_vlc_revision`이 공식 산출물과 다르다 | 동작에는 영향이 없다. 고지와 Release에 실제 해시를 기록한다. |

## 12. 이번 작업에서 실제로 확인한 것(WSL, sudo 없음)

- `~/vlcprobe/`(WSL 홈)에 clone했다.
  - libvlcjni: `libvlcjni-3.x` 브랜치
  - vlc-android: blobless
  - VLC `3.0.x`: blobless·sparse, `ac6c2a40` 체크아웃
- `compile-libvlc.sh`의 `--license`, 모듈 블랙리스트, 16 KB 링크 옵션, NDK 버전 검사, CI 이미지를 읽어서 확인했다.
- contrib `bootstrap`과 `rules.mak`의 GPL 분기, VLC GPL 모듈 44개와 GPL 헤더 파일 목록을 확인했다.
- 3.7.5 AAR에 박힌 리비전(`81bb02b`, `1e3502a298`)과 크기를 확인하고, 스캐너 기준선을 실행했다.
- Docker 동작, `docker` 그룹 소속, 두 이미지 매니페스트 조회를 확인했다. 이미지 pull과 빌드는 하지 않았다.

## 참고

- libvlcjni: https://code.videolan.org/videolan/libvlcjni (브랜치 `libvlcjni-3.x`, `buildsystem/compile-libvlc.sh`, `buildsystem/get-vlc.sh`, `buildsystem/gitlab/.gitlab-ci.yml`)
- VLC 3.0.x: https://code.videolan.org/videolan/vlc (`contrib/bootstrap`, `contrib/src/*/rules.mak`, `include/vlc_plugin.h`, `configure.ac`)
- AndroidCompile wiki: https://wiki.videolan.org/AndroidCompile/
- Maven Central: https://repo1.maven.org/maven2/org/videolan/android/
- LGPL 2.1: https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
- 16 KB 페이지: https://developer.android.com/guide/practices/page-sizes
