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
import androidx.compose.material.icons.filled.Language
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
import com.sadd.blocker.ui.WebsitesViewModel
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * بلاک وب‌سایت‌ها.
 * دامنه نرمال می‌شود (بدون scheme/www/مسیر) و تطابق مرز دامنه دارد:
 * example.com هرگز notexample.com را نمی‌گیرد.
 * صادقانه: مکانیزم بلاک وب (VPN لوکال) هنوز وصل نشده و در فاز بعدی می‌آید.
 */
@Composable
fun WebsitesScreen(
    viewModel: WebsitesViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var hostText by rememberSaveable { mutableStateOf("") }
    var matchSubdomains by rememberSaveable { mutableStateOf(true) }
    var delayTarget by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    MessageSnackbarEffect(
        message = uiState.message,
        snackbarHostState = snackbarHostState,
        onUndo = { viewModel.undoRemove() },
        onDismiss = { viewModel.dismissMessage() },
    )

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

    if (pendingDelete != null) {
        val target = uiState.websites.firstOrNull { it.id == pendingDelete }
        SaddDialog(
            title = "حذف قانون",
            text = "«${target?.host ?: ""}» از لیست وب‌سایت‌ها حذف شود؟",
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
                SaddTopBar(
                    "بلاک وب‌سایت‌ها",
                    "بلاک دامنه با VPN لوکال در فاز بعدی وصل می‌شود؛ فعلاً فقط لیست ساخته می‌شود.",
                )
                SaddTextField(
                    value = hostText,
                    onValueChange = { hostText = it },
                    label = "دامنه، مثل example.com",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                SaddSwitchRow(
                    label = "زیردامنه‌ها هم شامل شوند",
                    description = "مثل sub.example.com",
                    checked = matchSubdomains,
                    onCheckedChange = { matchSubdomains = it },
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                SaddButton(
                    text = "افزودن وب‌سایت",
                    onClick = {
                        viewModel.addWebsite(hostText, matchSubdomains)
                        hostText = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (uiState.websites.isEmpty()) {
                item { EmptyState("هنوز وب‌سایتی اضافه نشده است.", icon = Icons.Filled.Language) }
            } else {
                item { SectionTitle("وب‌سایت‌های بلاک‌شده (${uiState.websites.size})") }
            }
            items(uiState.websites, key = { it.id }) { site ->
                SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(site.host, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    if (site.matchSubdomains) "شامل زیردامنه‌ها" else "فقط همین دامنه",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = site.enabled,
                                onCheckedChange = { viewModel.setEnabled(site.id, it) },
                            )
                            IconButton(onClick = { pendingDelete = site.id }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف قانون")
                            }
                        }
                        DelayChip(delaySeconds = site.delaySeconds, onClick = { delayTarget = site.id })
                    }
                }
            }
            item { Spacer(Modifier.height(SaddSpacing.Large)) }
        }
    }
}
