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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sadd.blocker.domain.model.DelayOptions
import com.sadd.blocker.ui.DelayViewModel
import com.sadd.blocker.ui.RuleDelayOverride

/**
 * تنظیمات تاخیر — فاز ۲.
 * تاخیر سراسری: ۰ / ۳ / ۵ / ۱۰ / ۱۵ ثانیه (پیش‌فرض ۵).
 * تاخیر per-rule از صفحه همان قانون تنظیم می‌شود و اینجا قابل بازنشانی به سراسری است.
 */
@Composable
fun DelayScreen(
    viewModel: DelayViewModel = hiltViewModel(),
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
            "تنظیمات تاخیر",
            "بعد از تشخیص، چند ثانیه فرصت فکر کردن داری.",
            onBack = onBack,
        )
        Text(
            "تاخیر سراسری",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        for (seconds in DelayOptions.VALUES) {
            val label = if (seconds == 0) "بدون تاخیر" else "$seconds ثانیه"
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = uiState.globalDelay == seconds,
                    onClick = { viewModel.setGlobalDelay(seconds) },
                )
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (seconds == DelayOptions.DEFAULT_SECONDS) {
                    Spacer(Modifier.width(8.dp))
                    Text("(پیش‌فرض)", style = MaterialTheme.typography.bodySmall)
                }
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
            EmptyState("هیچ قانون تاخیر اختصاصی ندارد؛ همه از تاخیر سراسری استفاده می‌کنند.")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OverrideRow(override: RuleDelayOverride, onReset: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(override.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("${override.seconds} ثانیه", style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onReset) {
                Text("بازنشانی به سراسری")
            }
        }
    }
}
