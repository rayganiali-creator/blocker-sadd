package com.sadd.blocker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.domain.model.SessionDurations
import com.sadd.blocker.ui.DashboardViewModel
import com.sadd.blocker.ui.theme.Mint

/**
 * داشبورد — طبق موکاپ تاییدشده: حلقه سشن، ۳ کارت آمار،
 * دکمه شروع سشن، دسترسی سریع و نشانگر سلامت مجوزها.
 * همه اعداد از Room و سیستم خوانده می‌شوند، نه نمایشی.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDuration by remember { mutableStateOf(SessionDurations.OPTIONS[1]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("سد", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Text(
            "دستیار آگاهانهٔ دیجیتال — فقط وقتی خودت خواستی، جلوی دسترسی را می‌گیرد.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val progress = if (uiState.totalMs > 0) {
                    (uiState.remainingMs.toFloat() / uiState.totalMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                SessionRing(progress = progress, remainingText = uiState.remainingText, active = uiState.sessionActive)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (uiState.sessionActive) "سشن فعال است" else "سشنی فعال نیست",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (uiState.sessionActive && uiState.strict) {
                    Spacer(Modifier.height(4.dp))
                    AssistChip(onClick = {}, label = { Text("Strict — سخت‌گیرانه") })
                }
                Spacer(Modifier.height(12.dp))
                if (uiState.sessionActive) {
                    Button(onClick = { viewModel.endSession() }) {
                        Text("پایان سشن")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { onNavigate("sessions") }) {
                        Text("جزئیات سشن")
                    }
                } else {
                    Text("مدت سشن:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (duration in SessionDurations.OPTIONS) {
                            FilterChip(
                                selected = selectedDuration == duration,
                                onClick = { selectedDuration = duration },
                                label = { Text(TimeFormat.durationLabel(duration)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.startSession(selectedDuration) }) {
                        Text("شروع سشن")
                    }
                    if (uiState.blockMode == BlockMode.STRICT) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "حالت Strict فعال است — سشن با قوانین سخت‌گیرانه شروع می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard("اپ‌ها", uiState.appsCount.toString(), Modifier.weight(1f))
            StatCard("وب‌سایت‌ها", uiState.websitesCount.toString(), Modifier.weight(1f))
            StatCard("کلمات", uiState.keywordsCount.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.missingPermissions > 0) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
            ),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    if (uiState.missingPermissions > 0) {
                        "${uiState.missingPermissions} مجوز نیازمند توجه است"
                    } else {
                        "همه مجوزها فعال‌اند"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "بدون مجوزهای لازم، بلاک کار نمی‌کند. وضعیت دقیق را در عیب‌یابی ببین.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { onNavigate("diagnostics") }) {
                    Text("مجوزها و عیب‌یابی")
                }
            }
        }

        SectionTitle("دسترسی سریع")
        val quickItems = listOf(
            "اپ‌ها" to "apps",
            "وب‌سایت‌ها" to "websites",
            "کلمات" to "rules",
            "سشن‌ها" to "sessions",
            "زمان‌بندی" to "scheduling",
            "حالت بلاک" to "modes",
            "تاخیر" to "delay",
            "PIN و امنیت" to "pin",
            "بکاپ و ریستور" to "backup",
            "درباره" to "about",
        )
        for (row in quickItems.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for ((label, route) in row) {
                    OutlinedButton(
                        onClick = { onNavigate(route) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(label)
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SessionRing(progress: Float, remainingText: String, active: Boolean) {
    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            drawArc(
                color = Color.Gray.copy(alpha = 0.25f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx()),
            )
            if (active) {
                drawArc(
                    color = Mint,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx()),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (active) remainingText else "--:--:--",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text("باقی‌مانده", style = MaterialTheme.typography.bodySmall)
        }
    }
}
