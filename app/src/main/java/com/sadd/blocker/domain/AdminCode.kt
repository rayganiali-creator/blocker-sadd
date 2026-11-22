package com.sadd.blocker.domain

import java.security.MessageDigest
import java.util.Locale

/**
 * کد فعال‌سازی توسعه‌دهنده — خالص Kotlin، بدون وابستگی اندروید.
 *
 * فقط هش SHA-256 کد نگه داشته می‌شود؛ خود کد هیچ‌جا در سورس نیست.
 * کد واردشده با همان هش مقایسه می‌شود (مقایسه ثابت‌زمان). نرمال‌سازی
 * فقط بریدن فاصله‌های ابتدا/انتها است؛ رشته دقیق (UTF-8) هش می‌شود.
 */
object AdminCode {

    /**
     * SHA-256 (hex) کد فعال‌سازی سید. برای عوض‌کردن کد، هش کد جدید
     * را همین‌جا بگذار (SHA-256 رشته UTF-8 کد، بعد از trim).
     */
    const val ADMIN_CODE_SHA256_HEX: String =
        "286a63cb3be51d15fc7ac1bf0ef925bf8725bb39e61a9236d365976571b922db"

    /** نرمال‌سازی ورودی: فقط فاصله‌های ابتدا و انتها بریده می‌شود. */
    fun normalize(code: String): String = code.trim()

    /** SHA-256 رشته (UTF-8) به‌صورت hex کوچک. */
    fun sha256Hex(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append("%02x".format(Locale.US, b))
        }
        return sb.toString()
    }

    /**
     * آیا کد واردشده با هش مورد انتظار یکی است؟
     * مقایسه روی بایت‌های هش و ثابت‌زمان است (MessageDigest.isEqual).
     */
    fun verify(code: String, expectedHashHex: String = ADMIN_CODE_SHA256_HEX): Boolean {
        val actualHex = sha256Hex(normalize(code))
        val actual = runCatching { hexToBytes(actualHex) }.getOrNull() ?: return false
        val expected = runCatching { hexToBytes(expectedHashHex) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected, actual)
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
