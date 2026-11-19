package com.sadd.blocker.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sadd.blocker.ui.screens.SaddCard
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
import kotlin.math.sin

/** شناسه شش بازی کوچک صفحه بلاک. */
enum class GameId {
    BLOCKS, ECHO, FOLLOW, BREATHE, LET_IT_PASS, POP,
}

/** اطلاعات نمایشی یک بازی: عنوان، دسته، توضیح یک‌خطی و رنگ تأکید. */
data class GameInfo(
    val id: GameId,
    val title: String,
    val category: String,
    val description: String,
    val accent: Color,
)

/** شش بازی، به همان ترتیب نمایش در شبکه دو ستونی. */
val GAMES: List<GameInfo> = listOf(
    GameInfo(
        id = GameId.BLOCKS,
        title = "بلوک‌ها",
        category = "چشم و ذهن",
        description = "شکل‌ها را جا بده، خط‌ها را پاک کن",
        accent = Sky,
    ),
    GameInfo(
        id = GameId.ECHO,
        title = "پژواک",
        category = "حافظه",
        description = "نورها را ببین، همان را تکرار کن",
        accent = Grape,
    ),
    GameInfo(
        id = GameId.FOLLOW,
        title = "دنبال کن",
        category = "چشم",
        description = "نور شناور را با انگشتت دنبال کن",
        accent = Sunny,
    ),
    GameInfo(
        id = GameId.BREATHE,
        title = "نفس بکش",
        category = "بدن",
        description = "چهار ثانیه دم، شش ثانیه بازدم",
        accent = Teal,
    ),
    GameInfo(
        id = GameId.LET_IT_PASS,
        title = "بگذار برود",
        category = "افکار",
        description = "فکر را روی برگ بگذار و رهایش کن",
        accent = Mint,
    ),
    GameInfo(
        id = GameId.POP,
        title = "ترکاندن",
        category = "دست‌ها",
        description = "حباب‌هایی که هیچ‌وقت تمام نمی‌شوند",
        accent = Coral,
    ),
)

/**
 * شبکه دو ستونی کارت‌های بازی. عمداً با Row و chunked ساخته شده (نه
 * LazyVerticalGrid) تا داخل Column اسکرول‌شونده صفحه بلاک کرش nested-scroll ندهد.
 */
@Composable
fun GamesGrid(onGameClick: (GameInfo) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SaddSpacing.Medium)) {
        for (rowGames in GAMES.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Medium),
            ) {
                for (game in rowGames) {
                    GameCard(
                        game = game,
                        onClick = { onGameClick(game) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowGames.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GameCard(game: GameInfo, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SaddCard(modifier = modifier, onClick = onClick) {
        Column {
            // سربرگ تصویری کارت: با Canvas کشیده می‌شود، هیچ فایل تصویری لازم نیست.
            // پس‌زمینه تیره‌تر + حاشیه مویی Detox دور سربرگ.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(SaddSpacing.Small)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(SaddRadius.Chip))
                    .border(SaddGlass.BorderWidth, saddHairlineColor(), RoundedCornerShape(SaddRadius.Chip)),
            ) {
                GameHeaderCanvas(game = game, modifier = Modifier.fillMaxWidth().height(90.dp))
            }
            Column(modifier = Modifier.padding(SaddSpacing.Medium)) {
                Text(
                    game.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    game.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = game.accent,
                )
                Spacer(Modifier.height(SaddSpacing.XSmall))
                Text(
                    game.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** نقاشی سربرگ هر کارت با شکل‌های ساده Canvas (مربع، دایره، منحنی). */
@Composable
private fun GameHeaderCanvas(game: GameInfo, modifier: Modifier = Modifier) {
    val dimColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (game.id) {
            GameId.BLOCKS -> {
                // چند مربع رنگی در چیدمان تترمینویی.
                val cell = h * 0.22f
                val corner = CornerRadius(cell * 0.18f, cell * 0.18f)
                val colors = listOf(Sky, Sunny, Grape, Coral)
                val spots = listOf(
                    Offset(w * 0.18f, h * 0.55f),
                    Offset(w * 0.18f + cell, h * 0.55f),
                    Offset(w * 0.18f + cell, h * 0.55f - cell),
                    Offset(w * 0.55f, h * 0.35f),
                    Offset(w * 0.55f + cell, h * 0.35f),
                    Offset(w * 0.55f, h * 0.35f + cell),
                )
                for ((index, spot) in spots.withIndex()) {
                    drawRoundRect(
                        color = colors[index % colors.size],
                        topLeft = spot,
                        size = Size(cell * 0.92f, cell * 0.92f),
                        cornerRadius = corner,
                    )
                }
            }
            GameId.ECHO -> {
                // شبکه ۳×۳ نقطه، یکی بنفش و روشن.
                val radius = h * 0.09f
                for (row in 0..2) {
                    for (col in 0..2) {
                        val center = Offset(
                            w * (0.32f + col * 0.18f),
                            h * (0.28f + row * 0.22f),
                        )
                        if (row == 1 && col == 1) {
                            drawCircle(color = Grape.copy(alpha = 0.30f), radius = radius * 1.9f, center = center)
                            drawCircle(color = Grape, radius = radius * 1.25f, center = center)
                        } else {
                            drawCircle(color = dimColor, radius = radius, center = center)
                        }
                    }
                }
            }
            GameId.FOLLOW -> {
                // نقطه درخشان با ردّ کمرنگ پشت سرش.
                val head = Offset(w * 0.68f, h * 0.42f)
                for (step in 5 downTo 1) {
                    val t = step.toFloat()
                    drawCircle(
                        color = Sunny.copy(alpha = 0.10f + 0.06f * (6f - t)),
                        radius = h * 0.05f,
                        center = Offset(head.x - t * w * 0.07f, head.y + t * h * 0.05f),
                    )
                }
                drawCircle(color = Sunny.copy(alpha = 0.25f), radius = h * 0.20f, center = head)
                drawCircle(color = Sunny, radius = h * 0.10f, center = head)
            }
            GameId.BREATHE -> {
                // دایره‌های سبز هم‌پوشان.
                drawCircle(
                    color = Teal.copy(alpha = 0.35f),
                    radius = h * 0.30f,
                    center = Offset(w * 0.42f, h * 0.52f),
                )
                drawCircle(
                    color = Mint.copy(alpha = 0.45f),
                    radius = h * 0.24f,
                    center = Offset(w * 0.58f, h * 0.44f),
                )
                drawCircle(
                    color = Teal,
                    radius = h * 0.10f,
                    center = Offset(w * 0.50f, h * 0.48f),
                )
            }
            GameId.LET_IT_PASS -> {
                // جویبار موج‌دار و یک برگ روی آن.
                val path = Path()
                val points = 40
                for (i in 0..points) {
                    val x = w * i / points
                    val y = h * 0.55f + h * 0.16f * sin(i.toFloat() / points * 6.2831f * 1.5f)
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                drawPath(path = path, color = Sky, style = Stroke(width = 3.dp.toPx()))
                val leafCenter = Offset(w * 0.62f, h * 0.42f)
                drawCircle(color = Teal, radius = h * 0.13f, center = leafCenter)
                drawLine(
                    color = Teal,
                    start = leafCenter,
                    end = Offset(leafCenter.x + h * 0.16f, leafCenter.y - h * 0.10f),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            GameId.POP -> {
                // شبکه‌ای از نقطه‌های حبابی.
                val radius = h * 0.085f
                for (row in 0..2) {
                    for (col in 0..4) {
                        drawCircle(
                            color = Coral.copy(alpha = 0.75f),
                            radius = radius,
                            center = Offset(
                                w * (0.18f + col * 0.16f),
                                h * (0.28f + row * 0.22f),
                            ),
                        )
                    }
                }
            }
        }
    }
}
