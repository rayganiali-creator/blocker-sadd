package com.sadd.blocker.data.system

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** اپ نصب‌شده با قابلیت باز شدن توسط کاربر. */
data class InstalledAppInfo(
    val packageName: String,
    val label: String,
)

/**
 * لیست اپ‌های نصب‌شده از PackageManager.
 * فقط اپ‌هایی که Intent راه‌اندازی دارند (یعنی کاربر می‌تواند بازشان کند) برمی‌گردند؛
 * پکیج خود «سد» و اپ‌های سیستمی بدون لانچر حذف می‌شوند.
 */
@Singleton
class InstalledAppsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun load(): List<InstalledAppInfo> {
        val pm: PackageManager = context.packageManager
        val ownPackage = context.packageName
        val result = ArrayList<InstalledAppInfo>()
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in installed) {
            if (appInfo.packageName == ownPackage) continue
            if (pm.getLaunchIntentForPackage(appInfo.packageName) == null) continue
            val label = pm.getApplicationLabel(appInfo).toString()
            result.add(InstalledAppInfo(appInfo.packageName, label))
        }
        result.sortBy { it.label.lowercase() }
        return result
    }
}
