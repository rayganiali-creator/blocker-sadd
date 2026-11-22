package com.sadd.blocker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.ui.BackupViewModel
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.ChipPurple
import com.sadd.blocker.ui.theme.ChipYellow
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.screens.StrictUnlockDialog
import kotlinx.coroutines.launch

/**
 * بکاپ و ریستور.
 * خروجی JSON نسخه‌دار (version=1)؛ قبل از import کاملاً اعتبارسنجی می‌شود
 * و آیتم‌های خراب یا تکراری skip می‌شوند، نه اینکه کل ریستور خراب شود.
 * خطای خواندن/نوشتن فایل با پیام نشان داده می‌شود (M-10).
 */
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.buildExportJson()
                val result = viewModel.writeBackupText(uri, json)
                if (result.isFailure) {
                    localError = "ذخیره بکاپ ناموفق بود: " +
                        (result.exceptionOrNull()?.message ?: "خطای نامشخص")
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = viewModel.readBackupText(uri)
                if (result.isSuccess) {
                    viewModel.importJson(result.getOrThrow())
                } else {
                    localError = "خواندن فایل بکاپ ناموفق بود: " +
                        (result.exceptionOrNull()?.message ?: "خطای نامشخص")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SaddSpacing.Large),
    ) {
        SaddTopBar(
            "بکاپ و ریستور",
            "همه قوانین، وب‌سایت‌ها، کلمات و زمان‌بندی‌ها در یک فایل JSON.",
            onBack = onBack,
        )
        MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })
        MessageBar(
            message = localError,
            type = MessageType.ERROR,
            onDismiss = { localError = null },
        )
        // کارت خروجی بکاپ: چیپ آیکون + دکمه قرصی.
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SaddIconChip(icon = Icons.Filled.Shield, color = ChipMint)
                    Spacer(Modifier.width(SaddSpacing.Medium))
                    Text(
                        "خروجی بکاپ (JSON)",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Spacer(Modifier.height(SaddSpacing.Medium))
                SaddButton(
                    text = if (uiState.isPro) "خروجی بکاپ (JSON)" else "خروجی بکاپ (JSON) — پرو",
                    onClick = {
                        if (uiState.isPro) {
                            exportLauncher.launch("sadd-backup.json")
                        } else {
                            viewModel.showProPrompt()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Small))
        // کارت ورود بکاپ: چیپ آیکون + دکمه ثانویه قرصی.
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SaddIconChip(icon = Icons.Filled.Lock, color = ChipPurple)
                    Spacer(Modifier.width(SaddSpacing.Medium))
                    Text(
                        "ورود بکاپ از فایل",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                Spacer(Modifier.height(SaddSpacing.Medium))
                SaddButton(
                    text = if (uiState.isPro) "ورود بکاپ از فایل" else "ورود بکاپ از فایل — پرو",
                    onClick = {
                        if (uiState.isPro) {
                            importLauncher.launch(arrayOf("application/json"))
                        } else {
                            viewModel.showProPrompt()
                        }
                    },
                    style = SaddButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Large))
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
                verticalAlignment = Alignment.Top,
            ) {
                SaddIconChip(icon = Icons.Filled.Info, color = ChipYellow)
                Spacer(Modifier.width(SaddSpacing.Medium))
                Text(
                    "فایل بکاپ فقط روی همین دستگاه ساخته می‌شود و هیچ‌جا آپلود نمی‌شود.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Large))
    }
    // وسط سشن Strict، ریستور فقط بعد از بازگشایی Strict انجام می‌شود.
    if (uiState.strictUnlockVisible) {
        StrictUnlockDialog(
            method = uiState.strictUnlockMethod,
            cooldownHours = uiState.strictCooldownHours,
            cooldownRequest = uiState.strictCooldownRequest,
            onUnlocked = { viewModel.confirmStrictUnlocked() },
            onRecordCooldownRequest = { viewModel.recordStrictCooldownRequest() },
            onDismiss = { viewModel.cancelStrictUnlock() },
        )
    }
    if (uiState.proPromptVisible) {
        ProPromptDialog(
            text = "پشتیبان‌گیری و ریستور در سد پرو است.",
            onGoToPro = {
                viewModel.dismissProPrompt()
                onNavigate("pro")
            },
            onDismiss = { viewModel.dismissProPrompt() },
        )
    }
}
