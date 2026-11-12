package com.example.torrentstreamplayer.engine

/**
 * 엔진 예외를 로그용 한 줄로 바꾼다. 사용자 문구에는 예외 원문을 넣지 않고(`TorrentFailure.ENGINE`),
 * 원문은 디버그 빌드 로그에만 남긴다. 디버그 로그에서도 경로·URI·마그넷처럼 보이는 토큰은 가린다.
 */
internal object EngineFailureLog {
    private const val MAX_FRAMES = 5
    private const val REDACTED = "<redacted>"
    private val SENSITIVE_TOKEN = Regex("""\S*[/\\]\S*|magnet:\S*""", RegexOption.IGNORE_CASE)

    /** 디버그 빌드면 예외 종류·가린 원문·호출 위치를, 릴리스면 예외 종류만 돌려준다. */
    fun describe(error: Throwable, debug: Boolean): String {
        val type = error.javaClass.simpleName
        if (!debug) return type
        val message = error.message?.let { SENSITIVE_TOKEN.replace(it, REDACTED) }
        val frames = error.stackTrace.take(MAX_FRAMES)
            .joinToString(" < ") { "${it.className}.${it.methodName}:${it.lineNumber}" }
        return listOfNotNull(type, message).joinToString(": ") + if (frames.isEmpty()) "" else " at $frames"
    }
}
