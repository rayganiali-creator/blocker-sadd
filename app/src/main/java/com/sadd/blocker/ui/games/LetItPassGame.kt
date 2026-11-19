package com.sadd.blocker.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadd.blocker.ui.screens.SaddButton
import com.sadd.blocker.ui.screens.SaddTextField
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.Sky
import com.sadd.blocker.ui.theme.Teal
import com.sadd.blocker.ui.theme.saddHairlineColor
import kotlinx.coroutines.delay
import kotlin.math.sin

/** ارتفاع جویبار در مختصات x (منحنی سینوسی آرام روی بوم). */
private fun streamY(x: Float, width: Float, height: Float): Float {
    return height * 0.52f + height * 0.16f * sin(x / width * 6.2831f * 1.5f)
}

/**
 * بازی «بگذار برود»: فکر (اختیاری) روی برگ گذاشته می‌شود و برگ با جریان
 * جویبار می‌رود؛ متن فکر هرچه برگ دورتر شود کمرنگ‌تر می‌شود. امتیاز ندارد.
 * «رها کن» شناورشدن را از اول شروع می‌کند (حدود ۱۰ ثانیه تا لبه بوم).
 */
@Composable
fun LetItPassGame(accent: Color) {
    var thought by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var drifting by remember { mutableStateOf(false) }

    LaunchedEffect(drifting) {
        if (!drifting) {
            return@LaunchedEffect
        }
        while (progress < 1f) {
            delay(16)
            progress = (progress + 0.0016f).coerceAtMost(1f)
        }
        drifting = false
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // چسباندن متن در این فیلد آزاد است؛ محدودیت تایپ دستی فقط مال متن بازگشایی Strict است.
        SaddTextField(
            value = thought,
            onValueChange = { thought = it },
            label = "فکرت را بنویس (اختیاری)",
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                // قاب Detox جویبار: شعاع کارت + حاشیه مویی.
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(SaddRadius.Card))
                .border(SaddGlass.BorderWidth, saddHairlineColor(), RoundedCornerShape(SaddRadius.Card)),
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                val w = size.width
                val h = size.height
                // جویبار: منحنی سینوسی از لبه تا لبه.
                val path = Path()
                val points = 60
                for (i in 0..points) {
                    val x = w * i / points
                    val y = streamY(x, w, h)
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(path = path, color = Sky, style = Stroke(width = 4.dp.toPx()))
                drawPath(
                    path = path,
                    color = Sky.copy(alpha = 0.30f),
                    style = Stroke(width = 12.dp.toPx()),
                )
                // برگ روی جویبار: از نزدیک لبه شروع می‌کند و تا لبه دیگر می‌رود.
                val leafX = (0.05f + 0.90f * progress) * w
                val leafCenter = Offset(leafX, streamY(leafX, w, h) - h * 0.06f)
                val leafRadius = size.minDimension * 0.055f
                drawCircle(color = Teal, radius = leafRadius, center = leafCenter)
                drawLine(
                    color = Teal,
                    start = leafCenter,
                    end = Offset(leafCenter.x + leafRadius * 1.3f, leafCenter.y - leafRadius * 0.8f),
                    strokeWidth = 2.dp.toPx(),
                )
                // نقطه تأکید رنگ بازی روی برگ، تا با پالت‌ها هم‌خوان بماند.
                drawCircle(color = accent, radius = leafRadius * 0.4f, center = leafCenter)
            }
            if (thought.isNotBlank()) {
                Text(
                    thought,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(SaddSpacing.Medium)
                        .alpha(1f - progress),
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Medium))
        Text(
            "فکر را روی برگ بگذار و بگذار با آب برود",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        SaddButton(
            text = "رها کن",
            onClick = {
                progress = 0f
                drifting = true
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
