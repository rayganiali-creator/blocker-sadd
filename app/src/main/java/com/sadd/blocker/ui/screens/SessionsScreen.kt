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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.domain.JalaliDate
import com.sadd.blocker.ui.SessionsViewModel
import com.sadd.blocker.ui.theme.SaddSpacing

/** برچسب فارسی وضعیت سشن. */
private fun sessionStatusLabel(status: String): String {
    return when (status) {
        "active" -> "فعال"
        "ended" -> "پایان‌یافته"
        "expired" -> "منقضی‌شده"
        else -> status
    }
}

/** برچسب فارسی دلیل پایان سشن. */
private fun endedReasonLabel(reason: String?): String {
    return when (reason) {
        "user" -> "توسط کاربر"
        "expired" -> "انقضا"
        "permission-revoked" -> "لغو مجوز"
        null -> ""
        else -> reason
    }
}

/**
 * سشن‌های فعال.
 * باقی‌مانده زنده از endEpochMs، تمدید ۱۵ دقیقه‌ای، پایان سشن؛
 * پایان سشن Strict وقتی PIN تنظیم شده باشد فقط با PIN درست انجام می‌شود.
 * تاریخ‌ها جلالی‌اند. شروع سشن از داشبورد انجام می‌شود؛
 * زمان‌بندی خودکار در فاز ۵ وصل می‌شود.
 * تیکر فقط وقتی صفحه دیده می‌شود می‌دود.
 */
@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pinText by rememberSaveable { mutableStateOf("") }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setTicking(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.setTicking(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.pinRequired) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelPinDialog() },
            title = { Text("پایان سشن Strict", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("این سشن Strict است. برای پایان آن PIN را وارد کن.")
                    Spacer(Modifier.height(SaddSpacing.Medium))
                    SaddTextField(
                        value = pinText,
                        onValueChange = { pinText = it },
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
                        viewModel.endSessionWithPin(pinText)
                        pinText = ""
                    },
                ) { Text("تایید و پایان") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelPinDialog()
                        pinText = ""
                    },
                ) { Text("لغو") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = SaddSpacing.Large),
    ) {
        item {
            SaddTopBar("سشن‌های فعال", "بلاک فقط وسط سشن فعال کار می‌کند.")
            MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })
        }
        if (uiState.active == null) {
            item { EmptyState("سشنی فعال نیست. از داشبورد یک سشن شروع کن.", icon = Icons.Filled.Timer) }
        } else {
            val active = uiState.active!!
            item {
                SaddCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                        Text(
                            "سشن فعال",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            "باقی‌مانده: ${uiState.remainingText}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "شروع: ${JalaliDate.formatDateTime(active.startEpochMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "پایان: ${JalaliDate.formatDateTime(active.endEpochMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (active.strict) {
                            Spacer(Modifier.height(SaddSpacing.Small))
                            AssistChip(onClick = {}, label = { Text("Strict — سخت‌گیرانه") })
                        }
                        Spacer(Modifier.height(SaddSpacing.Medium))
                        Row {
                            SaddButton(
                                text = "تمدید ۱۵ دقیقه",
                                onClick = { viewModel.extendSession() },
                            )
                            Spacer(Modifier.width(SaddSpacing.Small))
                            SaddButton(
                                text = "پایان سشن",
                                onClick = { viewModel.requestEndSession() },
                                style = SaddButtonStyle.SECONDARY,
                            )
                        }
                    }
                }
            }
        }
        if (uiState.history.isNotEmpty()) {
            item { SectionTitle("تاریخچه سشن‌ها") }
        }
        items(uiState.history, key = { it.id }) { session ->
            val reason = endedReasonLabel(session.endedReason)
            SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                    Text(
                        "${JalaliDate.formatDateTime(session.startEpochMs)} تا ${JalaliDate.formatDateTime(session.endEpochMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "وضعیت: ${sessionStatusLabel(session.status)}" +
                            (if (reason.isNotEmpty()) " ($reason)" else "") +
                            (if (session.strict) " — Strict" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(SaddSpacing.Large)) }
    }
}
