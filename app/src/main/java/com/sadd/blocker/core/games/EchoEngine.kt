package com.sadd.blocker.core.games

import kotlin.random.Random

/**
 * منطق خالص بازی «پژواک» (حافظه توالی نورها روی شبکه ۳×۳).
 * سلول‌ها با اندیس ۰ تا ۸ مشخص می‌شوند (سطر به سطر از بالا-چپ شبکه).
 * توالی از [Random] با seed ساخته می‌شود تا تست‌ها قطعی باشند.
 */
object EchoEngine {

    const val CELL_COUNT = 9

    /** توالی تصادفیِ قطعی به طول داده‌شده؛ طول صفر یا منفی توالی خالی می‌دهد. */
    fun generateSequence(length: Int, seed: Long): List<Int> {
        if (length <= 0) {
            return emptyList()
        }
        val random = Random(seed)
        return List(length) { random.nextInt(CELL_COUNT) }
    }

    /** آیا ورودی‌های کاربر تا اینجا پیشوند درستی از توالی هستند؟ */
    fun isCorrectPrefix(sequence: List<Int>, inputs: List<Int>): Boolean {
        if (inputs.size > sequence.size) {
            return false
        }
        for (i in inputs.indices) {
            if (inputs[i] != sequence[i]) {
                return false
            }
        }
        return true
    }

    /** آیا کاربر کل توالی را درست و کامل تکرار کرده است؟ */
    fun isComplete(sequence: List<Int>, inputs: List<Int>): Boolean {
        return inputs.size == sequence.size && isCorrectPrefix(sequence, inputs)
    }
}
