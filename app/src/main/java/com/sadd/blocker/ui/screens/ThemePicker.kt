package com.sadd.blocker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.data.datastore.AppTheme
import com.sadd.blocker.data.datastore.ColorTheme
import com.sadd.blocker.ui.ThemeViewModel
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.colorSchemeFor
import com.sadd.blocker.ui.theme.paletteLabelFa

/**
 * انتخاب تم برنامه (روشن / تیره / سیستمی) + رنگ‌بندی — مشترک بین صفحه درباره و عیب‌یابی.
 * مقدار از DataStore خوانده و همان‌جا نوشته می‌شود؛ MainActivity تم را جمع می‌کند
 * و تغییر آن کل اپ را بی‌درنگ بازسازی می‌کند. رنگ‌بندی و روشن/تیره دو انتخاب
 * جدا هستند: رنگ‌بندی پالت را عوض می‌کند و روشن/تیره نسخه روشن یا تیره همان
 * پالت را می‌دهد.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    SectionTitle("رنگ‌بندی")
    Text(
        "رنگ‌بندی با روشن/تیره جداست؛ هر رنگ‌بندی روشن و تیره خودش را دارد و همه صفحه‌ها — داشبورد، سشن‌ها، قوانین، حالت‌ها، درباره و صفحه بلاک — همان را می‌گیرند.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.size(SaddSpacing.Small))
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
        verticalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
    ) {
        for (palette in ColorTheme.entries) {
            PaletteCard(
                selected = themeState.colorTheme == palette,
                palette = palette,
                onClick = { themeViewModel.setColorTheme(palette) },
            )
        }
    }
    Text(
        "صفحه بلاک همیشه تیره است، با رنگ‌بندی انتخاب‌شده.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** کارت نرم و بالشتی یک رنگ‌بندی: سه نقطه رنگ + نام فارسی؛ انتخاب‌شده قاب دارد. */
@Composable
private fun PaletteCard(selected: Boolean, palette: ColorTheme, onClick: () -> Unit) {
    // نقطه‌ها از نسخه روشن پالت می‌آیند تا در هر دو حالت تم خوانا باشند.
    val scheme = colorSchemeFor(palette, dark = false)
    val dots = listOf(scheme.primary, scheme.secondary, scheme.tertiary)
    val label = paletteLabelFa(palette)
    SaddCard(
        modifier = Modifier.then(
            if (selected) {
                Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(SaddRadius.Card),
                )
            } else {
                Modifier
            },
        ),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SaddSpacing.Medium, vertical = SaddSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (dot in dots) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(dot),
                )
            }
            Spacer(Modifier.size(SaddSpacing.Small))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}
