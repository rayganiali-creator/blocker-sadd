package com.sadd.blocker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadd.blocker.domain.model.DelayOptions
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddGlassBorderColor
import com.sadd.blocker.ui.theme.saddGlassDialogContainerColor

/** نوع پیام: موفقیت / خطا / اطلاع. */
enum class MessageType {
    SUCCESS, ERROR, INFO,
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
 * کارت مشترک «سد»: نرم و بالشتی — شعاع ۲۴dp (کارت hero ‏۲۸dp)، سطح تونال
 * (surfaceVariant) با elevation ملایم ۲dp (hero ‏۴dp)؛ هرگز سایه سیاه تند ندارد.
 * شیشه‌ای: حاشیه نازک ۱dp به رنگ onSurface با ۱۲٪ آلفا (توکن SaddGlass).
 */
@Composable
fun SaddCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    hero: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(if (hero) SaddRadius.Hero else SaddRadius.Card)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    val elevation = CardDefaults.cardElevation(defaultElevation = if (hero) 4.dp else 2.dp)
    val border = BorderStroke(SaddGlass.BorderWidth, saddGlassBorderColor())
    if (onClick != null) {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            onClick = onClick,
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
        ) {
            content()
        }
    }
}

enum class SaddButtonStyle {
    PRIMARY, SECONDARY, GHOST,
}

/** دکمه مشترک «سد»: اصلی / ثانویه (outlined) / شبح (text) — بالشتی، شعاع ۲۸dp و ارتفاع ۵۲dp. شیشه‌ای: دکمه اصلی سطح primary با ۸۵٪ آلفا + حاشیه نازک ۱dp (توکن SaddGlass)؛ متن روی آن ۱۰۰٪ می‌ماند. */
@Composable
fun SaddButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SaddButtonStyle = SaddButtonStyle.PRIMARY,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(SaddRadius.Button)
    // ارتفاع راحت برای دکمه‌های اصلی و ثانویه؛ شبح (text) ارتفاع طبیعی خودش را دارد.
    val tallModifier = modifier.height(52.dp)
    when (style) {
        SaddButtonStyle.PRIMARY -> Button(
            onClick = onClick,
            modifier = tallModifier,
            enabled = enabled,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = SaddGlass.ButtonContainerAlpha),
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            border = BorderStroke(SaddGlass.BorderWidth, saddGlassBorderColor()),
        ) {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
        SaddButtonStyle.SECONDARY -> OutlinedButton(
            onClick = onClick,
            modifier = tallModifier,
            enabled = enabled,
            shape = shape,
        ) {
            Text(text)
        }
        SaddButtonStyle.GHOST -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
        ) {
            Text(text)
        }
    }
}

/** فیلد متنی مشترک «سد» با شعاع ۲۰dp. */
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
    )
}

/** دیالوگ مشترک با دکمه‌های صریح تایید/لغو. شیشه‌ای: سطح surface با ۹۲٪ آلفا + حاشیه نازک ۱dp (توکن SaddGlass)، شعاع ۲۸dp. */
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
        modifier = Modifier.border(SaddGlass.BorderWidth, saddGlassBorderColor(), shape),
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
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = SaddSpacing.Small),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(SaddRadius.Button),
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

/** چیپ تاخیر قانون: null یعنی تاخیر سراسری. */
@Composable
fun DelayChip(delaySeconds: Int?, onClick: () -> Unit) {
    val label = if (delaySeconds == null) "تاخیر: سراسری" else "تاخیر: $delaySeconds ثانیه"
    AssistChip(onClick = onClick, label = { Text(label) })
}

/**
 * دیالوگ انتخاب تاخیر یک قانون (رفع M-19):
 * رادیو فقط انتخاب را عوض می‌کند؛ دکمه «تایید» اعمال می‌کند، «لغو» رد می‌کند.
 * شیشه‌ای مثل SaddDialog: سطح ۹۲٪ + حاشیه ۱dp، شعاع ۲۸dp.
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
        modifier = Modifier.border(SaddGlass.BorderWidth, saddGlassBorderColor(), shape),
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

/** ردیف برچسب + توضیح + سوییچ. */
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
                Text(description, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
