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

## 구조

- 판단 로직은 Android·Compose에 의존하지 않는 `internal object` 또는 작은 클래스로 분리하고 JVM 단위 테스트를 붙인다.
- 공개 범위는 가장 좁게: 모듈 밖에서 쓰지 않으면 `internal`, 파일 밖에서 쓰지 않으면 `private`.
- 매직 넘버(타임아웃, 크기, 간격)는 이름 있는 상수로 만든다. 같은 값을 두 곳에서 쓰면 한 곳에 둔다.
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
