package com.sadd.blocker.ui.screens

import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.ui.AppsViewModel
import com.sadd.blocker.ui.theme.SaddSpacing

/** کش حافظه‌ای آیکون اپ‌ها (حداکثر ۶۴ آیکون) — بدون بارگذاری دوباره از PackageManager. */
private val appIconCache = LruCache<String, ImageBitmap>(64)

/**
 * بلاک اپ‌ها.
 * لیست اپ‌های نصب‌شده از PackageManager؛ انتخاب چندتایی و افزودن به Room؛
 * قوانین ذخیره‌شده با تاگل فعال/غیرفعال، تاخیر per-rule و حذف با تایید + Undo.
 * لیست نصب‌شده سقفی ندارد (رفع M-20) و آیکون هر اپ نشان داده می‌شود.
 */
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
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

    var delayTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }

    val blockedPackages = uiState.blocked.map { it.packageName }.toSet()
    val query = uiState.query.trim().lowercase()
    val filteredInstalled = uiState.installed
        .filter { !blockedPackages.contains(it.packageName) }
        .filter { query.isEmpty() || it.label.lowercase().contains(query) || it.packageName.lowercase().contains(query) }

    MessageSnackbarEffect(
        message = uiState.message,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.undoRemove() },
        onDismiss = { viewModel.dismissMessage() },
    )

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

    if (pendingDelete != null) {
        val target = uiState.blocked.firstOrNull { it.packageName == pendingDelete }
        SaddDialog(
            title = "حذف قانون",
            text = "«${target?.label ?: pendingDelete}» از لیست بلاک حذف شود؟",
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
                SaddTopBar("بلاک اپ‌ها", "اپ‌هایی که باز شدنشان فقط وسط سشن بلاک می‌شود.")
                SaddTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.setQuery(it) },
                    label = "جستجو در اپ‌ها",
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                SaddButton(
                    text = "بخش‌های اپ‌ها — جستجو، ریلز، وضعیت و…",
                    onClick = { onNavigate("section_rules") },
                    style = SaddButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(SaddSpacing.Small))
            }
            if (uiState.selected.isNotEmpty()) {
                item {
                    SaddButton(
                        text = "افزودن ${uiState.selected.size} اپ انتخاب‌شده به لیست بلاک",
                        onClick = { viewModel.addSelected() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(SaddSpacing.Small))
                }
            }
            if (uiState.loading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(SaddSpacing.Medium))
                        Text("در حال خواندن اپ‌های نصب‌شده…")
                    }
                }
            } else if (filteredInstalled.isEmpty() && uiState.blocked.isEmpty()) {
                item { EmptyState("اپی پیدا نشد.", icon = Icons.Filled.Apps) }
            }
            if (filteredInstalled.isNotEmpty()) {
                item { SectionTitle("اپ‌های نصب‌شده (${filteredInstalled.size})") }
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
                    AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(SaddSpacing.Small))
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
                SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(packageName = app.packageName, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.width(SaddSpacing.Small))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = app.enabled,
                                onCheckedChange = { viewModel.setEnabled(app.packageName, it) },
                            )
                            IconButton(onClick = { pendingDelete = app.packageName }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف قانون")
                            }
                        }
                        DelayChip(delaySeconds = app.delaySeconds, onClick = { delayTarget = app.packageName })
                    }
                }
            }
            item { Spacer(Modifier.height(SaddSpacing.Large)) }
        }
    }
}

@Composable
private fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        appIconCache.get(packageName) ?: runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap()
        }.getOrNull()?.also { appIconCache.put(packageName, it) }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
    } else {
        Icon(
            Icons.Filled.Apps,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
