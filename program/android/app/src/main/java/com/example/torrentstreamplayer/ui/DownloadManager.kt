package com.example.torrentstreamplayer.ui
import com.example.torrentstreamplayer.R
import com.example.torrentstreamplayer.engine.*
import com.example.torrentstreamplayer.meta.TorrentFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
/**
 * Download task phases. Register what each phase means here; the playlist, file deletion and the network rules
 * read these properties instead of listing phases.
 */
internal enum class DownloadPhase(
    /**
     * The task is alive (running, queued or waiting for Wi-Fi): the playlist offers "중지", playback may borrow it,
     * and its files must not be deleted under it.
     */
    val stoppable: Boolean,
    /** The Wi-Fi only rule may move the task to [WAITING_FOR_WIFI]. */
    val networkGated: Boolean,
) {
    QUEUED(stoppable = true, networkGated = true),
    RUNNING(stoppable = true, networkGated = true),
    COMPLETE(stoppable = false, networkGated = false),
    FAILED(stoppable = false, networkGated = false),

    /** "Wi-Fi 대기": stopped by the Wi-Fi only rule ([DownloadNetworkPolicy]); queued again when it allows. */
    WAITING_FOR_WIFI(stoppable = true, networkGated = false),
}
internal data class DownloadEntry(val key: String, val phase: DownloadPhase = DownloadPhase.QUEUED,
    val snapshot: DownloadSnapshot? = null, val speed: Long = 0, val error: DownloadFailure? = null)
internal object DownloadQueuePlan {
    fun ready(entries: Collection<DownloadEntry>, limit: Int): List<String> {
        require(limit > 0)
        val slots = (limit - entries.count { it.phase == DownloadPhase.RUNNING }).coerceAtLeast(0)
        return entries.filter { it.phase == DownloadPhase.QUEUED }.take(slots).map { it.key }
    }
}
/**
 * @param onTaskAdded called with the magnet when [start] adds a task that does not run yet (queued or waiting for
 * Wi-Fi). Such a task has not started its controller (which saves the link), so the playlist records it here; without
 * this a 4th download had no playlist card until a slot freed up. A task that runs at once is not reported.
 */
internal class DownloadManager(private val scope: CoroutineScope,
    private val createController: () -> DownloadController, private val limit: Int = 3,
    private val onTaskAdded: (String) -> Unit = {}) {
    init { require(limit in 1..8) }
    private class Node(val magnet: String, val controller: DownloadController, var watcher: Job? = null)
    private val nodes = linkedMapOf<String, Node>()
    private val retired = java.util.concurrent.CopyOnWriteArrayList<DownloadController>()
    private val borrowed = mutableSetOf<String>()
    /** Keys the user allowed on mobile data this time; kept until the task is stopped. */
    private val allowedOnce = mutableSetOf<String>()
    private var network = NetworkConditions()
    private val mutableMobilePlayback = MutableStateFlow<String?>(null)
    /**
     * Key of the borrowed task that keeps playing on mobile data although Wi-Fi only is on, or null. The player shows
     * "모바일 데이터로 재생 중" once when this turns non-null ([DownloadNetworkPolicy.playsOnMobileData]).
     */
    val mobilePlayback = mutableMobilePlayback.asStateFlow()
    private val mutableTasks = MutableStateFlow<Map<String, DownloadEntry>>(emptyMap())
    val tasks = mutableTasks.asStateFlow()
    private val mutableError = MutableStateFlow<Int?>(null)
    /** The last refused start as a string resource ([R.string]), or null. */
    val error = mutableError.asStateFlow()
    private val legacyState = MutableStateFlow(DownloadTaskState())
    val state = legacyState.asStateFlow()
    val engine get() = nodes.getValue(state.value.key ?: nodes.keys.first()).controller.engine
    fun engineFor(key: String) = nodes[key]?.controller?.engine
    fun entry(key: String) = tasks.value[key]
    private fun publish(values: Map<String, DownloadEntry>) {
        mutableTasks.value = values
        val first = values.values.firstOrNull { it.phase == DownloadPhase.RUNNING } ?: values.values.firstOrNull()
        legacyState.value = if (first == null) DownloadTaskState() else DownloadTaskState(first.key,
            first.speed, first.error, complete = first.phase == DownloadPhase.COMPLETE)
        refreshMobilePlayback()
    }
    private fun refreshMobilePlayback() {
        mutableMobilePlayback.value = borrowed.firstOrNull { key ->
            tasks.value[key]?.phase?.networkGated == true &&
                DownloadNetworkPolicy.playsOnMobileData(network, allowedOnce = key in allowedOnce)
        }
    }
    private fun update(entry: DownloadEntry) { publish(tasks.value + (entry.key to entry)) }
    fun start(magnet: String, canStart: (String) -> Boolean) {
        val key = try { MagnetIdentity.key(magnet) } catch (_: Exception) {
            mutableError.value = R.string.error_magnet_required; return
        }
        if (!canStart(key)) { mutableError.value = TorrentFailure.ALREADY_DOWNLOADING.message; return }
        if (tasks.value[key]?.phase?.stoppable == true) return
        nodes.remove(key)?.let { retire(it) }
        mutableError.value = null
        nodes[key] = Node(magnet, createController())
        update(DownloadEntry(key, if (mayRun(key)) DownloadPhase.QUEUED else DownloadPhase.WAITING_FOR_WIFI))
        pump()
        // A task that runs now is saved by its controller; tell only about one left waiting.
        if (tasks.value[key]?.phase != DownloadPhase.RUNNING) onTaskAdded(magnet)
    }
    private fun pump() { DownloadQueuePlan.ready(tasks.value.values, limit).forEach { launch(it) } }
    private fun launch(key: String) {
        val node = nodes[key] ?: return
        update((tasks.value[key] ?: return).copy(phase = DownloadPhase.RUNNING, error = null))
        node.controller.start(node.magnet) { true }
        node.watcher = scope.launch {
            combine(node.controller.state, node.controller.engine.activeDownload) { state, snapshot -> state to snapshot }
                .collect { (state, snapshot) ->
                    if (nodes[key] !== node || tasks.value[key]?.phase != DownloadPhase.RUNNING) return@collect
                    val phase = when { state.error != null -> DownloadPhase.FAILED
                        state.complete -> DownloadPhase.COMPLETE; else -> DownloadPhase.RUNNING }
                    update(DownloadEntry(key, phase, snapshot ?: tasks.value[key]?.snapshot, state.speed, state.error))
                    if (phase == DownloadPhase.COMPLETE || phase == DownloadPhase.FAILED) {
                        if (key !in borrowed) node.controller.stop()
                        pump()
                    }
                }
        }
    }
    /**
     * The one Wi-Fi only decision before playback of [key]. Returns [PlaybackNetworkCheck.CONFIRM_MOBILE] when the
     * user must answer "이번만 재생" first; nothing changes then. On [PlaybackNetworkCheck.START] after the user's
     * [allowMobileOnce] or for a [complete] file (no data used), the task gets a one-time permission ([allowOnce]),
     * so [start] and [borrow] let it run on mobile data. Call before [start] and [borrow]; [borrow] refuses a task
     * that waits for Wi-Fi without that permission.
     */
    fun preparePlayback(key: String, complete: Boolean, allowMobileOnce: Boolean): PlaybackNetworkCheck {
        val check = DownloadNetworkPolicy.playbackStart(network,
            allowedOnce = allowMobileOnce || key in allowedOnce, complete = complete)
        if (check == PlaybackNetworkCheck.START && (allowMobileOnce || complete)) allowOnce(key)
        return check
    }

    /**
     * Lends [key]'s engine to playback. A borrowed task is left out of the Wi-Fi only rule: playback keeps going on
     * mobile data (stopping the engine would end playback with an error); [mobilePlayback] tells the player. A task
     * waiting for Wi-Fi is not started on mobile data without [allowOnce]: this returns null.
     */
    fun borrow(key: String): TorrentEngine? {
        val phase = tasks.value[key]?.phase ?: return null
        if (!phase.stoppable) return null
        if (phase == DownloadPhase.WAITING_FOR_WIFI && !mayRun(key)) return null
        borrowed.add(key)
        refreshMobilePlayback()
        if (phase != DownloadPhase.RUNNING) {
            if (tasks.value.values.count { it.phase == DownloadPhase.RUNNING } >= limit) {
                val victim = tasks.value.values.lastOrNull { it.phase == DownloadPhase.RUNNING && it.key !in borrowed }
                if (victim != null) pause(victim.key)
            }
            launch(key)
        }
        return nodes[key]?.controller?.engine
    }
    private fun pause(key: String, phase: DownloadPhase = DownloadPhase.QUEUED) {
        val node = nodes[key] ?: return
        node.watcher?.cancel(); node.controller.stop()
        update((tasks.value[key] ?: return).copy(phase = phase, speed = 0))
    }
    private fun mayRun(key: String) = DownloadNetworkPolicy.decide(network, allowedOnce = key in allowedOnce).mayRun

    /**
     * Applies new Wi-Fi only settings or a new connection. Tasks lent to playback are skipped. Running and queued
     * tasks that may not run wait for Wi-Fi (their engines stop through the engine queue, like any stop); waiting
     * tasks that may run are queued again. No connection keeps the last known one ([DownloadNetworkPolicy.settle]),
     * so a short drop changes nothing.
     */
    fun applyNetwork(conditions: NetworkConditions) {
        network = DownloadNetworkPolicy.settle(network, conditions)
        tasks.value.values.toList().filter { it.key !in borrowed }.forEach { entry ->
            if (entry.phase.networkGated && !mayRun(entry.key)) pause(entry.key, DownloadPhase.WAITING_FOR_WIFI)
            if (entry.phase == DownloadPhase.WAITING_FOR_WIFI && mayRun(entry.key))
                update(entry.copy(phase = DownloadPhase.QUEUED))
        }
        refreshMobilePlayback()
        pump()
    }

    /** "이번만 허용": lets [key] use mobile data until it is stopped, even while Wi-Fi only is on. */
    fun allowOnce(key: String) {
        allowedOnce += key
        tasks.value[key]?.takeIf { it.phase == DownloadPhase.WAITING_FOR_WIFI && mayRun(key) }?.let {
            update(it.copy(phase = DownloadPhase.QUEUED))
            pump()
        }
    }
    /** Ends playback's loan. A task left running on mobile data without a one-time permission now waits for Wi-Fi. */
    fun release(key: String) {
        if (!borrowed.remove(key)) return
        refreshMobilePlayback()
        val phase = tasks.value[key]?.phase
        if (phase == DownloadPhase.COMPLETE) nodes[key]?.controller?.stop()
        if (phase?.networkGated == true && !mayRun(key)) {
            pause(key, DownloadPhase.WAITING_FOR_WIFI)
            pump()
        }
    }
    fun stop(key: String) {
        stop(setOf(key))
    }
    fun stop(keys: Set<String>) {
        val removed = keys.mapNotNull { nodes.remove(it) }
        borrowed.removeAll(keys)
        allowedOnce.removeAll(keys)
        publish(tasks.value - keys)
        removed.forEach { retire(it) }
        pump()
    }
    fun stop() {
        val previous = nodes.values.toList()
        nodes.clear(); borrowed.clear(); allowedOnce.clear(); publish(emptyMap()); mutableError.value = null
        previous.forEach { retire(it) }
    }
    private fun retire(node: Node) {
        node.watcher?.cancel(); node.controller.stop(); retired.add(node.controller)
    }
    fun protectedFiles() = tasks.value.values.filter { it.phase == DownloadPhase.RUNNING }
        .mapNotNull { nodes[it.key]?.controller?.engine?.selectedVideoFile }
    /** Call on the main thread: [nodes] is not thread-safe, so a delete snapshots its writers here first. */
    fun stoppedWriters(): List<DownloadController> = nodes.values.map { it.controller } + retired
    /** Call from a background thread with a [stoppedWriters] snapshot: waits for their disk writes. */
    fun awaitStoppedWrites(writers: List<DownloadController>) {
        writers.forEach { it.awaitStoppedWrites(); retired.remove(it) }
    }
}
