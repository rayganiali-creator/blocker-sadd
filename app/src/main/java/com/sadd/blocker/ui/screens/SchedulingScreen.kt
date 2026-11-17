package com.sadd.blocker.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.ui.SchedulingViewModel

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
 * زمان‌بندی — فاز ۲: ذخیره و مدیریت دوره‌های تکرارشونده در Room.
 * اجرای خودکار دوره‌ها با WorkManager در فاز ۵ وصل می‌شود.
 */
@Composable
fun SchedulingScreen(
    viewModel: SchedulingViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDays by remember { mutableStateOf(setOf<String>()) }
    var startText by remember { mutableStateOf("08:00") }
    var endText by remember { mutableStateOf("22:00") }
    var strict by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        item {
            ScreenHeader("زمان‌بندی", "دوره‌های تکرارشونده بلاک؛ اجرای خودکار در فاز ۵ وصل می‌شود.", onBack = onBack)
            MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })
            Text("روزهای هفته:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            for (row in WEEK_DAYS.chunked(4)) {
                Row {
                    for ((code, label) in row) {
                        FilterChip(
                            selected = selectedDays.contains(code),
                            onClick = {
                                val next = selectedDays.toMutableSet()
                                if (next.contains(code)) next.remove(code) else next.add(code)
                                selectedDays = next
                            },
                            label = { Text(label) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row {
                OutlinedTextField(
                    value = startText,
                    onValueChange = { startText = it },
                    label = { Text("شروع (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = endText,
                    onValueChange = { endText = it },
                    label = { Text("پایان (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = strict, onCheckedChange = { strict = it })
                Spacer(Modifier.width(8.dp))
                Text("Strict (سخت‌گیرانه)")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.addSchedule(selectedDays, startText, endText, strict)
                    selectedDays = emptySet()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("افزودن زمان‌بندی")
            }
        }
        if (uiState.schedules.isEmpty()) {
            item { EmptyState("هنوز زمان‌بندی‌ای اضافه نشده است.") }
        } else {
            item { SectionTitle("زمان‌بندی‌ها (${uiState.schedules.size})") }
        }
        items(uiState.schedules, key = { it.id }) { schedule ->
            val dayLabels = schedule.daysOfWeek.split(",")
                .mapNotNull { code -> WEEK_DAYS.firstOrNull { it.first == code }?.second }
                .joinToString("، ")
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                dayLabels.ifEmpty { "بدون روز" },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "${TimeFormat.formatMinuteOfDay(schedule.startMinute)} تا ${TimeFormat.formatMinuteOfDay(schedule.endMinute)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (schedule.strict) {
                            AssistChip(onClick = {}, label = { Text("Strict") })
                        }
                        Switch(
                            checked = schedule.enabled,
                            onCheckedChange = { viewModel.setEnabled(schedule.id, it) },
                        )
                        IconButton(onClick = { viewModel.remove(schedule.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف زمان‌بندی")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
