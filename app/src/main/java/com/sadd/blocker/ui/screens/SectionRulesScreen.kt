package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.core.SectionDetector
import com.sadd.blocker.ui.SectionRulesViewModel
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * بخش‌های اپ («بخش‌های اپ» از صفحه اپ‌ها)، گروه‌بندی‌شده بر اساس اپ:
 * هر اپ یک سربرگ دارد و زیر آن بخش‌هایش — اول «خود اپ»، بعد جستجو،
 * بعد بقیه بخش‌ها — هر کدام با تاگل خودش. بلاک فقط وسط سشن فعال
 * انجام می‌شود و با BACK از بخش بیرون می‌روید.
 *
 * تشخیص بخش best-effort است (عنوان پنجره + فیلد جستجو + توضیح
 * محتوایی) و ممکن است با آپدیت آن اپ‌ها تغییر کند — متن همین را
 * بالای صفحه می‌گوید تا قول قطعی داده نشود. «خود اپ» تنها گزینه
 * قطعی است (باز شدن اپ = بلاک)؛ ریلز و شورتس چون برچسب‌شان در
 * ناوبری و صفحه اصلی هم هست، ممکن است بیش‌ازحد یا کم‌بلاک کنند.
 */
@Composable
fun SectionRulesScreen(
    viewModel: SectionRulesViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    if (uiState.strictUnlockVisible) {
        StrictUnlockDialog(
            method = uiState.strictUnlockMethod,
            cooldownHours = uiState.strictCooldownHours,
            cooldownRequest = uiState.strictCooldownRequest,
            onRecordCooldownRequest = { viewModel.recordStrictCooldownRequest() },
            onUnlocked = { viewModel.confirmStrictUnlocked() },
            onDismiss = { viewModel.cancelStrictUnlock() },
        )
    }

    MessageSnackbarEffect(
        message = uiState.message,
        snackbarHostState = snackbarHostState,
        onUndo = {},
        onDismiss = { viewModel.dismissMessage() },
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = SaddSpacing.Large),
        ) {
            item {
                SaddTopBar(
                    "بخش‌های اپ",
                    "کل اپ، جستجو، اکسپلور، ریلز و شورتس فقط وسط سشن بلاک می‌شود.",
                    onBack = onBack,
                )
                Text(
                    "«خود اپ» یعنی باز شدن آن اپ وسط سشن بلاک می‌شود. تشخیص جستجو، اکسپلور، ریلز و شورتس بر اساس عنوان پنجره، فیلد جستجو و توضیح محتوایی انجام می‌شود؛ بهترین تلاش است و ممکن است با نسخه‌های جدید این اپ‌ها تغییر کند — ریلز و شورتس گاهی صفحه اصلی را هم بلاک می‌کنند.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(SaddSpacing.Small))
            }
            val grouped = uiState.rules.groupBy { it.packageName }
            for ((packageName, rules) in grouped) {
                item { SectionTitle(appLabelFa(packageName)) }
                val ordered = rules.sortedBy { rule -> presetOrder(rule.sectionKey) }
                items(ordered, key = { it.packageName + "/" + it.sectionKey }) { rule ->
                    SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        SaddSwitchRow(
                            label = rule.labelFa,
                            description = sectionDescription(rule.sectionKey),
                            checked = rule.enabled,
                            onCheckedChange = {
                                viewModel.setEnabled(rule.packageName, rule.sectionKey, it)
                            },
                            modifier = Modifier.padding(SaddSpacing.Medium),
                        )
                    }
                }
            }
            if (uiState.rules.isEmpty()) {
                item { EmptyState("هنوز بخشی تعریف نشده است.") }
            }
            item { Spacer(Modifier.height(SaddSpacing.Large)) }
        }
    }
}

private fun appLabelFa(packageName: String): String {
    return when (packageName) {
        "org.telegram.messenger" -> "تلگرام"
        "com.instagram.android" -> "اینستاگرام"
        "com.google.android.youtube" -> "یوتیوب"
        else -> packageName
    }
}

/** ترتیب نمایش بخش‌ها = ترتیب SectionDetector.PRESETS (در هر اپ اول «خود اپ»). */
private fun presetOrder(sectionKey: String): Int {
    val index = SectionDetector.PRESETS.indexOfFirst { it.sectionKey == sectionKey }
    return if (index >= 0) index else Int.MAX_VALUE
}

private fun sectionDescription(sectionKey: String): String {
    return when (sectionKey) {
        SectionDetector.SECTION_TELEGRAM_APP,
        SectionDetector.SECTION_INSTAGRAM_APP,
        SectionDetector.SECTION_YOUTUBE_APP,
        -> "باز شدن اپ وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_TELEGRAM_SEARCH,
        SectionDetector.SECTION_INSTAGRAM_SEARCH,
        SectionDetector.SECTION_YOUTUBE_SEARCH,
        -> "ورود به جستجو وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_INSTAGRAM_EXPLORE -> "ورود به اکسپلور وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_INSTAGRAM_REELS -> "باز شدن ریلز وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_YOUTUBE_SHORTS -> "باز شدن شورتس وسط سشن بلاک می‌شود"
        else -> sectionKey
    }
}
