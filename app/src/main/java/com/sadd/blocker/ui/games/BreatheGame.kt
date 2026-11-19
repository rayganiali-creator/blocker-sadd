package com.sadd.blocker.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadd.blocker.core.games.BreatheEngine
import com.sadd.blocker.ui.screens.SaddButton
import com.sadd.blocker.ui.screens.SaddButtonStyle
import com.sadd.blocker.ui.screens.saddGlassPanel
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * بازی «نفس بکش»: دایره‌ای نورانی با حرکت نرم (ایزینگ هموار روی پیشرفت
 * موتور) در ۴ ثانیه بزرگ (دم) و در ۶ ثانیه کوچک (بازدم) می‌شود؛ هاله
 * محیطی همراهش نفس می‌کشد و در آغاز هر فاز یک لحن بسیار ملایم می‌آید.
 * زمان‌بندی و پیشرفت در [BreatheEngine] خالص است؛ تیکر فریم‌به‌فریم است.
 */
@Composable
fun BreatheGame(accent: Color) {
    val engine = remember { BreatheEngine() }
    var resetKey by remember { mutableStateOf(0) }
    var elapsedMs by remember { mutableFloatStateOf(0f) }
    var lastPhase by remember { mutableStateOf<BreatheEngine.Phase?>(null) }

    LaunchedEffect(resetKey) {
        var startMs = -1L
        lastPhase = null
        while (true) {
            withFrameMillis { frameMs ->
                if (startMs < 0L) {
                    startMs = frameMs
                }
                elapsedMs = (frameMs - startMs).toFloat()
                val phase = engine.phaseAt(elapsedMs)
                if (phase != lastPhase) {
                    // لحن بسیار ملایم آغاز هر فاز: دم بالاتر، بازدم پایین‌تر.
                    if (lastPhase != null) {
                        GameSounds.tone(if (phase == BreatheEngine.Phase.INHALE) 392f else 294f)
                    }
                    lastPhase = phase
                }
            }
        }
    }

    val phase = engine.phaseAt(elapsedMs)
    val rawProgress = engine.progressAt(elapsedMs)
    // ایزینگ هموار: حرکت در آغاز و پایان فاز آرام می‌شود تا نفس طبیعی حس شود.
    val eased = rawProgress * rawProgress * (3f - 2f * rawProgress)
    val scaleFraction = when (phase) {
        BreatheEngine.Phase.INHALE -> eased
        BreatheEngine.Phase.EXHALE -> 1f - eased
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            when (phase) {
                BreatheEngine.Phase.INHALE -> "دم…"
                BreatheEngine.Phase.EXHALE -> "بازدم…"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        // قاب دایره‌ای شیشه‌ای دور دایره تنفس.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .saddGlassPanel(CircleShape)
                .padding(SaddSpacing.Medium),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.minDimension / 2f
                val radius = maxRadius * (0.35f + 0.60f * scaleFraction)
                // هاله محیطی که با دایره نفس می‌کشد.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.10f + 0.22f * scaleFraction),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = maxRadius,
                    ),
                    radius = maxRadius,
                    center = center,
                )
                // دایره اصلی با گرادیان شعاعی (هسته روشن تا لبه رنگی).
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            lerp(accent, Color.White, 0.40f),
                            accent,
                            lerp(accent, Color.Black, 0.15f),
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
                // حلقه راهنمای کم‌رنگ در بیشینه اندازه.
                drawCircle(
                    color = accent.copy(alpha = 0.25f),
                    radius = maxRadius * 0.95f,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Medium))
        SaddButton(
            text = "از اول",
            onClick = { resetKey += 1 },
            modifier = Modifier.fillMaxWidth(),
            style = SaddButtonStyle.SECONDARY,
        )
    }
}
