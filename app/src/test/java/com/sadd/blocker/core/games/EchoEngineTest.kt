package com.sadd.blocker.core.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** تست‌های «پژواک»: قطعی‌بودن توالی با seed و درستیِ بررسی پیشوند و کامل‌بودن. */
class EchoEngineTest {

    @Test
    fun sequenceIsDeterministicForSameSeed() {
        val first = EchoEngine.generateSequence(length = 6, seed = 42L)
        val second = EchoEngine.generateSequence(length = 6, seed = 42L)
        assertEquals(first, second)
        assertEquals(6, first.size)
    }

    @Test
    fun sequenceValuesStayWithinGrid() {
        val sequence = EchoEngine.generateSequence(length = 50, seed = 7L)
        assertEquals(50, sequence.size)
        assertTrue(sequence.all { it in 0 until EchoEngine.CELL_COUNT })
    }

    @Test
    fun longerSequenceExtendsShorterOneWithSameSeed() {
        // کشیدن‌های پشت‌سرهم از یک Random: توالی کوتاه‌تر پیشوند بلندتر است.
        val short = EchoEngine.generateSequence(length = 4, seed = 123L)
        val long = EchoEngine.generateSequence(length = 9, seed = 123L)
        assertEquals(short, long.take(4))
    }

    @Test
    fun nonPositiveLengthGivesEmptySequence() {
        assertTrue(EchoEngine.generateSequence(length = 0, seed = 1L).isEmpty())
        assertTrue(EchoEngine.generateSequence(length = -3, seed = 1L).isEmpty())
    }

    @Test
    fun prefixChecks() {
        val sequence = listOf(1, 4, 7, 2)
        assertTrue(EchoEngine.isCorrectPrefix(sequence, emptyList()))
        assertTrue(EchoEngine.isCorrectPrefix(sequence, listOf(1)))
        assertTrue(EchoEngine.isCorrectPrefix(sequence, listOf(1, 4, 7)))
        assertTrue(EchoEngine.isCorrectPrefix(sequence, sequence))
        assertFalse(EchoEngine.isCorrectPrefix(sequence, listOf(2)))
        assertFalse(EchoEngine.isCorrectPrefix(sequence, listOf(1, 7)))
        // ورودی بلندتر از توالی هرگز پیشوند درست نیست.
        assertFalse(EchoEngine.isCorrectPrefix(sequence, listOf(1, 4, 7, 2, 0)))
    }

    @Test
    fun completeChecks() {
        val sequence = listOf(3, 0, 8)
        assertTrue(EchoEngine.isComplete(sequence, sequence))
        assertFalse(EchoEngine.isComplete(sequence, listOf(3, 0)))
        assertFalse(EchoEngine.isComplete(sequence, listOf(3, 0, 7)))
        assertFalse(EchoEngine.isComplete(sequence, emptyList()))
        assertFalse(EchoEngine.isComplete(sequence, listOf(3, 0, 8, 8)))
    }
}
