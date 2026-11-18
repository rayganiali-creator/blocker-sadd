package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.ui.ModesViewModel
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * حالت بلاک.
 * Normal: صفحه مداخله با فرصت تجدیدنظر و خروج کنترل‌شده.
 * Strict: وسط سشن فعال نمی‌توان قانون را غیرفعال یا حذف کرد و پایان سشن
 * فقط با PIN (اگر تنظیم شده باشد) انجام می‌شود.
 * صادقانه: بلاک واقعی (تشخیص نیت و بلاک وب با Accessibility)
 * هنوز وصل نشده و در فاز بعدی می‌آید؛ حالت‌ها فعلاً نحوه شروع سشن
 * و قفل قوانین وسط سشن را تعیین می‌کنند.
 * محدودیت صادقانه: اپ معمولی نمی‌تواند جلوی حذف نصب یا force-stop را قطعی بگیرد.
 */
@Composable
fun ModesScreen(
    viewModel: ModesViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SaddSpacing.Large),
    ) {
        SaddTopBar(
            "حالت بلاک",
            "حالت سشن‌های بعدی؛ سشن فعال با حالت خودش ادامه می‌یابد.",
            onBack = onBack,
        )
        ModeCard(
            selected = uiState.blockMode == BlockMode.NORMAL,
            title = "Normal — عادی",
            description = "صفحه مداخله نشان داده می‌شود، تاخیر تنظیم‌شده اعمال می‌شود " +
                "و می‌توانی با دکمه بازگشت از اقدام صرف‌نظر کنی.",
            onClick = { viewModel.setBlockMode(BlockMode.NORMAL) },
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        ModeCard(
            selected = uiState.blockMode == BlockMode.STRICT,
            title = "Strict — سخت‌گیرانه",
            description = "وسط سشن فعال نمی‌توان قانون را غیرفعال یا حذف کرد و پایان سشن " +
                "فقط با PIN (اگر تنظیم شده باشد) انجام می‌شود. " +
                "Strict جلوی حذف نصب یا force-stop را نمی‌گیرد؛ " +
                "اپ معمولی چنین قدرتی در اندروید ندارد.",
            onClick = { viewModel.setBlockMode(BlockMode.STRICT) },
        )
        if (uiState.blockMode == BlockMode.STRICT && !uiState.pinSet) {
            Spacer(Modifier.height(SaddSpacing.Large))
            Text(
                "در حالت Strict هنوز PIN تنظیم نکرده‌ای؛ بدون PIN، پایان سشن فقط سخت‌تر است، نه قفل‌شده.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(SaddSpacing.Small))
            SaddButton(
                text = "تنظیم PIN",
                onClick = { onNavigate("pin") },
                style = SaddButtonStyle.SECONDARY,
            )
        }
        Spacer(Modifier.height(SaddSpacing.Large))
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "مکانیزم بلاک واقعی (تشخیص نیت و بلاک وب با Accessibility) هنوز وصل نشده " +
                    "و در فاز بعدی می‌آید؛ تا آن زمان، حالت‌ها فقط شروع سشن و قفل قوانین را تعیین می‌کنند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(SaddSpacing.Large),
            )
        }
        Spacer(Modifier.height(SaddSpacing.Large))
    }
}

@Composable
private fun ModeCard(selected: Boolean, title: String, description: String, onClick: () -> Unit) {
    SaddCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.size(SaddSpacing.Small))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
