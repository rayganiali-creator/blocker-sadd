package com.sadd.blocker.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.data.system.PermissionStatus
import com.sadd.blocker.ui.DiagnosticsViewModel
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * مجوزها و عیب‌یابی.
 * همه وضعیت‌ها واقعی‌اند (از سیستم خوانده می‌شوند)؛
 * هر مجوز دکمه مستقیم به صفحه تنظیمات مربوطه دارد.
 * وضعیت با بازگشت به صفحه (ON_RESUME) تازه می‌شود.
 */
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // VPN: تایید از طریق دیالوگ سیستمی VpnService.prepare است، نه صفحه VPN Settings.
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refresh() }
    // اعلان‌ها: درخواست مستقیم runtime permission (اندروید ۱۳+)، نه فقط تنظیمات.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = SaddSpacing.Large),
    ) {
        item {
            SaddTopBar(
                "مجوزها و عیب‌یابی",
                "اگر مجوزی قطع شود، بلاک همان بخش از کار می‌افتد.",
                onBack = onBack,
            )
            SaddButton(
                text = "به‌روزرسانی وضعیت",
                onClick = { viewModel.refresh() },
                style = SaddButtonStyle.SECONDARY,
            )
            SectionTitle("مجوزها")
        }
        items(uiState.permissions, key = { it.key }) { permission ->
            // باز کردن مجوز: VPN و اعلان مسیر سیستمی خودشان را دارند؛ بقیه صفحه تنظیمات.
            val openPermission = {
                when (permission.key) {
                    "vpn" -> {
                        val prepareIntent = VpnService.prepare(context)
                        if (prepareIntent == null) {
                            Toast.makeText(context, "VPN لوکال آماده است.", Toast.LENGTH_SHORT).show()
                            viewModel.refresh()
                        } else {
                            vpnPrepareLauncher.launch(prepareIntent)
                        }
                    }
                    "notification" -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            openSettingsPage(context, permission)
                        }
                    }
                    else -> openSettingsPage(context, permission)
                }
            }
            SaddCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clickable { openPermission() },
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (permission.granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (permission.granted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                        Spacer(Modifier.size(SaddSpacing.Small))
                        Text(
                            permission.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (permission.granted) "فعال" else "غیرفعال",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (permission.granted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                    Text(
                        permission.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(SaddSpacing.Small))
                    SaddButton(
                        text = "رفتن به تنظیمات",
                        onClick = openPermission,
                        style = SaddButtonStyle.SECONDARY,
                    )
                }
            }
        }
        item {
            SectionTitle("دیتابیس و سشن")
            SaddCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                    InfoRow("اپ‌های فعال", uiState.appsCount.toString())
                    InfoRow("وب‌سایت‌های فعال", uiState.websitesCount.toString())
                    InfoRow("کلمات فعال", uiState.keywordsCount.toString())
                    InfoRow("سشن فعال", if (uiState.sessionActive) "بله" else "خیر")
                }
            }
            SectionTitle("نسخه و رویداد")
            SaddCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                    InfoRow("نسخه", "${uiState.versionName} (${uiState.versionCode})")
                    InfoRow("آخرین رویداد بلاک", "هنوز رویدادی ثبت نشده — ثبت رویداد با وصل‌شدن مکانیزم بلاک می‌آید.")
                }
            }
            Spacer(Modifier.height(SaddSpacing.Large))
        }
    }
}

/**
 * باز کردن صفحه تنظیمات مربوط به یک مجوز.
 * باگ قبلی: خطای startActivity بی‌صدا با runCatching خالی قورت داده می‌شد و
 * کاربر فقط می‌دید «هیچ اتفاقی نمی‌افتد». حالا FLAG_ACTIVITY_NEW_TASK اضافه
 * شده، در صورت شکست Toast نشان داده می‌شود و fallback به صفحه اطلاعات اپ است.
 */
private fun openSettingsPage(context: Context, permission: PermissionStatus) {
    val intent = Intent(permission.settingsAction).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (permission.key == "notification") {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        permission.settingsUri?.let { data = Uri.parse(it) }
    }
    val opened = runCatching { context.startActivity(intent) }.isSuccess
    if (!opened) {
        Toast.makeText(
            context,
            "صفحه تنظیمات باز نشد؛ از تنظیمات گوشی اقدام کن",
            Toast.LENGTH_LONG,
        ).show()
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }
}
