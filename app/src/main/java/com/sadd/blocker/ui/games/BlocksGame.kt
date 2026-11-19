package com.sadd.blocker.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.sadd.blocker.core.games.BlocksEngine
import com.sadd.blocker.ui.screens.SaddButton
import com.sadd.blocker.ui.screens.SaddButtonStyle
import com.sadd.blocker.ui.screens.saddGlassPanel
import com.sadd.blocker.ui.theme.Coral
import com.sadd.blocker.ui.theme.Grape
import com.sadd.blocker.ui.theme.Mint
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.Sky
import com.sadd.blocker.ui.theme.Sunny
import com.sadd.blocker.ui.theme.Teal
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// رنگ هر تترمینو به ترتیب ordinal آن (I, O, T, S, Z, J, L).
private val PIECE_COLORS: List<Color> = listOf(
    Sky, Sunny, Grape, Mint, Coral, Teal, Color(0xFFFF9EC7),
)

/**
 * بازی «بلوک‌ها»: صفحه با Canvas کشیده می‌شود و گرانش با یک حلقه
 * LaunchedEffect هر ۶۰۰ میلی‌ثانیه یک تیک می‌زند. حرکت قطعه با فنر نرم
 * انیمیت می‌شود (نه پرش ناگهانی)، چرخش بین دو حالت مورف می‌شود، پاک‌شدن
 * خط فلش سفید دارد و حرکت/چرخش/نشستن/پاک‌شدن صدا دارند.
 * موتور خالص در [BlocksEngine] است؛ شمارنده فریم بعد از هر تغییر موتور،
 * رندر دوباره را تحریک می‌کند (موتور خودش وضعیت Compose ندارد).
 */
@Composable
fun BlocksGame() {
    val engine = remember { BlocksEngine(seed = System.currentTimeMillis()) }
    var frame by remember { mutableStateOf(0) }
    val haptics = LocalHapticFeedback.current

    // موقعیت نمایشی قطعه (واحد سلول) که با فنر به موقعیت موتور می‌رسد.
    val animX = remember { Animatable(engine.currentX.toFloat()) }
    val animY = remember { Animatable(engine.currentY.toFloat()) }
    // پیشرفت مورف چرخش: ۰ یعنی شکل قبلی، ۱ یعنی شکل تازه.
    val rotAnim = remember { Animatable(1f) }
    var prevOffsets by remember { mutableStateOf(pieceOffsets(engine)) }
    var lastRotation by remember { mutableStateOf(engine.currentRotation) }
    var lastLockCount by remember { mutableStateOf(engine.lockCount) }
    // فلش پاک‌شدن خط: ۱ لحظه پاک‌شدن، بعد محو می‌شود.
    val clearFlash = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(600)
            if (!engine.gameOver) {
                engine.tick()
                frame += 1
            }
        }
    }

    // همگام‌سازی انیمیشن با موتور بعد از هر تغییر.
    LaunchedEffect(frame) {
        if (engine.lockCount != lastLockCount) {
            // قطعه تازه آمده است: موقعیت را بی‌پرش به جای تولد ببر و اثر نشستن را پخش کن.
            lastLockCount = engine.lockCount
            animX.snapTo(engine.currentX.toFloat())
            animY.snapTo(engine.currentY.toFloat())
            prevOffsets = pieceOffsets(engine)
            lastRotation = engine.currentRotation
            rotAnim.snapTo(1f)
            GameSounds.thud()
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            if (engine.lastClearedRows.isNotEmpty()) {
                GameSounds.chime()
                clearFlash.snapTo(1f)
                clearFlash.animateTo(0f, tween(durationMillis = 380))
            }
        } else {
            if (engine.currentRotation != lastRotation) {
                // چرخش تازه: از شکل قبلی به شکل تازه مورف شو.
                lastRotation = engine.currentRotation
                rotAnim.snapTo(0f)
                launch { rotAnim.animateTo(1f, tween(durationMillis = 160)) }
            } else {
                prevOffsets = pieceOffsets(engine)
            }
            coroutineScope {
                launch {
                    animX.animateTo(
                        engine.currentX.toFloat(),
                        spring(dampingRatio = 0.8f, stiffness = 420f),
                    )
                }
                launch {
                    animY.animateTo(
                        engine.currentY.toFloat(),
                        spring(dampingRatio = 0.85f, stiffness = 320f),
                    )
                }
            }
        }
    }

    LaunchedEffect(engine.gameOver) {
        if (engine.gameOver) {
            GameSounds.thud()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "امتیاز: ${engine.score}    خط‌ها: ${engine.lines}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.5f)
                // قاب شیشه‌ای Liquid Glass صفحه بازی.
                .saddGlassPanel(RoundedCornerShape(SaddRadius.Card)),
        ) {
            val cellW = size.width / BlocksEngine.WIDTH
            val cellH = size.height / BlocksEngine.HEIGHT
            val corner = CornerRadius(cellW * 0.22f, cellH * 0.22f)
            // سلول‌های قفل‌شده با گرادیان عمودی و برق بالا.
            for (y in 0 until BlocksEngine.HEIGHT) {
                for (x in 0 until BlocksEngine.WIDTH) {
                    val value = engine.cellAt(x, y)
                    if (value != 0) {
                        drawCell(PIECE_COLORS[value - 1], x.toFloat(), y.toFloat(), cellW, cellH, corner)
                    }
                }
            }
            // قطعه جاری با موقعیت فنری و مورف چرخش (در حالت پایان بازی دیگر قطعه‌ای در هوا نیست).
            if (!engine.gameOver) {
                val color = PIECE_COLORS[engine.current.ordinal]
                val currentOffsets = pieceOffsets(engine)
                val progress = rotAnim.value
                for (i in currentOffsets.indices) {
                    val (cx, cy) = currentOffsets[i]
                    val (px, py) = prevOffsets.getOrElse(i) { Pair(cx, cy) }
                    val dx = px + (cx - px) * progress
                    val dy = py + (cy - py) * progress
                    // هاله نرم پشت سلول قطعه جاری.
                    drawRoundRect(
                        color = color.copy(alpha = 0.22f),
                        topLeft = Offset((animX.value + dx) * cellW - cellW * 0.18f, (animY.value + dy) * cellH - cellH * 0.18f),
                        size = Size(cellW * 1.36f, cellH * 1.36f),
                        cornerRadius = corner,
                    )
                    drawCell(color, animX.value + dx, animY.value + dy, cellW, cellH, corner)
                }
            }
            // فلش سفید سطرهای پاک‌شده.
            if (clearFlash.value > 0f) {
                for (row in engine.lastClearedRows) {
                    drawRect(
                        color = Color.White.copy(alpha = clearFlash.value * 0.65f),
                        topLeft = Offset(0f, row * cellH),
                        size = Size(size.width, cellH),
                    )
                }
            }
        }
        Spacer(Modifier.height(SaddSpacing.Medium))
        if (engine.gameOver) {
            Text(
                "بازی تمام شد",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(SaddSpacing.Small))
            SaddButton(
                text = "بازی دوباره",
                onClick = {
                    engine.reset(seed = System.currentTimeMillis())
                    frame += 1
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // در چیدمان راست‌به‌چپ، اولین دکمه سمت راست می‌نشیند؛ پس ترتیب
            // «راست، چرخش، چپ» یعنی دکمه «چپ» فیزیکی سمت چپ صفحه باشد.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
            ) {
                SaddButton(
                    text = "راست",
                    onClick = {
                        if (engine.moveRight()) {
                            GameSounds.blip()
                        }
                        frame += 1
                    },
                    modifier = Modifier.weight(1f),
                    style = SaddButtonStyle.SECONDARY,
                )
                SaddButton(
                    text = "چرخش",
                    onClick = {
                        if (engine.rotate()) {
                            GameSounds.tone(700f)
                        }
                        frame += 1
                    },
                    modifier = Modifier.weight(1f),
                    style = SaddButtonStyle.SECONDARY,
                )
                SaddButton(
                    text = "چپ",
                    onClick = {
                        if (engine.moveLeft()) {
                            GameSounds.blip()
                        }
                        frame += 1
                    },
                    modifier = Modifier.weight(1f),
                    style = SaddButtonStyle.SECONDARY,
                )
            }
            Spacer(Modifier.height(SaddSpacing.Small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
            ) {
                SaddButton(
                    text = "پایین",
                    onClick = {
                        engine.softDrop()
                        frame += 1
                    },
                    modifier = Modifier.weight(1f),
                    style = SaddButtonStyle.SECONDARY,
                )
                SaddButton(
                    text = "رها",
                    onClick = {
                        engine.hardDrop()
                        frame += 1
                    },
                    modifier = Modifier.weight(1f),
                    style = SaddButtonStyle.SECONDARY,
                )
            }
        }
    }
}

/** آفست‌های سلول قطعه جاری نسبت به گوشه قطعه (برای مورف چرخش در UI). */
private fun pieceOffsets(engine: BlocksEngine): List<Pair<Int, Int>> {
    return engine.currentCells().map { (x, y) ->
        Pair(x - engine.currentX, y - engine.currentY)
    }
}

/** یک سلول با گرادیان عمودی (روشن بالا، رنگ وسط، تیره پایین) و گوشه گرد. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCell(
    color: Color,
    cellX: Float,
    cellY: Float,
    cellW: Float,
    cellH: Float,
    corner: CornerRadius,
) {
    val topLeft = Offset(cellX * cellW + 1f, cellY * cellH + 1f)
    val cellSize = Size(cellW - 2f, cellH - 2f)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                lerp(color, Color.White, 0.35f),
                color,
                lerp(color, Color.Black, 0.22f),
            ),
            startY = topLeft.y,
            endY = topLeft.y + cellSize.height,
        ),
        topLeft = topLeft,
        size = cellSize,
        cornerRadius = corner,
    )
}
