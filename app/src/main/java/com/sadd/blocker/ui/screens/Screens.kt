package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Placeholder واقعی فاز ۱: مسیر ناوبری + تیتر + توضیح اینکه در فاز بعد ساخته می‌شود.
 * این یک «دکمه مرده برای فیچر نهایی» نیست؛ صرفاً اسکلت ناوبری فاز ۱ است.
 */
@Composable
fun PlaceholderScreen(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable fun AppsScreen() = PlaceholderScreen(
    "بلاک اپ‌ها",
    "فاز ۲: لیست اپ‌های نصب‌شده، جستجو، انتخاب چندتایی و فعال/غیرفعال کردن هر قانون.",
)

@Composable fun WebsitesScreen() = PlaceholderScreen(
    "بلاک وب‌سایت‌ها",
    "فاز ۴: افزودن دامنه/زیردامنه، نرمال‌سازی و انتخاب VPN لوکال یا Accessibility.",
)

@Composable fun KeywordsScreen() = PlaceholderScreen(
    "مدیریت کلمات",
    "فاز ۲ و ۳: کلمه و عبارت فارسی/انگلیسی، دسته‌بندی و تطابق whole-word.",
)

@Composable fun SessionsScreen() = PlaceholderScreen(
    "سشن‌های فعال",
    "فاز ۵: باقی‌مانده زنده، تمدید ۱۵ دقیقه‌ای و پایان سشن (در Strict با PIN).",
)

@Composable fun SchedulingScreen() = PlaceholderScreen(
    "زمان‌بندی",
    "فاز ۵: دوره‌های تکرارشونده با WorkManager و بازسازی بعد از ری‌استارت.",
)

@Composable fun ModesScreen() = PlaceholderScreen(
    "حالت بلاک",
    "فاز ۴: Normal و Strict، با توضیح صادقانه محدودیت‌های اندروید.",
)

@Composable fun DelayScreen() = PlaceholderScreen(
    "تنظیمات تاخیر",
    "فاز ۴: ۰ / ۳ / ۵ / ۱۰ / ۱۵ ثانیه، پیش‌فرض ۵ ثانیه، و تاخیر per-rule.",
)

@Composable fun PinScreen() = PlaceholderScreen(
    "PIN و امنیت",
    "فاز ۶: تنظیم PIN با هش SHA-256 و salt؛ هرگز plaintext ذخیره نمی‌شود.",
)

@Composable fun DiagnosticsScreen() = PlaceholderScreen(
    "مجوزها و عیب‌یابی",
    "فاز ۶: وضعیت Accessibility، Usage، VPN، سرویس پیش‌زمینه، دیتابیس و آخرین رویداد.",
)

@Composable fun BackupScreen() = PlaceholderScreen(
    "بکاپ و ریستور",
    "فاز ۶: خروجی JSON نسخه‌دار با اعتبارسنجی کامل قبل از import.",
)

@Composable fun AboutScreen() = PlaceholderScreen(
    "درباره و مستندات",
    "فاز ۸: نسخه ۳٫۰٫۰، محدودیت‌های اندروید و راهنمای نصب و تست.",
)
