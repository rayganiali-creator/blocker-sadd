package com.sadd.blocker.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sadd.blocker.ui.screens.SaddButton
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddHairlineColor
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.sin

/** موقعیت نور شناور در زمان t روی مسیر نرم لیساژو (مختصات پیکسلی بوم). */
private fun followDotPosition(t: Float, width: Float, height: Float): Offset {
    val centerX = width / 2f
    val centerY = height / 2f
    return Offset(
        x = centerX + width * 0.38f * sin(0.9f * t),
        y = centerY + height * 0.38f * sin(1.3f * t + 1.2f),
    )
}

/** شعاع نور (و ناحیه امتیاز) متناسب با اندازه بوم. */
private fun followDotRadius(width: Float, height: Float): Float {
    return min(width, height) * 0.09f
}

/**
 * بازی «دنبال کن»: نوری درخشان روی مسیر لیساژو شناور است و کاربر با انگشت
 * دنبالش می‌کند. تا وقتی انگشت داخل شعاع نور باشد امتیاز جمع می‌شود.
 * بازی بی‌پایان است؛ «شروع دوباره» امتیاز و زمان را صفر می‌کند.
 */
@Composable
fun FollowGame(accent: Color) {
    var timeSec by remember { mutableStateOf(0f) }
    var score by remember { mutableStateOf(0) }
    var fingerPos by remember { mutableStateOf<Offset?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // تیکر زمان و امتیاز: هر فریم، اگر انگشت روی نور است امتیاز یکی جلو می‌رود.
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            timeSec += 0.016f
            val finger = fingerPos
            if (finger != null && canvasSize.width > 0 && canvasSize.height > 0) {
                val w = canvasSize.width.toFloat()
                val h = canvasSize.height.toFloat()
                val dot = followDotPosition(timeSec, w, h)
                if ((finger - dot).getDistance() <= followDotRadius(w, h)) {
                    score += 1
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "امتیاز: $score",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Small))
        Text(
            "انگشتت را روی نور نگه دار و دنبالش کن",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        // رنگ حلقه انگشت بیرون از Canvas خوانده می‌شود (خواندن تم داخل لمبدای رسم مجاز نیست).
        val fingerColor = MaterialTheme.colorScheme.onSurface
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                // قاب Detox بوم: شعاع کارت + حاشیه مویی.
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(SaddRadius.Card))
                .border(SaddGlass.BorderWidth, saddHairlineColor(), RoundedCornerShape(SaddRadius.Card))
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { fingerPos = it },
                        onDragEnd = { fingerPos = null },
                        onDragCancel = { fingerPos = null },
                        onDrag = { change, _ -> fingerPos = change.position },
                    )
                },
        ) {
            val w = size.width
            val h = size.height
            val radius = followDotRadius(w, h)
            // ردّ نور: موقعیت‌های چند لحظه قبل با آلفای کاهنده.
            for (step in 12 downTo 1) {
                val past = followDotPosition(timeSec - step * 0.06f, w, h)
                drawCircle(
                    color = accent.copy(alpha = 0.05f + 0.02f * (12 - step)),
                    radius = radius * 0.55f,
                    center = past,
                )
            }
            val dot = followDotPosition(timeSec, w, h)
            // هاله نور و خود نور.
            drawCircle(color = accent.copy(alpha = 0.25f), radius = radius * 2f, center = dot)
            drawCircle(color = accent, radius = radius, center = dot)
            // حلقه انگشت کاربر.
            val finger = fingerPos
            if (finger != null) {
                drawCircle(
                    color = fingerColor,
                    radius = radius * 0.8f,
                    center = finger,
                    style = Stroke(width = 3f),
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Medium))
        SaddButton(
            text = "شروع دوباره",
            onClick = {
                score = 0
                timeSec = 0f
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
