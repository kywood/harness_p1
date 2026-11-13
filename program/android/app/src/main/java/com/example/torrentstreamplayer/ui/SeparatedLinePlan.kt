package com.example.torrentstreamplayer.ui

/**
 * 구분점("·")으로 이은 항목들을 줄로 나눈다. 구분점은 같은 줄 항목 사이에만 들어가므로, 줄이 바뀌는 자리에 구분점만
 * 남지 않는다(메인 하단 링크 줄, #161 영어 화면 확인).
 */
internal object SeparatedLinePlan {
    /**
     * [widths] 항목을 차례로 [maxWidth] 안에 채운 줄들(항목 번호 범위). 같은 줄 항목 사이에는 [separator] 너비가 든다.
     * [maxWidth]보다 넓은 항목은 혼자 한 줄을 쓴다.
     */
    fun lines(widths: List<Int>, separator: Int, maxWidth: Int): List<IntRange> {
        val lines = mutableListOf<IntRange>()
        var start = 0
        var used = 0
        widths.forEachIndexed { index, width ->
            if (index > start && used + separator + width > maxWidth) {
                lines += start until index
                start = index
                used = width
            } else {
                used += if (index > start) separator + width else width
            }
        }
        if (widths.isNotEmpty()) lines += start until widths.size
        return lines
    }

    /** [line]의 너비: 항목 너비와 그 사이 구분점. */
    fun width(line: IntRange, widths: List<Int>, separator: Int): Int =
        line.sumOf { widths[it] } + separator * (line.count() - 1).coerceAtLeast(0)
}
