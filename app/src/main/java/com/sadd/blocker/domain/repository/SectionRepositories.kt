package com.sadd.blocker.domain.repository

import com.sadd.blocker.domain.model.SectionRule
import kotlinx.coroutines.flow.Flow

/**
 * قرارداد Repositoryهای بخش اپ و رویداد بلاک — خالص Kotlin.
 * پیاده‌سازی Room در data/repository است؛ پیاده‌سازی حافظه‌ای فقط
 * برای تست لازم شود اضافه می‌شود (تست‌های فعلی به آن نیاز ندارند).
 */
interface SectionRuleRepository {
    fun observe(): Flow<List<SectionRule>>
    suspend fun list(): List<SectionRule>
    suspend fun getEnabled(): List<SectionRule>

    /**
     * پیش‌فرض‌ها (جستجوی تلگرام/اینستاگرام/یوتیوب + اکسپلور اینستاگرام)
     * را اگر نیستند می‌سازد؛ تکراری نمی‌سازد (ایندکس یکتا در DB).
     */
    suspend fun ensurePresets()
    suspend fun setEnabled(packageName: String, sectionKey: String, enabled: Boolean)
}

interface BlockEventRepository {
    suspend fun log(kind: String, detail: String, timestampMs: Long = System.currentTimeMillis())
    fun countSince(sinceMs: Long): Flow<Int>
    suspend fun countSinceNow(sinceMs: Long): Int
}
