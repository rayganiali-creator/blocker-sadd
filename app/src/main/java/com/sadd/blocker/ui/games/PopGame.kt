package com.sadd.blocker.ui.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.sadd.blocker.core.games.PopEngine
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddHairlineColor
import kotlinx.coroutines.delay

/**
 * بازی «ترکاندن»: شبکه ۶×۸ حباب که با ضربه می‌ترکند (انیمیشن کوچک‌شدن و
 * کمرنگ‌شدن) و حدود ۲ ثانیه بعد دوباره ساخته می‌شوند تا هیچ‌وقت تمام نشوند.
 * وضعیت و شمارنده در [PopEngine] خالص است؛ فهرست آینه‌ای Compose فقط رندر را تازه می‌کند.
 */
@Composable
fun PopGame(accent: Color) {
    val engine = remember { PopEngine(rows = 8, cols = 6) }
    // آینه وضعیت ترکیده‌بودن برای Compose (موتور خودش وضعیت Compose ندارد).
    val poppedStates = remember {
        mutableStateListOf<Boolean>().apply {
            repeat(engine.size) { add(false) }
        }
    }
    val poppedAt = remember { LongArray(engine.size) }

    // بازسازی حباب‌ها حدود ۲ ثانیه بعد از ترکیدن.
    LaunchedEffect(Unit) {
        while (true) {
            delay(200)
            val now = System.currentTimeMillis()
            for (i in 0 until engine.size) {
                if (engine.isPopped(i) && now - poppedAt[i] >= 2000L) {
                    engine.regenerate(i)
                    poppedStates[i] = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "ترکیده: ${engine.poppedCount}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        for (row in 0 until engine.rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SaddSpacing.XSmall),
            ) {
                for (col in 0 until engine.cols) {
                    val index = row * engine.cols + col
                    Bubble(
                        popped = poppedStates[index],
                        color = accent,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (engine.pop(index)) {
                                poppedStates[index] = true
                                poppedAt[index] = System.currentTimeMillis()
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(SaddSpacing.XSmall))
        }
    }
}

@Composable
private fun Bubble(popped: Boolean, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (popped) 0.25f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "bubbleScale",
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(SaddSpacing.XSmall)
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = if (popped) 0.25f else 0.85f))
            // حاشیه مویی Detox دور حباب.
            .border(SaddGlass.BorderWidth, saddHairlineColor(), CircleShape)
            .clickable { onClick() },
    )
}
