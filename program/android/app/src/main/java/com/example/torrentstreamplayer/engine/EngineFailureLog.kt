package com.example.torrentstreamplayer.engine

import com.example.torrentstreamplayer.BuildConfig

/**
 * 엔진 예외를 로그용 한 줄로 바꾼다. 예외 메시지에는 영상 제목·경로·해시가 들어갈 수 있어서
 * 디버그 빌드에서도 원문을 넣지 않는다. 예외 종류(cause 체인 포함)와, 디버그면 호출 위치만 남긴다.
 */
internal object EngineFailureLog {
    private const val MAX_FRAMES = 5
    private const val MAX_CAUSES = 8

    /** 릴리스면 예외 종류 체인만, 디버그면 거기에 호출 위치를 덧붙여 돌려준다. */
    fun describe(error: Throwable, debug: Boolean): String {
        val types = generateSequence(error) { it.cause?.takeIf { cause -> cause !== it } }
            .take(MAX_CAUSES)
            .joinToString(" <- caused by ") { it.javaClass.simpleName.ifEmpty { it.javaClass.name } }
        if (!debug) return types
        val frames = error.stackTrace.take(MAX_FRAMES)
            .joinToString(" < ") { "${it.className}.${it.methodName}:${it.lineNumber}" }
        return if (frames.isEmpty()) types else "$types at $frames"
    }

    /** 현재 빌드 종류(`BuildConfig.DEBUG`)에 맞춰 [describe]한다. 엔진 `Log` 호출은 Throwable 대신 이 값을 넘긴다. */
    fun describe(error: Throwable): String = describe(error, BuildConfig.DEBUG)
}
