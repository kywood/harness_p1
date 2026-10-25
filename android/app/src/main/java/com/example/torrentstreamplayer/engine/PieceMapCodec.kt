package com.example.torrentstreamplayer.engine
import java.io.DataInputStream
import java.io.DataOutputStream

internal object PieceMapCodec {
    fun write(out: DataOutputStream, map: PieceMapSnapshot?) {
        out.writeBoolean(map != null)
        if (map == null) return
        out.writeInt(map.total); out.writeInt(map.verified); out.writeInt(map.cells.size)
        map.cells.forEach { out.writeByte(it.ordinal) }
    }
    fun read(input: DataInputStream): PieceMapSnapshot? {
        if (!input.readBoolean()) return null
        val total = input.readInt()
        val verified = input.readInt()
        val count = input.readInt()
        require(total >= 0 && verified in 0..total && count in 0..minOf(512, total))
        require((total == 0) == (count == 0))
        val cells = List(count) { PieceCell.entries.getOrNull(input.readUnsignedByte()) ?: error("Invalid piece cell") }
        return PieceMapSnapshot(cells, verified, total)
    }
}
