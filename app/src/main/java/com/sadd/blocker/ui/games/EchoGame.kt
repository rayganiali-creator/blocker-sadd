package com.sadd.blocker.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.sadd.blocker.core.games.EchoEngine
import com.sadd.blocker.ui.screens.SaddButton
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddHairlineColor
import kotlinx.coroutines.delay

/** فازهای بازی «پژواک»: نمایش توالی، نوبت کاربر، موفقیت، اشتباه. */
private enum class EchoPhase {
    SHOWING, INPUT, SUCCESS, WRONG,
}

/**
 * بازی «پژواک»: توالی نورها روی شبکه ۳×۳ چشمک می‌زند و کاربر باید همان را
 * تکرار کند. هر موفقیت توالی را یکی بلندتر می‌کند (مرحله ۱ = ۳ نور)؛
 * ضربه اشتباه بازی را تمام می‌کند و «دوباره» از مرحله ۱ شروع می‌کند.
 * منطق توالی در [EchoEngine] خالص است.
 */
@Composable
fun EchoGame(accent: Color) {
    var level by remember { mutableStateOf(1) }
    var score by remember { mutableStateOf(0) }
    var phase by remember { mutableStateOf(EchoPhase.SHOWING) }
    var roundSeed by remember { mutableStateOf(System.currentTimeMillis()) }
    var inputs by remember { mutableStateOf<List<Int>>(emptyList()) }
    var litCell by remember { mutableStateOf(-1) }
    var tappedCell by remember { mutableStateOf(-1) }

    val sequence = remember(level, roundSeed) {
        EchoEngine.generateSequence(length = level + 2, seed = roundSeed)
    }

    // نمایش توالی و رفتن خودکار به مرحله بعد بعد از موفقیت.
    LaunchedEffect(phase, sequence) {
        if (phase == EchoPhase.SHOWING) {
            litCell = -1
            delay(600)
            for (cell in sequence) {
                litCell = cell
                delay(550)
                litCell = -1
                delay(250)
            }
            inputs = emptyList()
            phase = EchoPhase.INPUT
        } else if (phase == EchoPhase.SUCCESS) {
            delay(900)
            level += 1
            roundSeed = System.currentTimeMillis()
            phase = EchoPhase.SHOWING
        }
    }

    // چشمک کوتاه سلولی که کاربر زده است.
    LaunchedEffect(tappedCell) {
        if (tappedCell >= 0) {
            delay(220)
            tappedCell = -1
        }
    }

    fun onCellTap(cell: Int) {
        if (phase != EchoPhase.INPUT) {
            return
        }
        tappedCell = cell
        val next = inputs + cell
        inputs = next
        if (!EchoEngine.isCorrectPrefix(sequence, next)) {
            phase = EchoPhase.WRONG
        } else if (EchoEngine.isComplete(sequence, next)) {
            score += sequence.size * 10
            phase = EchoPhase.SUCCESS
        }
    }

    val statusText = when (phase) {
        EchoPhase.SHOWING -> "خوب نگاه کن…"
        EchoPhase.INPUT -> "حالا تکرار کن"
        EchoPhase.SUCCESS -> "آفرین!"
        EchoPhase.WRONG -> "اشتباه شد"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "مرحله: $level    امتیاز: $score",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Small))
        Text(
            statusText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
            ) {
                for (col in 0..2) {
                    val cell = row * 3 + col
                    val lit = cell == litCell || cell == tappedCell
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(SaddSpacing.XSmall)
                            // سلول مربع گرد Detox با حاشیه مویی.
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (lit) accent else MaterialTheme.colorScheme.surfaceVariant,
                            )
                            .border(SaddGlass.BorderWidth, saddHairlineColor(), RoundedCornerShape(16.dp))
                            .clickable { onCellTap(cell) },
                    )
                }
            }
        }
        if (phase == EchoPhase.WRONG) {
            Spacer(Modifier.height(SaddSpacing.Medium))
            SaddButton(
                text = "دوباره",
                onClick = {
                    level = 1
                    score = 0
                    inputs = emptyList()
                    roundSeed = System.currentTimeMillis()
                    phase = EchoPhase.SHOWING
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
