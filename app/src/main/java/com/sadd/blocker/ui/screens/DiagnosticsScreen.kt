package com.sadd.blocker.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
            SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
                        onClick = {
                            val intent = Intent(permission.settingsAction)
                            if (permission.key == "notification") {
                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            if (permission.settingsUri != null) {
                                intent.data = Uri.parse(permission.settingsUri)
                            }
                            runCatching { context.startActivity(intent) }
                        },
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
