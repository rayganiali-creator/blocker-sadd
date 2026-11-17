package com.sadd.blocker.core

/**
 * تشخیص نیت جستجو — خالص Kotlin، قانون‌محور و قطعی.
 *
 * اصل: خواندن یا تایپ عادی هرگز بلاک نمی‌شود. بلاک فقط وقتی ساخته می‌شود
 * که فیلد «جستجو/آدرس» باشد + متن با قانون منطبق باشد + سابمیت/ناوبری رخ داده باشد.
 *
 * Confidence: بالا فقط وقتی هر سه شرط برقرارند. پایین → ALLOW (محتاطانه).
 * هیچ مدل ML یا «AI»ای در کار نیست؛ این یک موتور قانون‌محور v1 است.
 */
object IntentDetector {

    enum class Verdict { BLOCK, ALLOW }

    enum class FieldKind {
        SEARCH,      // فیلد جستجو/آدرس شناخته‌شده
        UNKNOWN,     // فیلد ورودی، نامشخص که جستجوست یا نه
        NON_SEARCH,  // پیام، یادداشت، متن عادی
        CONTENT_ONLY // متن صفحه (مقاله/فید) — نه ورودی کاربر
    }

    data class Input(
        val fieldKind: FieldKind,
        val submitted: Boolean,      // Enter / دکمه جستجو / ناوبری رخ داده؟
        val text: String,
        val rules: List<String>,
    )

    data class Result(
        val verdict: Verdict,
        val confidence: Double, // 0.0 تا 1.0
        val matchedRule: String?,
        val reason: String,
    )

    fun evaluate(input: Input): Result {
        // متن صفحه یا فیلد غیرجستجو: هرگز بلاک نمی‌شود.
        if (input.fieldKind == FieldKind.CONTENT_ONLY || input.fieldKind == FieldKind.NON_SEARCH) {
            return Result(Verdict.ALLOW, 1.0, null, "content_or_non_search_field")
        }
        val matched = KeywordMatcher.firstMatch(input.text, input.rules)
            ?: return Result(Verdict.ALLOW, 1.0, null, "no_rule_match")
        // تطابق هست ولی سابمیت نشده: فقط نامزد است، بلاک نمی‌سازد.
        if (!input.submitted) {
            return Result(Verdict.ALLOW, 0.9, matched, "match_but_not_submitted")
        }
        // فیلد نامشخص + سابمیت: اطمینان پایین → محتاطانه ALLOW.
        if (input.fieldKind == FieldKind.UNKNOWN) {
            return Result(Verdict.ALLOW, 0.4, matched, "low_confidence_unknown_field")
        }
        // فیلد جستجو + تطابق + سابمیت: بلاک با اطمینان بالا.
        return Result(Verdict.BLOCK, 0.95, matched, "search_field_match_submitted")
    }
}
