package com.sadd.blocker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sadd.blocker.R
import com.sadd.blocker.data.datastore.AppTheme
import com.sadd.blocker.data.datastore.ColorTheme

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

/**
 * رنگ‌بندی‌ها — ۶ پالت، هر کدام روشن و تیره خودش را دارد.
 * قرارداد کنتراست: در روشن، primary تیره است (متن سفید روی آن خواناست و
 * خودش روی پس‌زمینه روشن خواناست)؛ در تیره، primary روشن است و onPrimary
 * تیره. رنگ خطا در همه پالت‌ها یکی است تا معنای خطا ثابت بماند.
 */
private fun paletteColors(
    dark: Boolean,
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    secondary: Color,
    onSecondary: Color,
    tertiary: Color,
    onTertiary: Color,
    background: Color,
    onBackground: Color,
    surface: Color,
    onSurface: Color,
    surfaceVariant: Color,
    onSurfaceVariant: Color,
    outline: Color,
): ColorScheme {
    return if (dark) {
        darkColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = secondary, onSecondary = onSecondary,
            tertiary = tertiary, onTertiary = onTertiary,
            error = Coral, onError = NavyDeep,
            errorContainer = Color(0xFF4A1E12), onErrorContainer = Color(0xFFFFC9B8),
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            outline = outline,
        )
    } else {
        lightColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = secondary, onSecondary = onSecondary,
            tertiary = tertiary, onTertiary = onTertiary,
            error = Color(0xFFC93A1E), onError = Color.White,
            errorContainer = Color(0xFFFFE3DA), onErrorContainer = Color(0xFF5C130A),
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            outline = outline,
        )
    }
}

private fun sunsetColors(dark: Boolean): ColorScheme {
    return if (dark) {
        paletteColors(
            dark = true,
            primary = Color(0xFFFFB59A), onPrimary = Color(0xFF3A130A),
            primaryContainer = Color(0xFF7A2E12), onPrimaryContainer = Color(0xFFFFDCC8),
            secondary = Color(0xFFF5B544), onSecondary = Color(0xFF2A1C00),
            tertiary = Color(0xFFC9B0FF), onTertiary = Color(0xFF2A1150),
            background = Color(0xFF1C100A), onBackground = Color(0xFFFFF1E6),
            surface = Color(0xFF281710), onSurface = Color(0xFFFFF1E6),
            surfaceVariant = Color(0xFF3D2718), onSurfaceVariant = Color(0xFFE3BFA4),
            outline = Color(0xFF6B4A38),
        )
    } else {
        paletteColors(
            dark = false,
            primary = Color(0xFFB43A12), onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDCC8), onPrimaryContainer = Color(0xFF4A1500),
            secondary = Color(0xFF8A5A00), onSecondary = Color.White,
            tertiary = Color(0xFF6D28D9), onTertiary = Color.White,
            background = Color(0xFFFFF8F3), onBackground = Color(0xFF2B160B),
            surface = Color.White, onSurface = Color(0xFF2B160B),
            surfaceVariant = Color(0xFFF6E4D6), onSurfaceVariant = Color(0xFF6E4B34),
            outline = Color(0xFFC4A894),
        )
    }
}

private fun forestColors(dark: Boolean): ColorScheme {
    return if (dark) {
        paletteColors(
            dark = true,
            primary = Color(0xFF7EDFA0), onPrimary = Color(0xFF062B16),
            primaryContainer = Color(0xFF14532D), onPrimaryContainer = Color(0xFFC9EED6),
            secondary = Color(0xFFA8D98A), onSecondary = Color(0xFF16290A),
            tertiary = Color(0xFF7FD8E0), onTertiary = Color(0xFF00363C),
            background = Color(0xFF0A1A10), onBackground = Color(0xFFEAF6EC),
            surface = Color(0xFF122619), onSurface = Color(0xFFEAF6EC),
            surfaceVariant = Color(0xFF1F3A28), onSurfaceVariant = Color(0xFFB3CDB8),
            outline = Color(0xFF4A6B52),
        )
    } else {
        paletteColors(
            dark = false,
            primary = Color(0xFF1D6B38), onPrimary = Color.White,
            primaryContainer = Color(0xFFC9EED6), onPrimaryContainer = Color(0xFF062B16),
            secondary = Color(0xFF5B7C2A), onSecondary = Color.White,
            tertiary = Color(0xFF0E7C86), onTertiary = Color.White,
            background = Color(0xFFF3F9F3), onBackground = Color(0xFF12261A),
            surface = Color.White, onSurface = Color(0xFF12261A),
            surfaceVariant = Color(0xFFE2EEE2), onSurfaceVariant = Color(0xFF44564A),
            outline = Color(0xFFA8BFAE),
        )
    }
}

private fun oceanColors(dark: Boolean): ColorScheme {
    return if (dark) {
        paletteColors(
            dark = true,
            primary = Color(0xFF7CC4FF), onPrimary = Color(0xFF062A44),
            primaryContainer = Color(0xFF0F3D5C), onPrimaryContainer = Color(0xFFCFE8FF),
            secondary = Color(0xFF7FD8E0), onSecondary = Color(0xFF00363C),
            tertiary = Color(0xFFC9B0FF), onTertiary = Color(0xFF2A1150),
            background = Color(0xFF081826), onBackground = Color(0xFFEAF3FA),
            surface = Color(0xFF0F2438), onSurface = Color(0xFFEAF3FA),
            surfaceVariant = Color(0xFF1B3549), onSurfaceVariant = Color(0xFFB1C7D8),
            outline = Color(0xFF45617A),
        )
    } else {
        paletteColors(
            dark = false,
            primary = Color(0xFF0B5FA5), onPrimary = Color.White,
            primaryContainer = Color(0xFFCFE8FF), onPrimaryContainer = Color(0xFF062A44),
            secondary = Color(0xFF0E7C86), onSecondary = Color.White,
            tertiary = Color(0xFF6D28D9), onTertiary = Color.White,
            background = Color(0xFFF2F8FC), onBackground = Color(0xFF0C1F2E),
            surface = Color.White, onSurface = Color(0xFF0C1F2E),
            surfaceVariant = Color(0xFFE0EBF3), onSurfaceVariant = Color(0xFF41586C),
            outline = Color(0xFFA9BECD),
        )
    }
}

private fun grapeColors(dark: Boolean): ColorScheme {
    return if (dark) {
        paletteColors(
            dark = true,
            primary = Color(0xFFC4A5FF), onPrimary = Color(0xFF240B4D),
            primaryContainer = Color(0xFF4A2A86), onPrimaryContainer = Color(0xFFE4D8FF),
            secondary = Color(0xFFF0A0D0), onSecondary = Color(0xFF3D0A2E),
            tertiary = Color(0xFF7FD8E0), onTertiary = Color(0xFF00363C),
            background = Color(0xFF150A26), onBackground = Color(0xFFF2ECFD),
            surface = Color(0xFF1F1238), onSurface = Color(0xFFF2ECFD),
            surfaceVariant = Color(0xFF2E2149), onSurfaceVariant = Color(0xFFC3B6DE),
            outline = Color(0xFF5E4E80),
        )
    } else {
        paletteColors(
            dark = false,
            primary = Color(0xFF6D28D9), onPrimary = Color.White,
            primaryContainer = Color(0xFFE4D8FF), onPrimaryContainer = Color(0xFF240B4D),
            secondary = Color(0xFFA03D7A), onSecondary = Color.White,
            tertiary = Color(0xFF0E7C86), onTertiary = Color.White,
            background = Color(0xFFF8F5FE), onBackground = Color(0xFF1D1030),
            surface = Color.White, onSurface = Color(0xFF1D1030),
            surfaceVariant = Color(0xFFEAE4F6), onSurfaceVariant = Color(0xFF51456E),
            outline = Color(0xFFB7A9D1),
        )
    }
}

private fun roseColors(dark: Boolean): ColorScheme {
    return if (dark) {
        paletteColors(
            dark = true,
            primary = Color(0xFFFF9EC7), onPrimary = Color(0xFF3D0A24),
            primaryContainer = Color(0xFF7A2450), onPrimaryContainer = Color(0xFFFFD6E7),
            secondary = Color(0xFFFBBF24), onSecondary = Color(0xFF2A1C00),
            tertiary = Color(0xFFC9B0FF), onTertiary = Color(0xFF2A1150),
            background = Color(0xFF220D18), onBackground = Color(0xFFFBEFF4),
            surface = Color(0xFF301221), onSurface = Color(0xFFFBEFF4),
            surfaceVariant = Color(0xFF42202F), onSurfaceVariant = Color(0xFFD6A9BE),
            outline = Color(0xFF6E4456),
        )
    } else {
        paletteColors(
            dark = false,
            primary = Color(0xFFBE185D), onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD6E7), onPrimaryContainer = Color(0xFF3D0A24),
            secondary = Color(0xFFB45309), onSecondary = Color.White,
            tertiary = Color(0xFF6D28D9), onTertiary = Color.White,
            background = Color(0xFFFDF5F8), onBackground = Color(0xFF2B1019),
            surface = Color.White, onSurface = Color(0xFF2B1019),
            surfaceVariant = Color(0xFFF6E2EA), onSurfaceVariant = Color(0xFF6E4456),
            outline = Color(0xFFD0A9BC),
        )
    }
}

/** نام فارسی رنگ‌بندی برای انتخاب‌گر تم. */
fun paletteLabelFa(colorTheme: ColorTheme): String {
    return when (colorTheme) {
        ColorTheme.SADD -> "سد"
        ColorTheme.SUNSET -> "غروب"
        ColorTheme.FOREST -> "جنگل"
        ColorTheme.OCEAN -> "اقیانوس"
        ColorTheme.GRAPE -> "انگور"
        ColorTheme.ROSE -> "رز"
    }
}

/** رنگ‌بندی کامل برای حالت روشن/تیره داده‌شده. */
fun colorSchemeFor(colorTheme: ColorTheme, dark: Boolean): ColorScheme {
    return when (colorTheme) {
        ColorTheme.SADD -> if (dark) DarkColors else LightColors
        ColorTheme.SUNSET -> sunsetColors(dark)
        ColorTheme.FOREST -> forestColors(dark)
        ColorTheme.OCEAN -> oceanColors(dark)
        ColorTheme.GRAPE -> grapeColors(dark)
        ColorTheme.ROSE -> roseColors(dark)
    }
}

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

/** شعاع نرم و بالشتی: کارت ۲۴dp، کارت hero ‏۲۸dp، دکمه ۲۸dp، چیپ ۸dp، دیالوگ ۲۸dp، فیلد متنی ۲۰dp. */
object SaddRadius {
    val Card = 24.dp
    val Hero = 28.dp
    val Button = 28.dp
    val Chip = 8.dp
    val Dialog = 28.dp
    val TextField = 20.dp
}

/**
 * توکن‌های شیشه‌ای («سد» — دکمه‌ها و دیالوگ‌های نرم‌تر):
 * - ظرف دیالوگ: surface با آلفای ۰٫۹۲ — پس‌زمینه کمی از پشت دیده می‌شود،
 *   ولی متن با آلفای ۱۰۰٪ خوانا می‌ماند (کنتراست روی surface تیره/روشن
 *   همه ۶ پالت حفظ می‌شود، چون فقط ۸٪ شفافیت اضافه می‌شود).
 * - دکمه اصلی: primary با آلفای ۰٫۸۵ — روی پس‌زمینه سرمه‌ای/روشن هنوز
 *   onPrimary با کنتراست کامل خواناست.
 * - حاشیه: ۱dp از onSurface با آلفای ۰٫۱۲ — در تم تیره روشن و در تم
 *   روشن تیره دیده می‌شود، در هر ۶ پالت.
 * این‌ها فقط توکن‌اند؛ اعمال‌شان در کامپوننت‌های مشترک (Common.kt) است.
 */
object SaddGlass {
    val DialogContainerAlpha = 0.92f
    val ButtonContainerAlpha = 0.85f
    val BorderAlpha = 0.12f
    val BorderWidth = 1.dp
}

/** رنگ حاشیه شیشه‌ای: onSurface با آلفای کم — در تیره روشن، در روشن تیره. */
@Composable
fun saddGlassBorderColor(): Color =
    MaterialTheme.colorScheme.onSurface.copy(alpha = SaddGlass.BorderAlpha)

/** رنگ ظرف دیالوگ شیشه‌ای: surface با آلفای ۰٫۹۲. */
@Composable
fun saddGlassDialogContainerColor(): Color =
    MaterialTheme.colorScheme.surface.copy(alpha = SaddGlass.DialogContainerAlpha)

private val SaddShapes = Shapes(
    extraSmall = RoundedCornerShape(SaddRadius.Chip),
    small = RoundedCornerShape(SaddRadius.Button),
    medium = RoundedCornerShape(SaddRadius.Card),
    large = RoundedCornerShape(SaddRadius.Dialog),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * تم «سد»: روشن / تیره / سیستمی + رنگ‌بندی (پیش‌فرض «سد»).
 * رنگ‌بندی و روشن/تیره دو انتخاب جدا هستند: رنگ‌بندی پالت را عوض می‌کند و
 * روشن/تیره نسخه روشن یا تیره همان پالت را می‌دهد.
 * در SYSTEM از تنظیم سیستم پیروی می‌کند؛ پیش‌فرض DARK است.
 * جهت چیدمان کل اپ همیشه RTL است (فارسی) — همه صفحه‌ها از اینجا به ارث می‌برند.
 */
@Composable
fun BlockerTheme(
    theme: AppTheme = AppTheme.DARK,
    colorTheme: ColorTheme = ColorTheme.SADD,
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorSchemeFor(colorTheme, dark),
            typography = SaddTypography,
            shapes = SaddShapes,
            content = content,
        )
    }
}
