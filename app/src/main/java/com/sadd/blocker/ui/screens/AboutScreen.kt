package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sadd.blocker.BuildConfig
import com.sadd.blocker.ui.ThemeViewModel

/**
 * درباره و مستندات — فاز ۲.
 * نسخه، معماری و محدودیت‌های صادقانه اندروید؛ مستندات کامل در ARCHITECTURE.md ریپو است.
 */
@Composable
fun AboutScreen(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val themeState by themeViewModel.uiState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ScreenHeader("درباره و مستندات", onBack = onBack)
        Text("سد — Blocker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "نسخه ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) — ساخته‌شده از صفر",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        SectionTitle("تم")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "تم تیره",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = themeState.darkTheme,
                onCheckedChange = { themeViewModel.setDarkTheme(it) },
            )
        }
        Text(
            "تم روشن و تیره کامل پشتیبانی می‌شود؛ صفحه بلاک همیشه تیره سرمه‌ای است.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "«سد» کمک می‌کند عادت‌های دیجیتال ناخواسته را با تصمیم آگاهانه قطع کنی. " +
                "خواندن مقاله یا پیام هرگز بلاک نمی‌شود؛ فقط جستجوی عمدیِ سابمیت‌شده، " +
                "ناوبری به دامنه ممنوعه یا باز کردن اپ ممنوعه — و فقط وسط سشن فعال.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SectionTitle("معماری")
        Text(
            "Kotlin + Jetpack Compose + Material 3، MVVM، Room برای قوانین و سشن‌ها، " +
                "DataStore برای تنظیمات و PIN هش‌شده، WorkManager برای زمان‌بندی، " +
                "AccessibilityService برای تشخیص نیت و VPN لوکال برای بلاک دامنه.",
            style = MaterialTheme.typography.bodyMedium,
        )
        SectionTitle("محدودیت‌های صادقانه اندروید")
        val limits = listOf(
            "حذف نصب و force-stop را هیچ اپ معمولی نمی‌تواند قطعی بگیرد؛ تنها قفل مطلق Device Owner است که معمولاً ریست فکتوری می‌خواهد.",
            "بلاک وب بدون VPN در همه مرورگرها ممکن نیست؛ Chrome آدرس را به Accessibility نمی‌دهد.",
            "تشخیص سابمیت جستجو در اپ‌های ناشناس محتاطانه است تا مقاله اشتباهی بلاک نشود؛ بعضی جستجوها ممکن است از دست بروند.",
            "سامسونگ و بهینه‌سازی باتری سرویس پس‌زمینه را می‌کشند؛ معافیت باتری و WorkManager احتمال را کم می‌کند، صفر نمی‌کند.",
            "شروع دقیق زمان‌بندی بدون آلارم دقیق تضمین ندارد و ممکن است تا ۱۵ دقیقه دیر شود.",
        )
        for (limit in limits) {
            Text("• $limit", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
        }
        SectionTitle("حریم خصوصی")
        Text(
            "همه‌چیز لوکال پردازش می‌شود. هیچ حساب کاربری، هیچ آنالیتیکس و هیچ ارسالی " +
                "از جستجوها، تاریخچه مرور یا محتوای صفحه به بیرون وجود ندارد.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
    }
}
