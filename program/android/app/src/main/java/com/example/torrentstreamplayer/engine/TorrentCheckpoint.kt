package com.example.torrentstreamplayer.engine

import org.libtorrent4j.*
import org.libtorrent4j.alerts.*
import org.libtorrent4j.swig.*
import java.io.File
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference

internal class TorrentCheckpoint(
    private val session: SessionManager, private val handle: TorrentHandle,
    private val cache: TorrentCache
) : AlertListener {
    private val pending = AtomicReference<CountDownLatch?>(null)
    private val disk = Executors.newSingleThreadExecutor()
    @Volatile var latestProgress: DownloadSnapshot? = null
    fun saveProgress(snapshot: DownloadSnapshot?) {
        if (snapshot == null) return
        try { disk.execute { runCatching { cache.write("progress", snapshot.encode()) } } }
        catch (_: RejectedExecutionException) { /* Session has already closed. */ }
    }
    init { session.addListener(this) }
    override fun types(): IntArray = intArrayOf(
        AlertType.SAVE_RESUME_DATA.swig(), AlertType.SAVE_RESUME_DATA_FAILED.swig())
    override fun alert(alert: Alert<*>) {
        if (alert !is TorrentAlert<*> || alert.handle().infoHash() != handle.infoHash()) return
        val signal = pending.get() ?: return
        if (alert is SaveResumeDataAlert) {
            val bytes = try { AddTorrentParams.writeResumeDataBuf(alert.params()) }
            catch (_: Exception) { finish(signal); return }
            try {
                disk.execute {
                    try { cache.write("resume", bytes) }
                    catch (_: Exception) { /* Existing checkpoint remains valid. */ }
                    finally { finish(signal) }
                }
            } catch (_: RejectedExecutionException) { finish(signal) }
        } else { finish(signal) }
    }
    private fun finish(signal: CountDownLatch) {
        pending.compareAndSet(signal, null)
        signal.countDown()
    }
    fun save(waitMillis: Long = 0) {
        val signal = CountDownLatch(1)
        if (pending.compareAndSet(null, signal)) {
            try { handle.saveResumeData(TorrentHandle.FLUSH_DISK_CACHE.or_(TorrentHandle.SAVE_INFO_DICT)) }
            catch (_: Exception) { finish(signal) }
        }
        if (waitMillis > 0) pending.get()?.await(waitMillis, TimeUnit.MILLISECONDS)
    }
    internal val isClosed: Boolean get() = disk.isTerminated
    internal fun awaitClosed() { check(disk.awaitTermination(5, TimeUnit.SECONDS)) { "다운로드 기록 저장이 끝나지 않았습니다. 잠시 후 다시 삭제해 주세요." } }
    fun close() {
        saveProgress(latestProgress)
        session.removeListener(this)
        disk.shutdown()
        try { disk.awaitTermination(1500, TimeUnit.MILLISECONDS) }
        catch (_: InterruptedException) { Thread.currentThread().interrupt() }
    }
    companion object {
        fun restore(session: SessionManager, info: TorrentInfo, directory: File,
            cache: TorrentCache, videoIndex: Int): TorrentHandle {
            val restored = cache.read("resume")?.let { bytes ->
                try {
                    val error = error_code()
                    val data = libtorrent.read_resume_data_ex(Vectors.bytes2byte_vector(bytes), error)
                    if (error.value() == 0) data else null
                } catch (_: Exception) { null }
            }
            val params = restored ?: add_torrent_params()
            params.set_ti(info.swig())
            params.save_path = directory.absolutePath
            params.flags = params.flags.and_(TorrentFlags.PAUSED.inv())
                .and_(TorrentFlags.AUTO_MANAGED.inv()).or_(TorrentFlags.SEQUENTIAL_DOWNLOAD)
                .and_(TorrentFlags.UPLOAD_MODE.inv()).and_(TorrentFlags.STOP_WHEN_READY.inv())
            params.set_file_priorities(Vectors.bytes2byte_vector(ByteArray(info.numFiles()) {
                if (it == videoIndex) Priority.DEFAULT.swig() else Priority.IGNORE.swig()
            }))
            val error = ErrorCode(error_code())
            val result = SessionHandle(session.swig()).addTorrent(AddTorrentParams(params), error)
            check(error.value == 0 && result.isValid) { "Unable to restore torrent: " + error.message }
            result.unsetFlags(TorrentFlags.UPLOAD_MODE.or_(TorrentFlags.STOP_WHEN_READY)
                .or_(TorrentFlags.PAUSED).or_(TorrentFlags.AUTO_MANAGED))
            result.resume()
            return result
        }
    }
}
