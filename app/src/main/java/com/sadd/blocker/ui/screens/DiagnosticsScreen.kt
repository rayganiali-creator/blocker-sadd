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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sadd.blocker.ui.DiagnosticsViewModel

/**
 * مجوزها و عیب‌یابی — فاز ۲.
 * همه وضعیت‌ها واقعی‌اند (از سیستم خوانده می‌شوند)؛
 * هر مجوز دکمه مستقیم به صفحه تنظیمات مربوطه دارد.
 */
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        item {
            ScreenHeader(
                "مجوزها و عیب‌یابی",
                "اگر مجوزی قطع شود، بلاک همان بخش از کار می‌افتد.",
                onBack = onBack,
            )
            OutlinedButton(onClick = { viewModel.refresh() }) {
                Text("به‌روزرسانی وضعیت")
            }
            SectionTitle("مجوزها")
        }
        items(uiState.permissions, key = { it.key }) { permission ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (permission.granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (permission.granted) Color(0xFF2E7D32) else Color(0xFFED6C02),
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            permission.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (permission.granted) "فعال" else "غیرفعال",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(permission.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(
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
                    ) {
                        Text("رفتن به تنظیمات")
                    }
                }
            }
        }
        item {
            SectionTitle("دیتابیس و سشن")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    InfoRow("اپ‌های فعال", uiState.appsCount.toString())
                    InfoRow("وب‌سایت‌های فعال", uiState.websitesCount.toString())
                    InfoRow("کلمات فعال", uiState.keywordsCount.toString())
                    InfoRow("سشن فعال", if (uiState.sessionActive) "بله" else "خیر")
                }
            }
            SectionTitle("نسخه و رویداد")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    InfoRow("نسخه", "${uiState.versionName} (${uiState.versionCode})")
                    InfoRow("آخرین رویداد بلاک", "هنوز رویدادی ثبت نشده — ثبت رویداد در فاز ۴.")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
