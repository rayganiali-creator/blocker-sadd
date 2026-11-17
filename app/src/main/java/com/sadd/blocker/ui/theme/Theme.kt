package com.sadd.blocker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// سد — Material 3 مینیمال رنگی. مینت نسخه قبلی (#7CFFD9) به‌عنوان رنگ شاخص حفظ شده.
val Mint = Color(0xFF7CFFD9)
val Navy = Color(0xFF0B1B33)
val NavyDeep = Color(0xFF071224)
val Coral = Color(0xFFFF7A59)
val Sunny = Color(0xFFFFC53D)
val Sky = Color(0xFF5AA9FF)
val Grape = Color(0xFF9B7BFF)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A55),
    secondary = Color(0xFF3D7A6C),
    tertiary = Sky,
    background = Color(0xFFF6F7F4),
    surface = Color.White,
    onPrimary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Mint,
    secondary = Mint,
    tertiary = Sky,
    background = NavyDeep,
    surface = Navy,
    onPrimary = NavyDeep,
)

@Composable
fun BlockerTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
