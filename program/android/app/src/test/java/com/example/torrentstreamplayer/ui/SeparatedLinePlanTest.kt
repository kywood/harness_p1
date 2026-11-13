package com.example.torrentstreamplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SeparatedLinePlanTest {
    @Test fun everythingFitsOnOneLine() {
        assertEquals(listOf(0..2), SeparatedLinePlan.lines(listOf(30, 40, 20), separator = 5, maxWidth = 100))
        assertEquals(100, SeparatedLinePlan.width(0..2, listOf(30, 40, 20), separator = 5))
    }

    @Test fun aLinkThatDoesNotFitStartsTheNextLineWithoutASeparatorLeftBehind() {
        // 홈 360dp: "Usage guide · Privacy policy" 다음 "Open source licenses"가 넘친다.
        val lines = SeparatedLinePlan.lines(listOf(100, 140, 180), separator = 20, maxWidth = 300)
        assertEquals(listOf(0..1, 2..2), lines)
        assertEquals("줄 끝에 구분점 너비가 들어가지 않는다", 260, SeparatedLinePlan.width(lines[0], listOf(100, 140, 180), 20))
        assertEquals(180, SeparatedLinePlan.width(lines[1], listOf(100, 140, 180), 20))
    }

    @Test fun theSeparatorCountsOnlyBetweenItems() {
        assertEquals("100 + 10 + 90 = 200은 들어간다", listOf(0..1),
            SeparatedLinePlan.lines(listOf(100, 90), separator = 10, maxWidth = 200))
        assertEquals(listOf(0..0, 1..1), SeparatedLinePlan.lines(listOf(100, 91), separator = 10, maxWidth = 200))
    }

    @Test fun anItemWiderThanTheLineGetsItsOwnLine() {
        assertEquals(listOf(0..0, 1..1, 2..2), SeparatedLinePlan.lines(listOf(50, 400, 50), separator = 10, maxWidth = 300))
    }

    @Test fun noItemsNoLines() {
        assertEquals(emptyList<IntRange>(), SeparatedLinePlan.lines(emptyList(), separator = 10, maxWidth = 300))
    }
}
