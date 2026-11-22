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
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.ui.WebsitesViewModel
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.ChipPink
import com.sadd.blocker.ui.theme.ChipPurple
import com.sadd.blocker.ui.theme.ChipYellow
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddPrimaryTextColor

/**
 * بلاک وب‌سایت‌ها.
 * دامنه نرمال می‌شود (بدون scheme/www/مسیر) و تطابق مرز دامنه دارد:
 * example.com هرگز notexample.com را نمی‌گیرد.
 * صادقانه: بلاک وب با سرویس Accessibility انجام می‌شود (خواندن نوار
 * آدرس/جستجوی مرورگر)؛ بدون VPN است و به مرورگر وابسته است.
 */
@Composable
fun WebsitesScreen(
    viewModel: WebsitesViewModel = hiltViewModel(),
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

    if (uiState.proPromptVisible) {
        ProPromptDialog(
            text = "در نسخه رایگان حداکثر ۳ وب‌سایت فعال می‌شود. با سد پرو نامحدود است.",
            onGoToPro = {
                viewModel.dismissProPrompt()
                onNavigate("pro")
            },
            onDismiss = { viewModel.dismissProPrompt() },
        )
    }

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
                    "بلاک وب با \u2066Accessibility\u2069 از نوار آدرس مرورگر انجام می‌شود — کروم تأییدشده، بقیه بهترین تلاش‌اند؛ \u2066Incognito\u2069 هم بلاک می‌شود.",
                )
                // فرم افزودن دامنه داخل کارت Liquid Glass + کارت سوییچ زیردامنه.
                SaddCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                        SaddTextField(
                            value = hostText,
                            onValueChange = { hostText = it },
                            label = "دامنه، مثل example.com",
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Filled.Language, contentDescription = null) },
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
                }
                Spacer(Modifier.height(SaddSpacing.Small))
                SaddToggleCard(
                    label = "زیردامنه‌ها هم شامل شوند",
                    description = "مثل sub.example.com",
                    icon = Icons.Filled.Language,
                    iconColor = ChipMint,
                    checked = matchSubdomains,
                    onCheckedChange = { matchSubdomains = it },
                )
            }
            if (uiState.websites.isEmpty()) {
                item { EmptyState("هنوز وب‌سایتی اضافه نشده است.", icon = Icons.Filled.Language) }
            } else {
                item { SectionTitle("وب‌سایت‌های بلاک‌شده (${uiState.websites.size})") }
            }
            items(uiState.websites, key = { it.id }) { site ->
                // کارت قانون وب‌سایت: چیپ آیکون رنگی + سوییچ + چیپ تاخیر قرصی.
                SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SaddIconChip(icon = Icons.Filled.Language, color = websiteChipColor(site.id))
                            Spacer(Modifier.width(SaddSpacing.Medium))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(site.host, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    if (site.matchSubdomains) "شامل زیردامنه‌ها" else "فقط همین دامنه",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (site.id in uiState.pausedRuleIds) {
                                    Text(
                                        "متوقف — در نسخه رایگان فقط ۳ وب‌سایت اول فعال است (سد پرو).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = saddPrimaryTextColor(),
                                    )
                                }
                            }
                            Switch(
                                checked = site.enabled,
                                onCheckedChange = { viewModel.setEnabled(site.id, it) },
                                colors = saddSwitchColors(),
                            )
                            IconButton(onClick = { pendingDelete = site.id }) {
                                Icon(Icons.Filled.Delete, contentDescription = "حذف قانون")
                            }
                        }
                        Spacer(Modifier.height(SaddSpacing.Small))
                        DelayChip(delaySeconds = site.delaySeconds, onClick = { delayTarget = site.id })
                    }
                }
            }
            item { Spacer(Modifier.height(SaddSpacing.Large)) }
        }
    }
}

/** رنگ چیپ قانون وب‌سایت (فقط نمایشی): چرخش چهار رنگ Liquid Glass بر اساس شناسه قانون. */
private fun websiteChipColor(id: Long): Color {
    return when ((id % 4L).toInt()) {
        0 -> ChipMint
        1 -> ChipPurple
        2 -> ChipYellow
        else -> ChipPink
    }
}
