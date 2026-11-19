package com.sadd.blocker.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadd.blocker.core.games.FollowEngine
import com.sadd.blocker.ui.screens.SaddButton
import com.sadd.blocker.ui.screens.SaddButtonStyle
import com.sadd.blocker.ui.screens.saddGlassPanel
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import kotlin.math.sin

/**
 * بازی «دنبال کن»: نقطه‌ای نورانی با حرکت فنری‌نرم روی مسیر لیساژو می‌لغزد
 * و دنباله محوشونده دارد؛ انگشت را روی آن نگه می‌داری و دنبالش می‌روی.
 * وقتی روی نقطه‌ای، حلقه دور آن می‌تپد و هر نیم‌ثانیه یک بیپ ملایم و
 * لرزش خفیف می‌آید. بی‌نهایت است؛ «شروع دوباره» فقط زمان را صفر می‌کند.
 * مسیر و تشخیص روی‌هدف در [FollowEngine] خالص است.
 */
@Composable
fun FollowGame(accent: Color) {
    val engine = remember { FollowEngine() }
    var resetKey by remember { mutableStateOf(0) }
    var timeMs by remember { mutableFloatStateOf(0f) }
    var finger by remember { mutableStateOf<Offset?>(null) }
    var onTarget by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(Offset.Zero) }
    val haptics = LocalHapticFeedback.current

    // موقعیت نمایشی نقطه که با هموارسازی نمایی به هدف لیساژو می‌رسد (حرکت فنری‌نرم).
    var smoothX by remember { mutableFloatStateOf(0.5f) }
    var smoothY by remember { mutableFloatStateOf(0.5f) }
    // رد نقطه برای دنباله نورانی.
    val trail = remember { mutableListOf<Pair<Float, Float>>() }
    var lastBlipMs by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(resetKey) {
        var startMs = -1L
        smoothX = 0.5f
        smoothY = 0.5f
        trail.clear()
        lastBlipMs = 0f
        while (true) {
            withFrameMillis { frameMs ->
                if (startMs < 0L) {
                    startMs = frameMs
                }
                val t = (frameMs - startMs).toFloat()
                timeMs = t
                val (tx, ty) = engine.targetAt(t)
                // هموارسازی نمایی: نقطه نرم و فنری دنبال هدف می‌رود.
                smoothX += (tx - smoothX) * 0.16f
                smoothY += (ty - smoothY) * 0.16f
                trail.add(Pair(smoothX, smoothY))
                if (trail.size > 26) {
                    trail.removeAt(0)
                }
                // روی‌هدف بودن با موقعیت نمایشی نقطه سنجیده می‌شود.
                val currentFinger = finger
                val size = canvasSize
                onTarget = if (currentFinger != null && size.x > 0f && size.y > 0f) {
                    val dx = currentFinger.x / size.x - smoothX
                    val dy = currentFinger.y / size.y - smoothY
                    dx * dx + dy * dy <= FollowEngine.HIT_RADIUS * FollowEngine.HIT_RADIUS
                } else {
                    false
                }
                if (onTarget && t - lastBlipMs >= 550f) {
                    lastBlipMs = t
                    GameSounds.blip()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
            if (onTarget) "آفرین، روش ماندی…" else "انگشتت را روی نقطه نگه دار و دنبالش برو",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                // قاب شیشه‌ای Liquid Glass بوم.
                .saddGlassPanel(RoundedCornerShape(SaddRadius.Card))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> finger = offset },
                        onDragEnd = { finger = null },
                        onDragCancel = { finger = null },
                        onDrag = { change, _ ->
                            finger = change.position
                            change.consume()
                        },
                    )
                },
        ) {
            canvasSize = Offset(size.width, size.height)
            // مسیر راهنمای کم‌رنگ (لیساژو).
            val pathPoints = (0..120).map { i ->
                val (nx, ny) = engine.targetAt(i * 120f)
                Offset(nx * size.width, ny * size.height)
            }
            for (i in 1 until pathPoints.size) {
                drawLine(
                    color = accent.copy(alpha = 0.12f),
                    start = pathPoints[i - 1],
                    end = pathPoints[i],
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            // دنباله نورانی نقطه: دایره‌های کوچک‌تر و کم‌رنگ‌تر به عقب.
            for ((index, point) in trail.withIndex()) {
                val fraction = (index + 1).toFloat() / trail.size.toFloat()
                drawCircle(
                    color = accent.copy(alpha = 0.10f + 0.22f * fraction),
                    radius = 4.dp.toPx() + 8.dp.toPx() * fraction,
                    center = Offset(point.first * size.width, point.second * size.height),
                )
            }
            val dotCenter = Offset(smoothX * size.width, smoothY * size.height)
            // هاله شعاعی نقطه: هسته سفید، رنگ تأکید، محو به شفاف.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        accent,
                        accent.copy(alpha = 0.35f),
                        Color.Transparent,
                    ),
                    center = dotCenter,
                    radius = 34.dp.toPx(),
                ),
                radius = 34.dp.toPx(),
                center = dotCenter,
            )
            drawCircle(color = Color.White, radius = 7.dp.toPx(), center = dotCenter)
            // حلقه تپنده دور نقطه وقتی انگشت روی آن است.
            if (onTarget) {
                val pulse = 0.5f + 0.5f * sin(timeMs / 220f)
                drawCircle(
                    color = accent.copy(alpha = 0.35f + 0.35f * pulse),
                    radius = (20f + 6f * pulse).dp.toPx(),
                    center = dotCenter,
                    style = Stroke(width = 2.5.dp.toPx()),
                )
            }
            // نشانگر انگشت.
            finger?.let { pos ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.55f),
                    radius = 14.dp.toPx(),
                    center = pos,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Medium))
        SaddButton(
            text = "شروع دوباره",
            onClick = { resetKey += 1 },
            modifier = Modifier.fillMaxWidth(),
            style = SaddButtonStyle.SECONDARY,
        )
    }
}
