package com.sadd.blocker.core

import com.sadd.blocker.core.IntentDetector.FieldKind
import com.sadd.blocker.core.IntentDetector.Input
import com.sadd.blocker.core.IntentDetector.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

class IntentDetectorTest {

    private val rules = listOf("قمار", "example")

    @Test
    fun articleContentNeverBlocks() {
        val result = IntentDetector.evaluate(
            Input(FieldKind.CONTENT_ONLY, true, "مقاله‌ای درباره قمار و تاریخ آن", rules),
        )
        assertEquals(Verdict.ALLOW, result.verdict)
    }

    @Test
    fun messageFieldNeverBlocks() {
        val result = IntentDetector.evaluate(
            Input(FieldKind.NON_SEARCH, true, "قمار بده", rules),
        )
        assertEquals(Verdict.ALLOW, result.verdict)
    }

    @Test
    fun searchFieldSubmittedBlocks() {
        val result = IntentDetector.evaluate(
            Input(FieldKind.SEARCH, true, "قمار آنلاین", rules),
        )
        assertEquals(Verdict.BLOCK, result.verdict)
        assertEquals("قمار", result.matchedRule)
    }

    @Test
    fun searchFieldTypedButNotSubmittedDoesNotBlock() {
        val result = IntentDetector.evaluate(
            Input(FieldKind.SEARCH, false, "قمار", rules),
        )
        assertEquals(Verdict.ALLOW, result.verdict)
    }

    @Test
    fun unknownFieldWithSubmitIsLowConfidenceAllow() {
        val result = IntentDetector.evaluate(
            Input(FieldKind.UNKNOWN, true, "قمار", rules),
        )
        assertEquals(Verdict.ALLOW, result.verdict)
    }

    @Test
    fun noMatchAllowsEvenWhenSubmitted() {
        val result = IntentDetector.evaluate(
            Input(FieldKind.SEARCH, true, "آب و هوا", rules),
        )
        assertEquals(Verdict.ALLOW, result.verdict)
    }
}
