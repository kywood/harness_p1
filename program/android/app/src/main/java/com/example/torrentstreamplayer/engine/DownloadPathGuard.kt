package com.example.torrentstreamplayer.engine

import java.io.File

/** Checks that a path from torrent metadata or saved state stays inside an app folder after resolving `..`. */
internal object DownloadPathGuard {
    /** [relativePath] under [root] as a canonical file, or null when it would escape [root]. */
    fun resolve(root: File, relativePath: String): File? =
        File(root, relativePath).canonicalFile.takeIf { isInside(root, it) }

    /** True when [file]'s canonical path is [root] itself or below it. */
    fun isInside(root: File, file: File): Boolean =
        file.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())
}
