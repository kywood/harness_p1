package com.example.torrentstreamplayer.engine

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import java.io.RandomAccessFile

@UnstableApi
class TorrentFileDataSource(
    private val torrentEngine: TorrentEngine
) : BaseDataSource(/* isNetwork = */ false) {

    private var raf: RandomAccessFile? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0L
    private var currentPosition: Long = 0L
    private var opened = false

    companion object {
        private const val TAG = "TorrentFileDataSource"
    }

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        transferInitializing(dataSpec)

        val videoFile = torrentEngine.selectedVideoFile
            ?: throw IllegalStateException("Video file not ready")

        val totalLength = if (torrentEngine.selectedFileLength > 0) {
            torrentEngine.selectedFileLength
        } else if (videoFile.exists()) {
            videoFile.length()
        } else {
            C.LENGTH_UNSET.toLong()
        }

        currentPosition = dataSpec.position
        torrentEngine.prioritizeOffset(currentPosition)

        // Wait up to 20s for piece at currentPosition to be hash-verified by LibTorrent
        var waitAttempts = 0
        while (!torrentEngine.isByteDownloaded(currentPosition)) {
            torrentEngine.prioritizeOffset(currentPosition)
            if (waitAttempts > 200) {
                break
            }
            try {
                Thread.sleep(100)
            } catch (ignored: Exception) {
            }
            waitAttempts++
        }

        if (!videoFile.exists()) {
            throw IllegalStateException("Video file does not exist on disk")
        }

        raf = RandomAccessFile(videoFile, "r")
        raf?.seek(currentPosition)

        bytesRemaining = if (dataSpec.length != C.LENGTH_UNSET.toLong()) {
            dataSpec.length
        } else if (totalLength != C.LENGTH_UNSET.toLong()) {
            totalLength - currentPosition
        } else {
            videoFile.length() - currentPosition
        }

        if (bytesRemaining < 0) {
            bytesRemaining = 0
        }

        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val file = raf ?: return C.RESULT_END_OF_INPUT

        // Wait if piece at currentPosition is not hash-verified on disk yet
        var waitAttempts = 0
        while (!torrentEngine.isByteDownloaded(currentPosition)) {
            torrentEngine.prioritizeOffset(currentPosition)
            if (waitAttempts > 200) { // 20s timeout
                return C.RESULT_END_OF_INPUT
            }
            try {
                Thread.sleep(100)
            } catch (ignored: Exception) {
            }
            waitAttempts++
        }

        val bytesToRead = bytesRemaining.coerceAtMost(length.toLong()).toInt()
        val bytesRead = try {
            file.seek(currentPosition)
            file.read(buffer, offset, bytesToRead)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading from RAF", e)
            return C.RESULT_END_OF_INPUT
        }

        if (bytesRead <= 0) {
            return C.RESULT_END_OF_INPUT
        }

        currentPosition += bytesRead
        if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
            bytesRemaining -= bytesRead
        }
        bytesTransferred(bytesRead)
        return bytesRead
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        try {
            raf?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing RAF", e)
        }
        raf = null
        if (opened) {
            opened = false
            transferEnded()
        }
    }
}
