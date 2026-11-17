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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
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
import com.sadd.blocker.ui.KeywordsViewModel
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * مدیریت کلمات — طبق موکاپ قوانین: جستجو، فیلتر همه/فعال/غیرفعال،
 * کارت هر قانون با چیپ دسته + «کلمه کامل» + تاخیر، حذف با تایید + Undo.
 * فارسی/انگلیسی، تک‌کلمه و عبارت، تطابق whole-word.
 * تشخیص نیت جستجو در فاز ۳ وصل می‌شود.
 */
@Composable
fun KeywordsScreen(
    viewModel: KeywordsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var keywordText by rememberSaveable { mutableStateOf("") }
    var categoryText by rememberSaveable { mutableStateOf("عمومی") }
    var searchText by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("همه") }
    var delayTarget by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    val categories = listOf("عمومی", "قمار", "شبکه اجتماعی", "بازی", "خرید")

    val search = searchText.trim().lowercase()
    val visibleRules = uiState.rules
        .filter {
            when (filter) {
                "فعال" -> it.enabled
                "غیرفعال" -> !it.enabled
                else -> true
            }
        }
        .filter {
            search.isEmpty() || it.rawText.lowercase().contains(search) ||
                it.category.lowercase().contains(search)
        }
    val activeCount = uiState.rules.count { it.enabled }

    MessageSnackbarEffect(
        message = uiState.message,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.undoRemove() },
        onDismiss = { viewModel.dismissMessage() },
    )

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

    if (pendingDelete != null) {
        val target = uiState.rules.firstOrNull { it.id == pendingDelete }
        SaddDialog(
            title = "حذف قانون",
            text = "«${target?.rawText ?: ""}» از لیست کلمات حذف شود؟",
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
                SaddTopBar("کلمات کلیدی", "خواندن مقاله یا پیام هرگز بلاک نمی‌شود؛ فقط جستجوی عمدی.")
                SaddTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = "جستجوی قانون…",
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                Row {
                    for (option in listOf("همه", "فعال", "غیرفعال")) {
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(SaddSpacing.Small))
                Text(
                    "${uiState.rules.size} قانون • $activeCount فعال",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(SaddSpacing.Medium))
                SaddTextField(
                    value = keywordText,
                    onValueChange = { keywordText = it },
                    label = "کلمه یا عبارت، مثل قمار",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                SaddTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it },
                    label = "دسته‌بندی",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(SaddSpacing.Small))
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
                Spacer(Modifier.height(SaddSpacing.Small))
                SaddButton(
                    text = "افزودن کلمه",
                    onClick = {
                        viewModel.addKeyword(keywordText, categoryText)
                        keywordText = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (uiState.rules.isEmpty()) {
                item { EmptyState("هنوز کلمه‌ای اضافه نشده است.", icon = Icons.Filled.TextFields) }
            } else if (visibleRules.isEmpty()) {
                item { EmptyState("با این فیلتر قانونی پیدا نشد.", icon = Icons.Filled.Search) }
            } else {
                item { SectionTitle("کلمات بلاک‌شده (${visibleRules.size})") }
            }
            items(visibleRules, key = { it.id }) { rule ->
                SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    rule.rawText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "نرمال‌شده: ${rule.normalizedText}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { viewModel.setEnabled(rule.id, it) },
                            )
                            IconButton(onClick = { pendingDelete = rule.id }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف قانون")
                            }
                        }
                        Row {
                            AssistChip(onClick = {}, label = { Text(rule.category) })
                            Spacer(Modifier.width(SaddSpacing.Small))
                            AssistChip(onClick = {}, label = { Text("کلمه کامل") })
                            if (rule.isPhrase) {
                                Spacer(Modifier.width(SaddSpacing.Small))
                                AssistChip(onClick = {}, label = { Text("عبارت") })
                            }
                            Spacer(Modifier.width(SaddSpacing.Small))
                            DelayChip(delaySeconds = rule.delaySeconds, onClick = { delayTarget = rule.id })
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(SaddSpacing.Large)) }
        }
    }
}
