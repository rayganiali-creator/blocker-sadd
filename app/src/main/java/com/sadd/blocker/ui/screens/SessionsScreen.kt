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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sadd.blocker.ui.SessionsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * سشن‌های فعال — فاز ۲.
 * باقی‌مانده زنده از endEpochMs، تمدید ۱۵ دقیقه‌ای، پایان سشن؛
 * پایان سشن Strict وقتی PIN تنظیم شده باشد فقط با PIN درست انجام می‌شود.
 * شروع سشن از داشبورد انجام می‌شود؛ زمان‌بندی خودکار در فاز ۵ وصل می‌شود.
 */
@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var pinText by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US) }

    if (uiState.pinRequired) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelPinDialog() },
            title = { Text("پایان سشن Strict") },
            text = {
                Column {
                    Text("این سشن Strict است. برای پایان آن PIN را وارد کن.")
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { pinText = it },
                        label = { Text("PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.endSessionWithPin(pinText)
                        pinText = ""
                    },
                ) { Text("تایید و پایان") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelPinDialog() }) { Text("لغو") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        item {
            ScreenHeader("سشن‌های فعال", "بلاک فقط وسط سشن فعال کار می‌کند.")
            MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })
        }
        if (uiState.active == null) {
            item { EmptyState("سشنی فعال نیست. از داشبورد یک سشن شروع کن.") }
        } else {
            val active = uiState.active!! // else branch guarantees non-null
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            "سشن فعال",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "باقی‌مانده: ${uiState.remainingText}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "شروع: ${dateFormat.format(Date(active.startEpochMs))}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "پایان: ${dateFormat.format(Date(active.endEpochMs))}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (active.strict) {
                            Spacer(Modifier.height(8.dp))
                            AssistChip(onClick = {}, label = { Text("Strict — سخت‌گیرانه") })
                        }
                        Spacer(Modifier.height(12.dp))
                        Row {
                            Button(onClick = { viewModel.extendSession() }) {
                                Text("تمدید ۱۵ دقیقه")
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { viewModel.requestEndSession() }) {
                                Text("پایان سشن")
                            }
                        }
                    }
                }
            }
        }
        if (uiState.history.isNotEmpty()) {
            item { SectionTitle("تاریخچه سشن‌ها") }
        }
        items(uiState.history, key = { it.id }) { session ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(
                        "${dateFormat.format(Date(session.startEpochMs))} تا ${dateFormat.format(Date(session.endEpochMs))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "وضعیت: ${session.status}" +
                            (if (session.endedReason != null) " (${session.endedReason})" else "") +
                            (if (session.strict) " — Strict" else ""),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
