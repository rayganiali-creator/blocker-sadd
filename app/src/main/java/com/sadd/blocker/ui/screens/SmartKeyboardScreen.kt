package com.sadd.blocker.ui.screens

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sadd.blocker.core.KeyboardBlocker
import com.sadd.blocker.service.SaddKeyboardService
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * تب «کیبورد هوشمند» (خواسته سید ۲۰۲۶-۱۱-۲۱): کیبورد خود سد که وسط
 * سشن فعال کلمه‌های بلاک‌شده را موقع تایپ خودبه‌خود پاک می‌کند.
 *
 * این صفحه وضعیت زنده کیبورد را نشان می‌دهد (فعال شده؟ کیبورد فعلی
 * گوشی است؟) و دو دکمه دارد: فعال‌سازی در تنظیمات سیستم و انتخاب
 * کیبورد. هر دو لانچ هم‌زمان با ضربه‌اند و خطایشان با پیام واقعی +
 * مسیر جایگزین نشان داده می‌شود (قانون android_intent_launch_feedback).
 *
 * محدودیت صادقانه روی خود صفحه نوشته شده: اندروید می‌خواهد کیبورد
 * یک بار در تنظیمات فعال و انتخاب شود و سد نمی‌تواند کیبورد قبلی را
 * غیرفعال کند یا جلوی برگشتن به آن را بگیرد.
 */
@Composable
fun SmartKeyboardScreen() {
    val context = LocalContext.current
    var imeEnabled by remember { mutableStateOf(isImeEnabled(context)) }
    var imeSelected by remember { mutableStateOf(isImeSelected(context)) }
    var testText by remember { mutableStateOf("") }

    fun refresh() {
        imeEnabled = isImeEnabled(context)
        imeSelected = isImeSelected(context)
    }

    // وضعیت با برگشت به صفحه (از تنظیمات سیستم) تازه می‌شود.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SaddSpacing.Large),
    ) {
        SaddTopBar(
            title = "کیبورد هوشمند",
            subtitle = "کیبورد سد؛ کلمه بلاک‌شده را موقع تایپ پاک می‌کند",
        )
        Spacer(Modifier.height(SaddSpacing.Medium))

        // وضعیت زنده کیبورد در سیستم.
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                StatusRow(
                    label = "در تنظیمات سیستم فعال شده",
                    ok = imeEnabled,
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                StatusRow(
                    label = "کیبورد فعلی گوشی است",
                    ok = imeSelected,
                )
                if (!imeEnabled || !imeSelected) {
                    Spacer(Modifier.height(SaddSpacing.Small))
                    Text(
                        "قدم ۱: «فعال‌سازی در تنظیمات» را بزن و «کیبورد هوشمند سد» را روشن کن. " +
                            "قدم ۲: «انتخاب کیبورد» را بزن و سد را انتخاب کن.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(SaddSpacing.Medium))

        SaddButton(
            text = "فعال‌سازی در تنظیمات",
            onClick = { openInputMethodSettings(context) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Small))
        SaddButton(
            text = "انتخاب کیبورد",
            onClick = { showKeyboardPicker(context) },
            modifier = Modifier.fillMaxWidth(),
            style = SaddButtonStyle.SECONDARY,
        )
        Spacer(Modifier.height(SaddSpacing.Medium))

        // توضیح کوتاه + محدودیت صادقانه.
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                Text(
                    "وسط سشن فعال، این کیبورد کلمه‌های بلاک‌شده را همان لحظه که تمام می‌شوند " +
                        "(فاصله، علائم یا ارسال) خودبه‌خود پاک می‌کند؛ صفحه بلاک باز نمی‌شود و " +
                        "تاخیر هم ندارد. خارج از سشن مثل هر کیبوردی تایپ می‌کند.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(SaddSpacing.Small))
                Text(
                    "اندروید می‌خواهد کیبورد یک بار در تنظیمات فعال و انتخاب شود؛ سد نمی‌تواند " +
                        "کیبورد قبلی را غیرفعال کند یا جلوی برگشتن به آن را بگیرد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Medium))

        // فیلد امتحان: با همین کیبورد تایپ کن و پاک‌شدن را ببین.
        SaddTextField(
            value = testText,
            onValueChange = { testText = it },
            label = "اینجا با کیبورد سد امتحانش کن…",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.XLarge))
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (ok) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (ok) Color(0xFF34D399) else MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(SaddSpacing.Small))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (ok) "بله" else "نه",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** آیا کیبورد سد در تنظیمات سیستم فعال شده است؟ */
private fun isImeEnabled(context: Context): Boolean {
    return try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return false
        val ownId = ComponentName(context, SaddKeyboardService::class.java).flattenToString()
        imm.enabledInputMethodList.any { info ->
            info.id == ownId || info.packageName == context.packageName
        }
    } catch (_: Exception) {
        false
    }
}

/** آیا کیبورد سد، کیبورد فعلی گوشی است؟ */
private fun isImeSelected(context: Context): Boolean {
    return try {
        val current = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD,
        )
        KeyboardBlocker.isSaddImeCurrent(current, context.packageName)
    } catch (_: Exception) {
        false
    }
}

/**
 * باز کردن تنظیمات کیبوردها — هم‌زمان با ضربه، با خطای واقعی و مسیر
 * جایگزین (تنظیمات اصلی) اگر صفحه اختصاصی باز نشد.
 */
private fun openInputMethodSettings(context: Context) {
    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
    try {
        if (intent.resolveActivity(context.packageManager) == null) {
            throw IllegalStateException("صفحه تنظیمات کیبورد روی این گوشی پیدا نشد")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "باز نشد: ${e.message ?: e.javaClass.simpleName} — از تنظیمات اصلی برو به «زبان و ورودی»",
            Toast.LENGTH_LONG,
        ).show()
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: Exception) {
            // هیچ مسیری باز نشد؛ پیام بالا راه دستی را گفته است.
        }
    }
}

/** نشان‌دادن انتخاب‌گر کیبورد سیستم — هم‌زمان با ضربه، با خطای واقعی. */
private fun showKeyboardPicker(context: Context) {
    try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: throw IllegalStateException("سرویس کیبورد سیستم در دسترس نیست")
        imm.showInputMethodPicker()
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "انتخاب‌گر کیبورد باز نشد: ${e.message ?: e.javaClass.simpleName} — از تنظیمات سیستم کیبورد را عوض کن",
            Toast.LENGTH_LONG,
        ).show()
    }
}
