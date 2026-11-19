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
     * پیش‌فرض‌ها (۲۷ بخش: کل تلگرام/اینستاگرام/یوتیوب/واتساپ/تیک‌تاک/
     * ایکس/فیسبوک/اسنپ‌چت + جستجوی هر کدام + اکسپلور و ریلز اینستاگرام +
     * شورتس یوتیوب + وضعیت واتساپ + For You تیک‌تاک + خانه ایکس +
     * ریلز و واچ فیسبوک + اسپات‌لایت اسنپ‌چت)
     * را اگر نیستند می‌سازد؛ تکراری نمی‌سازد (ایندکس یکتا در DB).
     */
    suspend fun ensurePresets()
    suspend fun setEnabled(packageName: String, sectionKey: String, enabled: Boolean)

    /**
     * یک قانون بخش را حذف می‌کند — فقط برای قانون‌های دستی کاربر
     * («خود اپ» و بخش سفارشی) از UI صدا زده می‌شود؛ پیش‌فرض‌ها
     * دکمه حذف ندارند. پیاده‌سازی Room روی
     * SectionRuleDao.delete(packageName, sectionKey) می‌نشیند.
     */
    suspend fun remove(packageName: String, sectionKey: String)

    /**
     * یک قانون بخش دستی اضافه می‌کند (کل اپ یک اپ نصب‌شده یا بخش
     * سفارشی با متن نشانه). اگر همان (packageName, sectionKey) از
     * قبل باشد، false برمی‌گرداند و چیزی اضافه نمی‌شود.
     */
    suspend fun addRule(packageName: String, sectionKey: String, labelFa: String): Boolean
}

interface BlockEventRepository {
    suspend fun log(kind: String, detail: String, timestampMs: Long = System.currentTimeMillis())
    fun countSince(sinceMs: Long): Flow<Int>
    suspend fun countSinceNow(sinceMs: Long): Int
}
