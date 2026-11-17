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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import com.sadd.blocker.ui.AppsViewModel

/**
 * بلاک اپ‌ها — فاز ۲.
 * لیست اپ‌های نصب‌شده از PackageManager؛ انتخاب چندتایی و افزودن به Room؛
 * قوانین ذخیره‌شده با تاگل فعال/غیرفعال، تاخیر per-rule و حذف.
 */
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var delayTarget by remember { mutableStateOf<String?>(null) }
    val blockedPackages = uiState.blocked.map { it.packageName }.toSet()
    val query = uiState.query.trim().lowercase()
    val filteredInstalled = uiState.installed
        .filter { !blockedPackages.contains(it.packageName) }
        .filter { query.isEmpty() || it.label.lowercase().contains(query) || it.packageName.lowercase().contains(query) }
        .take(200)

    if (delayTarget != null) {
        val target = uiState.blocked.firstOrNull { it.packageName == delayTarget }
        DelayPickerDialog(
            currentDelay = target?.delaySeconds,
            onConfirm = { delay ->
                viewModel.setDelay(delayTarget!!, delay)
                delayTarget = null
            },
            onDismiss = { delayTarget = null },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        item {
            ScreenHeader("بلاک اپ‌ها", "اپ‌هایی که باز شدنشان فقط وسط سشن بلاک می‌شود.")
            MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.setQuery(it) },
                label = { Text("جستجو در اپ‌ها") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
        }
        if (uiState.selected.isNotEmpty()) {
            item {
                Button(
                    onClick = { viewModel.addSelected() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("افزودن ${uiState.selected.size} اپ انتخاب‌شده به لیست بلاک")
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (uiState.loading) {
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(12.dp))
                    Text("در حال خواندن اپ‌های نصب‌شده…")
                }
            }
        } else if (filteredInstalled.isEmpty() && uiState.blocked.isEmpty()) {
            item { EmptyState("اپی پیدا نشد.") }
        }
        if (filteredInstalled.isNotEmpty()) {
            item { SectionTitle("اپ‌های نصب‌شده") }
        }
        items(filteredInstalled, key = { it.packageName }) { app ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = uiState.selected.contains(app.packageName),
                    onCheckedChange = { viewModel.toggleSelect(app.packageName) },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (uiState.blocked.isNotEmpty()) {
            item { SectionTitle("اپ‌های بلاک‌شده (${uiState.blocked.size})") }
        }
        items(uiState.blocked, key = { it.packageName }) { app ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = app.enabled,
                            onCheckedChange = { viewModel.setEnabled(app.packageName, it) },
                        )
                        IconButton(onClick = { viewModel.remove(app.packageName) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف قانون")
                        }
                    }
                    DelayChip(delaySeconds = app.delaySeconds, onClick = { delayTarget = app.packageName })
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
