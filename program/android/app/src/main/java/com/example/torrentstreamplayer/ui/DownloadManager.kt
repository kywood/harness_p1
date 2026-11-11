package com.example.torrentstreamplayer.ui
import com.example.torrentstreamplayer.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
internal enum class DownloadPhase { QUEUED, RUNNING, COMPLETE, FAILED }
internal data class DownloadEntry(val key: String, val phase: DownloadPhase = DownloadPhase.QUEUED,
    val snapshot: DownloadSnapshot? = null, val speed: Long = 0, val error: String? = null)
internal object DownloadQueuePlan {
    fun ready(entries: Collection<DownloadEntry>, limit: Int): List<String> {
        require(limit > 0)
        val slots = (limit - entries.count { it.phase == DownloadPhase.RUNNING }).coerceAtLeast(0)
        return entries.filter { it.phase == DownloadPhase.QUEUED }.take(slots).map { it.key }
    }
}
internal class DownloadManager(private val scope: CoroutineScope,
    private val createController: () -> DownloadController, private val limit: Int = 3) {
    init { require(limit in 1..8) }
    private class Node(val magnet: String, val controller: DownloadController, var watcher: Job? = null)
    private val nodes = linkedMapOf<String, Node>()
    private val retired = java.util.concurrent.CopyOnWriteArrayList<DownloadController>()
    private val borrowed = mutableSetOf<String>()
    private val mutableTasks = MutableStateFlow<Map<String, DownloadEntry>>(emptyMap())
    val tasks = mutableTasks.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
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
    }
    private fun update(entry: DownloadEntry) { publish(tasks.value + (entry.key to entry)) }
    fun start(magnet: String, canStart: (String) -> Boolean) {
        val key = try { MagnetIdentity.key(magnet) } catch (_: Exception) {
            mutableError.value = "유효한 마그넷 주소가 필요합니다."; return
        }
        if (!canStart(key)) { mutableError.value = "재생 중인 영상은 이미 다운로드 중입니다."; return }
        if (tasks.value[key]?.phase in listOf(DownloadPhase.RUNNING, DownloadPhase.QUEUED)) return
        nodes.remove(key)?.let { retire(it) }
        mutableError.value = null
        nodes[key] = Node(magnet, createController())
        update(DownloadEntry(key))
        pump()
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
    fun borrow(key: String): TorrentEngine? {
        val phase = tasks.value[key]?.phase ?: return null
        if (phase != DownloadPhase.RUNNING && phase != DownloadPhase.QUEUED) return null
        borrowed.add(key)
        if (phase == DownloadPhase.QUEUED) {
            if (tasks.value.values.count { it.phase == DownloadPhase.RUNNING } >= limit) {
                val victim = tasks.value.values.lastOrNull { it.phase == DownloadPhase.RUNNING && it.key !in borrowed }
                if (victim != null) pause(victim.key)
            }
            launch(key)
        }
        return nodes[key]?.controller?.engine
    }
    private fun pause(key: String) {
        val node = nodes[key] ?: return
        node.watcher?.cancel(); node.controller.stop()
        update((tasks.value[key] ?: return).copy(phase = DownloadPhase.QUEUED, speed = 0))
    }
    fun release(key: String) {
        if (borrowed.remove(key) && tasks.value[key]?.phase == DownloadPhase.COMPLETE) nodes[key]?.controller?.stop()
    }
    fun stop(key: String) {
        stop(setOf(key))
    }
    fun stop(keys: Set<String>) {
        val removed = keys.mapNotNull { nodes.remove(it) }
        borrowed.removeAll(keys)
        publish(tasks.value - keys)
        removed.forEach { retire(it) }
        pump()
    }
    fun stop() {
        val previous = nodes.values.toList()
        nodes.clear(); borrowed.clear(); publish(emptyMap()); mutableError.value = null
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
