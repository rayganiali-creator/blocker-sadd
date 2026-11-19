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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.domain.model.DelayOptions
import com.sadd.blocker.ui.DelayViewModel
import com.sadd.blocker.ui.RuleDelayOverride
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.ChipPurple
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.screens.StrictUnlockDialog

/**
 * تنظیمات تاخیر.
 * تاخیر سراسری: ۰ / ۳ / ۵ / ۱۰ / ۱۵ ثانیه (پیش‌فرض ۰ — فوری).
 * تاخیر per-rule از صفحه همان قانون تنظیم می‌شود و اینجا قابل بازنشانی به سراسری است.
 * وسط سشن Strict، تغییر تاخیر (سراسری یا اختصاصی قانون فعال) فقط بعد
 * از بازگشایی Strict انجام می‌شود.
 */
@Composable
fun DelayScreen(
    viewModel: DelayViewModel = hiltViewModel(),
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
            "تنظیمات تاخیر",
            "بعد از تشخیص، چند ثانیه فرصت فکر کردن داری.",
            onBack = onBack,
        )
        // تاخیر سراسری داخل کارت Detox با کنترل قطعه‌ای قرصی.
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SaddIconChip(icon = Icons.Filled.Timer, color = ChipMint)
                    Spacer(Modifier.width(SaddSpacing.Medium))
                    Text(
                        "تاخیر سراسری",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(SaddSpacing.Medium))
                val delayLabels = DelayOptions.VALUES.map { seconds ->
                    if (seconds == 0) "بدون تاخیر" else "$seconds ثانیه"
                }
                SaddSegmentedControl(
                    options = delayLabels,
                    selectedIndex = DelayOptions.VALUES.indexOf(uiState.globalDelay).coerceAtLeast(0),
                    onSelect = { viewModel.setGlobalDelay(DelayOptions.VALUES[it]) },
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                Text(
                    "(پیش‌فرض)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (uiState.appOverrides.isNotEmpty()) {
            SectionTitle("تاخیر اختصاصی اپ‌ها")
            for (override in uiState.appOverrides) {
                OverrideRow(override) { viewModel.resetAppDelay(override.key) }
            }
        }
        if (uiState.websiteOverrides.isNotEmpty()) {
            SectionTitle("تاخیر اختصاصی وب‌سایت‌ها")
            for (override in uiState.websiteOverrides) {
                OverrideRow(override) { viewModel.resetWebsiteDelay(override.key) }
            }
        }
        if (uiState.keywordOverrides.isNotEmpty()) {
            SectionTitle("تاخیر اختصاصی کلمات")
            for (override in uiState.keywordOverrides) {
                OverrideRow(override) { viewModel.resetKeywordDelay(override.key) }
            }
        }
        if (uiState.appOverrides.isEmpty() && uiState.websiteOverrides.isEmpty() && uiState.keywordOverrides.isEmpty()) {
            EmptyState("هیچ قانون تاخیر اختصاصی ندارد؛ همه از تاخیر سراسری استفاده می‌کنند.", icon = Icons.Filled.Schedule)
        }
        Spacer(Modifier.height(SaddSpacing.Large))
    }
    // وسط سشن Strict، تغییر تاخیر فقط بعد از بازگشایی Strict انجام می‌شود.
    if (uiState.strictUnlockVisible) {
        StrictUnlockDialog(
            method = uiState.strictUnlockMethod,
            cooldownHours = uiState.strictCooldownHours,
            cooldownRequest = uiState.strictCooldownRequest,
            onUnlocked = { viewModel.confirmStrictUnlocked() },
            onRecordCooldownRequest = { viewModel.recordStrictCooldownRequest() },
            onDismiss = { viewModel.cancelStrictUnlock() },
        )
    }
}

@Composable
private fun OverrideRow(override: RuleDelayOverride, onReset: () -> Unit) {
    SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaddIconChip(icon = Icons.Filled.Schedule, color = ChipPurple)
            Spacer(Modifier.width(SaddSpacing.Medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(override.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${override.seconds} ثانیه",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onReset) {
                Text("بازنشانی به سراسری")
            }
        }
    }
}
