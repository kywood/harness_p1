# Play 업로드용 서비스 계정 준비 (fastlane supply)

사용자가 직접 따라 하는 안내다. launch·program 세션은 키를 만들거나 Play Console을 조작하지 않는다. 화면 이름은 2026-11 기준 공식 문서의 표기다.

## 1. Google Cloud 프로젝트와 API

1. 프로젝트 만들기: <https://console.cloud.google.com/projectcreate> (이미 있으면 그 프로젝트를 써도 된다).
2. 같은 프로젝트를 선택한 상태로 <https://console.developers.google.com/apis/api/androidpublisher.googleapis.com/> 를 열고 **Enable**을 누른다. 콘솔에는 이름이 **Google Play Developer API**로 나온다("Google Play Android Developer API"라는 이름은 지금 문서에 없다).
3. 개발자 계정을 Cloud 프로젝트에 "연결"하는 단계는 더 이상 필요 없다. Play Console의 API 액세스 페이지에서 연결할 필요가 없다.

근거: [Google Play Developer API 시작하기](https://developers.google.com/android-publisher/getting_started)

## 2. 서비스 계정과 JSON 키

1. <https://console.cloud.google.com/iam-admin/serviceaccounts> 에서 **Create service account**를 누른다.
2. 이름(예: `pieceplay-fastlane`)을 넣고 **Done**을 누른다. Cloud 역할은 주지 않는다(Play 권한은 3단계에서 Play Console이 준다).
3. 만든 계정의 이메일(`…@<프로젝트>.iam.gserviceaccount.com`)을 복사해 둔다.
4. 그 이메일을 눌러 **Keys** 탭 → **Add key** → **Create new key** → Key type **JSON** → **Create**. 파일이 바로 내려받아지고 다시 받을 수 없다.

주의:
- 2024-05-03 이후 만든 조직에서는 `iam.disableServiceAccountKeyCreation` 조직 정책이 기본으로 켜져 있어 키 생성이 막힐 수 있다. 개인 계정(조직 없음) 프로젝트는 해당하지 않는다. 막히면 조직 정책 관리자가 이 프로젝트만 예외로 둬야 한다.
- 키는 기본적으로 만료되지 않는다. 유출되면 같은 **Keys** 탭에서 바로 삭제하고 새로 만든다.
- 만든 직후 60초 정도는 인증이 실패할 수 있다.

근거: [서비스 계정 만들기](https://cloud.google.com/iam/docs/service-accounts-create), [서비스 계정 키 만들기·삭제](https://cloud.google.com/iam/docs/keys-create-delete)

## 3. Play Console 권한 (이 앱에만, 최소 권한)

1. Play Console 왼쪽 메뉴 **Users and permissions**(사용자 및 권한) → **Invite new users**(새 사용자 초대).
2. 이메일 주소에 2-3에서 복사한 서비스 계정 이메일을 넣는다.
3. **Account permissions**(계정 권한)는 하나도 켜지 않는다. **Make this user an account admin**도 끈다.
4. **App permissions**(앱 권한)에서 **Add app** → `PiecePlay (com.gamenuri.pieceplay)`만 고르고 아래만 켠다.

| 켤 권한 | 이유 |
|---|---|
| Release apps to testing tracks | 내부·비공개 테스트 트랙 업로드 |
| Release to production, exclude devices, and use Play App Signing | 프로덕션 출시. 테스트 트랙만 쓸 동안은 꺼 두고 프로덕션을 fastlane으로 올릴 때 켠다 |
| Manage store presence | 스토어 등록정보 문구·그래픽·스크린샷 업로드 |
| View app information (read-only) | 앱 정보 조회(fastlane이 트랙·버전을 읽음) |

5. **Invite user**를 누른다. 사용자 목록에 계정이 보이는지 확인한다.

켜지 않는 권한: **Admin (all permissions)**, **View financial data**, **View financial data, orders, and cancellation data survey responses**, **Manage orders and subscriptions**, **Manage policy declarations**, **Reply to reviews**, **Manage testing tracks and edit tester lists**(테스터 명단은 사용자가 Console에서 직접 관리).

근거: [개발자 계정 사용자 추가 및 권한 관리](https://support.google.com/googleplay/android-developer/answer/9844686), [Google Play Developer API 시작하기](https://developers.google.com/android-publisher/getting_started)

## 4. 키 파일 위치

- 내려받은 JSON을 `launch/private/play-service-account.json`으로 옮긴다. 다른 곳(다운로드 폴더 등)에 남은 사본은 지운다.
- 이 파일은 커밋하지 않는다. 채팅·이슈·PR·문서에 내용을 붙여 넣지 않는다.
- **주의(2026-11-12 확인):** 저장소에 `.gitignore`가 없고 `launch/private/`는 이 PC의 `.git/info/exclude`에서만 무시된다. 다른 PC나 새 클론에서는 무시되지 않으므로 PM이 저장소 `.gitignore`에 `launch/private/`를 넣어야 한다. 넣기 전에는 `git status`에 이 파일이 보이지 않는지 꼭 확인한다.

```powershell
git check-ignore -v launch/private/play-service-account.json   # 경로가 출력되면 무시되는 상태
```

## 5. 확인

`program/android/`에서 실행한다(fastlane 설치 후).

```powershell
fastlane run validate_play_store_json_key json_key:../../launch/private/play-service-account.json
```

성공하면 Play Store 연결 성공 메시지가 나온다. 실패하면:
- 401/403: 3단계 권한이 이 앱에 들어갔는지, API를 같은 프로젝트에서 켰는지 확인한다.
- 키 생성 직후라면 1~2분 뒤 다시 실행한다.

근거: [fastlane Android 설정 — Collect your Google credentials](https://docs.fastlane.tools/getting-started/android/setup/)
