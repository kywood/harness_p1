# LibVLC GPL 제거 2단계 — 4 ABI 빌드와 AAR 기록

작성: program, 2026-11-12. 계획: `program/libvlc-lgpl-plan.md` 8절 작업 5. 1단계: `program/libvlc-lgpl-phase1.md`. 이슈 #101.
앱 의존성(`libvlc-all 3.7.5`)은 바꾸지 않았다(4단계).

## 1. 결과

- Release: https://github.com/kywood/pieceplay-libvlc/releases/tag/3.7.5-lgpl.1
  - `libvlc-lgpl-3.7.5-lgpl.1.aar` (82,108,564 B, SHA-256 `a6da29b4644804eefab90f396c0da4d681a85e82a4a94f08e5f756b21218db04`)
  - `SHA256SUMS`: AAR, `classes.jar`, ABI별 `.so` 12개
- 태그 `3.7.5-lgpl.1` = 공개 저장소 `5d4e9e8`("Add a Docker script that packages the 4-ABI AAR"). `.so` 4 ABI는 `a78116a`에서 빌드했고, `5d4e9e8`은 그 위에 AAR 포장 스크립트만 더한 커밋이다.
- 4 ABI 모두 빌드 안 GPL 게이트(contrib 라이브러리, `lib*_plugin.a`, 링크된 `.so`)와 앱 저장소 `program/tools/libvlc_gpl_scan.py`를 통과했다.

## 2. ABI별 빌드

모두 `buildsystem/pieceplay-docker-build.sh <abi>`(`--release --license a`), ABI마다 따로 clone한 폴더에서 빌드했다.

| ABI | 이미지 | 소요 | VLC HEAD(패치 적용 후) | GPL | LOAD 정렬(libvlc / libvlcjni / libc++) |
|---|---|---|---|---|---|
| arm64-v8a | `vlc-debian-android:20260611225331` | 12분 | `c5dc8a68e8` | 없음 | 16384 / 16384 / 16384 |
| x86_64 | `vlc-debian-android:20260611225331` | 13.5분 | `521d9467f7` | 없음 | 16384 / 16384 / 16384 |
| armeabi-v7a | `vlc-debian-android-3.0:20260701183656` | 11분 | `80831543ce` | 없음 | 16384 / 16384 / 4096 |
| x86 | `vlc-debian-android-3.0:20260701183656` | 12분 | `0f66478cce` | 없음 | 16384 / 16384 / 4096 |

- 32비트 `libc++_shared.so`(NDK r21)는 4096 정렬이다. 공식 AAR의 같은 파일과 바이트까지 같다. 16 KB 요건은 64비트 대상이라 스캐너도 통과로 본다.
- AAR 포장: `buildsystem/pieceplay-docker-aar.sh`(64비트 이미지, Gradle 9.3.1, `GRADLE_ABI=all`), 54초. 이미지의 SDK가 읽기 전용이고 android-36 플랫폼이 없어 SDK를 컨테이너 안 쓰기 가능한 위치로 복사해 쓴다(첫 시도 실패 원인이었다).
- AAR `res/values/values.xml`: `build_libvlc_revision=5d4e9e8`, `build_vlc_revision=c5dc8a68e8`(arm64 체크아웃 기준). 공식은 `81bb02b`, `1e3502a298`.

## 3. 공식 AAR과 구조 비교

| 항목 | 결과 |
|---|---|
| 파일 목록 | `assets/lua/**`(33개, 128,335 B)를 빼면 같다 |
| `AndroidManifest.xml`, `R.txt`, `aar-metadata.properties`, `assets/hrtfs/*.sofa`, `res/layout/*` | 같다 |
| `res/values/values.xml` | 리비전 문자열 두 개만 다르다 |
| `classes.jar` | 크기 +705 B. 아래 4절 참고 |

## 4. 공개 API 차이

- 클래스: 공식·새 AAR 모두 137개, 이름 목록이 같다.
- `javap -protected`(public·protected 멤버) 출력이 줄 단위로 같다(1,213줄). `javap -p`(private 포함)도 같다.
- `javap -c -p`에서 상수 풀 번호(`#n`)만 지우면 바이트코드도 같다. `classes.jar` 크기 차이는 상수 풀 배치 차이다.
- **결론: 공개 API 차이 없음.** 앱 코드는 바꾸지 않아도 된다(Lua로 처리하던 플레이리스트 파싱은 기능으로 빠지지만 API는 남는다).

## 5. 크기

AAR 전체: 공식 93,158,778 B → 새 82,108,564 B (**−11,050,214 B, −11.9%**).

ABI별 파일(원본 크기 / deflate 압축 크기, APK·다운로드 근사):

| 파일 | 공식 원본 | 새 원본 | 차이 | 공식 압축 | 새 압축 | 차이 |
|---|---|---|---|---|---|---|
| arm64-v8a `libvlc.so` | 46,087,168 | 40,338,600 | −5,748,568 | 21,883,095 | 19,159,026 | −2,724,069 |
| armeabi-v7a `libvlc.so` | 40,090,588 | 34,396,892 | −5,693,696 | 20,362,923 | 17,634,464 | −2,728,459 |
| x86 `libvlc.so` | 50,150,232 | 44,473,104 | −5,677,128 | 21,686,546 | 19,123,211 | −2,563,335 |
| x86_64 `libvlc.so` | 54,588,400 | 48,200,240 | −6,388,160 | 23,580,588 | 20,606,574 | −2,974,014 |
| arm64-v8a `libvlcjni.so` | 94,440 | 86,480 | −7,960 | 25,679 | 24,534 | −1,145 |
| armeabi-v7a `libvlcjni.so` | 67,112 | 67,112 | 0 | 23,963 | 20,148 | −3,815 |
| x86 `libvlcjni.so` | 116,200 | 83,432 | −32,768 | 29,012 | 25,413 | −3,599 |
| x86_64 `libvlcjni.so` | 90,648 | 84,016 | −6,632 | 27,459 | 26,683 | −776 |
| `libc++_shared.so`(4 ABI) | — | — | 0 | — | — | 0 |

Play 기기별 분할 다운로드 기준으로 ABI당 약 2.6~3.0 MB, 설치 후 크기로 약 5.7~6.4 MB 줄어든다. 여기에 공통 `assets/lua/**`(압축 46,347 B)가 더 빠진다.

## 6. 3단계(GitHub Actions) 전에 알아야 할 것

1. **ABI당 12~14분(12코어)**. 표준 러너 4코어면 3~4배로 보고 ABI당 40~60분이다. 6시간 제한에는 여유가 있다. job은 ABI별 matrix 4개 + AAR 포장 job 1개로 나눈다.
2. **디스크**: 빌드 트리가 ABI당 4.7~5.9 GB, 64비트 이미지 압축 해제 8.1 GB, 32비트 7.7 GB. 표준 러너 여유 약 14 GB로는 빠듯하므로 시작할 때 러너 기본 도구(.NET, Android SDK, CodeQL 등)를 지워야 한다.
3. **32비트와 64비트 이미지가 다르다.** 같은 체크아웃을 섞어 쓰지 않는다(`extras/tools`가 이미지마다 다르게 만들어진다). job마다 새로 checkout하면 자연히 지켜진다.
4. **AAR 포장은 `.so` 12개를 한 체크아웃의 `libvlc/jni/libs/<abi>/`에 모은 뒤 실행한다.** 이번에는 arm64 체크아웃에 나머지 3 ABI를 복사해(바이트 비교로 확인) 포장했다. Actions에서는 ABI job이 `libs/<abi>`를 아티팩트로 올리고 포장 job이 내려받는다. 포장 스크립트는 SDK를 `/tmp/sdk`로 복사하므로 컨테이너를 root로 돌려도 동작한다.
5. **비트 단위 재현이 아니다.** VLC HEAD 해시가 ABI·실행마다 다르다(`git am` 시각). Actions 빌드의 `.so` SHA-256은 이번 Release와 다를 것이다. Release는 "빌드마다 해시를 기록"하는 방식으로 가거나, `get-vlc.sh`에 `GIT_COMMITTER_DATE`·`GIT_AUTHOR_DATE` 고정을 넣는 패치를 먼저 검토한다.
6. **contrib 소스 묶음은 아직 Release에 없다.** WSL `~/pplibvlc/artifacts/contrib-tarballs/`(약 442 MB, arm64 빌드에서 받은 것)에 보관했다. 32비트와 64비트에서 받는 tarball 목록이 같은지는 확인하지 않았다. Actions에서 `make fetch` 결과를 모아 Release에 올리는 것을 3단계 완료 조건으로 둔다.
7. **libvlcjni `values.xml`의 `build_libvlc_revision`은 포장한 체크아웃의 HEAD**다. Actions에서는 모든 job이 같은 커밋이므로 문제가 없다.
8. 이미지 태그는 VideoLAN CI가 쓰는 고정 태그다. 레지스트리에서 지워질 수 있으므로 digest(64비트 `sha256:5f041ff5…`)로 고정하는 것을 검토한다.

## 7. 하지 않은 것

- 앱 의존성 교체, `abiFilters`, 고지 목록 수정(4단계).
- 에뮬레이터·실기기 재생 확인. 블랙리스트로 빠진 rotate·deinterlace·hqdn3d·channel mixer의 회귀 확인도 4단계 전에 필요하다.
- GitHub Actions 재현 빌드(3단계).

## 8. WSL 정리

- 지운 것: ABI별 빌드 트리 4개(`~/pplibvlc/build/*`, 약 21 GB), 비교용 압축 해제 폴더. 1단계 시도 1 트리는 이미 없었다.
- 남긴 것: `~/pplibvlc/artifacts/`(AAR, `SHA256SUMS`, contrib tarball), `logs/`, `old-aar/`(공식 AAR과 `libvlc.so`), `pieceplay-libvlc/`(공개 저장소 체크아웃). 합계 677 MB.
- Docker 이미지 두 개(약 16 GB)는 남겼다. 로컬 재빌드가 더는 필요 없으면 `docker rmi`로 지울 수 있다. vlc 이미지 컨테이너는 남은 것이 없다.
