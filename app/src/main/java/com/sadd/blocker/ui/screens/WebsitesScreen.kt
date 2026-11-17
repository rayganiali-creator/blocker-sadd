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
import com.sadd.blocker.ui.WebsitesViewModel

/**
 * بلاک وب‌سایت‌ها — فاز ۲.
 * دامنه نرمال می‌شود (بدون scheme/www/مسیر) و تطابق مرز دامنه دارد:
 * example.com هرگز notexample.com را نمی‌گیرد.
 * مکانیزم بلاک وب (VPN لوکال) در فاز ۴ وصل می‌شود.
 */
@Composable
fun WebsitesScreen(
    viewModel: WebsitesViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var hostText by remember { mutableStateOf("") }
    var matchSubdomains by remember { mutableStateOf(true) }
    var delayTarget by remember { mutableStateOf<Long?>(null) }

    if (delayTarget != null) {
        val target = uiState.websites.firstOrNull { it.id == delayTarget }
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
            ScreenHeader("بلاک وب‌سایت‌ها", "دامنه‌ها با VPN لوکال بلاک می‌شوند (فاز ۴).")
            MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })
            OutlinedTextField(
                value = hostText,
                onValueChange = { hostText = it },
                label = { Text("دامنه، مثل example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = matchSubdomains, onCheckedChange = { matchSubdomains = it })
                Spacer(Modifier.width(8.dp))
                Text("زیردامنه‌ها هم شامل شوند (مثل sub.example.com)")
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.addWebsite(hostText, matchSubdomains)
                    hostText = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("افزودن وب‌سایت")
            }
        }
        if (uiState.websites.isEmpty()) {
            item { EmptyState("هنوز وب‌سایتی اضافه نشده است.") }
        } else {
            item { SectionTitle("وب‌سایت‌های بلاک‌شده (${uiState.websites.size})") }
        }
        items(uiState.websites, key = { it.id }) { site ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(site.host, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(
                                if (site.matchSubdomains) "شامل زیردامنه‌ها" else "فقط همین دامنه",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = site.enabled,
                            onCheckedChange = { viewModel.setEnabled(site.id, it) },
                        )
                        IconButton(onClick = { viewModel.remove(site.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف قانون")
                        }
                    }
                    DelayChip(delaySeconds = site.delaySeconds, onClick = { delayTarget = site.id })
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
