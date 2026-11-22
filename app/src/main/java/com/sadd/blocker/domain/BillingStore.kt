package com.sadd.blocker.domain

/**
 * انتخاب فروشگاه پرداخت بر اساس نصب‌کننده اپ — خالص Kotlin، قابل تست.
 *
 * «سد» از کافه‌بازار یا مایکت نصب می‌شود و پرداخت درون‌برنامه‌ای هر
 * فروشگاه فقط وقتی کار می‌کند که اپ از همان فروشگاه نصب شده باشد.
 * نصب دستی (sideload) یا دیباگ: هیچ فروشگاهی در دسترس نیست و صفحه
 * پرو خرید را غیرفعال نشان می‌دهد.
 */
enum class BillingStore {
    BAZAAR,
    MYKET,
    UNAVAILABLE,
}

object BillingStoreSelector {

    /** پکیج نصب‌کننده کافه‌بازار. */
    const val BAZAAR_INSTALLER: String = "com.farsitel.bazaar"

    /** پکیج نصب‌کننده مایکت. */
    const val MYKET_INSTALLER: String = "ir.mservices.market"

    /** فروشگاه پرداخت برای یک نصب‌کننده؛ نامشخص/دستی → در دسترس نیست. */
    fun storeForInstaller(installerPackage: String?): BillingStore =
        when (installerPackage) {
            BAZAAR_INSTALLER -> BillingStore.BAZAAR
            MYKET_INSTALLER -> BillingStore.MYKET
            else -> BillingStore.UNAVAILABLE
        }
}
