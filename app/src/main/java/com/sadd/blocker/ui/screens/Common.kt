package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sadd.blocker.domain.model.DelayOptions

/** سرتیتر هر صفحه؛ onBack فقط در صفحه‌های ثانویه داده می‌شود. */
@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "بازگشت")
            }
        }
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

@Composable
fun EmptyState(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

/** نوار پیام (موفقیت/خطا) بالای صفحه؛ با ضربدر بسته می‌شود. */
@Composable
fun MessageBar(message: String?, onDismiss: () -> Unit) {
    if (message == null) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = "بستن پیام")
            }
        }
    }
}

/** چیپ تاخیر قانون: null یعنی تاخیر سراسری. */
@Composable
fun DelayChip(delaySeconds: Int?, onClick: () -> Unit) {
    val label = if (delaySeconds == null) "تاخیر: سراسری" else "تاخیر: $delaySeconds ثانیه"
    AssistChip(onClick = onClick, label = { Text(label) })
}

/** دیالوگ انتخاب تاخیر یک قانون. */
@Composable
fun DelayPickerDialog(
    currentDelay: Int?,
    onConfirm: (Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val options: List<Int?> = listOf(null) + DelayOptions.VALUES
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تاخیر این قانون") },
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
                            selected = currentDelay == option,
                            onClick = { onConfirm(option) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "تاخیر یعنی بعد از تشخیص، چند ثانیه فرصت فکر کردن داری.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
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
