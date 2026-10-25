package com.example.torrentstreamplayer.engine

internal class ReadDemandRegistry {
    private var next = 0L
    private val positions = linkedMapOf<Long, Long>()
    @Synchronized fun open(position: Long): Long = (++next).also { positions[it] = position }
    @Synchronized fun update(id: Long, position: Long) { if (id in positions) positions[id] = position }
    @Synchronized fun close(id: Long) { positions.remove(id) }
    @Synchronized fun clear() { positions.clear() }
    @Synchronized fun offsets(): List<Long> = positions.values.toList()
}
