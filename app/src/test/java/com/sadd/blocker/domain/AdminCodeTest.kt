package com.sadd.blocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست کد فعال‌سازی توسعه‌دهنده با یک جفت کد/هش دورریختنیِ تست —
 * کد واقعی سید در تست نیست و نباید باشد.
 */
class AdminCodeTest {

    private val throwawayCode = "throwaway-test-code-42"
    private val throwawayHash = AdminCode.sha256Hex(throwawayCode)

    @Test
    fun correctCodeVerifies() {
        assertTrue(AdminCode.verify(throwawayCode, throwawayHash))
    }

    @Test
    fun wrongCodeFails() {
        assertFalse(AdminCode.verify("throwaway-test-code-43", throwawayHash))
        assertFalse(AdminCode.verify("", throwawayHash))
        assertFalse(AdminCode.verify("   ", throwawayHash))
    }

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertTrue(AdminCode.verify("  $throwawayCode  ", throwawayHash))
        assertTrue(AdminCode.verify("\n$throwawayCode\t", throwawayHash))
    }

    @Test
    fun innerWhitespaceMatters() {
        assertFalse(AdminCode.verify("throwaway test code 42", throwawayHash))
    }

    @Test
    fun hashIsDeterministicSha256Hex() {
        // SHA-256("abc") — مقدار شناخته‌شده استاندارد.
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            AdminCode.sha256Hex("abc"),
        )
    }

    @Test
    fun storedConstantIsWellFormedHex() {
        val constant = AdminCode.ADMIN_CODE_SHA256_HEX
        assertEquals(64, constant.length)
        assertTrue(constant.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun malformedExpectedHashFailsClosed() {
        assertFalse(AdminCode.verify(throwawayCode, "not-hex"))
        assertFalse(AdminCode.verify(throwawayCode, ""))
    }
}
