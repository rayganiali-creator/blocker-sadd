package com.sadd.blocker.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sadd.blocker.ui.theme.BlockerTheme

/**
 * صفحه بلاک — تیره سرمه‌ای، خرس «سد» به‌عنوان ماسکوت (حفظ هویت نسخه قبلی).
 * در فاز ۱ فقط ساختار و دکمه بازگشت (fail-open) را دارد؛
 * تصویر خرس در فاز ۴ با نسخه نهایی جایگزین می‌شود.
 * هرگز HOME یا قفل گوشی فراخوانی نمی‌شود — فقط بازگشت.
 */
class BlockedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reason = intent.getStringExtra("reason") ?: "قانون فعال"
        setContent {
            BlockerTheme(darkTheme = true) {
                BlockedContent(reason = reason, onBack = { finish() })
            }
        }
    }
}

@Composable
fun BlockedContent(reason: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🐻", style = MaterialTheme.typography.displayLarge)
        Text("سد", style = MaterialTheme.typography.displayMedium)
        Text("این محتوا بلاک شد", style = MaterialTheme.typography.titleLarge)
        Text(reason, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) {
            Text("بازگشت")
        }
    }
}
