package com.sadd.blocker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.R
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.domain.model.SessionDurations
import com.sadd.blocker.ui.DashboardViewModel
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * داشبورد — طبق موکاپ تاییدشده v2: سرتیتر با خرس «سد»، حلقه سشن،
 * دسترسی سریع، آمار قوانین و سلامت مجوزها.
 * همه اعداد از Room و سیستم خوانده می‌شوند، نه نمایشی.
 * تیکر فقط وقتی صفحه دیده می‌شود می‌دود (ON_RESUME روشن، ON_PAUSE خاموش).
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDuration by rememberSaveable { mutableStateOf(SessionDurations.OPTIONS[1]) }
    var customMinutes by rememberSaveable { mutableStateOf("") }
    var customError by rememberSaveable { mutableStateOf<String?>(null) }
    var endPin by rememberSaveable { mutableStateOf("") }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refreshPermissions()
                    viewModel.setTicking(true)
                }
                Lifecycle.Event.ON_PAUSE -> viewModel.setTicking(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.endPinRequired) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelEndPin() },
            title = { Text("پایان سشن Strict", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("این سشن Strict است. برای پایان آن PIN را وارد کن.")
                    Spacer(Modifier.height(SaddSpacing.Medium))
                    SaddTextField(
                        value = endPin,
                        onValueChange = { endPin = it },
                        label = "PIN",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.confirmEndSession(endPin)
                        endPin = ""
                    },
                ) { Text("تایید و پایان") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelEndPin()
                        endPin = ""
                    },
                ) { Text("لغو") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SaddSpacing.Large),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.sadd_bear),
                contentDescription = "خرس سد",
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.size(SaddSpacing.Small))
            Column(modifier = Modifier.weight(1f)) {
                Text("سد", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(
                    "دستیار آگاهانهٔ دیجیتال — فقط وقتی خودت خواستی، جلوی دسترسی را می‌گیرد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Large))
        MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })

        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val progress = if (uiState.totalMs > 0) {
                    (uiState.remainingMs.toFloat() / uiState.totalMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                SessionRing(progress = progress, remainingText = uiState.remainingText, active = uiState.sessionActive)
                Spacer(Modifier.height(SaddSpacing.Small))
                Text(
                    if (uiState.sessionActive) "محافظت فعال است" else "سشنی فعال نیست",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    if (uiState.sessionActive) "زمان باقی‌مانده جلسه" else "بلاک فقط وسط سشن فعال کار می‌کند",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (uiState.sessionActive && uiState.strict) {
                    Spacer(Modifier.height(SaddSpacing.Small))
                    AssistChip(onClick = {}, label = { Text("Strict — سخت‌گیرانه") })
                }
                Spacer(Modifier.height(SaddSpacing.Medium))
                if (uiState.sessionActive) {
                    SaddButton(
                        text = "پایان سشن",
                        onClick = { viewModel.requestEndSession() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(SaddSpacing.Small))
                    SaddButton(
                        text = "جزئیات سشن",
                        onClick = { onNavigate("sessions") },
                        style = SaddButtonStyle.SECONDARY,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text("مدت سشن:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(SaddSpacing.Small))
                    Row(horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small)) {
                        for (duration in SessionDurations.OPTIONS) {
                            FilterChip(
                                selected = selectedDuration == duration,
                                onClick = { selectedDuration = duration },
                                label = { Text(TimeFormat.durationLabel(duration)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(SaddSpacing.Small))
                    SaddButton(
                        text = "شروع سشن (${TimeFormat.durationLabel(selectedDuration)})",
                        onClick = { viewModel.startSession(selectedDuration) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(SaddSpacing.Medium))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SaddTextField(
                            value = customMinutes,
                            onValueChange = {
                                customMinutes = it
                                customError = null
                            },
                            label = "مدت دلخواه (دقیقه)",
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Spacer(Modifier.size(SaddSpacing.Small))
                        SaddButton(
                            text = "شروع",
                            onClick = {
                                val minutes = customMinutes.trim().toIntOrNull()
                                if (minutes == null || minutes <= 0) {
                                    customError = "مدت را به دقیقه وارد کن."
                                } else {
                                    val durationMs = minutes * 60_000L
                                    if (durationMs < SessionDurations.MIN_MS) {
                                        customError = "حداقل مدت سشن ۳۰ ثانیه است."
                                    } else if (durationMs > SessionDurations.MAX_MS) {
                                        customError = "حداکثر مدت سشن ۹۰ روز (۱۲۹٬۶۰۰ دقیقه) است."
                                    } else {
                                        viewModel.startSession(durationMs)
                                    }
                                }
                            },
                            style = SaddButtonStyle.SECONDARY,
                        )
                    }
                    if (customError != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            customError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (uiState.blockMode == BlockMode.STRICT) {
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            "حالت Strict فعال است — سشن با قوانین سخت‌گیرانه شروع می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(SaddSpacing.Large))
        Row(horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small)) {
            QuickTile(
                icon = Icons.Filled.Apps,
                label = "اپ‌ها",
                onClick = { onNavigate("apps") },
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                icon = Icons.Filled.Language,
                label = "وب‌سایت‌ها",
                onClick = { onNavigate("websites") },
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                icon = Icons.Filled.TextFields,
                label = "کلمات",
                onClick = { onNavigate("rules") },
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                icon = Icons.Filled.Timer,
                label = "سشن‌ها",
                onClick = { onNavigate("sessions") },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(SaddSpacing.Large))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
        ) {
            StatCard("قانون فعال", uiState.activeRulesCount.toString(), Modifier.weight(1f))
            StatCard("اپ‌ها", uiState.appsCount.toString(), Modifier.weight(1f))
            StatCard("وب‌سایت‌ها", uiState.websitesCount.toString(), Modifier.weight(1f))
            StatCard("کلمات", uiState.keywordsCount.toString(), Modifier.weight(1f))
        }

        Spacer(Modifier.height(SaddSpacing.Large))
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                SaddButton(
                    text = "مجوزها و عیب‌یابی",
                    onClick = { onNavigate("diagnostics") },
                    style = SaddButtonStyle.SECONDARY,
                )
            }
        }

        SectionTitle("دسترسی سریع")
        val quickItems = listOf(
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
                horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
            ) {
                for ((label, route) in row) {
                    SaddButton(
                        text = label,
                        onClick = { onNavigate(route) },
                        style = SaddButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(SaddSpacing.Large))
    }
}

@Composable
private fun QuickTile(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SaddCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = SaddSpacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    SaddCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SessionRing(progress: Float, remainingText: String, active: Boolean) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val arcColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.size(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx()),
            )
            if (active) {
                drawArc(
                    color = arcColor,
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
