package com.sadd.blocker.ui.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadd.blocker.core.games.EchoEngine
import com.sadd.blocker.ui.screens.SaddButton
import com.sadd.blocker.ui.screens.SaddButtonStyle
import com.sadd.blocker.ui.screens.saddGlassPanel
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddGlassSurfaceColor
import com.sadd.blocker.ui.theme.saddSpecularBorderColor
import kotlinx.coroutines.delay
import kotlin.math.pow

private enum class EchoPhase { SHOWING, INPUT, SUCCESS, WRONG }

// نردبان پنج‌نوتی هر سلول (نیم‌پرده نسبت به لا ۴۴۰) تا هر سلول صدای خودش را داشته باشد.
private val CELL_SEMITONES = intArrayOf(0, 2, 4, 7, 9, 12, 14, 16, 19)

private fun cellToneHz(index: Int): Float {
    val semitone = CELL_SEMITONES[index.coerceIn(0, CELL_SEMITONES.lastIndex)]
    return 440f * 2f.pow(semitone / 12f)
}

/**
 * بازی «پژواک»: شبکه ۳×۳ سلول شیشه‌ای؛ الگو با نور و صدا نشان داده می‌شود
 * (سلول روشن با فنر بزرگ می‌شود و هاله نورانی دارد)، بعد کاربر همان را
 * تکرار می‌کند — هر سلول لحن خودش را دارد و مرحله‌ها نرم عوض می‌شوند.
 * موتور خالص در [EchoEngine] است؛ این فایل فقط نمایش و مرحله‌ها را مدیریت می‌کند.
 */
@Composable
fun EchoGame(accent: Color) {
    val engine = remember { EchoEngine(cellCount = 9, seed = System.currentTimeMillis()) }
    var phase by remember { mutableStateOf(EchoPhase.SHOWING) }
    var litCell by remember { mutableStateOf(-1) }
    var roundKey by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf("") }
    val haptics = LocalHapticFeedback.current

    // نمایش الگو: هر سلول ۶۰۰ میلی‌ثانیه روشن می‌شود، با ۲۵۰ میلی‌ثانیه مکث بین آن‌ها.
    LaunchedEffect(roundKey) {
        phase = EchoPhase.SHOWING
        statusText = "خوب نگاه کن…"
        litCell = -1
        delay(600)
        for (cell in engine.sequence) {
            litCell = cell
            GameSounds.tone(cellToneHz(cell))
            delay(600)
            litCell = -1
            delay(250)
        }
        engine.beginInput()
        phase = EchoPhase.INPUT
        statusText = "حالا تو تکرار کن"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            statusText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        // پنل شیشه‌ای شبکه پژواک.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .saddGlassPanel(RoundedCornerShape(SaddRadius.Card))
                .padding(SaddSpacing.Medium),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                for (row in 0 until 3) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 3) {
                            val index = row * 3 + col
                            EchoCell(
                                lit = litCell == index,
                                accent = accent,
                                enabled = phase == EchoPhase.INPUT,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    GameSounds.tone(cellToneHz(index))
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    when (engine.press(index)) {
                                        EchoEngine.PressResult.CORRECT -> {
                                            // درست است؛ منتظر ضربه بعدی می‌مانیم.
                                        }
                                        EchoEngine.PressResult.COMPLETE -> {
                                            GameSounds.chime()
                                            phase = EchoPhase.SUCCESS
                                            statusText = "آفرین! الگو کامل شد"
                                        }
                                        EchoEngine.PressResult.WRONG -> {
                                            GameSounds.tone(160f)
                                            phase = EchoPhase.WRONG
                                            statusText = "اشتباه شد؛ از اول ببین"
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(SaddSpacing.Medium))
        when (phase) {
            EchoPhase.SUCCESS -> {
                SaddButton(
                    text = "مرحله بعد",
                    onClick = {
                        engine.nextRound()
                        roundKey += 1
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            EchoPhase.WRONG -> {
                SaddButton(
                    text = "دوباره ببین",
                    onClick = {
                        engine.restart()
                        roundKey += 1
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {
                SaddButton(
                    text = "از اول",
                    onClick = {
                        engine.restart()
                        roundKey += 1
                    },
                    modifier = Modifier.fillMaxWidth(),
                    style = SaddButtonStyle.SECONDARY,
                )
            }
        }
    }
}

@Composable
private fun EchoCell(
    lit: Boolean,
    accent: Color,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    // سلول روشن با فنر کمی بزرگ می‌شود و هاله‌اش نرم می‌آید و می‌رود.
    val scale by animateFloatAsState(
        targetValue = if (lit) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "echoCellScale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (lit) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "echoCellGlow",
    )
    val cellShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        // هاله نورانی پشت سلول روشن.
        if (glowAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(1.25f)
                    .alpha(glowAlpha)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.55f), Color.Transparent),
                        ),
                        cellShape,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .clip(cellShape)
                .background(
                    if (lit) {
                        Brush.radialGradient(
                            colors = listOf(
                                lerp(accent, Color.White, 0.45f),
                                accent,
                            ),
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(saddGlassSurfaceColor(), saddGlassSurfaceColor()),
                        )
                    },
                )
                .border(
                    SaddGlass.BorderWidth,
                    if (lit) Color.White.copy(alpha = 0.7f) else saddSpecularBorderColor(),
                    cellShape,
                )
                .clickable(enabled = enabled, onClick = onClick),
        )
    }
}
