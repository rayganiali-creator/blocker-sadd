package com.sadd.blocker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.ui.SchedulingViewModel
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.ChipPurple
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddGlassSurfaceColor
import com.sadd.blocker.ui.theme.saddSpecularBorderColor

private val WEEK_DAYS = listOf(
    "SAT" to "شنبه",
    "SUN" to "یکشنبه",
    "MON" to "دوشنبه",
    "TUE" to "سه‌شنبه",
    "WED" to "چهارشنبه",
    "THU" to "پنجشنبه",
    "FRI" to "جمعه",
)

/**
 * زمان‌بندی: ذخیره و مدیریت دوره‌های تکرارشونده در Room.
 * پنجره شبانه (پایان زودتر از شروع) یعنی ادامه تا روز بعد.
 * اجرای خودکار دوره‌ها با WorkManager در فاز ۵ وصل می‌شود.
 */
@Composable
fun SchedulingScreen(
    viewModel: SchedulingViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    if (uiState.strictUnlockVisible) {
        StrictUnlockDialog(
            method = uiState.strictUnlockMethod,
            cooldownHours = uiState.strictCooldownHours,
            cooldownRequest = uiState.strictCooldownRequest,
            onRecordCooldownRequest = { viewModel.recordStrictCooldownRequest() },
            onUnlocked = { viewModel.confirmStrictUnlocked() },
            onDismiss = { viewModel.cancelStrictUnlock() },
        )
    }

    var selectedDays by rememberSaveable { mutableStateOf(listOf<String>()) }
    var startText by rememberSaveable { mutableStateOf("08:00") }
    var endText by rememberSaveable { mutableStateOf("22:00") }
    var strict by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    MessageSnackbarEffect(
        message = uiState.message,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.undoRemove() },
        onDismiss = { viewModel.dismissMessage() },
    )

    if (pendingDelete != null) {
        SaddDialog(
            title = "حذف زمان‌بندی",
            text = "این زمان‌بندی حذف شود؟",
            confirmText = "حذف",
            onConfirm = {
                viewModel.remove(pendingDelete!!)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = SaddSpacing.Large),
        ) {
            item {
                SaddTopBar("زمان‌بندی", "دوره‌های تکرارشونده بلاک؛ اجرای خودکار در فاز ۵ وصل می‌شود.", onBack = onBack)
                // فرم افزودن به سبک Liquid Glass: داخل کارت با حاشیه مویی؛ چیپ‌های روز قرصی با انتخاب توپر.
                SaddCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                        Text("روزهای هفته:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(SaddSpacing.Small))
                        for (row in WEEK_DAYS.chunked(4)) {
                            Row {
                                for ((code, label) in row) {
                                    FilterChip(
                                        selected = selectedDays.contains(code),
                                        onClick = {
                                            selectedDays = if (selectedDays.contains(code)) {
                                                selectedDays - code
                                            } else {
                                                selectedDays + code
                                            }
                                        },
                                        label = { Text(label) },
                                        modifier = Modifier.padding(end = 4.dp),
                                        shape = RoundedCornerShape(SaddRadius.Button),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = saddGlassSurfaceColor(),
                                            labelColor = MaterialTheme.colorScheme.onSurface,
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                        border = BorderStroke(SaddGlass.BorderWidth, saddSpecularBorderColor()),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Row {
                            SaddTextField(
                                value = startText,
                                onValueChange = { startText = it },
                                label = "شروع (HH:MM)",
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(SaddSpacing.Small))
                            SaddTextField(
                                value = endText,
                                onValueChange = { endText = it },
                                label = "پایان (HH:MM)",
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(SaddSpacing.Small))
                        SaddSwitchRow(
                            label = "سخت‌گیرانه (\u2066Strict\u2069)",
                            checked = strict,
                            onCheckedChange = { strict = it },
                        )
                        Spacer(Modifier.height(SaddSpacing.Small))
                        SaddButton(
                            text = "افزودن زمان‌بندی",
                            onClick = {
                                viewModel.addSchedule(selectedDays.toSet(), startText, endText, strict)
                                selectedDays = emptyList()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            if (uiState.schedules.isEmpty()) {
                item { EmptyState("هنوز زمان‌بندی‌ای اضافه نشده است.", icon = Icons.Filled.Schedule) }
            } else {
                item { SectionTitle("زمان‌بندی‌ها (${uiState.schedules.size})") }
            }
            items(uiState.schedules, key = { it.id }) { schedule ->
                val dayLabels = schedule.daysOfWeek.split(",")
                    .mapNotNull { code -> WEEK_DAYS.firstOrNull { it.first == code }?.second }
                    .joinToString("، ")
                val overnight = schedule.endMinute < schedule.startMinute
                val timeLabel = if (overnight) {
                    "${TimeFormat.formatMinuteOfDay(schedule.startMinute)} تا ${TimeFormat.formatMinuteOfDay(schedule.endMinute)} (تا روز بعد)"
                } else {
                    "${TimeFormat.formatMinuteOfDay(schedule.startMinute)} تا ${TimeFormat.formatMinuteOfDay(schedule.endMinute)}"
                }
                SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // چیپ آیکون زمان‌بندی به سبک Liquid Glass: بنفش برای Strict، مینت برای معمولی.
                            SaddIconChip(
                                icon = Icons.Filled.Schedule,
                                color = if (schedule.strict) ChipPurple else ChipMint,
                            )
                            Spacer(Modifier.width(SaddSpacing.Small))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    dayLabels.ifEmpty { "بدون روز" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    timeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (schedule.strict) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Strict") },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = saddGlassSurfaceColor(),
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    border = BorderStroke(SaddGlass.BorderWidth, saddSpecularBorderColor()),
                                )
                            }
                            Switch(
                                checked = schedule.enabled,
                                onCheckedChange = { viewModel.setEnabled(schedule.id, it) },
                                colors = saddSwitchColors(),
                            )
                            IconButton(onClick = { pendingDelete = schedule.id }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف زمان‌بندی")
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(SaddSpacing.Large)) }
        }
    }
}
