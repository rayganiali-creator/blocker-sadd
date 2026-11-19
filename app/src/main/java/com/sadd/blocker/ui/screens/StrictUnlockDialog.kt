package com.sadd.blocker.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sadd.blocker.domain.STRICT_PASSAGES
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.ChipYellow
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddGlassBorderColor
import com.sadd.blocker.ui.theme.saddHairlineColor
import kotlinx.coroutines.delay

/**
 * دیالوگ بازگشایی Strict — قابل استفاده از هر صفحه‌ای که یک تغییرِ
 * محافظت‌شده در Strict را می‌خواهد (پایان سشن، تغییر تنظیمات Strict و…).
 *
 * - PASSAGE: کاربر باید متن نشان‌داده‌شده را دقیقاً تایپ کند؛ نوار پیشرفت
 *   و پیام عدم تطابق زنده نشان داده می‌شود؛ «متن دیگر» متن دیگری می‌آورد.
 * - COOLDOWN: اگر درخواستی ثبت نشده باشد، دکمه «شروع انتظار» آن را با زمان
 *   دیواری همین حالا ثبت می‌کند ([onRecordCooldownRequest])؛ اگر ثبت شده باشد،
 *   شمارش معکوس زنده (هر ثانیه) نشان داده می‌شود و دکمه تایید فقط وقتی
 *   انتظار کامل گذشته باشد فعال است.
 *
 * تایید (تطابق کامل متن یا پایان انتظار) فقط از راه [onUnlocked] به
 * فراخواننده خبر داده می‌شود؛ خود دیالوگ هیچ کاری را اعمال نمی‌کند.
 */
@Composable
fun StrictUnlockDialog(
    method: StrictUnlockMethod,
    cooldownHours: Int,
    cooldownRequest: StrictCooldownRequest?,
    onRecordCooldownRequest: () -> Unit,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit,
) {
    // تیکر شمارش معکوس: فقط برای حالت COOLDOWN لازم است، ولی ساده‌ترین
    // شکل این است که دیالوگ خودش ساعت را هر ثانیه به‌روز کند.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
    }
    var passageIndex by rememberSaveable {
        mutableStateOf(StrictUnlock.randomPassageIndex())
    }
    // مقدار فیلد تایپ متن بازگشایی؛ TextFieldValue تا بتوان ترکیب‌بندی
    // IME (composition) را از چسباندن واقعی جدا کرد و چسباندن را رد کرد.
    var fieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    Dialog(onDismissRequest = onDismiss) {
        SaddCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    SaddGlass.BorderWidth,
                    saddGlassBorderColor(),
                    RoundedCornerShape(SaddRadius.Hero),
                ),
            hero = true,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SaddSpacing.Large)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // چیپ قفل Detox کنار عنوان دیالوگ (فقط ظاهری).
                    SaddIconChip(icon = Icons.Filled.Lock, color = ChipYellow)
                    Spacer(Modifier.width(SaddSpacing.Medium))
                    Text(
                        "بازگشایی Strict",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(SaddSpacing.Small))
                if (method == StrictUnlockMethod.PASSAGE) {
                    val passage = STRICT_PASSAGES[passageIndex]
                    val typed = fieldValue.text
                    val matched = StrictUnlock.isPassageMatch(typed, passage.text)
                    val progress = StrictUnlock.passageProgress(typed, passage.text)
                    val isPrefix = typed.isBlank() || StrictUnlock.isPassagePrefix(typed, passage.text)
                    Text(
                        "متن زیر را دقیقاً تایپ کن؛ عمداً طول می‌کشد تا تصمیم عجولانه نگیری.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(SaddSpacing.Medium))
                    SaddCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                SaddGlass.BorderWidth,
                                saddGlassBorderColor(),
                                RoundedCornerShape(SaddRadius.Card),
                            ),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                            Text(passage.text, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(SaddSpacing.Small))
                            Text(
                                "منبع: ${passage.source}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(SaddSpacing.Medium))
                    // چسباندن متن ممنوع است: نوارابزار انتخاب متن بی‌عمل است
                    // تا منوی «چسباندن» اصلاً نیاید و ویرایش چندنویسه‌ای هم
                    // رد می‌شود؛ متن بازگشایی باید با دست تایپ شود.
                    CompositionLocalProvider(LocalTextToolbar provides NoOpTextToolbar) {
                        OutlinedTextField(
                            value = fieldValue,
                            onValueChange = { new ->
                                when {
                                    // در حال ترکیب‌بندی IME است؛ بپذیر.
                                    new.composition != null -> fieldValue = new
                                    // چسباندن است؛ رد کن و مقدار قبلی بماند.
                                    StrictUnlock.isPasteInsert(fieldValue.text, new.text) -> Unit
                                    else -> fieldValue = new
                                }
                            },
                            label = { Text("متن را اینجا تایپ کن") },
                            singleLine = false,
                            shape = RoundedCornerShape(SaddRadius.TextField),
                            // رنگ‌های Detox فیلد تایپ (فقط ظاهری؛ منطق رد چسباندن بالا دست‌نخورده است).
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = saddHairlineColor(),
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedLabelColor = MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                cursorColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(SaddSpacing.Small))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${(progress * 100).toInt()}% کامل شده",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!isPrefix) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "متن تایپ‌شده با متن بالا مطابقت ندارد؛ " +
                                "از جایی که اشتباه شده، دوباره بنویس.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(SaddSpacing.Medium))
                    SaddButton(
                        text = "تایید و بازگشایی",
                        onClick = onUnlocked,
                        enabled = matched,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(SaddSpacing.Small))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SaddButton(
                            text = "متن دیگر",
                            onClick = {
                                passageIndex = StrictUnlock.randomPassageIndex(except = passageIndex)
                                fieldValue = TextFieldValue("")
                            },
                            style = SaddButtonStyle.SECONDARY,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(SaddSpacing.Small))
                        SaddButton(
                            text = "لغو",
                            onClick = onDismiss,
                            style = SaddButtonStyle.GHOST,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    CooldownUnlockBody(
                        cooldownHours = cooldownHours,
                        cooldownRequest = cooldownRequest,
                        nowMs = nowMs,
                        onRecordCooldownRequest = onRecordCooldownRequest,
                        onUnlocked = onUnlocked,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun CooldownUnlockBody(
    cooldownHours: Int,
    cooldownRequest: StrictCooldownRequest?,
    nowMs: Long,
    onRecordCooldownRequest: () -> Unit,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (cooldownRequest == null) {
        Text(
            "باید $cooldownHours ساعت صبر کنی؛ زمان از همین حالا ثبت می‌شود و " +
                "با بستن اپ یا ری‌استارت صفر نمی‌شود.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        SaddButton(
            text = "شروع انتظار ($cooldownHours ساعت)",
            onClick = onRecordCooldownRequest,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Small))
        SaddButton(
            text = "لغو",
            onClick = onDismiss,
            style = SaddButtonStyle.GHOST,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    val remaining = StrictUnlock.cooldownRemainingMs(cooldownRequest, nowMs)
    if (remaining > 0L) {
        Text(
            "زمان انتظار ثبت شده است؛ بستن اپ یا ری‌استارت هم آن را صفر نمی‌کند.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SaddIconChip(icon = Icons.Filled.Schedule, color = ChipMint)
            Spacer(Modifier.width(SaddSpacing.Medium))
            Text(
                StrictUnlock.formatCooldownRemaining(remaining),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "باقی‌مانده تا پایان انتظار",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        SaddButton(
            text = "باشه",
            onClick = onDismiss,
            style = SaddButtonStyle.SECONDARY,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Text(
            "زمان انتظار کامل گذشت. اگر هنوز مطمئنی، تایید کن.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        SaddButton(
            text = "تایید و بازگشایی",
            onClick = onUnlocked,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Small))
        SaddButton(
            text = "لغو",
            onClick = onDismiss,
            style = SaddButtonStyle.GHOST,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * نوارابزار متن بی‌عمل: منوی انتخاب متن (از جمله «چسباندن») هیچ‌وقت
 * نشان داده نمی‌شود؛ روی فیلد تایپ متن بازگشایی Strict نصب می‌شود تا
 * متن فقط با دست تایپ شود.
 */
private object NoOpTextToolbar : TextToolbar {

    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        // عمداً خالی: منویی نشان داده نمی‌شود.
    }

    override fun hide() {
        // عمداً خالی: چیزی برای پنهان‌کردن نیست.
    }
}
