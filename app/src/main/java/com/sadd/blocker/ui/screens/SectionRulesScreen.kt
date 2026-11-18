package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.core.SectionDetector
import com.sadd.blocker.ui.SectionRulesViewModel
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * بخش‌های اپ («بخش‌های اپ» از صفحه اپ‌ها)، گروه‌بندی‌شده بر اساس اپ:
 * هر اپ یک سربرگ دارد و زیر آن بخش‌هایش — اول «خود اپ»، بعد جستجو،
 * بعد بقیه بخش‌ها — هر کدام با تاگل خودش. بلاک فقط وسط سشن فعال
 * انجام می‌شود و با BACK از بخش بیرون می‌روید.
 *
 * تشخیص بخش best-effort است (عنوان پنجره + فیلد جستجو + توضیح
 * محتوایی) و ممکن است با آپدیت آن اپ‌ها تغییر کند — متن همین را
 * بالای صفحه می‌گوید تا قول قطعی داده نشود. «خود اپ» تنها گزینه
 * قطعی است (باز شدن اپ = بلاک)؛ ریلز، شورتس، وضعیت، For You و
 * تایم‌لاین خانه چون برچسب‌شان در ناوبری و صفحه اصلی هم هست، ممکن
 * است بیش‌ازحد یا کم‌بلاک کنند. هر اپ نصب‌شده دیگری را هم می‌توان
 * اضافه کرد: «خود اپ» (قطعی) یا بخش سفارشی با متن نشانه (بهترین تلاش).
 */
@Composable
fun SectionRulesScreen(
    viewModel: SectionRulesViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val installedLabels = remember(uiState.installed) {
        uiState.installed.associate { it.packageName to it.label }
    }

    var addPanelVisible by rememberSaveable { mutableStateOf(false) }
    var appQuery by rememberSaveable { mutableStateOf("") }
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var addWholeApp by rememberSaveable { mutableStateOf(true) }
    var customName by rememberSaveable { mutableStateOf("") }
    var customSignal by rememberSaveable { mutableStateOf("") }

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

    MessageSnackbarEffect(
        message = uiState.message,
        snackbarHostState = snackbarHostState,
        onUndo = {},
        onDismiss = { viewModel.dismissMessage() },
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = SaddSpacing.Large),
        ) {
            item {
                SaddTopBar(
                    "بخش‌های اپ",
                    "کل اپ، جستجو، اکسپلور، ریلز و شورتس فقط وسط سشن بلاک می‌شود.",
                    onBack = onBack,
                )
                Text(
                    "«خود اپ» قطعی است؛ تشخیص بخش‌ها (جستجو، ریلز، شورتس…) بهترین تلاش است و گاهی صفحه اصلی را هم بلاک می‌کند.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                SaddButton(
                    text = if (addPanelVisible) "بستن افزودن" else "افزودن اپ یا بخش",
                    onClick = { addPanelVisible = !addPanelVisible },
                    style = SaddButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(SaddSpacing.Small))
            }
            if (addPanelVisible) {
                item {
                    AddSectionPanel(
                        installed = uiState.installed,
                        query = appQuery,
                        onQueryChange = { appQuery = it },
                        selectedPackage = selectedPackage,
                        onSelectPackage = { selectedPackage = it },
                        addWholeApp = addWholeApp,
                        onAddWholeAppChange = { addWholeApp = it },
                        customName = customName,
                        onCustomNameChange = { customName = it },
                        customSignal = customSignal,
                        onCustomSignalChange = { customSignal = it },
                        onSave = {
                            val pkg = selectedPackage ?: return@AddSectionPanel
                            val label = installedLabels[pkg] ?: pkg
                            if (addWholeApp) {
                                viewModel.addWholeApp(pkg, label)
                            } else {
                                viewModel.addCustomSection(pkg, label, customName, customSignal)
                            }
                            selectedPackage = null
                            customName = ""
                            customSignal = ""
                            addPanelVisible = false
                        },
                    )
                    Spacer(Modifier.height(SaddSpacing.Small))
                }
            }
            val grouped = uiState.rules.groupBy { it.packageName }
            for ((packageName, rules) in grouped) {
                item { SectionTitle(appLabelFa(packageName, installedLabels)) }
                val ordered = rules.sortedBy { rule -> presetOrder(rule.sectionKey) }
                items(ordered, key = { it.packageName + "/" + it.sectionKey }) { rule ->
                    SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        SaddSwitchRow(
                            label = rule.labelFa,
                            description = sectionDescription(rule.sectionKey),
                            checked = rule.enabled,
                            onCheckedChange = {
                                viewModel.setEnabled(rule.packageName, rule.sectionKey, it)
                            },
                            modifier = Modifier.padding(SaddSpacing.Medium),
                        )
                    }
                }
            }
            if (uiState.rules.isEmpty()) {
                item { EmptyState("هنوز بخشی تعریف نشده است.") }
            }
            item { Spacer(Modifier.height(SaddSpacing.Large)) }
        }
    }
}

/**
 * پنل افزودن اپ یا بخش: اپ از لیست نصب‌شده‌ها انتخاب می‌شود؛ بعد یا
 * «خود اپ» (قطعی) اضافه می‌شود یا یک بخش سفارشی با نام و متن نشانه
 * (بهترین تلاش). لیست اپ‌ها برای جلوگیری از اسکرول تودرتو فقط ۸
 * نتیجه اول فیلتر را نشان می‌دهد.
 */
@Composable
private fun AddSectionPanel(
    installed: List<com.sadd.blocker.data.system.InstalledAppInfo>,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedPackage: String?,
    onSelectPackage: (String) -> Unit,
    addWholeApp: Boolean,
    onAddWholeAppChange: (Boolean) -> Unit,
    customName: String,
    onCustomNameChange: (String) -> Unit,
    customSignal: String,
    onCustomSignalChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val filtered = remember(installed, query) {
        val q = query.trim().lowercase()
        val matches = if (q.isEmpty()) {
            installed
        } else {
            installed.filter {
                it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
            }
        }
        matches.take(8)
    }
    val canSave = selectedPackage != null &&
        (addWholeApp || (customName.isNotBlank() && customSignal.trim().length >= 2))
    SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
            SaddTextField(
                value = query,
                onValueChange = onQueryChange,
                label = "جستجوی اپ…",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(SaddSpacing.Small))
            for (app in filtered) {
                val selected = app.packageName == selectedPackage
                SaddCard(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    onClick = { onSelectPackage(app.packageName) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(app.label, style = MaterialTheme.typography.bodyMedium)
                        if (selected) Text("✓", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (filtered.isEmpty()) {
                Text(
                    "اپی پیدا نشد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(SaddSpacing.Small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
            ) {
                SaddButton(
                    text = "کل اپ",
                    onClick = { onAddWholeAppChange(true) },
                    style = if (addWholeApp) SaddButtonStyle.PRIMARY else SaddButtonStyle.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
                SaddButton(
                    text = "بخش سفارشی",
                    onClick = { onAddWholeAppChange(false) },
                    style = if (!addWholeApp) SaddButtonStyle.PRIMARY else SaddButtonStyle.SECONDARY,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!addWholeApp) {
                Spacer(Modifier.height(SaddSpacing.Small))
                SaddTextField(
                    value = customName,
                    onValueChange = onCustomNameChange,
                    label = "نام بخش، مثلاً فید اصلی",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                SaddTextField(
                    value = customSignal,
                    onValueChange = onCustomSignalChange,
                    label = "متن نشانه، مثلاً \u2066For You\u2069",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                Text(
                    "وقتی این متن روی صفحه دیده شود، وسط سشن بلاک می‌شود؛ نشانه کوتاه یا عمومی بیش‌بلاک می‌کند.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(SaddSpacing.Small))
            SaddButton(
                text = "افزودن",
                onClick = onSave,
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun appLabelFa(packageName: String, installedLabels: Map<String, String>): String {
    return when (packageName) {
        "org.telegram.messenger" -> "تلگرام"
        "com.instagram.android" -> "اینستاگرام"
        "com.google.android.youtube" -> "یوتیوب"
        "com.whatsapp" -> "واتساپ"
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill" -> "تیک‌تاک"
        "com.twitter.android" -> "ایکس"
        "com.facebook.katana" -> "فیسبوک"
        "com.snapchat.android" -> "اسنپ‌چت"
        else -> installedLabels[packageName] ?: packageName
    }
}

/** ترتیب نمایش بخش‌ها = ترتیب SectionDetector.PRESETS (در هر اپ اول «خود اپ»). */
private fun presetOrder(sectionKey: String): Int {
    // «خود اپ» دستی همیشه اول گروه خودش است؛ بخش‌های سفارشی آخر می‌آیند.
    if (sectionKey == SectionDetector.SECTION_CUSTOM_APP) return -1
    if (SectionDetector.isCustomSectionKey(sectionKey)) return Int.MAX_VALUE
    val index = SectionDetector.PRESETS.indexOfFirst { it.sectionKey == sectionKey }
    return if (index >= 0) index else Int.MAX_VALUE
}

private fun sectionDescription(sectionKey: String): String {
    if (SectionDetector.isCustomSectionKey(sectionKey)) {
        return "بخش سفارشی — وقتی متن نشانه روی صفحه باشد بلاک می‌شود (بهترین تلاش)"
    }
    return when (sectionKey) {
        SectionDetector.SECTION_TELEGRAM_APP,
        SectionDetector.SECTION_INSTAGRAM_APP,
        SectionDetector.SECTION_YOUTUBE_APP,
        SectionDetector.SECTION_WHATSAPP_APP,
        SectionDetector.SECTION_TIKTOK_APP,
        SectionDetector.SECTION_X_APP,
        SectionDetector.SECTION_FACEBOOK_APP,
        SectionDetector.SECTION_SNAPCHAT_APP,
        SectionDetector.SECTION_CUSTOM_APP,
        -> "باز شدن اپ وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_TELEGRAM_SEARCH,
        SectionDetector.SECTION_INSTAGRAM_SEARCH,
        SectionDetector.SECTION_YOUTUBE_SEARCH,
        SectionDetector.SECTION_WHATSAPP_SEARCH,
        SectionDetector.SECTION_TIKTOK_SEARCH,
        SectionDetector.SECTION_X_SEARCH,
        SectionDetector.SECTION_FACEBOOK_SEARCH,
        SectionDetector.SECTION_SNAPCHAT_SEARCH,
        -> "ورود به جستجو وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_INSTAGRAM_EXPLORE -> "ورود به اکسپلور وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_INSTAGRAM_REELS -> "باز شدن ریلز وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_YOUTUBE_SHORTS -> "باز شدن شورتس وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_WHATSAPP_STATUS -> "باز شدن وضعیت وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_TIKTOK_FORYOU -> "باز شدن فید \u2066For You\u2069 وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_X_HOME -> "باز شدن تایم‌لاین خانه وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_FACEBOOK_REELS -> "باز شدن ریلز و واچ وسط سشن بلاک می‌شود"
        SectionDetector.SECTION_SNAPCHAT_SPOTLIGHT -> "باز شدن اسپات‌لایت وسط سشن بلاک می‌شود"
        else -> sectionKey
    }
}
