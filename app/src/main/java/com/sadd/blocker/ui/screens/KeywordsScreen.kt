package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.sadd.blocker.ui.KeywordsViewModel

/**
 * مدیریت کلمات — فاز ۲.
 * فارسی/انگلیسی، تک‌کلمه و عبارت، دسته‌بندی، تطابق whole-word
 * (نرمال‌سازی ي→ی، ك→ک، نیم‌فاصله در موتور core انجام می‌شود).
 * تشخیص نیت جستجو در فاز ۳ وصل می‌شود.
 */
@Composable
fun KeywordsScreen(
    viewModel: KeywordsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var keywordText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("عمومی") }
    var delayTarget by remember { mutableStateOf<Long?>(null) }
    val categories = listOf("عمومی", "قمار", "شبکه اجتماعی", "بازی", "خرید")

    if (delayTarget != null) {
        val target = uiState.rules.firstOrNull { it.id == delayTarget }
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
            ScreenHeader("مدیریت کلمات", "خواندن مقاله یا پیام هرگز بلاک نمی‌شود؛ فقط جستجوی عمدی.")
            MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })
            OutlinedTextField(
                value = keywordText,
                onValueChange = { keywordText = it },
                label = { Text("کلمه یا عبارت، مثل قمار") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = categoryText,
                onValueChange = { categoryText = it },
                label = { Text("دسته‌بندی") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                for (category in categories) {
                    FilterChip(
                        selected = categoryText == category,
                        onClick = { categoryText = category },
                        label = { Text(category) },
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.addKeyword(keywordText, categoryText)
                    keywordText = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("افزودن کلمه")
            }
        }
        if (uiState.rules.isEmpty()) {
            item { EmptyState("هنوز کلمه‌ای اضافه نشده است.") }
        } else {
            item { SectionTitle("کلمات بلاک‌شده (${uiState.rules.size})") }
        }
        items(uiState.rules, key = { it.id }) { rule ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.rawText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "نرمال‌شده: ${rule.normalizedText}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = rule.enabled,
                            onCheckedChange = { viewModel.setEnabled(rule.id, it) },
                        )
                        IconButton(onClick = { viewModel.remove(rule.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف قانون")
                        }
                    }
                    Row {
                        AssistChip(onClick = {}, label = { Text(rule.category) })
                        Spacer(Modifier.width(8.dp))
                        if (rule.isPhrase) {
                            AssistChip(onClick = {}, label = { Text("عبارت") })
                        }
                        Spacer(Modifier.width(8.dp))
                        DelayChip(delaySeconds = rule.delaySeconds, onClick = { delayTarget = rule.id })
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
