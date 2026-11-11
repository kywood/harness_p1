# 시안 B: 오픈소스 라이선스 화면

- 근거: `launch/releases/v.2026.11.12.003-precheck.md` "오픈소스 라이선스 고지 — 수정 필요(LGPL 고지와 라이선스 사본 제공)"
- 진입: 메인 하단 "오픈소스 라이선스"(시안 A)
- 상태: 시안. 화면 추가(`AppScreen`)와 전문 텍스트 리소스는 program

## 1. 구성

두 단계: **목록** → 항목을 누르면 **전문 보기**. 광고 없음(CONVENTIONS 광고 원칙: 메인·재생 목록만 배너).

### 1-1. 목록

```
┌ AppTopBar: ←  오픈소스 라이선스 ───────────────┐
│ PiecePlay는 아래 오픈소스 소프트웨어를 사용합니다.   │  bodyMedium onSurfaceVariant
│ 항목을 누르면 라이선스 전문을 볼 수 있습니다.        │
│                                                   │
│ ┌ SectionCard (누를 수 있음) ─────────────────┐ │
│ │ LibVLC                                  ›   │ │  titleMedium
│ │ 3.7.5 · LGPL 2.1 이상                        │ │  labelMedium onSurfaceVariant (tnum)
│ │ 소스 코드를 받을 수 있습니다.                  │ │  bodySmall onSurfaceVariant (LGPL 항목만)
│ └─────────────────────────────────────────────┘ │
│ ┌ libtorrent4j  2.1.0-39 · MIT             › ┐ │
│ ...                                               │
│ 그 밖의 구성요소                                   │  titleSmall (제목 역할)
│ Google 모바일 광고 SDK, 사용자 메시지 플랫폼(UMP)은 │  bodySmall
│ 오픈소스가 아니며 Google 약관을 따릅니다.            │
└───────────────────────────────────────────────────┘
```

| 요소 | 컴포넌트·토큰 |
|---|---|
| 상단 바 | `AppTopBar(title = action_open_source_licenses, onBack)` |
| 목록 | `LazyColumn`, 가로 여백 `Spacing.screenGutter`, 카드 사이 `Spacing.md`, 본문 최대 폭 `Sizes.contentMaxWidth` 가운데 정렬 |
| 항목 | `SectionCard(onClick)`. 오른쪽 끝 `KeyboardArrowRight`(코어 아이콘, `Sizes.icon`, `onSurfaceVariant`). 카드 전체가 48dp 이상 |
| 버전·라이선스 줄 | `labelMedium` + 고정폭 숫자(renewal-plan 3절 "보조·수치") |

### 1-2. 전문 보기

```
┌ AppTopBar: ←  LibVLC ─────────────────────────┐
│ ┌ SectionCard ───────────────────────────────┐ │
│ │ Copyright © VideoLAN and VLC authors        │ │  bodyMedium (저작권 줄, 항목별)
│ │ LibVLC는 LGPL 2.1 이상으로 배포됩니다. …       │ │  bodyMedium (LGPL 안내, LibVLC만)
│ │ [소스 코드 ↗]                               │ │  TextAction(DEFAULT, trailingIcon ic_open_in_new)
│ └─────────────────────────────────────────────┘ │
│ GNU LESSER GENERAL PUBLIC LICENSE              │  전문: bodySmall, FontFamily.Monospace
│ Version 2.1, February 1999 …                    │  onSurface, 길게 누르면 복사(SelectionContainer)
└───────────────────────────────────────────────────┘
```

- 전문은 `res/raw/` 텍스트를 문단 단위로 나눠 `LazyColumn` 항목으로 그린다(LGPL 전문 약 500줄, 한 `Text`로 그리면 느리다).
- 고정폭 글꼴은 시스템 `FontFamily.Monospace`(새 글꼴 파일 없음). 크기는 `bodySmall` 그대로, 줄바꿈 허용(가로 스크롤 없음).
- 같은 라이선스(예: Apache 2.0)는 전문 파일 하나를 여러 항목이 같이 쓴다(공용 리소스 풀링).

## 2. 목록 내용

버전은 `gradle/libs.versions.toml`·`app/build.gradle.kts` 기준. **저작권 줄과 포함 모듈은 program이 실제 산출물(AAR/JAR의 LICENSE·NOTICE, POM)에서 확인해 채운다.** design은 표시 형식만 정한다.

| 순서 | 이름 | 버전 | 라이선스 | 소스·홈 | 전문 파일 | 확인할 것 |
|---|---|---|---|---|---|---|
| 1 | LibVLC | 3.7.5 (`libvlc-all`) | LGPL 2.1 이상 | https://code.videolan.org/videolan/vlc-android | `license_lgpl_2_1.txt` | `libvlc-all`에 들어 있는 FFmpeg 등 서드파티 모듈과 각 라이선스. GPL 모듈이 들어 있는지 |
| 2 | libtorrent4j | 2.1.0-39 | MIT | https://github.com/aldenml/libtorrent4j | `license_mit_libtorrent4j.txt`(저작권 줄 포함) | |
| 3 | libtorrent | (libtorrent4j에 포함된 버전) | BSD 3-Clause | https://github.com/arvidn/libtorrent | `license_bsd3_libtorrent.txt` | 버전 |
| 4 | Boost | (libtorrent4j에 포함) | Boost Software License 1.0 | https://www.boost.org | `license_bsl_1_0.txt` | 포함 여부 |
| 5 | OpenSSL | (libtorrent4j에 포함) | Apache 2.0(3.x) 또는 OpenSSL/SSLeay(1.x) | https://www.openssl.org | `license_apache_2_0.txt` 또는 별도 | 포함 여부와 버전 |
| 6 | NanoHTTPD | 2.3.1 | BSD 3-Clause | https://github.com/NanoHttpd/nanohttpd | `license_bsd3_nanohttpd.txt` | |
| 7 | AndroidX, Jetpack Compose, Material 3 | Compose BOM 2026.02.01 등 | Apache 2.0 | https://developer.android.com/jetpack | `license_apache_2_0.txt` | 하나로 묶어 표시 |
| 8 | Kotlin 표준 라이브러리, kotlinx.coroutines | 2.2.10 등 | Apache 2.0 | https://kotlinlang.org | `license_apache_2_0.txt` | |
| 9 | Material 아이콘(벡터 포함) | — | Apache 2.0 | https://fonts.google.com/icons | `license_apache_2_0.txt` | 직접 넣은 벡터(`ic_folder`, `ic_content_paste`, `ic_replay_10` 등) 포함 |

- 순서: LGPL(고지 의무가 가장 큰 것) → 앱의 핵심(토렌트·서버) → 플랫폼 라이브러리.
- 오픈소스가 아닌 Google 광고 SDK·UMP는 목록 아래 "그 밖의 구성요소"에 한 문단으로만 적는다.
- `com.google.android.gms:oss-licenses` 플러그인으로 자동 생성하는 방법도 있으나 외부 의존성 추가라 사용자 승인이 필요하다. 이 시안은 손으로 관리하는 목록을 전제로 한다. 의존성을 바꾸면 이 목록도 같이 바꾼다(program 체크 항목).

## 3. 상태별 모습

| 상태 | 모습 |
|---|---|
| 목록 | 위 1-1 |
| 전문 보기 | 위 1-2. 처음 열면 맨 위 |
| 전문 파일을 읽지 못함 | 카드 안 저작권·소스 링크는 보이고, 전문 자리에 `EmptyState`(경고 아이콘 + `licenses_load_error`, 버튼 없음) |
| 브라우저를 열 수 없음 | 스낵바 `error_browser_unavailable`(시안 A와 같은 키) |
| 뒤로 | 전문 → 목록 → 메인. 상단 바 ←와 시스템 뒤로가 같다 |
| 가로 | 본문 최대 폭 600dp, 가운데 정렬. 상단 바는 화면 폭 |
| 글자 200% | 카드 제목 2줄까지, 버전 줄 줄바꿈 허용. 전문은 줄바꿈되며 가로 스크롤 없음 |

## 4. 문구 (strings.xml)

| 키 | 문구 |
|---|---|
| `action_open_source_licenses` | 오픈소스 라이선스 (시안 A와 같은 키, 화면 제목) |
| `licenses_intro` | PiecePlay는 아래 오픈소스 소프트웨어를 사용합니다. 항목을 누르면 라이선스 전문을 볼 수 있습니다. |
| `licenses_item_meta` | %1$s · %2$s (버전 · 라이선스 이름) |
| `licenses_source_available` | 소스 코드를 받을 수 있습니다. |
| `licenses_lgpl_notice` | LibVLC는 GNU LGPL 2.1 이상으로 배포됩니다. 소스 코드는 아래 주소에서 받을 수 있으며, 라이선스에 따라 이 라이브러리를 수정하거나 다른 버전으로 바꿔 쓸 수 있습니다. |
| `licenses_source_code` | 소스 코드 |
| `licenses_others_title` | 그 밖의 구성요소 |
| `licenses_others_body` | Google 모바일 광고 SDK와 사용자 메시지 플랫폼(UMP)은 오픈소스가 아니며 Google 약관을 따릅니다. |
| `licenses_load_error` | 라이선스 문서를 열지 못했습니다. 앱을 다시 열어 주세요. |
| 이름·버전·라이선스 이름·URL | `translatable="false"` 리소스 또는 코드 상수(고유명사). program이 목록 객체 하나에 모은다 |

- `licenses_lgpl_notice`의 법적 문장은 design이 쓴 안내 문구다. launch(또는 사용자)가 LGPL 고지 요건에 맞는지 한 번 확인한다.

## 5. 토큰·컴포넌트

- 새 토큰 없음. `AppTopBar`, `SectionCard(onClick)`, `TextAction`(+`trailingIcon`, 시안 A), `EmptyState`, `ic_open_in_new`(시안 A)를 쓴다.
- 새 화면 파일: `ui/screens/OpenSourceLicensesScreen.kt`(목록·전문 두 상태). 화면 하나에서만 쓰므로 `components/`에 두지 않는다.

## 6. 접근성

- 상단 바 제목, "그 밖의 구성요소"는 제목(`heading()`) 역할.
- 항목 카드는 하나의 버튼으로 읽힌다: "LibVLC, 3.7.5, LGPL 2.1 이상, 소스 코드를 받을 수 있습니다". 화살표 아이콘은 장식(`contentDescription = null`).
- 전문은 문단 단위로 탐색된다.
