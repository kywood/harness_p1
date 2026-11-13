package com.example.torrentstreamplayer.data

import android.content.Context
import com.example.torrentstreamplayer.engine.MagnetHistory
import com.example.torrentstreamplayer.engine.MagnetIdentity
import com.example.torrentstreamplayer.engine.SavedMagnet

/**
 * 재생 목록 저장소. 암호화된 [MagnetHistory]를 읽고 쓰고, 목록 항목에 저장된 다운로드 정보를 붙인다.
 * 디스크를 건드리므로 메인 스레드가 아닌 곳에서 부른다.
 *
 * @param lookup 목록 항목에 다운로드 크기·받은 양·조각 맵을 채운다.
 */
internal class PlaylistRepository(
    private val history: MagnetHistory,
    private val lookup: (SavedMagnet) -> SavedMagnet,
) {
    fun savedMagnets(): List<SavedMagnet> = history.list().map(lookup)

    /** 재생 목록에 있는 링크의 마그넷 키. 다운로드 정보는 읽지 않는다. 깨진 항목은 [MagnetHistory.list]가 건너뛴다. */
    fun keys(): Set<String> = history.list().map { MagnetIdentity.key(it.magnet) }.toSet()

    /** 마그넷 키가 [key]인 항목에 다운로드 정보를 붙여 돌려준다. 없으면 null. */
    fun find(key: String): SavedMagnet? =
        history.list().firstOrNull { MagnetIdentity.key(it.magnet) == key }?.let(lookup)

    fun add(magnet: String) = history.add(magnet)

    /** 재생할 [magnet]을 목록에 넣고, 이어볼 위치(ms)를 돌려준다. 처음이면 0이다. */
    fun addForPlayback(magnet: String): Long {
        history.add(magnet)
        val key = MagnetIdentity.key(magnet)
        return history.list().firstOrNull { MagnetIdentity.key(it.magnet) == key }?.lastPositionMs ?: 0
    }

    fun updateTitle(magnet: String, title: String) = history.updateTitle(magnet, title)

    fun markPlayed(magnet: String) = history.markPlayed(magnet)

    fun delete(magnet: String) = history.delete(magnet)

    fun savePosition(magnet: String, positionMs: Long, durationMs: Long) =
        history.savePosition(magnet, positionMs.coerceAtLeast(0), durationMs.coerceAtLeast(0))

    fun resetPosition(magnet: String) = history.savePosition(magnet, 0, 0)

    companion object {
        /** 재생 목록 파일 위치(noBackupFilesDir 아래). 바꾸면 기존 목록을 읽지 못한다. */
        private const val HISTORY_FILE = "magnet_history"

        fun create(context: Context, lookup: (SavedMagnet) -> SavedMagnet): PlaylistRepository =
            PlaylistRepository(MagnetHistory(AppStateCache.create(context, HISTORY_FILE)), lookup)
    }
}
