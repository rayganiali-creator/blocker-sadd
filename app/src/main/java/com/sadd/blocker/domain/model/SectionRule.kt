package com.sadd.blocker.domain.model

/** قانون «بخشِ اپ»: مثلاً جستجوی تلگرام یا اکسپلور اینستاگرام. */
data class SectionRule(
    val id: Long = 0,
    val packageName: String,
    val sectionKey: String,
    val labelFa: String,
    val enabled: Boolean = true,
)

/** یک رویداد بلاک ثبت‌شده — داشبورد بعداً «بلاک امروز» را از آن می‌شمارد. */
data class BlockEvent(
    val id: Long = 0,
    val timestampMs: Long,
    val kind: String,
    val detail: String,
)

/** نوع رویداد بلاک (مقدارهای ذخیره‌شده در ستون kind جدول block_events). */
object BlockEventKind {
    /** کلمه ممنوعه موقع تایپ از فیلد حذف شد. */
    const val KEYWORD_TYPED = "keyword_typed"
    /** ورود به بخش بلاک‌شده یک اپ (جستجو/اکسپلور) با BACK برگردانده شد. */
    const val SECTION_OPENED = "section_opened"
    /** ورود به اپِ کاملاً بلاک‌شده (قانون صفحه اپ‌ها) با BACK برگردانده شد. */
    const val APP_OPENED = "app_opened"
    /** سایت بلاک‌شده در مرورگر با BACK برگردانده شد. */
    const val WEBSITE_OPENED = "website_opened"
    /** نگهبان حذف نصب/توقف اجباری صفحه محافظت‌شده را رهگیری کرد. */
    const val GUARD_BLOCKED = "guard_blocked"
}
