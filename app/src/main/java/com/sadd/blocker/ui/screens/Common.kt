package com.sadd.blocker.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadd.blocker.domain.model.DelayOptions
import com.sadd.blocker.ui.theme.Navy
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddGlassDialogContainerColor
import com.sadd.blocker.ui.theme.saddGlassSheenBrush
import com.sadd.blocker.ui.theme.saddGlassSurfaceColor
import com.sadd.blocker.ui.theme.saddSpecularBorderBrush
import com.sadd.blocker.ui.theme.saddSpecularBorderColor

/** نوع پیام: موفقیت / خطا / اطلاع. */
enum class MessageType {
    SUCCESS, ERROR, INFO,
}

/**
 * مقیاس فشرده‌شدن نرم Liquid Glass: هنگام فشار، عنصر تا ۰٫۹۷ کوچک می‌شود
 * و با فنر برمی‌گردد (بدون ریپل متریال؛ حس دکمه شیشه‌ای iOS).
 */
@Composable
fun rememberPressScale(interactionSource: MutableInteractionSource, enabled: Boolean = true): State<Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "pressScale",
    )
}

/**
 * پنل شیشه‌ای Liquid Glass برای قاب‌های دستی (بوم بازی‌ها، قاب کارت بازی):
 * سطح نیمه‌شفاف + حاشیه درخشان گرادیانی، با همان شکل داده‌شده.
 */
@Composable
fun Modifier.saddGlassPanel(shape: Shape): Modifier {
    return this
        .background(saddGlassSurfaceColor(), shape)
        .border(SaddGlass.BorderWidth, saddSpecularBorderBrush(), shape)
}

/** سرتیتر هر صفحه؛ onBack فقط در صفحه‌های ثانویه داده می‌شود (فلش RTL-درست). */
@Composable
fun SaddTopBar(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = SaddSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

/** نام قدیمی سرتیتر — برای سازگاری، همان SaddTopBar است. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    SaddTopBar(title = title, subtitle = subtitle, onBack = onBack)
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start,
        modifier = Modifier.padding(top = SaddSpacing.Large, bottom = SaddSpacing.Small),
    )
}

/**
 * کارت مشترک «سد» به سبک Liquid Glass: سطح شیشه‌ای نیمه‌شفاف روی
 * پس‌زمینه گرادیانی، شعاع ۲۴dp (کارت hero ‏۲۸dp)، حاشیه درخشان گرادیانی
 * (لبه بالا روشن‌تر)، برق نور بالا و سایه نرم هم‌رنگ primary.
 * متن داخل کارت آلفای ۱۰۰٪ دارد تا خوانایی حفظ شود.
 */
@Composable
fun SaddCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    hero: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(if (hero) SaddRadius.Hero else SaddRadius.Card)
    val colors = CardDefaults.cardColors(containerColor = saddGlassSurfaceColor())
    // سایه را Modifier.shadow رنگی می‌سازد؛ ارتفاع خود کارت صفر است تا سایه دوتایی نشود.
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    val glassModifier = modifier
        .shadow(
            elevation = if (hero) 18.dp else 12.dp,
            shape = shape,
            ambientColor = glowColor,
            spotColor = glowColor,
        )
        .border(SaddGlass.BorderWidth, saddSpecularBorderBrush(), shape)
    // برق نور بالا روی سطح شیشه، زیر محتوای کارت.
    val sheenBrush = saddGlassSheenBrush()
    val glassContent: @Composable () -> Unit = {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(sheenBrush, shape),
            )
            content()
        }
    }
    if (onClick != null) {
        Card(
            modifier = glassModifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            onClick = onClick,
        ) {
            glassContent()
        }
    } else {
        Card(
            modifier = glassModifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
        ) {
            glassContent()
        }
    }
}

enum class SaddButtonStyle {
    PRIMARY, SECONDARY, GHOST,
}

/**
 * دکمه مشترک «سد» به سبک Liquid Glass — شعاع ۲۸dp و ارتفاع ۵۲dp:
 * - اصلی: قرص پرشده با گرادیان عمودی نرم primary + برق نور بالا + حاشیه درخشان.
 * - ثانویه: قرص شیشه‌ای مات با حاشیه درخشان و متن primary.
 * - شبح (text): TextButton متریال.
 * همه سبک‌ها هنگام فشار تا ۰٫۹۷ با فنر فشرده می‌شوند.
 */
@Composable
fun SaddButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SaddButtonStyle = SaddButtonStyle.PRIMARY,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(SaddRadius.Button)
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by rememberPressScale(interactionSource, enabled)
    val sheenBrush = saddGlassSheenBrush()
    // ارتفاع راحت برای دکمه‌های اصلی و ثانویه؛ شبح (text) ارتفاع طبیعی خودش را دارد.
    val tallModifier = modifier.height(52.dp)
    when (style) {
        SaddButtonStyle.PRIMARY -> {
            val primary = MaterialTheme.colorScheme.primary
            val gradient = Brush.verticalGradient(
                colors = listOf(lerp(primary, Color.White, 0.22f), primary),
            )
            Box(
                modifier = tallModifier
                    .scale(pressScale)
                    .alpha(if (enabled) 1f else 0.45f)
                    .shadow(
                        elevation = 12.dp,
                        shape = shape,
                        ambientColor = primary.copy(alpha = 0.45f),
                        spotColor = primary.copy(alpha = 0.45f),
                    )
                    .clip(shape)
                    .background(gradient)
                    .border(SaddGlass.BorderWidth, saddSpecularBorderBrush(), shape)
                    .semantics { role = Role.Button }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick,
                    )
                    .padding(horizontal = SaddSpacing.Large),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(sheenBrush, shape),
                )
                Text(
                    text,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        SaddButtonStyle.SECONDARY -> {
            Box(
                modifier = tallModifier
                    .scale(pressScale)
                    .alpha(if (enabled) 1f else 0.45f)
                    .shadow(
                        elevation = 8.dp,
                        shape = shape,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    )
                    .clip(shape)
                    .background(saddGlassSurfaceColor())
                    .border(SaddGlass.BorderWidth, saddSpecularBorderBrush(), shape)
                    .semantics { role = Role.Button }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick,
                    )
                    .padding(horizontal = SaddSpacing.Large),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(sheenBrush, shape),
                )
                Text(
                    text,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        SaddButtonStyle.GHOST -> TextButton(
            onClick = onClick,
            modifier = modifier.scale(pressScale),
            enabled = enabled,
            shape = shape,
            interactionSource = interactionSource,
        ) {
            Text(text)
        }
    }
}

/** فیلد متنی مشترک «سد» به سبک Liquid Glass: شعاع ۲۰dp، سطح شیشه‌ای نیمه‌شفاف و حاشیه درخشان (فوکوس: primary توپر). */
@Composable
fun SaddTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(SaddRadius.TextField),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = saddSpecularBorderColor(),
            focusedContainerColor = saddGlassSurfaceColor(),
            unfocusedContainerColor = saddGlassSurfaceColor(),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

/** دیالوگ مشترک با دکمه‌های صریح تایید/لغو. شیشه‌ای: سطح نیمه‌شفاف + حاشیه درخشان گرادیانی، شعاع ۲۸dp. */
@Composable
fun SaddDialog(
    title: String,
    text: String,
    confirmText: String,
    cancelText: String = "لغو",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(SaddRadius.Dialog)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(SaddGlass.BorderWidth, saddSpecularBorderBrush(), shape),
        shape = shape,
        containerColor = saddGlassDialogContainerColor(),
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelText) }
        },
    )
}

/** حالت خالی با آیکون. */
@Composable
fun EmptyState(text: String, icon: ImageVector? = null) {
    SaddCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = SaddSpacing.Small),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(SaddSpacing.Small))
            }
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * نوار پیام بالای صفحه با نوع (موفقیت/خطا/اطلاع) و بستن خودکار بعد از ۴ ثانیه.
 * بستن دستی با ضربدر هم ممکن است.
 */
@Composable
fun MessageBar(message: String?, type: MessageType = MessageType.INFO, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        if (message == null) {
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(4000)
        onDismiss()
    }
    if (message == null) {
        return
    }
    val containerColor = when (type) {
        MessageType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        MessageType.ERROR -> MaterialTheme.colorScheme.errorContainer
        MessageType.INFO -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (type) {
        MessageType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
        MessageType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        MessageType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (type) {
        MessageType.SUCCESS -> Icons.Filled.CheckCircle
        MessageType.ERROR -> Icons.Filled.Warning
        MessageType.INFO -> Icons.Filled.Info
    }
    val barShape = RoundedCornerShape(SaddRadius.Button)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SaddSpacing.Small)
            .border(SaddGlass.BorderWidth, saddSpecularBorderBrush(), barShape),
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.85f)),
        shape = barShape,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(Modifier.width(SaddSpacing.Small))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "بستن پیام", tint = contentColor)
            }
        }
    }
}

/**
 * اثر Snackbar برای پیام‌های ViewModel در صفحه‌های قوانین:
 * پیام «… حذف شد.» دکمه «بازگردانی» می‌گیرد که undoRemove را صدا می‌زند؛
 * بقیه پیام‌ها بدون اکشن نشان داده می‌شوند.
 */
@Composable
fun MessageSnackbarEffect(
    message: String?,
    snackbarHostState: SnackbarHostState,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(message) {
        if (message == null) {
            return@LaunchedEffect
        }
        val actionLabel = if (message.endsWith("حذف شد.")) "بازگردانی" else null
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) {
            onUndo()
        }
        onDismiss()
    }
}

/** چیپ تاخیر قانون به سبک Liquid Glass (سطح شیشه‌ای + حاشیه درخشان): null یعنی تاخیر سراسری. */
@Composable
fun DelayChip(delaySeconds: Int?, onClick: () -> Unit) {
    val label = if (delaySeconds == null) "تاخیر: سراسری" else "تاخیر: $delaySeconds ثانیه"
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = saddGlassSurfaceColor(),
            labelColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(SaddGlass.BorderWidth, saddSpecularBorderColor()),
    )
}

/**
 * دیالوگ انتخاب تاخیر یک قانون (رفع M-19):
 * رادیو فقط انتخاب را عوض می‌کند؛ دکمه «تایید» اعمال می‌کند، «لغو» رد می‌کند.
 * شیشه‌ای مثل SaddDialog: سطح نیمه‌شفاف + حاشیه درخشان، شعاع ۲۸dp.
 */
@Composable
fun DelayPickerDialog(
    currentDelay: Int?,
    onConfirm: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(currentDelay) }
    val options: List<Int?> = listOf(null) + DelayOptions.VALUES
    val shape = RoundedCornerShape(SaddRadius.Dialog)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.border(SaddGlass.BorderWidth, saddSpecularBorderBrush(), shape),
        shape = shape,
        containerColor = saddGlassDialogContainerColor(),
        title = { Text("تاخیر این قانون", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                for (option in options) {
                    val label = if (option == null) {
                        "سراسری (پیش‌فرض)"
                    } else if (option == 0) {
                        "بدون تاخیر"
                    } else {
                        "$option ثانیه"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected == option,
                            onClick = { selected = option },
                        )
                        Spacer(Modifier.width(SaddSpacing.Small))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(SaddSpacing.Small))
                Text(
                    "تاخیر یعنی بعد از تشخیص، چند ثانیه فرصت فکر کردن داری.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("تایید") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("لغو") }
        },
    )
}

/** ردیف ساده برچسب + مقدار برای صفحه عیب‌یابی و درباره. */
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

/** رنگ‌های سوییچ Liquid Glass: روشن = primary توپر، خاموش = سطح شیشه‌ای با حاشیه درخشان. */
@Composable
fun saddSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = MaterialTheme.colorScheme.primary,
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
    uncheckedTrackColor = saddGlassSurfaceColor(),
    uncheckedBorderColor = saddSpecularBorderColor(),
)

/** ردیف برچسب + توضیح + سوییچ (سوییچ به سبک Liquid Glass). */
@Composable
fun SaddSwitchRow(
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
            )
            if (description != null) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = saddSwitchColors(),
        )
    }
}

/**
 * چیپ آیکون Liquid Glass: مربع گرد ۴۴dp با رنگ توپر، برق نور بالا و
 * حاشیه روشن؛ رنگ آیکون از روشنایی رنگ چیپ انتخاب می‌شود تا روی رنگ‌های
 * روشن تیره و روی رنگ‌های تیره سفید باشد (مثل چیپ‌های رنگی موکاپ‌ها).
 * رنگ را صدا‌زننده می‌دهد (مثلاً ChipMint / ChipPurple / ChipYellow / ChipPink).
 */
@Composable
fun SaddIconChip(icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(14.dp)
    val iconTint = if (color.luminance() > 0.5f) Navy else Color.White
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(
                elevation = 10.dp,
                shape = chipShape,
                ambientColor = color.copy(alpha = 0.55f),
                spotColor = color.copy(alpha = 0.55f),
            )
            .clip(chipShape)
            .background(color)
            .border(1.dp, Color.White.copy(alpha = 0.35f), chipShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(saddGlassSheenBrush(), chipShape),
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * کنترل قطعه‌ای Liquid Glass: ظرف قرصی شیشه‌ای با حاشیه درخشان؛
 * قطعه انتخاب‌شده گرادیان primary با برق نور بالا و متن onPrimary،
 * بقیه ساده با متن کم‌رنگ. هر قطعه هنگام فشار نرم فشرده می‌شود.
 * در RTL قطعه اول سمت راست (شروع) دیده می‌شود.
 */
@Composable
fun SaddSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerShape = RoundedCornerShape(SaddRadius.Button)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(containerShape)
            .background(saddGlassSurfaceColor())
            .border(SaddGlass.BorderWidth, saddSpecularBorderBrush(), containerShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, option ->
            SegmentItem(
                label = option,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val segmentShape = RoundedCornerShape(SaddRadius.Button)
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by rememberPressScale(interactionSource)
    val primary = MaterialTheme.colorScheme.primary
    val selectedBrush = Brush.verticalGradient(
        colors = listOf(lerp(primary, Color.White, 0.20f), primary),
    )
    Box(
        modifier = modifier
            .scale(pressScale)
            .clip(segmentShape)
            .then(
                if (selected) {
                    Modifier.background(selectedBrush)
                } else {
                    Modifier.background(Color.Transparent)
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp, horizontal = SaddSpacing.Small),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(saddGlassSheenBrush(), segmentShape),
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * کارت سوییچ Liquid Glass: کارت با چیپ آیکون رنگی + برچسب/توضیح + سوییچ
 * (مثل کارت‌های «Live count» و «Protected typing» اسکرین‌شات‌ها).
 * SaddSwitchRow ساده سر جایش می‌ماند؛ این نسخه کارتی آن است.
 */
@Composable
fun SaddToggleCard(
    label: String,
    description: String? = null,
    icon: ImageVector,
    iconColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SaddCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaddIconChip(icon = icon, color = iconColor)
            Spacer(Modifier.width(SaddSpacing.Medium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                )
                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                    )
                }
            }
            Spacer(Modifier.width(SaddSpacing.Small))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = saddSwitchColors(),
            )
        }
    }
}
