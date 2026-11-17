package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.ui.ModesViewModel

/**
 * حالت بلاک — فاز ۲.
 * Normal: صفحه مداخله با فرصت تجدیدنظر و خروج کنترل‌شده.
 * Strict: غیرفعال کردن قانون وسط سشن و پایان سشن (با PIN) سخت‌تر می‌شود.
 * محدودیت صادقانه: اپ معمولی نمی‌تواند جلوی حذف نصب یا force-stop را قطعی بگیرد.
 */
@Composable
fun ModesScreen(
    viewModel: ModesViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ScreenHeader(
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
        Spacer(Modifier.height(12.dp))
        ModeCard(
            selected = uiState.blockMode == BlockMode.STRICT,
            title = "Strict — سخت‌گیرانه",
            description = "وسط سشن فعال نمی‌توان قانون را غیرفعال کرد و پایان سشن " +
                "فقط با PIN (اگر تنظیم شده باشد) انجام می‌شود. " +
                "Strict جلوی حذف نصب یا force-stop را نمی‌گیرد؛ " +
                "اپ معمولی چنین قدرتی در اندروید ندارد.",
            onClick = { viewModel.setBlockMode(BlockMode.STRICT) },
        )
        if (uiState.blockMode == BlockMode.STRICT && !uiState.pinSet) {
            Spacer(Modifier.height(16.dp))
            Text(
                "در حالت Strict هنوز PIN تنظیم نکرده‌ای؛ بدون PIN، پایان سشن فقط سخت‌تر است، نه قفل‌شده.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { onNavigate("pin") }) {
                Text("تنظیم PIN")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ModeCard(selected: Boolean, title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
