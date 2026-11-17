package com.sadd.blocker.core

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * رمزنگاری PIN — خالص Kotlin، بدون وابستگی اندروید.
 * PIN هرگز plaintext ذخیره نمی‌شود: فقط SHA-256(salt + PIN) + salt نگه داشته می‌شود.
 * SettingsStore از همین توابع استفاده می‌کند تا منطق در یک جا باشد و تست‌پذیر بماند.
 */
object PinCrypto {

    /** PIN معتبر: فقط ارقام ASCII، ۴ تا ۸ کاراکتر. */
    fun isValidPin(pin: String): Boolean {
        if (pin.length < 4 || pin.length > 8) return false
        for (c in pin) {
            if (c !in '0'..'9') return false
        }
        return true
    }

    /** Salt تصادفی ۱۶ بایتی به‌صورت hex. */
    fun generateSaltHex(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return bytesToHex(salt)
    }

    /** SHA-256(salt + PIN) به‌صورت hex. */
    fun hashPin(pin: String, saltHex: String): String {
        val salt = hexToBytes(saltHex)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return bytesToHex(digest.digest())
    }

    /** بررسی PIN با مقایسه ثابت‌زمان (مقاوم در برابر timing attack). */
    fun verifyPin(pin: String, saltHex: String, expectedHashHex: String): Boolean {
        val expected = runCatching { hexToBytes(expectedHashHex) }.getOrNull() ?: return false
        val actual = runCatching { hexToBytes(hashPin(pin, saltHex)) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, actual)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append("%02x".format(b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "hex length must be even" }
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            out[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        return out
    }
}
