package com.sadd.blocker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sadd.blocker.BuildConfig
import com.sadd.blocker.R
import com.sadd.blocker.ui.ThemeViewModel
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * درباره و مستندات.
 * نسخه، معماری و محدودیت‌های صادقانه اندروید؛ مستندات کامل در ARCHITECTURE.md ریپو است.
 * صادقانه: AccessibilityService برای تشخیص نیت و بلاک وب وصل است و فقط وسط
 * سشن فعال کار می‌کند؛ WorkManager برای اجرای خودکار زمان‌بندی هنوز وصل نشده
 * است — در فاز بعدی می‌آید.
 * بلاک وب با Accessibility انجام می‌شود، نه با VPN.
 */
@Composable
fun AboutScreen(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SaddSpacing.Large),
    ) {
        SaddTopBar("درباره و مستندات", onBack = onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.sadd_bear),
                contentDescription = "خرس سد",
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.size(SaddSpacing.Small))
            Column {
                Text("سد — Blocker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "نسخه ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) — ساخته‌شده از صفر",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Large))
        ThemePickerSection(themeViewModel = themeViewModel)
        Spacer(Modifier.height(SaddSpacing.Large))
        Text(
            "«سد» کمک می‌کند عادت‌های دیجیتال ناخواسته را با تصمیم آگاهانه قطع کنی. " +
                "خواندن مقاله یا پیام هرگز بلاک نمی‌شود؛ فقط جستجوی عمدیِ سابمیت‌شده، " +
                "ناوبری به دامنه ممنوعه یا باز کردن اپ ممنوعه — و فقط وسط سشن فعال.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SectionTitle("معماری")
        Text(
            "Kotlin + Jetpack Compose + Material 3، MVVM، Room برای قوانین و سشن‌ها، " +
                "DataStore برای تنظیمات و PIN هش‌شده، و موتور تشخیص نیت قانون‌محور قطعی " +
                "(بدون مدل عصبی). مکانیزم بلاک واقعی — AccessibilityService برای تشخیص نیت " +
                "و بلاک وب — وصل است و فقط وسط سشن فعال کار می‌کند؛ " +
                "WorkManager برای اجرای خودکار زمان‌بندی هنوز وصل نشده و در فاز بعدی می‌آید. " +
                "بلاک وب با Accessibility انجام " +
                "می‌شود، نه با VPN.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SectionTitle("محدودیت‌های صادقانه اندروید")
        val limits = listOf(
            "حذف نصب و force-stop را هیچ اپ معمولی نمی‌تواند قطعی بگیرد؛ تنها قفل مطلق Device Owner است که معمولاً ریست فکتوری می‌خواهد.",
            "محافظت از حذف نصب (مدیر دستگاه، در صفحه حالت‌ها) حذف نصب را مسدود می‌کند، " +
                "اما غیرفعال‌کردن آن از تنظیمات سیستم همیشه ممکن است و فقط یک هشدار نشان می‌دهد؛ " +
                "جلوی force-stop را هم نمی‌گیرد.",
            "بلاک وب با Accessibility به خواندن آدرس یا کادر جستجوی مرورگر وابسته است؛ " +
                "در همه مرورگرها به یک اندازه کار نمی‌کند و به اندازه VPN قطعی نیست.",
            "تشخیص سابمیت جستجو در اپ‌های ناشناس محتاطانه است تا مقاله اشتباهی بلاک نشود؛ بعضی جستجوها ممکن است از دست بروند.",
            "سامسونگ و بهینه‌سازی باتری سرویس پس‌زمینه را می‌کشند؛ معافیت باتری و WorkManager احتمال را کم می‌کند، صفر نمی‌کند.",
            "شروع دقیق زمان‌بندی بدون آلارم دقیق تضمین ندارد و ممکن است تا ۱۵ دقیقه دیر شود.",
        )
        for (limit in limits) {
            Text("• $limit", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = SaddSpacing.Small))
        }
        SectionTitle("حریم خصوصی")
        Text(
            "همه‌چیز لوکال پردازش می‌شود. هیچ حساب کاربری، هیچ آنالیتیکس و هیچ ارسالی " +
                "از جستجوها، تاریخچه مرور یا محتوای صفحه به بیرون وجود ندارد.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(SaddSpacing.Large))
    }
}
