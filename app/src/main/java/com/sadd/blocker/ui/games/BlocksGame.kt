package com.sadd.blocker.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadd.blocker.core.games.BlocksEngine
import com.sadd.blocker.ui.screens.SaddButton
import com.sadd.blocker.ui.screens.SaddButtonStyle
import com.sadd.blocker.ui.theme.Coral
import com.sadd.blocker.ui.theme.Grape
import com.sadd.blocker.ui.theme.Mint
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.Sky
import com.sadd.blocker.ui.theme.Sunny
import com.sadd.blocker.ui.theme.Teal
import com.sadd.blocker.ui.theme.saddHairlineColor
import kotlinx.coroutines.delay

// رنگ هر تترمینو به ترتیب ordinal آن (I, O, T, S, Z, J, L).
private val PIECE_COLORS: List<Color> = listOf(
    Sky, Sunny, Grape, Mint, Coral, Teal, Color(0xFFFF9EC7),
)

/**
 * بازی «بلوک‌ها»: صفحه با Canvas کشیده می‌شود و گرانش با یک حلقه
 * LaunchedEffect هر ۶۰۰ میلی‌ثانیه یک تیک می‌زند. موتور خالص در
 * [BlocksEngine] است؛ شمارنده فریم بعد از هر تغییر موتور، رندر دوباره را
 * تحریک می‌کند (موتور خودش وضعیت Compose ندارد).
 */
@Composable
fun BlocksGame() {
    val engine = remember { BlocksEngine(seed = System.currentTimeMillis()) }
    var frame by remember { mutableStateOf(0) }
    // خواندن فریم در بدنه یعنی بعد از هر حرکت موتور، امتیاز و صفحه تازه شوند.
    val currentFrame = frame

    LaunchedEffect(Unit) {
        while (true) {
            delay(600)
            if (!engine.gameOver) {
                engine.tick()
                frame += 1
            }
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
        key(currentFrame) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.5f)
                    // قاب Detox صفحه بازی: شعاع کارت + حاشیه مویی.
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(SaddRadius.Card))
                    .border(SaddGlass.BorderWidth, saddHairlineColor(), RoundedCornerShape(SaddRadius.Card)),
            ) {
                val cellW = size.width / BlocksEngine.WIDTH
                val cellH = size.height / BlocksEngine.HEIGHT
                // سلول‌های قفل‌شده.
                for (y in 0 until BlocksEngine.HEIGHT) {
                    for (x in 0 until BlocksEngine.WIDTH) {
                        val value = engine.cellAt(x, y)
                        if (value != 0) {
                            drawRect(
                                color = PIECE_COLORS[value - 1],
                                topLeft = Offset(x * cellW + 1f, y * cellH + 1f),
                                size = Size(cellW - 2f, cellH - 2f),
                            )
                        }
                    }
                }
                // قطعه جاری (در حالت پایان بازی دیگر قطعه‌ای در هوا نیست).
                if (!engine.gameOver) {
                    val color = PIECE_COLORS[engine.current.ordinal]
                    for ((x, y) in engine.currentCells()) {
                        drawRect(
                            color = color,
                            topLeft = Offset(x * cellW + 1f, y * cellH + 1f),
                            size = Size(cellW - 2f, cellH - 2f),
                        )
                    }
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
                        engine.moveRight()
                        frame += 1
                    },
                    modifier = Modifier.weight(1f),
                    style = SaddButtonStyle.SECONDARY,
                )
                SaddButton(
                    text = "چرخش",
                    onClick = {
                        engine.rotate()
                        frame += 1
                    },
                    modifier = Modifier.weight(1f),
                    style = SaddButtonStyle.SECONDARY,
                )
                SaddButton(
                    text = "چپ",
                    onClick = {
                        engine.moveLeft()
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
