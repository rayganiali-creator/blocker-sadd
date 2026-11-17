package com.sadd.blocker.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sadd.blocker.R
import com.sadd.blocker.data.datastore.AppTheme
import com.sadd.blocker.ui.theme.BlockerTheme
import com.sadd.blocker.ui.theme.Mint

/**
 * صفحه بلاک — همیشه تیره سرمه‌ای، خرس «سد» به‌عنوان ماسکوت (حفظ هویت نسخه قبلی).
 * طبق موکاپ تاییدشده: خرس، دلیل بلاک، دسته و فقط دکمه «بازگشت».
 * شمارش معکوس تاخیر و منطق مداخله در فاز ۴ وصل می‌شود؛
 * هرگز HOME یا قفل گوشی فراخوانی نمی‌شود — فقط بازگشت (fail-open).
 */
class BlockedActivity : ComponentActivity() {
    // M-8: با launchMode=singleTop، بازفرستادن intent باید دلیل بلاک را به‌روز کند.
    private val reasonState = mutableStateOf("قانون فعال")
    private val categoryState = mutableStateOf<String?>(null)
    private val delayState = mutableStateOf<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyIntent(intent)
        setContent {
            BlockerTheme(theme = AppTheme.DARK) {
                BlockedContent(
                    reason = reasonState.value,
                    category = categoryState.value,
                    delaySeconds = delayState.value,
                    onBack = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntent(intent)
    }

    private fun applyIntent(intent: Intent?) {
        reasonState.value = intent?.getStringExtra("reason") ?: "قانون فعال"
        categoryState.value = intent?.getStringExtra("category")
        val delay = intent?.getIntExtra("delaySeconds", -1) ?: -1
        delayState.value = if (delay >= 0) delay else null
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
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.sadd_bear),
            contentDescription = "خرس سد",
            modifier = Modifier.size(160.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "سد",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Mint,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "این محتوا بلاک شد",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (category != null) {
            Spacer(Modifier.height(12.dp))
            AssistChip(onClick = {}, label = { Text("دسته: $category") })
        }
        if (delaySeconds != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "تاخیر مداخله: $delaySeconds ثانیه",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "یک لحظه مکث کن — این انتخاب آگاهانه توست، نه یک اتفاق.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SaddButton(
            text = "بازگشت",
            onClick = onBack,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}
