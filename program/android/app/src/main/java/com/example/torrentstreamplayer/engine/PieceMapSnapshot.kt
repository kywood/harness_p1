package com.example.torrentstreamplayer.engine

enum class PieceCell { MISSING, PARTIAL, VERIFIED, LOW, MEDIUM, HIGH }
data class PieceMapSnapshot(val cells: List<PieceCell>, val verified: Int, val total: Int) {
    companion object {
        val EMPTY = PieceMapSnapshot(emptyList(), 0, 0)
        fun capture(first: Int, last: Int, maxCells: Int = 256,
            havePiece: (Int) -> Boolean): PieceMapSnapshot {
            require(maxCells in 1..512)
            if (first < 0 || last < first) return EMPTY
            val total = last.toLong() - first + 1
            require(total <= Int.MAX_VALUE)
            val count = minOf(total.toInt(), maxCells)
            var verified = 0
            val cells = List(count) { cell ->
                val start = (cell.toLong() * total / count).toInt()
                val end = ((cell + 1L) * total / count).toInt()
                var ready = 0
                for (piece in start until end) if (havePiece(first + piece)) ready++
                verified += ready
                when (ready) {
                    0 -> PieceCell.MISSING
                    end - start -> PieceCell.VERIFIED
                    else -> when {
                        ready * 100L < (end - start) * 34L -> PieceCell.LOW
                        ready * 100L < (end - start) * 67L -> PieceCell.MEDIUM
                        else -> PieceCell.HIGH
                    }
                }
            }
            return PieceMapSnapshot(cells, verified, total.toInt())
        }
    }
}
