package com.example.torrentstreamplayer.engine
import java.io.File
internal object ActiveTorrentFiles {
    private val owners = mutableMapOf<String, Any>()
    @Synchronized fun <T> deleting(action: () -> T): T = action()
    @Synchronized fun requireUnused(file: File) {
        val target = file.canonicalFile.toPath()
        check(owners.keys.none { File(it).toPath().startsWith(target) }) { "다른 다운로드에서 사용 중인 파일입니다." }
    }
    @Synchronized fun acquire(owner: Any, file: File) {
        val path = file.canonicalPath
        check(owners[path] == null || owners[path] === owner) { "다른 다운로드에서 사용 중인 파일입니다." }
        owners[path] = owner
    }
    @Synchronized fun release(owner: Any) { owners.entries.removeAll { it.value === owner } }
}
