package com.sadd.blocker.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadd.blocker.core.games.LetItPassEngine
import com.sadd.blocker.ui.screens.SaddButton
import com.sadd.blocker.ui.screens.SaddButtonStyle
import com.sadd.blocker.ui.screens.saddGlassPanel
import com.sadd.blocker.ui.theme.Mint
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.Sky
import kotlin.math.sin

/**
 * بازی «بگذار برود»: فکری که در ذهن است روی برگ گذاشته می‌شود و برگ با
 * حرکت نرم و شناور روی جویبار می‌رود؛ آب موج‌دار و برق‌زن است و متن فکر
 * هم‌زمان با رفتن برگ محو می‌شود. بدون امتیاز؛ فقط رهاکردن آرام و تکرار.
 * پیشرفت برگ در [LetItPassEngine] خالص است؛ تیکر فریم‌به‌فریم است.
 */
@Composable
fun LetItPassGame(accent: Color) {
    var releasedAtMs by remember { mutableStateOf<Long?>(null) }
    var nowMs by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var startMs = -1L
        while (true) {
            withFrameMillis { frameMs ->
                if (startMs < 0L) {
                    startMs = frameMs
                }
                nowMs = (frameMs - startMs).toFloat()
            }
        }
    }

    val released = releasedAtMs
    val progress = if (released == null) {
        0f
    } else {
        LetItPassEngine.progressAt(nowMs - released.toFloat())
    }
    // ایزینگ هموار برای رفتن برگ: آغاز آرام، پایان آرام‌تر.
    val eased = progress * progress * (3f - 2f * progress)
    val thoughtAlpha = 1f - eased

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            if (released == null) {
                "فکری که الان در ذهنت هست را روی برگ بگذار…"
            } else {
                "دارد می‌رود… فقط نگاهش کن"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        // قاب شیشه‌ای جویبار.
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .saddGlassPanel(RoundedCornerShape(SaddRadius.Card)),
        ) {
            val streamY = size.height * 0.62f
            // موج‌های آب: سه خط سینوسی با فاز زمانی متفاوت و آلفای متغیر (برق آب).
            for (wave in 0..2) {
                val waveY = streamY + (wave - 1) * 14.dp.toPx()
                val phase = nowMs / (700f + wave * 260f)
                val wavePath = Path()
                var x = 0f
                while (x <= size.width) {
                    val y = waveY + sin(x / 46f + phase) * 5.dp.toPx()
                    if (x == 0f) {
                        wavePath.moveTo(x, y)
                    } else {
                        wavePath.lineTo(x, y)
                    }
                    x += 8f
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Sky.copy(alpha = 0.15f),
                            Sky.copy(alpha = 0.45f),
                            Sky.copy(alpha = 0.15f),
                        ),
                    ),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            // برگ: با پیشرفت نرم از چپ به راست می‌رود، شناور می‌شود و کمی می‌چرخد.
            val leafX = size.width * (0.12f + 0.76f * eased)
            val bob = sin(nowMs / 480f) * 4.dp.toPx()
            val leafY = streamY - 8.dp.toPx() + bob
            val leafTilt = sin(nowMs / 900f) * 14f
            rotate(degrees = leafTilt, pivot = Offset(leafX, leafY)) {
                val leafPath = Path().apply {
                    moveTo(leafX - 26.dp.toPx(), leafY)
                    quadraticBezierTo(
                        leafX, leafY - 20.dp.toPx(),
                        leafX + 26.dp.toPx(), leafY,
                    )
                    quadraticBezierTo(
                        leafX, leafY + 14.dp.toPx(),
                        leafX - 26.dp.toPx(), leafY,
                    )
                    close()
                }
                // هاله نرم برگ.
                drawPath(
                    path = leafPath,
                    color = Mint.copy(alpha = 0.20f),
                    style = Stroke(width = 8.dp.toPx()),
                )
                drawPath(
                    path = leafPath,
                    brush = Brush.linearGradient(
                        colors = listOf(lerp(Mint, Color.White, 0.30f), Mint),
                        start = Offset(leafX - 26.dp.toPx(), leafY - 20.dp.toPx()),
                        end = Offset(leafX + 26.dp.toPx(), leafY + 14.dp.toPx()),
                    ),
                )
                // رگ برگ.
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(leafX - 22.dp.toPx(), leafY),
                    end = Offset(leafX + 22.dp.toPx(), leafY),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Medium))
        // متن فکر با رفتن برگ محو می‌شود.
        Text(
            "این فکر هم می‌گذرد…",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (released == null) 1f else thoughtAlpha),
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        if (released == null) {
            SaddButton(
                text = "رها کن",
                onClick = {
                    GameSounds.blip()
                    releasedAtMs = nowMs.toLong()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            SaddButton(
                text = "فکر دیگری رها کن",
                onClick = { releasedAtMs = null },
                modifier = Modifier.fillMaxWidth(),
                style = SaddButtonStyle.SECONDARY,
            )
        }
    }
}
