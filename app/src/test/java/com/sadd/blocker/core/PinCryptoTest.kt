package com.sadd.blocker.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinCryptoTest {

    @Test
    fun validPinsAccepted() {
        assertTrue(PinCrypto.isValidPin("1234"))
        assertTrue(PinCrypto.isValidPin("12345678"))
        assertTrue(PinCrypto.isValidPin("0000"))
    }

    @Test
    fun invalidPinsRejected() {
        assertFalse(PinCrypto.isValidPin("123"))
        assertFalse(PinCrypto.isValidPin("123456789"))
        assertFalse(PinCrypto.isValidPin("12a4"))
        assertFalse(PinCrypto.isValidPin(""))
    }

    @Test
    fun persianAndArabicDigitsAreAcceptedAsPin() {
        // L-2: ارقام فارسی/عربی همان PIN با ارقام لاتین‌اند.
        assertTrue(PinCrypto.isValidPin("۱۲۳۴"))
        assertTrue(PinCrypto.isValidPin("١٢٣٤"))
        assertEquals("1234", PinCrypto.normalizeDigits("۱۲۳۴"))
        assertEquals("1234", PinCrypto.normalizeDigits("١٢٣٤"))
        val salt = PinCrypto.generateSaltHex()
        val hash = PinCrypto.hashPin("1234", salt)
        assertTrue(PinCrypto.verifyPin("۱۲۳۴", salt, hash))
    }

    @Test
    fun samePinDifferentSaltGivesDifferentHash() {
        val salt1 = PinCrypto.generateSaltHex()
        val salt2 = PinCrypto.generateSaltHex()
        assertNotEquals(salt1, salt2)
        assertNotEquals(PinCrypto.hashPin("1234", salt1), PinCrypto.hashPin("1234", salt2))
    }

    @Test
    fun verifyCorrectPin() {
        val salt = PinCrypto.generateSaltHex()
        val hash = PinCrypto.hashPin("9876", salt)
        assertTrue(PinCrypto.verifyPin("9876", salt, hash))
    }

    @Test
    fun verifyWrongPinFails() {
        val salt = PinCrypto.generateSaltHex()
        val hash = PinCrypto.hashPin("9876", salt)
        assertFalse(PinCrypto.verifyPin("9877", salt, hash))
        assertFalse(PinCrypto.verifyPin("9876", PinCrypto.generateSaltHex(), hash))
        assertFalse(PinCrypto.verifyPin("9876", salt, "00"))
        assertFalse(PinCrypto.verifyPin("9876", "zz", hash))
    }

    @Test
    fun hashIsPbkdf2Hex() {
        // H-8: خروجی PBKDF2 با ۲۵۶ بیت، ۶۴ کاراکتر hex است.
        val salt = PinCrypto.generateSaltHex()
        val hash = PinCrypto.hashPin("1234", salt)
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun legacySha256HashVerifiesForMigration() {
        // هش‌های قدیمی نسخه ۱ موقع مهاجرت به PBKDF2 قابل بررسی‌اند.
        val salt = PinCrypto.generateSaltHex()
        val legacyHash = PinCrypto.hashPinLegacy("9876", salt)
        assertTrue(PinCrypto.verifyPinLegacy("9876", salt, legacyHash))
        assertFalse(PinCrypto.verifyPinLegacy("9877", salt, legacyHash))
        // هش قدیمی با تابع جدید تأیید نمی‌شود (الگوریتم عوض شده).
        assertFalse(PinCrypto.verifyPin("9876", salt, legacyHash))
    }
}
