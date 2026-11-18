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
                Text("سد — \u2066Blocker\u2069", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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
            "«سد» عادت‌های دیجیتال را با تصمیم آگاهانه قطع می‌کند؛ خواندن مقاله یا پیام هرگز بلاک نمی‌شود — " +
                "فقط جستجوی عمدی، ناوبری ممنوعه و باز کردن اپ ممنوعه، وسط سشن فعال.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SectionTitle("معماری")
        Text(
            "Kotlin + Compose + Material 3، Room و DataStore؛ تشخیص نیت قانون‌محور، بدون مدل عصبی. " +
                "بلاک وب با Accessibility است، نه با VPN. اجرای خودکار زمان‌بندی (WorkManager) هنوز وصل نشده است.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SectionTitle("محدودیت‌های صادقانه اندروید")
        val limits = listOf(
            "حذف نصب و \u2066force-stop\u2069 را هیچ اپ معمولی نمی‌تواند قطعی بگیرد؛ تنها قفل مطلق \u2066Device Owner\u2069 است که معمولاً ریست فکتوری می‌خواهد.",
            "مدیر دستگاه حذف نصب را مسدود می‌کند، اما غیرفعال‌کردنش از تنظیمات سیستم همیشه ممکن است؛ جلوی \u2066force-stop\u2069 را نمی‌گیرد.",
            "نگهبان فقط وقتی Accessibility روشن است کار می‌کند؛ ضربه خیلی سریع ممکن است جلو بزند و صفحه غیرفعال‌سازی تنظیمات مسدود نمی‌شود.",
            "بلاک وب به خواندن نوار آدرس مرورگر وابسته است؛ در همه مرورگرها یکسان نیست و به اندازه VPN قطعی نیست.",
            "تشخیص سابمیت در اپ‌های ناشناس محتاطانه است؛ بعضی جستجوها ممکن است از دست بروند.",
            "سامسونگ و بهینه‌سازی باتری سرویس پس‌زمینه را می‌کشند؛ معافیت باتری و WorkManager احتمال را کم می‌کند، صفر نمی‌کند.",
            "شروع دقیق زمان‌بندی بدون آلارم دقیق تضمین ندارد و ممکن است تا ۱۵ دقیقه دیر شود.",
        )
        for (limit in limits) {
            Text("• $limit", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = SaddSpacing.Small))
        }
        SectionTitle("حریم خصوصی")
        Text(
            "همه‌چیز لوکال است؛ هیچ ارسالی از جستجوها یا تاریخچه مرور به بیرون نیست.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(SaddSpacing.Large))
    }
}
