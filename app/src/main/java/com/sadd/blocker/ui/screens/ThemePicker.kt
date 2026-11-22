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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import com.sadd.blocker.ui.theme.saddPrimaryTextColor
import com.sadd.blocker.ui.theme.paletteLabelFa
import com.sadd.blocker.ui.screens.StrictUnlockDialog

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
    onNavigate: (String) -> Unit = {},
) {
    val themeState by themeViewModel.uiState.collectAsStateWithLifecycle()
    SectionTitle("تم")
    // انتخاب روشن/تیره/سیستمی با کنترل قطعه‌ای Liquid Glass.
    val themeOptions = listOf(
        AppTheme.LIGHT to "روشن",
        AppTheme.DARK to "تیره",
        AppTheme.SYSTEM to "سیستمی (پیروی از سیستم)",
    )
    SaddSegmentedControl(
        options = themeOptions.map { it.second },
        selectedIndex = themeOptions.indexOfFirst { it.first == themeState.theme }.coerceAtLeast(0),
        onSelect = { themeViewModel.setTheme(themeOptions[it].first) },
    )
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
                locked = palette != ColorTheme.SADD && !themeState.isPro,
                onClick = { themeViewModel.setColorTheme(palette) },
            )
        }
    }
    Text(
        "صفحه بلاک همیشه تیره است، با رنگ‌بندی انتخاب‌شده.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // وسط سشن Strict، تغییر تم/رنگ‌بندی فقط بعد از بازگشایی Strict انجام می‌شود.
    if (themeState.strictUnlockVisible) {
        StrictUnlockDialog(
            method = themeState.strictUnlockMethod,
            cooldownHours = themeState.strictCooldownHours,
            cooldownRequest = themeState.strictCooldownRequest,
            onUnlocked = { themeViewModel.confirmStrictUnlocked() },
            onRecordCooldownRequest = { themeViewModel.recordStrictCooldownRequest() },
            onDismiss = { themeViewModel.cancelStrictUnlock() },
        )
    }
    if (themeState.proPromptVisible) {
        ProPromptDialog(
            text = "این رنگ‌بندی در سد پرو است.",
            onGoToPro = {
                themeViewModel.dismissProPrompt()
                onNavigate("pro")
            },
            onDismiss = { themeViewModel.dismissProPrompt() },
        )
    }
}

/** کارت نرم و بالشتی یک رنگ‌بندی: سه نقطه رنگ + نام فارسی؛ انتخاب‌شده قاب دارد. رنگ‌بندی پرو در رایگان آیکون قفل دارد. */
@Composable
private fun PaletteCard(selected: Boolean, palette: ColorTheme, locked: Boolean, onClick: () -> Unit) {
    // نقطه‌ها از نسخه روشن پالت می‌آیند تا در هر دو حالت تم خوانا باشند.
    val scheme = colorSchemeFor(palette, dark = false)
    val dots = listOf(scheme.primary, scheme.secondary, scheme.tertiary)
    val label = paletteLabelFa(palette)
    SaddCard(
        modifier = Modifier.then(
            if (selected) {
                Modifier.border(
                    2.dp,
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
            modifier = Modifier.padding(horizontal = SaddSpacing.Medium, vertical = SaddSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (dot in dots) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(lerp(dot, Color.White, 0.35f), dot),
                            ),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                )
            }
            Spacer(Modifier.size(SaddSpacing.Small))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    saddPrimaryTextColor()
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (locked) {
                Spacer(Modifier.size(SaddSpacing.Small))
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Lock,
                    contentDescription = "سد پرو",
                    modifier = Modifier.size(SaddSpacing.Medium),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
