package com.sadd.blocker.domain.usecase

import com.sadd.blocker.domain.repository.SessionRepository

/**
 * قفل قوانین در سشن Strict (H-2).
 *
 * وقتی سشن Strict فعال است، غیرفعال‌کردن قانون فعال و حذف قانون فعال
 * ممنوع است — این همان قولی است که صفحه حالت‌ها به کاربر می‌دهد و قبلاً
 * هیچ کدی آن را اعمال نمی‌کرد. فعال‌کردن قانون (تقویت بلاک) و حذف
 * قانونی که از قبل غیرفعال است، آزاد است.
 */
class StrictSessionGuard(private val sessions: SessionRepository) {

    suspend fun isStrictSessionActive(nowMs: Long = System.currentTimeMillis()): Boolean {
        val active = sessions.getActive(nowMs)
        return active?.strict == true
    }
}
