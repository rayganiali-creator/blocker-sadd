package com.sadd.blocker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadd.blocker.R
import com.sadd.blocker.data.datastore.AppTheme

// سد — توکن‌های تاییدشده (AUDIT.md §۳٫۲ + موکاپ‌های v2):
// سرمه‌ای عمیق + مینت/تیل، پس‌زمینه روشن off-white، بدون مشکی خالص.
val Mint = Color(0xFF7CFFD9)
val Teal = Color(0xFF0E9F8A)
val TealDark = Color(0xFF0B7A6B)
val Navy = Color(0xFF0B1220)
val NavyDeep = Color(0xFF071224)
val NavySurface = Color(0xFF131E33)
val Coral = Color(0xFFFF7A59)
val Sunny = Color(0xFFFFC53D)
val Sky = Color(0xFF5AA9FF)
val Grape = Color(0xFF9B7BFF)
val LightBackground = Color(0xFFF7F8FA)

private val LightColors = lightColorScheme(
    primary = TealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8F5E9),
    onPrimaryContainer = Color(0xFF00382E),
    secondary = Sky,
    onSecondary = Color.White,
    tertiary = Grape,
    onTertiary = Color.White,
    error = Color(0xFFC93A1E),
    onError = Color.White,
    errorContainer = Color(0xFFFFE3DA),
    onErrorContainer = Color(0xFF5C130A),
    background = LightBackground,
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    surfaceVariant = Color(0xFFE8EDF1),
    onSurfaceVariant = Color(0xFF44506A),
    outline = Color(0xFFB9C2CF),
)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = NavyDeep,
    primaryContainer = Color(0xFF0E3D34),
    onPrimaryContainer = Mint,
    secondary = Sky,
    onSecondary = NavyDeep,
    tertiary = Grape,
    onTertiary = NavyDeep,
    error = Coral,
    onError = NavyDeep,
    errorContainer = Color(0xFF4A1E12),
    onErrorContainer = Color(0xFFFFC9B8),
    background = NavyDeep,
    onBackground = Color(0xFFF7F8FA),
    surface = Navy,
    onSurface = Color(0xFFF7F8FA),
    surfaceVariant = NavySurface,
    onSurfaceVariant = Color(0xFFB9C6DA),
    outline = Color(0xFF3A4A66),
)

/** فونت وزیرمتن (res/font) — Regular / Medium / Bold. */
val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private val SaddTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Vazirmatn, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp,
    ),
)

/** فاصله‌گذاری پایه 4dp (8/12/16/24). */
object SaddSpacing {
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 24.dp
}

/** شعاع: کارت 16dp، دکمه 12dp، چیپ 8dp، دیالوگ 20dp. */
object SaddRadius {
    val Card = 16.dp
    val Button = 12.dp
    val Chip = 8.dp
    val Dialog = 20.dp
}

private val SaddShapes = Shapes(
    extraSmall = RoundedCornerShape(SaddRadius.Chip),
    small = RoundedCornerShape(SaddRadius.Button),
    medium = RoundedCornerShape(SaddRadius.Card),
    large = RoundedCornerShape(SaddRadius.Dialog),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * تم «سد»: روشن / تیره / سیستمی.
 * در SYSTEM از تنظیم سیستم پیروی می‌کند؛ پیش‌فرض DARK است.
 */
@Composable
fun BlockerTheme(theme: AppTheme = AppTheme.DARK, content: @Composable () -> Unit) {
    val dark = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = SaddTypography,
        shapes = SaddShapes,
        content = content,
    )
}
