# program 코드 컨벤션

`program/CLAUDE.md`와 함께 적용한다. 새로 쓰는 코드와 이번 변경에서 손대는 코드에 적용하고, 기존 코드를 이 규칙에 맞추려고 따로 일괄 수정하지 않는다(`engine/`은 한 파일·10줄 규칙이 우선한다).

## 형식

- Kotlin 공식 스타일(`kotlin.code.style=official`): 들여쓰기 4칸, 한 줄 최대 120자.
- import를 쓴다. `com.example.torrentstreamplayer.ui.Foo`처럼 코드 안에 전체 경로를 인라인으로 쓰지 않는다. 기존 인라인 경로는 그 줄을 고칠 때 import로 바꾼다.
- 한 줄에 문장 하나. `;`로 여러 문장을 잇지 않는다.
- 와일드카드 import(`foo.*`)는 기존 파일에 이미 있을 때만 유지한다.

## 이름

| 대상 | 규칙 | 예 |
|---|---|---|
| 판단 로직 객체 | `…Policy` | `CompletionAdPolicy` |
| 순서·범위 계산 | `…Plan` | `StartupBufferPlan` |
| 화면 상태 값 | `…State`, `…UiState` | `SeekUiState` |
| 표시 문자열 계산 | `…Label`, `…Text` | `DownloadProgressLabel` |
| 화면 컴포저블 | `…Screen` | `StoredFilesScreen` |
| 작은 컴포저블 | 명사 | `PieceMap`, `SwipeSeekBar` |
| 테스트 | `<대상>Test` | `MagnetIdentityTest` |
| 상수 | `UPPER_SNAKE_CASE` (`const val` 또는 `companion object`) | `MAX_BYTES` |

시간 값에는 단위를 붙인다: `timeoutMs`, `delaySec`. 바이트는 `…Bytes`.

## OOP 구조 설계

새 기능과 리팩터링은 객체지향 구조로 설계한다. 코드를 쓰기 전에 어떤 객체가 어떤 책임을 지고 누구에게 무엇을 요청하는지 먼저 정한다.

### 계층

의존은 위에서 아래로만 흐른다. 아래 계층은 위 계층을 모른다.

| 계층 | 책임 | 현재 위치 |
|---|---|---|
| UI | 상태를 그리고 사용자 입력을 ViewModel에 전달 | `ui/*Screen.kt`, 컴포저블 |
| ViewModel | 화면 상태(`StreamUiState`) 소유, 사용 흐름 조율 | `MainViewModel` |
| 도메인 | 다운로드·재생·기록 같은 업무 규칙과 판단 | `DownloadManager`, `*Policy`, `*Plan` |
| 엔진·데이터 | libtorrent, 파일, 암호화 저장, 네트워크 | `engine/` |

- 컴포저블은 엔진을 직접 부르지 않는다. 새 코드에서 `viewModel.torrentEngine`을 화면에 넘기는 방식을 늘리지 않는다.
- 엔진은 UI 타입(`StreamUiState`, Compose)을 import하지 않는다.

### 책임과 캡슐화

- 클래스 하나는 바뀌는 이유가 하나다. 다운로드 제어, 우선순위 계산, 저장, 통계가 한 클래스에 섞이면 나눈다.
- 상태는 그 상태를 바꾸는 객체 안에 `private`으로 둔다. 밖에는 읽기 전용 값(`StateFlow`, `val`)이나 의도가 드러나는 메서드(`pause()`, `seekTo()`)만 연다. 다른 객체의 내부 필드를 꺼내 조합하지 않는다(묻지 말고 시킨다).
- `var` 공개 프로퍼티와 `private set` 남발을 피한다. 여러 값이 함께 바뀌면 하나의 불변 `data class`로 묶어 한 번에 교체한다.

### 경계와 의존성

- 외부 시스템(libtorrent, LibVLC, AdMob, 파일 시스템, 시계, 네트워크)은 작은 `interface` 뒤에 두고, 쓰는 쪽은 인터페이스에 의존한다. 실제 구현은 생성자로 주입한다. 테스트에서는 가짜 구현을 넣는다.
- 의존 객체는 생성자에서 받는다. 클래스 안에서 다른 핵심 객체를 직접 `new`하지 않는다(팩토리 함수를 주입받는 것은 허용, 현재 `DownloadManager`의 `createController` 방식).
- 상속보다 조합을 쓴다. `open class`·추상 클래스는 여러 구현이 실제로 있을 때만 만든다.

### 상태 모델링

- 상태와 결과는 `sealed interface`/`sealed class`로 표현하고 `when`으로 빠짐없이 처리한다(`StreamUiState` 방식). 불리언 플래그 여러 개로 상태를 표현하지 않는다(`showFiles`, `showHistory` 같은 방식은 늘리지 않는다).
- 실패 가능성이 있는 연산의 결과는 예외 대신 `Result` 또는 sealed 결과 타입으로 돌려준다. 예외는 프로그래밍 오류와 호출 측이 처리할 수 없는 상황에만 쓴다.
- 의미 있는 원시값은 값 객체로 감싼다(예: 마그넷 키 64자리 문자열 → `@JvmInline value class MagnetKey`). 새 코드부터 적용한다.

### 하드코딩 금지: enum과 메타 테이블

값과 종류를 코드 곳곳에 직접 적지 않는다. 종류는 `enum`으로 정의하고, 종류마다 다른 값·동작은 그 종류의 메타 테이블에 등록한 뒤 조회해서 쓴다. 새 종류를 추가할 때 **테이블에 항목 하나만 추가하면 되고, 사용하는 코드는 고치지 않게** 만드는 것이 목표다.

1. **종류는 enum**: 문자열·정수 상수로 종류를 구분하지 않는다(`"magnet"`, `phase == 2` 금지). `DownloadPhase`, `PieceCell`처럼 enum으로 만든다.
2. **종류별 속성은 enum 항목에 등록**: 표시 이름, 정렬 기준, 아이콘, 제한값처럼 종류마다 달라지는 것은 enum 생성자 인자나 프로퍼티로 그 항목에 함께 둔다. 사용하는 쪽에서 `if (x == A) … else …`나 `when`으로 값을 고르지 않는다.
3. **설정값은 메타 테이블에**: 시간·크기·횟수·URL·광고 단위 ID 같은 값은 기능별 메타 객체(`…Spec`, `…Config`) 한 곳에 이름을 붙여 등록한다. 같은 값을 두 곳 이상에 적지 않는다. 메타 객체는 `meta/` 패키지(`com.example.torrentstreamplayer.meta`)에 둔다.
4. **확장 가능한 목록은 레지스트리로**: 지원 확장자, 컨테이너 형식, 광고 노출 화면처럼 늘어날 수 있는 목록은 `enum` 또는 `Map<Key, Spec>` 테이블로 등록하고, 판단은 그 테이블을 조회한다(`VideoFormat.entries.any { it.matches(path) }`).
5. **`when`은 빠짐없이**: enum에 대한 `when`은 `else` 없이 모든 항목을 나열해, 항목을 추가하면 컴파일 오류로 처리할 곳이 드러나게 한다. 다만 2번처럼 항목에 등록할 수 있는 값이면 `when`보다 등록을 먼저 쓴다.
6. **예외**: 0, 1, -1 같은 산술 상수와 테스트 데이터는 허용한다. 사용자에게 보이는 문구·색·간격은 enum이 아니라 `strings.xml`과 테마 토큰에 두고(design 소유), enum 항목에는 그 리소스 ID(`@StringRes`)를 등록한다.

예시: 현재 `PlaylistSort`는 표시 이름은 항목에 있지만 정렬 기준은 사용하는 쪽에서 `if (sort == PlaylistSort.RECENT_PLAY)`로 고른다. 메타 테이블 방식에서는 정렬 기준도 항목에 등록한다.

```kotlin
enum class PlaylistSort(@StringRes val label: Int, val key: (SavedMagnet) -> Long) {
    RECENT_PLAY(R.string.sort_recent_play, SavedMagnet::lastPlayedAt),
    RECEIVED(R.string.sort_received, SavedMagnet::addedAt),
}
// 사용하는 쪽: entries.sortedByDescending(sort.key) — 새 정렬을 추가해도 이 줄은 바뀌지 않는다.
```

현재 하드코딩된 값이 모여 있는 곳(새로 고칠 때 메타 테이블로 옮긴다):

- 광고: `CompletionAdPolicy`(5분·10분·5회·단위 ID), `TestAdPolicy`(배너 단위 ID, 노출 화면 조건)
- 엔진 시간·크기: `TorrentEngine`의 조각 deadline(1000·5000ms), 진행 저장 주기(5초·15초), 메타데이터 대기 시간
- 재생 판단: `TorrentPlayback`의 영상 확장자 목록
- 입력: `TorrentInputResolver`의 최대 크기·타임아웃·리디렉션 횟수

### 순수 로직

- 판단·계산 로직은 상태 없는 `internal object` 또는 작은 불변 클래스로 두고 Android·Compose에 의존하지 않는다. 이 객체들은 OOP 구조의 도메인 계층에 속하며, JVM 단위 테스트를 붙인다.

### 큰 클래스 분리

`TorrentEngine`(약 600줄), `VlcPlayerScreen`(약 600줄), `MainActivity`는 책임이 섞여 있다. 한 번에 다시 쓰지 않고 다음 순서로 나눈다.

1. 떼어 낼 책임 하나를 정하고 인터페이스를 먼저 만든다(이슈 하나).
2. 새 클래스에 구현을 옮기고 기존 클래스는 그 클래스에 위임한다.
3. 단계마다 기존 테스트가 통과해야 다음 단계로 간다.

`engine/`의 한 파일·10줄 규칙은 기존 파일 수정에 적용한다. 분리로 생기는 새 파일은 새 파일 하나 단위로 추가하고, 기존 파일에서 위임으로 바꾸는 변경을 10줄 이내로 나눈다.

## 구조

- 판단 로직은 Android·Compose에 의존하지 않는 `internal object` 또는 작은 클래스로 분리하고 JVM 단위 테스트를 붙인다.
- 공개 범위는 가장 좁게: 모듈 밖에서 쓰지 않으면 `internal`, 파일 밖에서 쓰지 않으면 `private`.
- 매직 넘버(타임아웃, 크기, 간격)는 "하드코딩 금지: enum과 메타 테이블" 규칙에 따라 메타 테이블에 등록한다.
- 새 파일 하나에는 한 가지 책임만 둔다. 300줄을 넘는 파일(`TorrentEngine.kt`, `VlcPlayerScreen.kt`)에는 새 기능을 추가하지 말고 별도 파일로 뺀다.

## 리소스 풀링

비싼 리소스는 매번 새로 만들지 말고 공유·재사용한다. 새로 만들어야 한다면 그 이유와 해제 지점을 코드에 남긴다.

| 리소스 | 규칙 |
|---|---|
| libtorrent 세션 | `NativeSessionLease`로 하나를 공유한다(현재 방식 유지). 직접 `SessionManager()`를 만들지 않는다. |
| 스레드 | 새 `Thread { }`나 `Executors.newSingleThreadExecutor()`를 만들지 않는다. 코루틴 디스패처(`Dispatchers.IO`) 또는 앱 단위로 공유하는 실행기를 쓴다. |
| 코루틴 범위 | `viewModelScope` 또는 생명주기가 분명한 범위만 쓴다. `GlobalScope`, 새 `runBlocking` 금지. |
| 정규식 | `Regex(...)`는 함수 안에서 만들지 말고 `private val` 상수로 미리 만든다. |
| 버퍼 | 반복 호출되는 경로의 `ByteArray`는 호출마다 만들지 말고 인스턴스 필드로 재사용한다. 스레드 간 공유는 하지 않는다. |
| 파일 핸들 | `RandomAccessFile`·스트림은 `use {}`로 닫거나, 스트림 객체가 열고 `close()`에서 닫는다. |
| 플레이어(LibVLC) | 무거운 네이티브 객체다. 화면마다 새로 만드는 범위를 넓히지 않는다. |
| Compose | 비용이 큰 객체는 `remember`로 한 번만 만들고, 해제가 필요하면 `DisposableEffect`의 `onDispose`에서 해제한다. |

현재 코드에서 이 규칙과 다른 곳(새로 고칠 때 이슈로 다룬다):

- `TorrentEngine`: 엔진마다 raw `Thread` 생성
- `TorrentCheckpoint`: 체크포인트마다 `newSingleThreadExecutor()` 생성
- `MagnetIdentity`, `TorrentCache`, `TorrentRemoval`, `TorrentPlayback`: 호출마다 `Regex` 생성. 재생 목록 갱신 시 반복 호출된다.
- `VlcPlayerScreen`: 재생할 때마다 `LibVLC` 인스턴스 생성
- `MainViewModel.deleteSavedMagnet`: `runBlocking` 사용

## 오류 처리

- 코루틴 안에서 `catch (e: Exception)`을 쓸 때는 앞에 `catch (e: CancellationException) { throw e }`를 둔다.
- 오류를 삼킬 때(`runCatching { }`)는 실패해도 되는 이유가 분명한 곳만, 필요하면 `Log.w`로 남긴다. 로그에 마그넷·URL 원문을 넣지 않는다.
- 사용자에게 보이는 오류 문구는 한국어로 쓰고, 문구 자체는 design 소유다(새 문구는 design에 요청하거나 `strings.xml`에 둔다).

## 테스트

- 테스트 이름은 동작을 설명한다: `` `빈 링크는 오류를 낸다`() `` 또는 `emptyLinkFails()`. 파일 안에서는 한 방식으로 통일한다.
- 단위 테스트는 시간·파일·네트워크를 함수 인자로 주입해 실제 시계와 디스크 없이 돌게 한다(`MagnetHistory.add(now = …)`, `TorrentInputResolver(fetch = …)` 방식).
- 네이티브·화면 흐름은 `androidTest`에서 검증한다.
