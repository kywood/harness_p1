# LibVLC GPL 제거 3단계 — 재현 빌드(GitHub Actions) 기록

작성: program, 2026-11-14. 계획: `program/libvlc-lgpl-plan.md` 8절 작업 6. 앞 단계: `program/libvlc-lgpl-phase1.md`, `program/libvlc-lgpl-phase2.md`. 이슈 #101.
앱 의존성(`libvlc-all 3.7.5`)은 바꾸지 않았다(4단계).

## 1. 결과

- PR: https://github.com/kywood/pieceplay-libvlc/pull/1 (브랜치 `pieceplay/reproducible-ci`, 병합은 PM)
- 워크플로: `.github/workflows/reproduce.yml`("Reproduce AAR"). `buildsystem/`·`libvlc/`·워크플로를 바꾸는 PR과 수동 실행(`workflow_dispatch`)에서 돈다.
- **사실상 비트 단위 재현 빌드가 된다.** 같은 커밋(`c845e23`)을 이 PC(WSL)와 GitHub Actions에서 따로 빌드하면 `.so` 12개 가운데 11개가 바이트 단위로 같다. 나머지 하나(armeabi-v7a `libvlc.so`)도 `.note.gnu.build-id` 20바이트만 다르다(5절). AAR 안의 다른 파일, `classes.jar`, `values.xml`(리비전 문자열 포함)도 같다.
- 2단계까지는 빌드마다 바이트가 달랐다. 원인 다섯 가지를 찾아 넷을 고쳤다(3절).
- 릴리스 `3.7.5-lgpl.1`은 이 수정 전에 빌드했으므로 `libvlc.so` 4개는 다시 빌드한 것과 바이트가 다르다. 그 밖의 비교(파일 목록, 다른 파일 바이트, `classes.jar`, GPL 스캔)는 통과한다.

| 비교 | 결과 |
|---|---|
| Actions 재빌드(`c845e23`) ↔ 릴리스 `3.7.5-lgpl.1` | PASS. 파일 22/22, 다른 파일 8/8 같음, `classes.jar` 같음, `.so` 12개 중 8개 같음(`libvlc.so` 4개만 다름) |
| Actions 재빌드 ↔ 로컬 WSL 재빌드(같은 커밋, `--require-identical-native`) | PASS. `.so` 11/12 같음, 1개(armeabi-v7a `libvlc.so`)는 build ID만 다름. VLC HEAD `b793163796` 양쪽 같음 |
| GPL 스캔(`libvlc_gpl_scan.py`, Actions AAR과 `.so` 12개) | 모두 ok, 64비트 LOAD 정렬 16384 |

## 2. 워크플로 구성

| job | 내용 | 실측 시간(표준 러너 4코어) |
|---|---|---|
| `build <abi>` ×4 (matrix, 병렬) | 러너 기본 도구 삭제(디스크 확보) → 이미지 pull(digest 고정) → `buildsystem/pieceplay-docker-build.sh <abi>`(로컬과 같은 명령, 빌드 안 GPL 게이트 포함) → `.so`·로그·VLC HEAD·contrib tarball 목록 업로드. arm64 job은 contrib 원본 tarball 묶음도 업로드 | 15~20분 |
| `package` | `.so` 12개 내려받기 → `buildsystem/pieceplay-docker-aar.sh`(VLC 체크아웃이 없으면 `get-vlc.sh`부터) → `libvlc_gpl_scan.py` → 릴리스 AAR 내려받기(릴리스 `SHA256SUMS`로 확인) → `buildsystem/pieceplay-compare-aar.py` 비교 → `SHA256SUMS` 작성 | 약 6분 |

- 산출물(워크플로 아티팩트, 30일 보관): `aar`(AAR, `SHA256SUMS`, `compare.md`, `gpl-scan.txt`), `libs-<abi>`, `log-<abi>`(빌드 로그, VLC HEAD, `.so` SHA-256, contrib tarball SHA-256 목록), `contrib-tarballs`.
- 비교 결과는 실행 요약(Summary)에도 Markdown 표로 남는다.

### 계획서와 다르게 한 것과 이유

| 계획(plan 3절·phase2 6절) | 실제 | 이유 |
|---|---|---|
| `container:`로 VideoLAN 이미지를 job 컨테이너로 쓴다 | 러너 호스트에서 `pieceplay-docker-build.sh`가 `docker run`을 한다 | 로컬과 **같은 스크립트·같은 명령**이어야 재현 비교가 의미 있다. 또 job 컨테이너 안에서는 러너 기본 도구를 지워 디스크를 확보할 수 없다. |
| 6시간 제한 때문에 contrib job을 따로 나눌 수 있다 | ABI당 job 하나 | 실측 15~20분이라 나눌 필요가 없다. |
| 캐시 검토 | 캐시 없음 | 시간 여유가 크고, contrib 캐시를 쓰면 원본 내려받기 실패나 원본 변경이 가려진다. 재현 검증 목적에는 매번 원본부터 받는 것이 맞다. |
| Release에 contrib 소스 tarball을 올린다 | 워크플로 아티팩트(`contrib-tarballs`)로 만든다. Release 업로드는 아직 안 했다 | 아티팩트는 30일 뒤 지워지므로 소스 제공 위치로는 부족하다. 다음 Release(5절)에 자산으로 붙인다. |
| 이미지 태그 사용 | digest로 pull한 뒤 태그를 다시 붙인다 | VideoLAN CI 태그는 지워지거나 옮겨질 수 있다(phase2 6절 8). |

## 3. 재현이 안 되던 원인과 수정

| 원인 | 증상 | 수정(공개 저장소 커밋) |
|---|---|---|
| `get-vlc.sh`의 `git am`이 실행 시각으로 커밋을 만든다 | VLC HEAD 해시가 빌드마다 다르고, 그 해시 문자열이 `libvlc.so`에 들어간다 | `pieceplay-docker-build.sh`에서 `GIT_COMMITTER_DATE`·`SOURCE_DATE_EPOCH`를 VLC 기준 커밋(`ac6c2a40`) 시각 `1783181575`(2026-07-04T16:12:55Z)로 고정. 저장소 HEAD 시각을 쓰지 않으므로 문서만 바꾼 커밋으로는 결과가 바뀌지 않는다 |
| VLC configure가 빌드 호스트 이름(`VLC_COMPILE_HOST`)을 기록한다 | 컨테이너 ID가 호스트 이름이라 실행마다 다르다 | `docker run --hostname pieceplay-build` |
| `compile-libvlc.sh`가 정적 모듈 목록을 `find` 순서 그대로 만든다 | 디렉터리 해시 순서는 파일 시스템마다 달라 링크 순서·코드 배치가 바뀐다. WSL과 Actions에서 VLC 해시는 같았는데 `libvlc.so`가 달랐다(x86_64는 `.text`가 약 1.3 KB 차이) | `find ... \| LC_ALL=C sort` |
| AAR 포장 job에 VLC 체크아웃이 없다 | 첫 실행에서 `assets/hrtfs/dodeca_and_7channel_3DSL_HRTF.sofa`가 빠지고 `build_vlc_revision=unknown` | `pieceplay-docker-aar.sh`가 `vlc/`가 없으면 같은 고정 값으로 `get-vlc.sh`를 먼저 돌린다 |
| VLC `src/config/help.c`가 `__DATE__`·`__TIME__`을 쓴다(`vlc --version` 안내문) | 32비트(NDK r21, clang 9)는 `SOURCE_DATE_EPOCH`를 따르지 않아 `libvlc.so`에 빌드 시각이 들어간다. 64비트(NDK r29)는 따른다 | VLC 패치 `libvlc/patches/0021`로 이 안내문에서 빌드 시각을 뺀다(Android 앱에서 쓰이지 않는 문구). 기존 패치처럼 `Message-Id`를 넣어 `get-vlc.sh` 검사를 통과한다 |
| (못 고침) armeabi-v7a `libvlc.so`의 build ID | 위를 모두 고친 뒤에도 20바이트(`.note.gnu.build-id`)만 다르다. 링커가 strip 전 바이너리(디버그 정보 포함)로 계산하는 값이라, strip 뒤 남는 코드·데이터는 같다. 디버그 정보 안의 어떤 값이 다른지는 찾지 않았다 | `pieceplay-compare-aar.py`가 이 경우를 "build ID only"로 구분해 보이고, `--require-identical-native`에서도 통과로 본다 |

## 4. 실행 기록

| 실행 | 커밋 | 결과 | 시간 | 내용 |
|---|---|---|---|---|
| [43564816512](https://github.com/kywood/pieceplay-libvlc/actions/runs/43564816512) | `fa97753` | 빌드 4개 성공, package 실패 | 빌드 15~20분, package 6분 | 비교에서 `assets/hrtfs/*.sofa` 누락 발견. 로컬과 VLC 해시는 같았지만 `libvlc.so`가 4개 모두 다름 → 모듈 목록 정렬, AAR 포장 수정 |
| [43567608497](https://github.com/kywood/pieceplay-libvlc/actions/runs/43567608497) | `06c2ab5` | 성공(PASS) | 약 26분 | 64비트 `.so`는 로컬과 같아짐. 32비트 `libvlc.so` 2개가 빌드 시각 문자열로 다름 → 패치 0021 |
| [43570163852](https://github.com/kywood/pieceplay-libvlc/actions/runs/43570163852) | `c845e23` | 성공(PASS) | 약 23분 | 로컬과 11/12 같음, armeabi-v7a는 build ID만 다름 |
| [43572752753](https://github.com/kywood/pieceplay-libvlc/actions/runs/43572752753) | `85f5d0d` | 성공(PASS) | 약 24분 | 비교 스크립트의 build ID 구분 추가. 빌드 입력이 같으므로 `.so` 12개가 `c845e23` 실행과 SHA-256까지 모두 같았다(armeabi-v7a 포함. Actions끼리는 build ID도 같다) |

로컬 대조 빌드는 같은 커밋을 ABI마다 새로 clone해 `pieceplay-docker-build.sh`로 두 개씩 병렬 빌드하고(12코어, 4 ABI 약 30분), arm64 체크아웃에서 AAR을 포장했다. 같은 커밋을 로컬에서 두 번 빌드한 결과(arm64)도 바이트까지 같았다.

`c845e23` 빌드의 SHA-256(Actions, `aar` 아티팩트 `SHA256SUMS`):

```
e32dfe2bade0d7f2be76ee84e25026d17aaa77d1f86018630b724ec9681b4cfa  classes.jar
0c52cfab2df0d957d8b346a2bdc5ae8d71feca2591924d77e1cd724d5bf74352  jni/arm64-v8a/libc++_shared.so
1537d2ca4d86efbf1470af4c7cb93b7771e70766195236a67501d7feea46a4bf  jni/arm64-v8a/libvlc.so
e63232be5d0dcb9a08da86855b76f445ec38b0d4a7f174df66d7bdc7c3c10b2d  jni/arm64-v8a/libvlcjni.so
092a44574c247982e265743acb1227e11a2f079a765814fef1b627dbfb2b79d5  jni/armeabi-v7a/libc++_shared.so
d4b2e8121425a8e0d248e3042afc0d8b3f2f8006d326ca308d67a9c9bdaa6cfc  jni/armeabi-v7a/libvlc.so   (로컬: 976ee12c…, build ID만 다름)
782213c718b9a1574d68826e701f919d0fb68e5ff93859d9eab6539079408546  jni/armeabi-v7a/libvlcjni.so
3051dd001301b81024779099e11f657f10e983d02aa1b16f18e711ec33685314  jni/x86/libc++_shared.so
f9e12240a74fa90fb927787f974e43fdfb8233bd708d81a345ef78f6a4b5265a  jni/x86/libvlc.so
593e0440d42ea7946d26c13051b0622f4328dc0760d30df10189311192b358f4  jni/x86/libvlcjni.so
e28cb9e0f456caf8498d0e8562b3ac3f73bf78c07555a52b5602396f240abec3  jni/x86_64/libc++_shared.so
9b8c45b11230932c6928265297fb82ae3b92921c1bcfb2805b18f897464c4a96  jni/x86_64/libvlc.so
f53baf6d1fde363bb601ded31a89552a40cf4308834b0fb353abf50a4b2dee60  jni/x86_64/libvlcjni.so
```

`libvlcjni.so` 4개와 `libc++_shared.so` 4개는 2단계 릴리스 `3.7.5-lgpl.1`과도 같다. `classes.jar`도 릴리스와 같다.

## 5. build ID 차이에 대해

- armeabi-v7a `libvlc.so`는 오프셋 477~496의 20바이트만 다르다. `readelf` 섹션 크기·문자열이 모두 같고, 이 위치는 `.note.gnu.build-id`의 SHA-1 값이다.
- build ID는 링크 때 strip 전 바이너리로 계산되므로, 디버그 정보(DWARF) 안의 값 하나가 환경마다 다르면 이렇게 된다. x86(같은 32비트 이미지)은 완전히 같았으므로 ARM 쪽 소스 하나(어셈블리 등)의 디버그 정보로 추정한다. 찾지 않았다.
- 고치려면 strip 전 `.so`(`libvlc/jni/obj/local/armeabi-v7a/libvlc.so`)를 두 환경에서 받아 `.debug_*`를 비교하면 된다. 재현 검증에는 지장이 없어 다음 과제로 둔다.

## 6. 남은 위험과 [확인 필요]

- **앱에 넣을 AAR 결정(PM):** 4단계에서 쓸 AAR로 (a) 지금 릴리스 `3.7.5-lgpl.1`(로컬 빌드, 바이트 재현 불가)을 쓰거나 (b) 이 PR 병합 뒤 main에서 `3.7.5-lgpl.2`를 새로 내고 contrib 소스 tarball을 같은 Release에 붙여 쓰는 방법이 있다. 기능 차이는 없다(달라지는 것은 VLC 해시 문자열, 모듈 링크 순서, `vlc --version` 안내문뿐). **추천: (b).** 바이너리와 공개 소스의 대응을 누구나 다시 빌드해 확인할 수 있고, 소스 제공 묶음도 한 Release에 모인다. (b)로 가면 4단계 회귀는 `lgpl.2`로 한다.
- 비트 단위 재현은 같은 Docker 이미지(digest)와 같은 contrib 원본에 기대고 있다. VideoLAN이 이미지를 지우면 digest 이미지를 받을 수 없게 된다. 이미지를 GHCR 등에 따로 보관할지는 정하지 않았다.
- contrib 원본은 매번 각 프로젝트 서버에서 받는다(체크섬은 VLC `SHA512SUMS`로 확인). 원본 서버가 내려가면 빌드가 실패한다. Release에 tarball을 보관하면 대비가 된다.
- `.so` 크기 외 기능 차이는 없다. 3단계는 빌드만 확인했고 재생 회귀는 4단계에서 본다.
- [확인 필요] contrib tarball 묶음을 Release에 올리는 것으로 LGPL 2.1 §6의 소스 제공 방식으로 충분한지(계획 6절 2번).
