package com.sadd.blocker.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sadd.blocker.ui.BackupViewModel
import kotlinx.coroutines.launch

/**
 * بکاپ و ریستور — فاز ۲.
 * خروجی JSON نسخه‌دار (version=1)؛ قبل از import کاملاً اعتبارسنجی می‌شود
 * و آیتم‌های خراب یا تکراری skip می‌شوند، نه اینکه کل ریستور خراب شود.
 */
@Composable
fun BackupScreen(
    viewModel: BackupViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.buildExportJson()
                writeTextToUri(context, uri, json)
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = readTextFromUri(context, uri)
                if (json != null) {
                    viewModel.importJson(json)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ScreenHeader(
            "بکاپ و ریستور",
            "همه قوانین، وب‌سایت‌ها، کلمات و زمان‌بندی‌ها در یک فایل JSON.",
            onBack = onBack,
        )
        MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })
        Button(
            onClick = { exportLauncher.launch("sadd-backup.json") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("خروجی بکاپ (JSON)")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { importLauncher.launch(arrayOf("application/json")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("ورود بکاپ از فایل")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "فایل بکاپ فقط روی همین دستگاه ساخته می‌شود و هیچ‌جا آپلود نمی‌شود. " +
                "ورود فایل ناشناس امن است: نسخه بررسی می‌شود، آیتم خراب skip می‌شود " +
                "و قانون تکراری دوباره اضافه نمی‌شود.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
    }
}

private fun writeTextToUri(context: Context, uri: android.net.Uri, text: String) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        }
    }
}

private fun readTextFromUri(context: Context, uri: android.net.Uri): String? {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }
    }.getOrNull()
}
