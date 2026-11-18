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
import com.sadd.blocker.ui.SectionRulesViewModel
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * بخش‌های اپ («بخش‌های اپ» از صفحه اپ‌ها).
 * هر بخش یک تاگل دارد: جستجوی تلگرام، جستجوی اینستاگرام، اکسپلور
 * اینستاگرام، جستجوی یوتیوب. بلاک فقط وسط سشن فعال انجام می‌شود و
 * با BACK از بخش بیرون می‌روید.
 *
 * تشخیص بخش best-effort است (عنوان پنجره + فیلد جستجو + توضیح
 * محتوایی) و ممکن است با آپدیت آن اپ‌ها تغییر کند — متن همین را
 * بالای صفحه می‌گوید تا قول قطعی داده نشود.
 */
@Composable
fun SectionRulesScreen(
    viewModel: SectionRulesViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
                    "جستجو و اکسپلور اپ‌ها فقط وسط سشن بلاک می‌شود.",
                    onBack = onBack,
                )
                Text(
                    "تشخیص بخش بر اساس عنوان پنجره، فیلد جستجو و توضیح محتوایی انجام می‌شود؛ بهترین تلاش است و ممکن است با نسخه‌های جدید این اپ‌ها تغییر کند.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(SaddSpacing.Small))
            }
            val grouped = uiState.rules.groupBy { it.packageName }
            for ((packageName, rules) in grouped) {
                item { SectionTitle(appLabelFa(packageName)) }
                items(rules, key = { it.packageName + "/" + it.sectionKey }) { rule ->
                    SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        SaddSwitchRow(
                            label = rule.labelFa,
                            description = rule.packageName,
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
