package com.sadd.blocker.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sadd.blocker.ui.theme.BlockerTheme
import com.sadd.blocker.ui.theme.NavyDeep

/**
 * صفحه بلاک — تیره سرمه‌ای، خرس «سد» به‌عنوان ماسکوت (حفظ هویت نسخه قبلی).
 * طبق موکاپ تاییدشده: خرس داخل سپر، دلیل بلاک، دسته و دکمه «بازگشت».
 * شمارش معکوس تاخیر و منطق مداخله در فاز ۴ وصل می‌شود؛
 * هرگز HOME یا قفل گوشی فراخوانی نمی‌شود — فقط بازگشت (fail-open).
 */
class BlockedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reason = intent.getStringExtra("reason") ?: "قانون فعال"
        val category = intent.getStringExtra("category")
        val delaySeconds = intent.getIntExtra("delaySeconds", -1)
        setContent {
            BlockerTheme(darkTheme = true) {
                BlockedContent(
                    reason = reason,
                    category = category,
                    delaySeconds = if (delaySeconds >= 0) delaySeconds else null,
                    onBack = { finish() },
                )
            }
        }
    }
}

@Composable
fun BlockedContent(
    reason: String,
    category: String? = null,
    delaySeconds: Int? = null,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🐻", style = MaterialTheme.typography.displayLarge)
        Text(
            "سد",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "این محتوا بلاک شد",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(reason, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        if (category != null) {
            Spacer(Modifier.height(12.dp))
            AssistChip(onClick = {}, label = { Text("دسته: $category") })
        }
        if (delaySeconds != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "تاخیر مداخله: $delaySeconds ثانیه",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "یک لحظه مکث کن — این انتخاب آگاهانه توست، نه یک اتفاق.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
        )
        Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) {
            Text("بازگشت")
        }
    }
}
