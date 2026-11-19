package com.sadd.blocker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
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
import com.sadd.blocker.ui.ThemeViewModel
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.ChipPink
import com.sadd.blocker.ui.theme.ChipPurple
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddErrorTextColor
import com.sadd.blocker.ui.theme.saddPrimaryTextColor

/**
 * مجوزها و عیب‌یابی.
 * همه وضعیت‌ها واقعی‌اند (از سیستم خوانده می‌شوند)؛
 * هر مجوز دکمه مستقیم به صفحه تنظیمات مربوطه دارد.
 * وضعیت با بازگشت به صفحه (ON_RESUME) تازه می‌شود.
 */
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // لانچرهای سیستمی درخواست مجوز — منطق باز کردن در PermissionIntents.kt است.
    val launchers = rememberPermissionLaunchers { viewModel.refresh() }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
            ) {
                SaddButton(
                    text = "به‌روزرسانی وضعیت",
                    onClick = { viewModel.refresh() },
                    style = SaddButtonStyle.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
                SaddButton(
                    text = "راه‌اندازی دوباره",
                    onClick = { onNavigate("onboarding") },
                    style = SaddButtonStyle.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
            }
            // چرخ‌دنده تنظیمات داشبورد به اینجا می‌رسد، پس انتخاب تم هم اینجاست.
            ThemePickerSection(themeViewModel = themeViewModel)
            SectionTitle("مجوزها")
        }
        items(uiState.permissions, key = { it.key }) { permission ->
            // باز کردن مجوز — منطق مشترک در PermissionIntents.kt است.
            val openPermission = {
                requestPermission(context, permission, launchers) { viewModel.refresh() }
            }
            SaddCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clickable { openPermission() },
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // چیپ آیکون Detox: فعال = مینت، غیرفعال = صورتی.
                        SaddIconChip(
                            icon = if (permission.granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            color = if (permission.granted) ChipMint else ChipPink,
                        )
                        Spacer(Modifier.width(SaddSpacing.Medium))
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
                                saddPrimaryTextColor()
                            } else {
                                saddErrorTextColor()
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SaddIconChip(icon = Icons.Filled.Settings, color = ChipPurple)
                        Spacer(Modifier.width(SaddSpacing.Medium))
                        Text(
                            "دیتابیس و سشن",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(SaddSpacing.Small))
                    InfoRow("اپ‌های فعال", uiState.appsCount.toString())
                    InfoRow("وب‌سایت‌های فعال", uiState.websitesCount.toString())
                    InfoRow("کلمات فعال", uiState.keywordsCount.toString())
                    InfoRow("سشن فعال", if (uiState.sessionActive) "بله" else "خیر")
                }
            }
            SectionTitle("نسخه و رویداد")
            SaddCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SaddIconChip(icon = Icons.Filled.Info, color = ChipMint)
                        Spacer(Modifier.width(SaddSpacing.Medium))
                        Text(
                            "نسخه و رویداد",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(SaddSpacing.Small))
                    InfoRow("نسخه", "${uiState.versionName} (${uiState.versionCode})")
                    InfoRow("آخرین رویداد بلاک", "هنوز رویدادی ثبت نشده — ثبت رویداد با وصل‌شدن مکانیزم بلاک می‌آید.")
                }
            }
            Spacer(Modifier.height(SaddSpacing.Large))
        }
    }
}
