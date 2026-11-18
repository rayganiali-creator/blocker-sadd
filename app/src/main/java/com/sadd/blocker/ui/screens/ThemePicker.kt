package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.data.datastore.AppTheme
import com.sadd.blocker.ui.ThemeViewModel
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * انتخاب تم برنامه (روشن / تیره / سیستمی) — مشترک بین صفحه درباره و عیب‌یابی.
 * مقدار از DataStore خوانده و همان‌جا نوشته می‌شود؛ MainActivity تم را جمع می‌کند
 * و تغییر آن کل اپ را بی‌درنگ بازسازی می‌کند.
 */
@Composable
fun ThemePickerSection(
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val themeState by themeViewModel.uiState.collectAsStateWithLifecycle()
    SectionTitle("تم")
    for ((theme, label) in listOf(
        AppTheme.LIGHT to "روشن",
        AppTheme.DARK to "تیره",
        AppTheme.SYSTEM to "سیستمی (پیروی از سیستم)",
    )) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = themeState.theme == theme,
                onClick = { themeViewModel.setTheme(theme) },
            )
            Spacer(Modifier.size(SaddSpacing.Small))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
    Text(
        "تم روشن و تیره کامل پشتیبانی می‌شود؛ صفحه بلاک همیشه تیره سرمه‌ای است.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
