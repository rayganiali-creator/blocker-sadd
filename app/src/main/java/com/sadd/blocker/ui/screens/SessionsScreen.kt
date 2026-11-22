package com.sadd.blocker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.data.system.UninstallProtection
import com.sadd.blocker.domain.JalaliDate
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.domain.model.SessionDurations
import com.sadd.blocker.ui.SessionsViewModel
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.ChipPurple
import com.sadd.blocker.ui.theme.ChipYellow
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddGlassDialogContainerColor
import com.sadd.blocker.ui.theme.saddErrorTextColor
import com.sadd.blocker.ui.theme.saddGlassSurfaceColor
import com.sadd.blocker.ui.theme.saddPrimaryTextColor
import com.sadd.blocker.ui.theme.saddSelectedContainerColor
import com.sadd.blocker.ui.theme.saddSpecularBorderBrush
import com.sadd.blocker.ui.theme.saddSpecularBorderColor

/** برچسب فارسی وضعیت سشن. */
private fun sessionStatusLabel(status: String): String {
    return when (status) {
        "active" -> "فعال"
        "ended" -> "پایان‌یافته"
        "expired" -> "منقضی‌شده"
        else -> status
    }
}

/** برچسب فارسی دلیل پایان سشن. */
private fun endedReasonLabel(reason: String?): String {
    return when (reason) {
        "user" -> "توسط کاربر"
        "expired" -> "انقضا"
        "permission-revoked" -> "لغو مجوز"
        null -> ""
        else -> reason
    }
}

/**
 * سشن‌های فعال + شروع سشن جدید.
 * باقی‌مانده زنده از endEpochMs، تمدید ۱۵ دقیقه‌ای، پایان سشن؛
 * پایان سشن Strict فقط با بازگشایی Strict (تایپ متن / انتظار) انجام می‌شود.
 * تاریخ‌ها جلالی‌اند. شروع سشن (چیپ‌های مدت + مدت دلخواه) از داشبورد
 * به اینجا منتقل شده؛ چیپ‌ها در FlowRow می‌پیچند تا «۸ ساعت» له نشود.
 * زمان‌بندی خودکار در فاز ۵ وصل می‌شود.
 * تیکر فقط وقتی صفحه دیده می‌شود می‌دود.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedDuration by rememberSaveable { mutableStateOf(SessionDurations.OPTIONS[1]) }
    var customMinutes by rememberSaveable { mutableStateOf("") }
    var customError by rememberSaveable { mutableStateOf<String?>(null) }
    // مدتی که کاربر برای شروع انتخاب کرده و منتظر انتخاب حالت است؛
    // حالت (Strict یا معمولی) موقع هر شروع پرسیده می‌شود و ذخیره نمی‌شود.
    var pendingStartDuration by rememberSaveable { mutableStateOf<Long?>(null) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.setTicking(true)
                    viewModel.refreshAdminState()
                }
                Lifecycle.Event.ON_PAUSE -> viewModel.setTicking(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
            text = "شروع سشن سخت‌گیرانه (Strict) در سد پرو است.",
            onGoToPro = {
                viewModel.dismissProPrompt()
                onNavigate("pro")
            },
            onDismiss = { viewModel.dismissProPrompt() },
        )
    }

    // انتخاب حالت موقع هر شروع سشن — بدون گزینه «به خاطر بسپار»؛
    // حالت ذخیره‌شده فقط پیش‌فرضِ پیشنهادی است.
    val pendingDuration = pendingStartDuration
    if (pendingDuration != null) {
        StartModeDialog(
            durationLabel = TimeFormat.durationLabel(pendingDuration),
            defaultStrict = uiState.blockMode == BlockMode.STRICT,
            strictLocked = !uiState.isPro,
            onPick = { strict ->
                pendingStartDuration = null
                viewModel.startSession(pendingDuration, strict)
            },
            onStrictLocked = {
                pendingStartDuration = null
                onNavigate("pro")
            },
            onDismiss = { pendingStartDuration = null },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = SaddSpacing.Large),
    ) {
        item {
            SaddTopBar("سشن‌های فعال", "بلاک فقط وسط سشن فعال کار می‌کند.")
            MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })
        }
        if (uiState.active == null) {
            item {
                SaddCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                        Text(
                            "شروع سشن جدید",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text("مدت سشن:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(SaddSpacing.Small))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
                            verticalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
                        ) {
                            for (duration in SessionDurations.OPTIONS) {
                                // چیپ مدت به سبک Liquid Glass: قرصی شیشه‌ای، انتخاب‌شده = primary توپر.
                                FilterChip(
                                    selected = selectedDuration == duration,
                                    onClick = { selectedDuration = duration },
                                    label = { Text(TimeFormat.durationLabel(duration)) },
                                    shape = RoundedCornerShape(SaddRadius.Button),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = saddGlassSurfaceColor(),
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        selectedContainerColor = saddSelectedContainerColor(),
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                    border = BorderStroke(SaddGlass.BorderWidth, saddSpecularBorderColor()),
                                )
                            }
                        }
                        Spacer(Modifier.height(SaddSpacing.Small))
                        SaddButton(
                            text = "شروع سشن (${TimeFormat.durationLabel(selectedDuration)})",
                            onClick = { pendingStartDuration = selectedDuration },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(SaddSpacing.Medium))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SaddTextField(
                                value = customMinutes,
                                onValueChange = {
                                    customMinutes = it
                                    customError = null
                                },
                                label = "مدت دلخواه (دقیقه)",
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                            Spacer(Modifier.width(SaddSpacing.Small))
                            SaddButton(
                                text = "شروع",
                                onClick = {
                                    val minutes = customMinutes.trim().toIntOrNull()
                                    if (minutes == null || minutes <= 0) {
                                        customError = "مدت را به دقیقه وارد کن."
                                    } else {
                                        val durationMs = minutes * 60_000L
                                        if (durationMs < SessionDurations.MIN_MS) {
                                            customError = "حداقل مدت سشن ۳۰ ثانیه است."
                                        } else if (durationMs > SessionDurations.MAX_MS) {
                                            customError = "حداکثر مدت سشن ۹۰ روز (۱۲۹٬۶۰۰ دقیقه) است."
                                        } else {
                                            pendingStartDuration = durationMs
                                        }
                                    }
                                },
                                style = SaddButtonStyle.SECONDARY,
                            )
                        }
                        if (customError != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                customError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = saddErrorTextColor(),
                            )
                        }
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            "حالت سشن موقع هر شروع پرسیده می‌شود؛ انتخاب «حالت بلاک» فقط پیش‌فرض است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            val active = uiState.active!!
            item {
                SaddCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                        // سرتیتر کارت سشن فعال به سبک Liquid Glass: چیپ آیکون تایمر + عنوان.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SaddIconChip(icon = Icons.Filled.Timer, color = ChipMint)
                            Spacer(Modifier.width(SaddSpacing.Small))
                            Text(
                                "سشن فعال",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            "باقی‌مانده: ${uiState.remainingText}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "شروع: ${JalaliDate.formatDateTime(active.startEpochMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "پایان: ${JalaliDate.formatDateTime(active.endEpochMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (active.strict) {
                            Spacer(Modifier.height(SaddSpacing.Small))
                            AssistChip(
                                onClick = {},
                                label = { Text("\u2066Strict\u2069 — سخت‌گیرانه") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = saddGlassSurfaceColor(),
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                border = BorderStroke(SaddGlass.BorderWidth, saddSpecularBorderColor()),
                            )
                        }
                        if (active.strict && uiState.uninstallProtectionOn && !uiState.uninstallAdminActive) {
                            Spacer(Modifier.height(SaddSpacing.Medium))
                            Text(
                                "محافظت از حذف نصب روشن است اما مدیر دستگاه فعال نیست؛ " +
                                    "تا فعال نشود، حذف نصب «سد» مسدود نیست.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = saddErrorTextColor(),
                            )
                            Spacer(Modifier.height(SaddSpacing.Small))
                            SaddButton(
                                text = "فعال‌سازی محافظت از حذف نصب",
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            UninstallProtection.enableIntent(context),
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.height(SaddSpacing.Medium))
                        Row {
                            SaddButton(
                                text = "تمدید ۱۵ دقیقه",
                                onClick = { viewModel.requestExtendSession() },
                            )
                            Spacer(Modifier.width(SaddSpacing.Small))
                            SaddButton(
                                text = "پایان سشن",
                                onClick = { viewModel.requestEndSession() },
                                style = SaddButtonStyle.SECONDARY,
                            )
                        }
                    }
                }
            }
        }
        if (uiState.history.isNotEmpty()) {
            item { SectionTitle("تاریخچه سشن‌ها") }
        }
        items(uiState.history, key = { it.id }) { session ->
            val reason = endedReasonLabel(session.endedReason)
            SaddCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                // ردیف تاریخچه به سبک Liquid Glass: چیپ آیکون تایمر + بازه و وضعیت.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SaddIconChip(icon = Icons.Filled.Timer, color = ChipPurple)
                    Spacer(Modifier.width(SaddSpacing.Small))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${JalaliDate.formatDateTime(session.startEpochMs)} تا ${JalaliDate.formatDateTime(session.endEpochMs)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "وضعیت: ${sessionStatusLabel(session.status)}" +
                                (if (reason.isNotEmpty()) " ($reason)" else "") +
                                (if (session.strict) " — Strict" else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(SaddSpacing.Large)) }
    }
}

/**
 * دیالوگ انتخاب حالت موقع شروع سشن — موقع هر شروع نشان داده می‌شود،
 * بدون گزینه «به خاطر بسپار». گزینه پیش‌فرض (از صفحه «حالت بلاک»)
 * با برچسب «پیش‌فرض» مشخص است، ولی شروع فقط با زدن یکی از دو گزینه
 * انجام می‌شود. شروع سشن Strict امکان سد پرو است؛ در رایگان گزینه‌اش
 * برچسب «پرو» دارد و ضربه‌اش به صفحه پرو می‌رود. سشن معمولی آزاد است.
 */
@Composable
private fun StartModeDialog(
    durationLabel: String,
    defaultStrict: Boolean,
    strictLocked: Boolean,
    onPick: (Boolean) -> Unit,
    onStrictLocked: () -> Unit,
    onDismiss: () -> Unit,
) {
    // شیشه‌ای مثل SaddDialog: سطح نیمه‌شفاف + حاشیه درخشان، شعاع ۲۸dp.
    val dialogShape = RoundedCornerShape(SaddRadius.Dialog)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(SaddGlass.BorderWidth, saddSpecularBorderBrush(), dialogShape),
        shape = dialogShape,
        containerColor = saddGlassDialogContainerColor(),
        title = { Text("سشن $durationLabel را با کدام حالت شروع کنم؟", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "هر بار موقع شروع پرسیده می‌شود؛ انتخابت ذخیره نمی‌شود.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(SaddSpacing.Medium))
                StartModeOption(
                    title = "\u2066Strict\u2069 — سخت‌گیرانه",
                    description = "پایان سشن و تغییر قانون فقط با تایپ متن طولانی یا انتظار ممکن است.",
                    icon = Icons.Filled.Lock,
                    iconColor = ChipYellow,
                    isDefault = defaultStrict,
                    locked = strictLocked,
                    onClick = { if (strictLocked) onStrictLocked() else onPick(true) },
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                StartModeOption(
                    title = "معمولی",
                    description = "پایان سشن و تغییر قانون آزاد است.",
                    icon = Icons.Filled.PlayArrow,
                    iconColor = ChipMint,
                    isDefault = !defaultStrict,
                    locked = false,
                    onClick = { onPick(false) },
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = saddPrimaryTextColor()),
            ) { Text("لغو") }
        },
    )
}

/** یک گزینه حالت در دیالوگ شروع سشن به سبک Liquid Glass: چیپ آیکون رنگی + عنوان و توضیح + برچسب پیش‌فرض؛ سطح شیشه‌ای و حاشیه پیش‌فرض primary توپر، بقیه حاشیه درخشان. گزینه قفل‌دار برچسب «پرو» می‌گیرد. */
@Composable
private fun StartModeOption(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    isDefault: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(saddGlassSurfaceColor(), RoundedCornerShape(SaddRadius.Card))
            .border(
                1.dp,
                if (isDefault) {
                    MaterialTheme.colorScheme.primary
                } else {
                    saddSpecularBorderColor()
                },
                RoundedCornerShape(SaddRadius.Card),
            )
            .clickable(onClick = onClick)
            .padding(SaddSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SaddIconChip(icon = icon, color = iconColor)
        Spacer(Modifier.width(SaddSpacing.Small))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (locked) {
                    Text(
                        "پرو",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = saddPrimaryTextColor(),
                    )
                } else if (isDefault) {
                    Text(
                        "پیش‌فرض",
                        style = MaterialTheme.typography.bodySmall,
                        color = saddPrimaryTextColor(),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
