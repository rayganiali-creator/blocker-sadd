package com.sadd.blocker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sadd.blocker.ui.games.GameHost
import com.sadd.blocker.ui.games.GameInfo
import com.sadd.blocker.ui.games.GamesGrid
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * تب «بازی‌ها» (خواسته سید ۲۰۲۶-۱۱-۱۹): همان شش بازی کوچک صفحه بلاک،
 * این‌بار به‌عنوان تب جدا داخل خود برنامه و همیشه در دسترس — مستقل از
 * سشن و Strict و بدون هیچ اثری روی بلاک. ورود بازی‌ها از صفحه بلاک سر
 * جایش می‌ماند؛ اینجا فقط یک درِ دوم به همان بازی‌هاست.
 * بازی انتخاب‌شده تمام‌صفحه داخل همین تب باز می‌شود (GameHost مشترک) و
 * بستن آن — با دکمه بستن یا دکمه بازگشت سیستم — به شبکه بازی‌ها برمی‌گردد.
 */
@Composable
fun GamesScreen() {
    var selectedGame by remember { mutableStateOf<GameInfo?>(null) }
    val openGame = selectedGame
    // بازگشت سیستم وقتی بازی باز است فقط بازی را می‌بندد، نه اینکه از تب بیرون برود.
    BackHandler(enabled = openGame != null) {
        selectedGame = null
    }
    if (openGame != null) {
        GameHost(game = openGame, onClose = { selectedGame = null })
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(SaddSpacing.Large),
        ) {
            SaddTopBar(title = "بازی‌ها", subtitle = "وقتی موج میل می‌آید")
            Spacer(Modifier.height(SaddSpacing.Medium))
            GamesGrid(onGameClick = { selectedGame = it })
            Spacer(Modifier.height(SaddSpacing.XLarge))
        }
    }
}
