# 시안 A: 메인 하단 법적 진입점

- 요구사항: `planning/requests/policy-notice-and-privacy.md`, `launch/releases/v.2026.11.12.003-precheck.md`(저작권 안내·라이선스 고지 "수정 필요")
- 화면: 메인(`ui/screens/MainScreen.kt` `Footer`)
- 리뷰: `design/reviews/planning-60.md` 1(a)
- 상태: 시안. 구현은 PM 배정 후 program·design

## 1. 배치

지금 `Footer`(버전 + 광고 개인정보 설정)를 아래로 바꾼다. 바로가기 카드 두 개 아래, 같은 스크롤 안이다.

```
┌ 화면 폭 (가운데 정렬, 최대 contentMaxWidth 600dp) ────────────┐
│ [재생 목록 카드]          [저장 파일 관리 카드]                 │
│                                                    ↕ Spacing.xl │
│        이용할 권리가 있는 콘텐츠만 재생하세요.        ① 안내    │
│                                                    ↕ Spacing.xs │
│  이용 안내 · 개인정보처리방침↗ · 오픈소스 라이선스    ② 링크    │
│            · 광고 개인정보 설정                    (줄바꿈 허용)│
│                                                    ↕ Spacing.xs │
│                 버전 v.2026.11.12.003               ③ 버전      │
│                                                    ↕ Spacing.xl │
├──────────────── (AppRoot의 배너 광고) ─────────────────────────┤
```

| 요소 | 컴포넌트 | 글자 | 색 | 크기·간격 |
|---|---|---|---|---|
| 묶음 전체 | `Column`, 가운데 정렬 | | | 위 `Spacing.xl`(바로가기 카드와 구분), 아래 `Spacing.xl`(배너와 거리, 잘못 누르기 방지) |
| ① 저작권 안내 | `Text`, 가운데 정렬 | `bodySmall` | `onSurfaceVariant` | 최대 2줄, 줄임표 없음(잘리면 안 되는 문장) |
| ② 링크 묶음 | `FlowRow`(가운데 정렬) 안에 `TextAction(tone = MUTED)` | `labelLarge`(TextAction 기본) | `onSurfaceVariant`(배경 대비 약 9.1:1) | 각 링크 높이 48dp(`Sizes.touchTarget`, TextAction 기본). 링크 사이 구분점 "·"은 `labelLarge` `outline`이 아닌 `onSurfaceVariant` 50%로 장식(접근성에서 숨김) |
| 개인정보처리방침 뒤 아이콘 | `TextAction(icon = ic_open_in_new)`, 아이콘은 글자 **뒤** | | | `Sizes.iconSmall` 18dp. 외부 브라우저로 나간다는 표시 |
| 광고 개인정보 설정 | 같은 `TextAction(MUTED)` | | | 동의 필요 지역에서만(`onPrivacyOptions != null`, 지금과 같음) |
| ③ 버전 | `Text` | `labelSmall` | `onSurfaceVariant` | 누를 수 없음 |

- `TextAction`은 아이콘을 글자 앞에 두는 구조라, 아이콘을 뒤에 둘 수 있게 `trailingIcon` 인자를 더한다(design이 `ui/components/TextAction.kt`에서 처리, 기존 호출 영향 없음).
- 링크 순서는 고정: 이용 안내 → 개인정보처리방침 → 오픈소스 라이선스 → 광고 개인정보 설정. 좁은 화면에서 줄이 바뀌면 "오픈소스 라이선스" 앞에서 바뀌는 경우가 많다(360dp, 글자 100% 기준 추정).
- 링크는 최대 4개로 제한한다(리뷰 1(a)). 도움말·의견 보내기 등은 이용 안내 시트(시안 C)에 넣는다.

### 가로 화면

메인 전체가 이미 세로 스크롤이고 본문 폭은 600dp로 제한된다. 하단 묶음은 스크롤 맨 끝에 있으므로 가로에서는 스크롤해야 보인다. 링크 묶음은 600dp 안에서 한 줄에 들어간다.

### 키보드가 올라온 상태

`imePadding` + 스크롤 유지(지금과 같음). 하단 묶음은 입력 중에는 가려져도 된다.

## 2. 동작

| 누르는 것 | 결과 | 담당 |
|---|---|---|
| 이용 안내 | 이용 안내 시트(시안 C)를 연다 | program: 메인 화면 안 상태(내비게이션 변경 없이 가능) |
| 개인정보처리방침 | 외부 브라우저로 `url_privacy_policy`를 연다(`ACTION_VIEW`) | program |
| 오픈소스 라이선스 | 오픈소스 라이선스 화면(시안 B)으로 | program: `AppScreen` 추가 |
| 광고 개인정보 설정 | UMP 옵션 창(지금과 같음) | — |

브라우저를 열 수 없으면(브라우저 앱 없음·비활성) 스낵바: `error_browser_unavailable`.

## 3. 상태별 모습

| 상태 | 모습 |
|---|---|
| 동의 필요 지역 | 링크 4개(2줄로 바뀔 수 있음) |
| 동의 불필요 지역 | 링크 3개(대개 한 줄) |
| 메인 오류 표시 중 | 하단 묶음은 그대로. 오류는 입력 카드 안(시안 D) |
| 글자 크기 200% | 링크가 한 줄에 하나씩 쌓여도 `FlowRow`라 잘리지 않는다. ① 문장은 3~4줄 |
| 버전명 조회 실패 | ③ 줄을 숨긴다(빈 "버전 " 글자를 보이지 않게) |
| 개인정보처리방침 URL 미정 | **출시하지 않는다.** 링크를 숨기는 분기는 만들지 않는다(심사 필수 항목이 조용히 빠지는 것을 막기 위해) |

## 4. 문구 (strings.xml)

| 키 | 문구 | 비고 |
|---|---|---|
| `main_rights_notice` | 이용할 권리가 있는 콘텐츠만 재생하세요. | 신규. 이용 안내 시트·첫 사용 안내에서도 이 키를 재사용 |
| `action_usage_guide` | 이용 안내 | 신규. 시트 제목에도 재사용 |
| `action_privacy_policy` | 개인정보처리방침 | 신규 |
| `action_open_source_licenses` | 오픈소스 라이선스 | 신규. 라이선스 화면 제목에도 재사용 |
| `action_ad_privacy` | 광고 개인정보 설정 | 기존 유지 |
| `main_version` | 버전 %1$s | 신규 |
| `url_privacy_policy` | (launch가 정한 URL) | 신규, `translatable="false"`. Play Console 등록정보 URL과 같아야 한다 |
| `cd_opens_in_browser` | 브라우저에서 열림 | 신규. 개인정보처리방침 링크의 읽기 문구 "개인정보처리방침, 브라우저에서 열림" |
| `error_browser_unavailable` | 웹 브라우저를 열지 못했습니다. 브라우저 앱이 켜져 있는지 확인해 주세요. | 신규 |

## 5. 토큰·리소스

- 새 색·간격 토큰 없음. `Spacing.xs`·`xl`, `Sizes.touchTarget`·`iconSmall`, `TextActionTone.MUTED`를 쓴다.
- 새 아이콘: `res/drawable/ic_open_in_new.xml`(Material Symbols "open_in_new", 24dp 벡터 하나, 18dp로 축소해 씀). 코어 아이콘 묶음에 없어서 벡터로 한 번만 둔다.
- 컴포넌트 변경: `TextAction`에 `trailingIcon: ImageVector? = null`.

## 6. 접근성

- 각 링크는 버튼 역할(TextButton)이고 이름은 글자 그대로. 개인정보처리방침만 "브라우저에서 열림"을 덧붙인다.
- 구분점 "·"은 `clearAndSetSemantics {}`로 읽지 않는다.
- 대비: `onSurfaceVariant`(#A7B3D1) / `background`(#0A0F1E) 약 9.1:1, `bodySmall`도 AA 통과.
- 저작권 안내 문장은 일반 글자로 읽힌다(제목 아님).

## 7. 영향

- 광고 노출 조건 변경 없음. 배너와 하단 링크 사이 24dp 이상.
- 화면 테스트: 메인 테스트가 "광고 개인정보 설정" 글자를 찾는다면 위치만 바뀌고 문구는 같다.
