package com.example.torrentstreamplayer.engine
import com.example.torrentstreamplayer.meta.TorrentFailure
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
private val KEY = Regex("[0-9a-f]{64}")
private val KEY_FOLDER = Regex("[0-9a-fA-F]{64}")
internal class TorrentRemoval(private val downloads: File, private val state: File) {
    fun delete(key: String, legacyPaths: List<String>, protectedFiles: List<File> = emptyList()) {
        ActiveTorrentFiles.deleting { removeFiles(key, legacyPaths, protectedFiles) }
    }
    private fun removeFiles(key: String, legacyPaths: List<String>, protectedFiles: List<File>) {
        require(key.matches(KEY))
        fun keyed(root: File): File {
            val target = File(root, key)
            if (target.canonicalFile.parentFile != root.canonicalFile) throw TorrentFailure.ITEM_PATH.invalid()
            return target
        }
        val data = keyed(downloads)
        val cache = keyed(state)
        val legacy = legacyPaths.distinct().map { path ->
            require(path.isNotBlank() && !File(path).isAbsolute)
            require(!path.split('/', '\\').first().matches(KEY_FOLDER))
            val file = File(downloads, path)
            if (file.canonicalFile == downloads.canonicalFile ||
                !file.canonicalFile.toPath().startsWith(downloads.canonicalFile.toPath())) throw TorrentFailure.OUTSIDE_STORAGE.invalid()
            if (file.isDirectory) throw TorrentFailure.FOLDER_DELETE.invalid()
            file
        }
        fun tree(folder: File): List<File> =
            if (!Files.exists(folder.toPath(), LinkOption.NOFOLLOW_LINKS)) emptyList()
            else Files.walk(folder.toPath()).use { stream ->
                stream.sorted(Comparator.reverseOrder()).map { it.toFile() }.toArray().map { it as File }
            }
        val dataFiles = tree(data)
        val cacheFiles = tree(cache)
        val all = (dataFiles + legacy + cacheFiles).distinctBy { it.absolutePath }
        all.forEach { file ->
            ActiveTorrentFiles.requireUnused(file)
            if (Files.isSymbolicLink(file.toPath())) throw TorrentFailure.SYMLINK_DELETE.invalid()
            val target = file.canonicalFile.toPath()
            require(listOf(downloads, state).any { target != it.canonicalFile.toPath() && target.startsWith(it.canonicalFile.toPath()) })
            if (protectedFiles.any { it.canonicalFile.toPath().startsWith(target) }) throw TorrentFailure.ITEM_IN_USE.blocked()
        }
        all.forEach { Files.deleteIfExists(it.toPath()) }
    }
}
