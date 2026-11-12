# LibVLC GPL 제거 1단계 — 패치와 arm64 시험 빌드 기록

작성: program, 2026-11-12. 계획: `program/libvlc-lgpl-plan.md` 8절 작업 2~3. 이슈 #101.
앱 의존성(`libvlc-all 3.7.5`)은 바꾸지 않았다(4단계).

## 1. 공개 저장소

- https://github.com/kywood/pieceplay-libvlc (공개, 브랜치 `main`)
- 기반: libvlcjni `81bb02b`(원본 이력 그대로) + VLC 3.0.x `ac6c2a40` + 동봉 패치 20개(그대로).
- 커밋 작성자는 앱 저장소와 같은 익명 설정(`Your Name <you@example.com>`)을 저장소 로컬 설정으로 썼다. WSL 전역 git 설정에는 개인 이메일이 있어 쓰지 않았다.

### 패치 목록(`git log 81bb02b..main`)

| 커밋 | 내용 |
|---|---|
| `5b68c21` compile-libvlc: do not force build realrtsp | `--enable-realrtsp` 제거(상위 `ddde54f`와 같음) |
| `025e564` Disable Lua and stop shipping the Lua scripts | contrib·configure `--disable-lua`, Gradle `luaPlaylistCopy`·`luaMetaCopy`·`luaModuleCopy` 제거 |
| `cbca78f` compile-libvlc: explicitly disable GPL modules for LGPL licenses | `--license l` 또는 `a`에서 `--disable-x264 --disable-x26410b --disable-x265 --disable-postproc --disable-dvdnav --disable-dvdread --disable-a52 --disable-faad --disable-mad --disable-realrtsp --disable-lua`, `a`는 `--disable-gnutls --disable-live555` 추가 |
| `bde9585` compile-libvlc: blacklist GPL VLC modules | 블랙리스트 추가: dummy, access_realrtsp, lua, dolby_surround_decoder, headphone_channel_mixer, mono, rotate, deinterlace, hqdn3d, file_logger, syslog, stream_out_rtp, stream_out_cycle, vod_rtsp, audioscrobbler, x264, x26410b, x265, postproc, dvdnav, dvdread |
| `925f27e` compile-libvlc: add a GPL scan gate for LGPL builds | contrib 설치 디렉터리의 GPL 라이브러리 검사, 정적 모듈(`lib*_plugin.a`)과 링크된 `.so` 스캔, 검사 대상이 없으면 실패 |
| `ed6ddc1` Add PiecePlay README and Docker build script | README(첫 줄 "이 빌드는 PiecePlay 앱의 LGPL 준수용"), 원본 README는 `README.libvlcjni.md`, `buildsystem/pieceplay-docker-build.sh` |
| `1c29650` compile-libvlc: restore the executable bit | Windows에서 편집하며 빠진 실행 권한 복구(첫 빌드 실패 원인) |
| `a78116a` libvlc_gpl_scan: do not use "x264 - core" as an x264 marker | 스캐너 오탐 수정(3절) |

## 2. 빌드

- 환경: WSL Ubuntu-26.04, 12코어, Docker 29.7.2, 이미지 `registry.videolan.org/vlc-debian-android:20260611225331`(NDK r29, API 21). sudo 사용 안 함.
- 명령: `./buildsystem/pieceplay-docker-build.sh arm64` → 컨테이너 안에서 `get-vlc.sh` + `compile-libvlc.sh -a arm64 --release --license a`.
- 작업 폴더(WSL): `~/pplibvlc/build/arm64`(공개 저장소에서 새로 clone), 로그 `~/pplibvlc/logs/arm64-build.log`. 첫 시도 산출물과 로그는 `~/pplibvlc/build/arm64-attempt1`, `~/pplibvlc/logs/arm64-build-attempt1.log`.
- 계획 2.1의 [확인 필요](공식 AAR과 같은 VLC 해시가 나오는가): 나오지 않는다. 아래 시도 3 설명 참고. 소스 내용과는 무관하다.

### 시도 기록

| # | 결과 | 원인 / 조치 |
|---|---|---|
| 1 | 실패(곧바로) | `compile-libvlc.sh` 실행 권한 없음(`Permission denied`). Windows에서 `\\wsl$` 경로로 편집하면서 모드가 644로 바뀜 → `1c29650`으로 복구 |
| 2 | 링크까지 성공, 마지막 `.so` 게이트에서 실패 | contrib 게이트 ok, 정적 모듈 252개 스캔 ok. `libvlc.so`에서 `x264=2`. 확인해 보니 x264 코드가 아니라 LGPL FFmpeg `libavcodec/h2645_sei.c`의 x264 스트림 감지 문자열(`"x264 - core %d"`, `"x264 - core 0000"`)이었다. FFmpeg는 `CONFIG_GPL 0`, `CONFIG_VERSION3 0`. → 표식을 `x264_encoder_open`과 x264 인코더 배너로 바꿈(`a78116a`, 앱 쪽 `program/tools/libvlc_gpl_scan.py`도 같이). 바꾼 표식으로도 기존 3.7.5 AAR은 4 ABI 모두 x264를 잡는다(`x264=1`) |
| 3 | **성공**(`exit=0`) | 공개 저장소(`a78116a`)에서 새로 clone해 처음부터 다시 빌드. 모든 게이트 통과 |

소요 시간(시도 3, 처음부터): 11:18:32Z → 11:30:41Z, **약 12분**. VLC clone, contrib 원본 내려받기, contrib 57개 소스 빌드, VLC 빌드, ndk-build를 모두 포함한다. 계획서의 추정(ABI당 1.5~3시간)보다 훨씬 짧다. 빌드 트리는 ABI당 약 5.8 GB다.

- 패치 적용 후 VLC 커밋 해시는 빌드마다 바뀐다(시도 2 `8569cb2558`, 시도 3 `c5dc8a68e8`, 공식 AAR `1e3502a298`). `get-vlc.sh`의 `git am`이 실행 시각으로 커밋을 만들기 때문이다. `libvlc.so`도 크기는 같지만 바이트 단위로 같지는 않다(비트 단위 재현 빌드가 아니다).

## 3. GPL 검사 결과

빌드 안 게이트와, 앱 저장소 `program/tools/libvlc_gpl_scan.py`로 따로 돌린 결과가 같다.

| 대상 | 결과 |
|---|---|
| contrib 설치 디렉터리 GPL 라이브러리 | 없음(ok) |
| 정적 모듈 `lib*_plugin.a` 252개(링크되는 전부) | 252개 모두 ok, GPL 모듈 라이선스 문자열 0 |
| `libvlc.so` | `gpl=-`(GPL 흔적 0), LOAD 정렬 16384 |
| `libvlcjni.so` | `gpl=-`, LOAD 정렬 16384 |
| `libc++_shared.so` | `gpl=-`, LOAD 정렬 16384 |
| `libvlc/assets` | `lua/` 없음(Gradle은 돌리지 않았지만 Copy 작업 자체를 지웠다) |

비교: 같은 스캐너로 기존 `libvlc-all-3.7.5` arm64는 `x264=1, libpostproc=1, libdvdnav=4, libdvdread=10, realrtsp=2, lua=1`로 FAIL이다.

SHA-256(시도 3): `libvlc.so` `7b325bf866718f7e6431813ec3650ca775988bd2fec43d1d8ecea484de4218f9`, `libvlcjni.so` `e63232be5d0dcb9a08da86855b76f445ec38b0d4a7f174df66d7bdc7c3c10b2d`, `libc++_shared.so` `0c52cfab2df0d957d8b346a2bdc5ae8d71feca2591924d77e1cd724d5bf74352`(NDK r29 원본과 같은 파일).

- contrib 설치 디렉터리: `libx264.a`, `libx265.a`, `libpostproc.a`, `libdvdnav.a`, `libdvdread.a`, `libdvdcss.a`, `liba52.a`, `libfaad.a`, `libmad.a`, `libmpeg2.a`, `liblua*.a`, `libgnutls.a`, `libnettle.a`, `libhogweed.a`, `libgmp.a`, `libliveMedia.a` 모두 없음.
- 빌드된 contrib: aom ass bluray cddb dav1d dvbpsi ebml ffmpeg flac fluidlite fontconfig freetype2 fribidi gcrypt gettext gme gpg-error gsm harfbuzz iconv jpeg lame libarchive libdsm libplacebo librist libtasn1 libxml2 luac matroska microdns modplug mpg123 mysofa nfs ogg openapv openjpeg opus png protobuf shout smb2 soxr spatialaudio speex speexdsp ssh2 taglib theora twolame upnp vorbis vpx zlib zvbi. (`luac`는 빌드 머신용 도구로 `liblua*.a`는 남지 않았다.)
- 블랙리스트에 넣은 모듈과 gnutls·live555 모듈은 정적 모듈 목록(`libvlcjni-modules.c`)에 없다. 재생에 쓰는 모듈(avcodec, mediacodec, android_audiotrack, opensles_android, mkv, mp4, avformat, http, filesystem, freetype, libass, subsdec, android_window, egl_android, gles2, adaptive, ts, es, ps, packetizer_h264·hevc 등)은 있다.
- 남은 "GNU General Public License" 문자열(정보): `libvlccore.a`의 `src/config/help.c`(LGPL)가 `--version` 안내문으로 VLC 라이선스 문구를 갖고 있고, `libgpg-error.a`의 `argparse.c`(LGPL-2.1+)가 프로그램용 라이선스 안내문을 갖고 있다. 둘 다 문자열이며 GPL 코드가 아니다. **[확인 필요: 법률 검토 때 함께 확인]**

## 4. 크기(arm64-v8a, 지금 AAR `libvlc-all-3.7.5`과 비교)

둘 다 strip된 상태다.

| 파일 | 기존 3.7.5 | 새 빌드 | 차이 |
|---|---|---|---|
| `libvlc.so` 원본 크기 | 46,087,168 B (46.1 MB) | 40,338,600 B (40.3 MB) | −5,748,568 B (−12.5%) |
| `libvlc.so` deflate 압축(APK·다운로드 근사) | 21,883,095 B (21.9 MB) | 19,159,026 B (19.2 MB) | −2,724,069 B (−12.4%) |
| `libvlcjni.so` | 94,440 B | 86,480 B | −7,960 B |
| `libc++_shared.so` | 9,290,184 B | 9,290,184 B | 0 |

AAR에서 `assets/lua/**`(약 0.15 MB)도 빠진다. 계획서 추정(ABI당 3~6 MB 감소, 다운로드 1~3 MB 감소)과 맞는다.

## 5. 2단계 전에 알아야 할 것

1. **빌드 시간이 짧다.** 64비트 한 ABI가 처음부터 약 12분이다. 32비트도 비슷하면 4 ABI를 한 시간 안에 끝낼 수 있고, GitHub Actions의 6시간 제한도 문제가 안 될 가능성이 크다(표준 러너는 4코어라 3~4배 걸린다고 보면 ABI당 40~50분). 디스크는 ABI당 약 5.8 GB다.
2. **32비트(armeabi-v7a, x86)는 다른 이미지다.** `vlc-debian-android-3.0:20260701183656`(NDK r21, API 17). 아직 pull·빌드하지 않았다. 같은 vlc 체크아웃에서 64비트 다음에 돌리면 `extras/tools`(protoc, ninja)를 다른 이미지에서 만든 것을 다시 쓰게 되므로, **ABI마다 따로 clone한 폴더에서** 빌드하는 것이 안전하다. 32비트는 기존 AAR에서 GPL 모듈 라이선스 문자열이 `.so`에도 보였으므로 `.so` 게이트가 더 엄격하게 작동한다.
3. **AAR 합치기는 아직 안 했다.** `.so`만 만들었다. 2단계에서 Gradle(`GRADLE_ABI`, 이미지 안 `/opt/tools/gradle` 9.3.1)로 AAR을 만들고, 4 ABI를 하나로 합치는 방법을 정해야 한다. 이때 AAR `assets/`에 `lua/`가 없는지 스캐너 외에 따로 확인한다.
4. **비트 단위 재현이 아니다.** VLC 해시와 `.so` 바이트가 빌드마다 다르다. Release에는 실제 빌드의 해시·SHA-256을 기록하는 방식으로 간다. 필요하면 `get-vlc.sh`에 `GIT_COMMITTER_DATE` 고정을 넣는 패치를 검토한다.
5. **스캐너 표식은 휴리스틱이다.** 이번에 FFmpeg 문자열 오탐을 하나 고쳤다. 다른 ABI에서 새 오탐이 나오면 원인 문자열의 출처(어느 `.a`/소스)를 확인한 뒤에만 표식을 바꾼다.
6. **기능 회귀 확인은 4단계 전에 필요하다.** 블랙리스트로 rotate·deinterlace·hqdn3d·channel mixer가 빠졌다. 정적 모듈 목록상 재생 경로 모듈(avcodec, mediacodec, mkv, mp4, http, libass, freetype 등)은 남아 있지만, 실제 재생은 확인하지 않았다(계획 5.2).
7. **남은 법률 확인**: 3절의 GPL 안내 문자열 2건, 계획 6절 항목.
8. contrib 원본 tarball은 WSL `~/pplibvlc/build/arm64/vlc/contrib/tarballs/`에 있다. 3단계 Release에 올릴 소스 묶음으로 쓸 수 있다.

## 6. 하지 않은 것

- 앱 의존성 교체, AAR 만들기(Gradle), 다른 ABI 빌드, GitHub Actions.
- 실기기·에뮬레이터 재생 확인(새 `.so`를 앱에 넣지 않았으므로 이번에는 할 수 없다).
