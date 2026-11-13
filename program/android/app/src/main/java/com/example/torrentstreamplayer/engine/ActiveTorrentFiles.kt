package com.example.torrentstreamplayer.engine
import com.example.torrentstreamplayer.meta.TorrentFailure
import java.io.File
internal object ActiveTorrentFiles {
    private val owners = mutableMapOf<String, Any>()
    @Synchronized fun <T> deleting(action: () -> T): T = action()
    @Synchronized fun requireUnused(file: File) {
        val target = file.canonicalFile.toPath()
        if (owners.keys.any { File(it).toPath().startsWith(target) }) throw TorrentFailure.FILE_IN_USE.blocked()
    }
    @Synchronized fun acquire(owner: Any, file: File) {
        val path = file.canonicalPath
        if (owners[path] != null && owners[path] !== owner) throw TorrentFailure.FILE_IN_USE.blocked()
        owners[path] = owner
    }
    @Synchronized fun release(owner: Any) { owners.entries.removeAll { it.value === owner } }
}
