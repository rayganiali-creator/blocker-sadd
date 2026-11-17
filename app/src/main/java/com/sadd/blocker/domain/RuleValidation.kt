package com.sadd.blocker.domain

import com.sadd.blocker.core.KeywordMatcher
import com.sadd.blocker.core.PersianNormalizer

/**
 * اعتبارسنجی ورودی قوانین — خالص Kotlin.
 * پیام‌های خطا فارسی‌اند و مستقیم در UI نمایش داده می‌شوند.
 */
object RuleValidation {

    const val MAX_KEYWORD_LENGTH = 100
    const val MAX_HOST_LENGTH = 253

    data class Check(val ok: Boolean, val error: String? = null)

    fun validateKeyword(raw: String): Check {
        val normalized = PersianNormalizer.normalize(raw)
        if (normalized.isEmpty()) {
            return Check(false, "متن کلمه خالی است.")
        }
        if (normalized.length > MAX_KEYWORD_LENGTH) {
            return Check(false, "کلمه خیلی طولانی است.")
        }
        return Check(true)
    }

    fun validateWebsite(raw: String): Check {
        val host = KeywordMatcher.normalizeHost(raw)
        if (host.isEmpty()) {
            return Check(false, "دامنه خالی است.")
        }
        if (host.length > MAX_HOST_LENGTH) {
            return Check(false, "دامنه خیلی طولانی است.")
        }
        // M-2: «user@example.com» یا کاراکترهای غیرمجاز دامنه رد می‌شوند.
        if (host.contains(' ') || host.contains('/') || host.contains('@') ||
            host.contains('\\') || host.contains('?') || host.contains('#') ||
            host.contains('%') || host.contains('_') || host.contains(':')
        ) {
            return Check(false, "دامنه معتبر نیست.")
        }
        if (!host.contains('.')) {
            return Check(false, "دامنه باید نقطه داشته باشد (مثل example.com).")
        }
        // هر برچسب (label) باید غیرخالی باشد و با خط‌تیره شروع/تمام نشود؛
        // TLD حداقل ۲ کاراکتر است.
        val labels = host.split('.')
        if (labels.any { it.isEmpty() || it.startsWith('-') || it.endsWith('-') }) {
            return Check(false, "دامنه معتبر نیست.")
        }
        if (labels.last().length < 2) {
            return Check(false, "دامنه معتبر نیست.")
        }
        return Check(true)
    }

    /** عبارت چندکلمه‌ای است؟ (بعد از نرمال‌سازی، فاصله دارد.) */
    fun isPhrase(normalizedText: String): Boolean {
        return normalizedText.contains(' ')
    }
}
