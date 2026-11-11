package com.example.torrentstreamplayer.engine

import android.util.Log
import fi.iki.elonen.NanoHTTPD

class TorrentStreamServer(
    port: Int = 8080,
    private val torrentEngine: TorrentEngine
) : NanoHTTPD("127.0.0.1", port) {

    companion object {
        private const val TAG = "TorrentStreamServer"
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        Log.d(TAG, "Request URI: $uri, Method: ${session.method}")

        if (uri != "/stream") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
        }

        val videoFile = torrentEngine.selectedVideoFile
            ?: return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "Video file metadata not initialized yet."
            )

        val totalLength = if (torrentEngine.selectedFileLength > 0) {
            torrentEngine.selectedFileLength
        } else if (videoFile.exists()) {
            videoFile.length()
        } else {
            0L
        }

        if (totalLength == 0L) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "Video file size unknown."
            )
        }

        val rangeHeader = session.headers["range"]
        val range = TorrentPlayback.httpRange(rangeHeader, totalLength)
        if (range == null) {
            return newFixedLengthResponse(Response.Status.RANGE_NOT_SATISFIABLE,
                MIME_PLAINTEXT, "Invalid byte range").apply {
                addHeader("Content-Range", "bytes */$totalLength")
            }
        }
        val startByte = range.first
        val endByte = range.last
        val isRangeRequest = rangeHeader != null

        // Inform torrent engine to prioritize requested byte offset in LibTorrent
        val generation = torrentEngine.seekRequests.acceptRequest()
        val readId = torrentEngine.openRead(startByte, generation)
        torrentEngine.prioritizeRequest(startByte, generation, exactRange = true, readId = readId)

        val contentLength = endByte - startByte + 1

        val torrentInputStream = VerifiedTorrentInputStream(
            videoFile, startByte, endByte + 1,
            torrentEngine::isByteDownloaded, { torrentEngine.prioritizeRequest(it, generation, readId = readId) },
            { isAlive && torrentEngine.isStreamingFile(videoFile) &&
                torrentEngine.seekRequests.isCurrent(generation) }, torrentEngine::readablePieceBytes,
            prefetch = { torrentEngine.prioritizeRequest(it, generation, readId = readId) },
            canWait = { torrentEngine.seekRequests.isPriorityCurrent(generation) },
            onClosed = { torrentEngine.closeRead(readId) }
        )

        val mimeType = getMimeType(videoFile.name)
        val status = if (isRangeRequest) Response.Status.PARTIAL_CONTENT else Response.Status.OK

        val response = newFixedLengthResponse(
            status,
            mimeType,
            torrentInputStream,
            contentLength
        )

        if (isRangeRequest) {
            response.addHeader("Content-Range", "bytes $startByte-$endByte/$totalLength")
        }
        response.addHeader("Accept-Ranges", "bytes")

        return response
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "webm" -> "video/webm"
            "mov" -> "video/quicktime"
            else -> "video/mp4"
        }
    }
}
