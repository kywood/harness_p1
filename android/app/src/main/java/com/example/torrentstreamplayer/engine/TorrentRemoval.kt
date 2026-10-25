package com.example.torrentstreamplayer.engine
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
internal class TorrentRemoval(private val downloads: File, private val state: File) {
    fun delete(key: String, legacyPaths: List<String>, protectedFiles: List<File> = emptyList()) {
        require(key.matches(Regex("[0-9a-f]{64}")))
        fun keyed(root: File): File {
            val target = File(root, key)
            require(target.canonicalFile.parentFile == root.canonicalFile) { "항목 저장 경로가 올바르지 않습니다." }
            return target
        }
        val data = keyed(downloads)
        val cache = keyed(state)
        val legacy = legacyPaths.distinct().map { path ->
            require(path.isNotBlank() && !File(path).isAbsolute)
            require(!path.split('/', '\\').first().matches(Regex("[0-9a-fA-F]{64}")))
            val file = File(downloads, path)
            require(file.canonicalFile != downloads.canonicalFile &&
                file.canonicalFile.toPath().startsWith(downloads.canonicalFile.toPath())) { "파일이 앱 저장 공간 밖을 가리킵니다." }
            require(!file.isDirectory) { "개별 파일 대신 폴더를 삭제할 수 없습니다." }
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
            require(!Files.isSymbolicLink(file.toPath())) { "심볼릭 링크는 삭제하지 않습니다." }
            val target = file.canonicalFile.toPath()
            require(listOf(downloads, state).any { target != it.canonicalFile.toPath() && target.startsWith(it.canonicalFile.toPath()) })
            check(protectedFiles.none { it.canonicalFile.toPath().startsWith(target) }) { "다른 항목에서 사용 중인 파일입니다." }
        }
        all.forEach { Files.deleteIfExists(it.toPath()) }
    }
}
