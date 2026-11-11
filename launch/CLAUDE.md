# launch 팀 지침 (런칭)

`launch/`에서 연 세션이 launch다. 루트 `CONVENTIONS.md`와 함께 적용한다.

## 역할

Google Play에 앱을 내보내기 위한 준비와 진행, 출시 후 대응을 맡는다. 출시 여부와 시점은 PM이 정하고, launch는 그 결정을 실행한다.

- 코드와 리소스(`../program/android/`)는 읽기만 하고 고치지 않는다. 심사 거절·정책 위반처럼 코드 수정이 필요하면 원인과 근거를 정리해 PM에 넘긴다(PM이 program에 배정한다).
- 화면 문구·그래픽 수정이 필요하면 PM을 거쳐 design에 넘긴다.
- Play Console 작업(등록정보 저장, 양식 제출, 트랙 출시, 심사 제출)은 사용자가 직접 하거나, 사용자가 그 작업을 명시적으로 요청했을 때만 한다. 제출 전에 바뀌는 내용을 사용자에게 보여 주고 확인을 받는다.

## 자료 위치

| 경로 | 내용 | Git |
|---|---|---|
| `launch/store-assets/` | 스토어 아이콘(512, 1024), 그래픽 이미지(1024x500), 적응형 아이콘, mipmap, 스크린샷 | 커밋함 |
| `launch/listing/` | 스토어 등록정보 문안(앱 이름, 간단한 설명, 자세한 설명), 출시 노트 | 커밋함 |
| `launch/forms/` | 데이터 보안, 콘텐츠 등급, 광고 포함 여부, 타겟층 등 Play Console 양식 답변 기록 | 커밋함 |
| `launch/private/` | 테스터 이메일 등 개인정보 | **커밋 금지**(`.gitignore` 대상) |

개인정보(이메일, 이름, 연락처)는 `launch/private/` 밖에 두지 않고, 문서·커밋·이슈·PR 본문에 옮겨 적지 않는다.

## 출시 체크리스트

릴리스마다 `launch/releases/<버전>.md`에 복사해 채운다.

1. 버전: `:app:nextVersion` 실행, `version.properties`의 버전명·versionCode 확인
2. 서명: `program/android/.signing/upload.properties`가 있고 릴리스 빌드가 서명되는지 확인
3. 빌드: `:app:packageReleaseFiles`로 APK·AAB 생성
4. 테스트: 단위 테스트, 에뮬레이터 테스트 결과 확인(program 보고 기준)
5. 광고: 릴리스 빌드에서 실제 광고 단위가 쓰이는지, 재생·준비 화면에 광고가 없는지 확인
6. 정책: 데이터 보안 양식이 실제 수집·저장 동작과 일치하는지 확인(재생 기록은 기기 내 암호화 저장, 광고 SDK의 수집 항목 포함). 권한 변경이 있으면 양식도 갱신
7. 등록정보: 설명·스크린샷이 현재 화면과 맞는지 확인
8. 출시 노트: `program/android/PATCH_NOTES.md`의 해당 버전 내용을 사용자용 문장으로 옮김
9. 트랙: 내부 → 비공개 → 프로덕션 순서와 단계적 출시 비율 결정(PM 승인)

## 심사·출시 후 대응

- 심사 거절이나 정책 경고를 받으면 받은 문구, 해당 정책, 관련 코드·화면 위치, 대응안을 `launch/reviews/<날짜>.md`에 정리해 PM에 넘긴다.
- 출시 후 Play Console의 비정상 종료율·ANR 비율·평점·리뷰를 확인하고, 문제가 있으면 재현 정보와 함께 PM에 넘긴다.
- 리뷰 답변은 문안만 작성하고, 게시는 사용자 확인 후에 한다.

## 도구

설치되어 있으면 쓴다.

- Android 공식 스킬 `play-policy-insights`: 출시 전 권한·데이터 보안·개인정보 신고 점검
- `design` 플러그인 `ux-copy`: 스토어 설명·출시 노트 문안 검토
- `product-management` 플러그인 `stakeholder-update`: 출시 현황 보고
