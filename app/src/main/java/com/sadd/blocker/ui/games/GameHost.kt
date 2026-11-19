package com.sadd.blocker.ui.games

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.sadd.blocker.ui.screens.SaddCard
import com.sadd.blocker.ui.screens.SaddIconChip
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * قاب تمام‌صفحه یک بازی داخل صفحه بلاک: ردیف بالا با عنوان بازی و دکمه
 * بستن، و زیر آن خود بازی. بستن فقط به محتوای صفحه بلاک برمی‌گردد —
 * هرگز اکتیویتی را نمی‌بندد و کاربر را به اپ بلاک‌شده برنمی‌گرداند.
 * پس‌زمینه از SaddBackdrop صفحه بلاک می‌آید؛ قاب پس‌زمینه خودش را ندارد.
 */
@Composable
fun GameHost(game: GameInfo, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(SaddSpacing.Large),
    ) {
        // سربرگ بازی داخل کارت شیشه‌ای با چیپ رنگ تأکید بازی.
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SaddIconChip(icon = Icons.Filled.Close, color = game.accent)
                Spacer(Modifier.size(SaddSpacing.Medium))
                Text(
                    game.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "بستن")
                }
            }
        }
        Spacer(Modifier.height(SaddSpacing.Medium))
        when (game.id) {
            GameId.BLOCKS -> BlocksGame()
            GameId.ECHO -> EchoGame(accent = game.accent)
            GameId.FOLLOW -> FollowGame(accent = game.accent)
            GameId.BREATHE -> BreatheGame(accent = game.accent)
            GameId.LET_IT_PASS -> LetItPassGame(accent = game.accent)
            GameId.POP -> PopGame(accent = game.accent)
        }
    }
}
