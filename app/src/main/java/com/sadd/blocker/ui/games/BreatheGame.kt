package com.sadd.blocker.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadd.blocker.core.games.BreathPhase
import com.sadd.blocker.core.games.BreatheEngine
import com.sadd.blocker.ui.screens.SaddButton
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddHairlineColor
import kotlinx.coroutines.delay

/**
 * بازی «نفس بکش»: دایره‌ای که با ریتم [BreatheEngine] (۴ ثانیه دم،
 * ۶ ثانیه بازدم) بزرگ و کوچک می‌شود. تیکر هر ۵۰ میلی‌ثانیه زمان را جلو
 * می‌برد؛ «شروع»/«توقف» اجرا را کنترل می‌کند و شمارنده چرخه‌های کامل را نشان می‌دهد.
 */
@Composable
fun BreatheGame(accent: Color) {
    var running by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableStateOf(0L) }

    LaunchedEffect(running) {
        if (!running) {
            return@LaunchedEffect
        }
        while (true) {
            delay(50)
            elapsedMs += 50L
        }
    }

    val state = BreatheEngine.breathPhaseAt(elapsedMs)
    // دم: دایره از ۶۰٪ تا ۱۰۰٪ بزرگ می‌شود؛ بازدم برعکس.
    val scale = if (state.phase == BreathPhase.INHALE) {
        0.6f + 0.4f * state.progress
    } else {
        1.0f - 0.4f * state.progress
    }
    val cueText = when {
        !running && elapsedMs == 0L -> "آماده‌ای؟"
        state.phase == BreathPhase.INHALE -> "دم…"
        else -> "بازدم…"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            cueText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        Canvas(
            modifier = Modifier
                .size(240.dp)
                // قاب دایره‌ای Detox دور دایره تنفس.
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                .border(SaddGlass.BorderWidth, saddHairlineColor(), CircleShape),
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f - 8f
            // حلقه راهنما و دایره تنفس.
            drawCircle(
                color = accent.copy(alpha = 0.35f),
                radius = maxRadius,
                center = center,
                style = Stroke(width = 3f),
            )
            drawCircle(
                color = accent.copy(alpha = 0.55f),
                radius = maxRadius * scale,
                center = center,
            )
            drawCircle(
                color = accent,
                radius = maxRadius * scale * 0.45f,
                center = center,
            )
        }
        Spacer(Modifier.height(SaddSpacing.Medium))
        Text(
            "چرخه: ${state.completedCycles}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        SaddButton(
            text = if (running) "توقف" else "شروع",
            onClick = { running = !running },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
