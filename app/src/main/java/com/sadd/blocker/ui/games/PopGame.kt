package com.sadd.blocker.ui.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sadd.blocker.core.games.PopEngine
import com.sadd.blocker.ui.screens.saddGlassPanel
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import kotlinx.coroutines.delay

/**
 * بازی «ترکاندن»: شبکه ۶×۸ حباب براق داخل پنل شیشه‌ای. با کشیدن انگشت
 * روی حباب‌ها (یا ضربه) می‌ترکند — هر ترکیدن صدای ترکیدن و لرزش ملایم
 * دارد و حباب با فنر کوچک و کمرنگ می‌شود؛ حدود ۲ ثانیه بعد دوباره ساخته
 * می‌شود تا هیچ‌وقت تمام نشود.
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
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    val haptics = LocalHapticFeedback.current

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

    // ترکاندن حبابی که زیر نقطه لمس است (مختصات فیزیکی بوم شبکه).
    // در چیدمان راست‌به‌چپ، ستون اول سمت راست است؛ پس ستون منطقی از
    // فاصله از لبه چپ برعکس حساب می‌شود.
    fun popAt(position: Offset) {
        if (gridSize.width <= 0 || gridSize.height <= 0) {
            return
        }
        val cellW = gridSize.width.toFloat() / engine.cols
        val cellH = gridSize.height.toFloat() / engine.rows
        val visualCol = (position.x / cellW).toInt().coerceIn(0, engine.cols - 1)
        val col = engine.cols - 1 - visualCol
        val row = (position.y / cellH).toInt().coerceIn(0, engine.rows - 1)
        val index = row * engine.cols + col
        if (engine.pop(index)) {
            poppedStates[index] = true
            poppedAt[index] = System.currentTimeMillis()
            GameSounds.pop()
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
        // پنل شیشه‌ای شبکه حباب‌ها؛ لایه لمس روی کل شبکه است تا کشیدن
        // انگشت از حبابی به حباب دیگر همه را بترکاند.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .saddGlassPanel(RoundedCornerShape(SaddRadius.Card))
                .padding(SaddSpacing.Small),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(engine.cols.toFloat() / engine.rows.toFloat())
                    .onSizeChanged { gridSize = it }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            popAt(down.position)
                            down.consume()
                            do {
                                val event = awaitPointerEvent()
                                for (change in event.changes) {
                                    if (change.pressed) {
                                        popAt(change.position)
                                        change.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    },
            ) {
                for (row in 0 until engine.rows) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until engine.cols) {
                            val index = row * engine.cols + col
                            Bubble(
                                popped = poppedStates[index],
                                color = accent,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Bubble(popped: Boolean, color: Color, modifier: Modifier) {
    // فنر کم‌میرا: حباب موقع ترکیدن با جهش کوچک و برگشت فنری جمع می‌شود.
    val scale by animateFloatAsState(
        targetValue = if (popped) 0.2f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 420f),
        label = "bubbleScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (popped) 0.25f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "bubbleAlpha",
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .scale(scale)
            .alpha(alpha),
    ) {
        // بدنه براق حباب: گرادیان شعاعی از روشن به رنگ و لبه تیره‌تر.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            lerp(color, Color.White, 0.55f),
                            color,
                            lerp(color, Color.Black, 0.25f),
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape),
        )
        // برق کوچک بالای حباب.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 7.dp, y = 6.dp)
                .size(9.dp)
                .background(Color.White.copy(alpha = 0.75f), CircleShape),
        )
    }
}
