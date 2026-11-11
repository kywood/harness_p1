package com.example.torrentstreamplayer.engine

import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

data class StoredTorrentFile(val path: String, val name: String, val size: Long)

internal class TorrentDiskStore(private val roots: List<File>) {
    fun list(): List<StoredTorrentFile> = roots.flatMap { root ->
        if (!root.isDirectory) emptyList() else Files.walk(root.toPath()).use { paths ->
            paths.filter { Files.isRegularFile(it, LinkOption.NOFOLLOW_LINKS) }
                .map { it.toFile() }.filter(::allowed)
                .map { StoredTorrentFile(it.canonicalPath, root.name + "/" +
                    it.relativeTo(root).invariantSeparatorsPath, it.length()) }
                .toArray().map { it as StoredTorrentFile }
        }
    }.sortedBy { it.name }
    private fun allowed(file: File): Boolean = roots.any {
        val root = it.canonicalFile.toPath()
        val target = file.canonicalFile.toPath()
        target != root && target.startsWith(root)
    }
    fun delete(path: String, activeFile: File? = null): Boolean {
        val target = File(path)
        require(allowed(target)) { "File is outside app download storage" }
        require(activeFile == null || target.canonicalFile != activeFile.canonicalFile) {
            "Stop playback or downloading before deleting this file"
        }
        return target.isFile && target.delete()
    }
}
