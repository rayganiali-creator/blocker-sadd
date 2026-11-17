package com.sadd.blocker.core

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * رمزنگاری PIN — خالص Kotlin، بدون وابستگی اندروید.
 * PIN هرگز plaintext ذخیره نمی‌شود؛ فقط KDF + salt نگه داشته می‌شود.
 *
 * نسخه ۲ (رفع H-8): PBKDF2-HMAC-SHA256 با ۱۲۰٬۰۰۰ تکرار و خروجی ۲۵۶ بیت.
 * PIN چهاررقمی فقط ۱۰٬۰۰۰ حالت دارد؛ هش تک‌مرحله‌ای SHA-256 آفلاین در
 * کسری از ثانیه brute-force می‌شود. PBKDF2 هر حدس را ~۱۰۰ms کند می‌کند.
 * هش‌های قدیمی (SHA-256 تک‌مرحله‌ای، نسخه ۱) با [hashPinLegacy] و
 * [verifyPinLegacy] قابل بررسی‌اند تا SettingsStore موقع ورود موفق،
 * آن‌ها را شفاف به نسخه ۲ ارتقا بدهد.
 *
 * ارقام فارسی/عربی (L-2): «۱۲۳۴» و «1234» یک PIN‌اند؛ همه ورودی‌ها اول
 * با [normalizeDigits] به ASCII تبدیل می‌شوند.
 */
object PinCrypto {

    /** تعداد تکرار PBKDF2 — بالا نگه داشته شده تا brute-force آفلاین گران باشد. */
    const val PBKDF2_ITERATIONS = 120_000
    const val PBKDF2_KEY_BITS = 256
    const val HASH_VERSION_V1_SHA256 = 1
    const val HASH_VERSION_V2_PBKDF2 = 2

    /** ارقام فارسی (۰-۹) و عربی (٠-٩) را به ASCII تبدیل می‌کند. */
    fun normalizeDigits(pin: String): String {
        val sb = StringBuilder(pin.length)
        for (c in pin) {
            when (c) {
                in '۰'..'۹' -> sb.append('0' + (c - '۰'))
                in '٠'..'٩' -> sb.append('0' + (c - '٠'))
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /** PIN معتبر: ۴ تا ۸ رقم (بعد از تبدیل ارقام فارسی/عربی به ASCII). */
    fun isValidPin(pin: String): Boolean {
        val normalized = normalizeDigits(pin)
        if (normalized.length < 4 || normalized.length > 8) return false
        for (c in normalized) {
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

    /** PBKDF2-HMAC-SHA256(salt, PIN) به‌صورت hex (۶۴ کاراکتر). */
    fun hashPin(pin: String, saltHex: String): String {
        val salt = hexToBytes(saltHex)
        val spec = PBEKeySpec(normalizeDigits(pin).toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return bytesToHex(factory.generateSecret(spec).encoded)
    }

    /** بررسی PIN با مقایسه ثابت‌زمان (مقاوم در برابر timing attack). */
    fun verifyPin(pin: String, saltHex: String, expectedHashHex: String): Boolean {
        val expected = runCatching { hexToBytes(expectedHashHex) }.getOrNull() ?: return false
        val actual = runCatching { hexToBytes(hashPin(pin, saltHex)) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, actual)
    }

    // ---- سازگاری با هش‌های قدیمی (نسخه ۱: SHA-256 تک‌مرحله‌ای) ----

    /** SHA-256(salt + PIN) — فقط برای بررسی هش‌های قدیمی موقع مهاجرت. */
    fun hashPinLegacy(pin: String, saltHex: String): String {
        val salt = hexToBytes(saltHex)
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(normalizeDigits(pin).toByteArray(Charsets.UTF_8))
        return bytesToHex(digest.digest())
    }

    /** بررسی PIN در برابر هش قدیمی نسخه ۱. */
    fun verifyPinLegacy(pin: String, saltHex: String, expectedHashHex: String): Boolean {
        val expected = runCatching { hexToBytes(expectedHashHex) }.getOrNull() ?: return false
        val actual = runCatching { hexToBytes(hashPinLegacy(pin, saltHex)) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, actual)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            // Locale صریح: در لوکال fa، بدون آن ارقام hex بومی می‌شوند و
            // hexToBytes بعداً روی آن‌ها خطا می‌دهد.
            sb.append("%02x".format(Locale.US, b))
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
