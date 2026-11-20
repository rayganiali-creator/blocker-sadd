package com.sadd.blocker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
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

// سد — سیستم دیزاین Liquid Glass (سبک ۲۰۲۵ اپل، انتخاب سید ۲۰۲۶-۱۱-۱۹):
// پس‌زمینه ساده تک‌رنگ با گرادیان عمودی ملایم (انتخاب سید ۲۰۲۶-۱۱-۱۹:
// بدون لکه رنگی، تا متن سفید همیشه خوانا بماند)، کارت‌های شیشه‌ای مات با
// حاشیه درخشان (specular) که لبه بالایی روشن‌تر است، دکمه‌های قرصی
// شیشه‌ای با برجستگی نور بالا و فشرده‌شدن فنری، چیپ‌های آیکون رنگی توپر
// با برق شیشه، ناوبری پایین قرصی شناور با انتخاب زرد.
// نکته صادقانه: بلور واقعی پس‌زمینه (backdrop blur) بدون وابستگی تازه ممکن
// نیست؛ مات‌بودن با سطح نیمه‌شفاف روی پس‌زمینه ساده شبیه‌سازی می‌شود.
// پس‌زمینه روشن off-white می‌ماند تا متن خوانا بماند؛ بدون مشکی خالص.
val Mint = Color(0xFF7CFFD9)
val Teal = Color(0xFF0E9F8A)
val TealDark = Color(0xFF0B7A6B)
// پس‌زمینه/سطح تیره Liquid Glass: سرمه‌ای عمیق‌تر از قبل.
val Navy = Color(0xFF0B1626)
val NavyDeep = Color(0xFF050B18)
val NavySurface = Color(0xFF12203A)
val Coral = Color(0xFFFF7A59)
val Sunny = Color(0xFFFFC53D)
val Sky = Color(0xFF5AA9FF)
val Grape = Color(0xFF9B7BFF)
val LightBackground = Color(0xFFF7F8FA)
// رنگ‌های چیپ آیکون (مثل اسکرین‌شات‌ها: مینت/تیل، بنفش، زرد، صورتی).
// خود چیپ پس‌زمینه را با آلفای کم همین رنگ می‌سازد و آیکون تمام‌رنگ است.
val ChipMint = Color(0xFF5EEAD4)
val ChipPurple = Color(0xFFB79CFF)
val ChipYellow = Color(0xFFFFC53D)
val ChipPink = Color(0xFFFF8FAB)

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
    // قانون سید (۲۰۲۶-۱۱-۱۹): در حالت تیره همه متن‌ها سفیدند — متن روی
    // دکمه‌های پرشده هم سفید است و پرشده‌ها برای خوانایی تیره‌تر می‌شوند
    // (saddPrimaryButtonBrush / saddSelectedContainerColor).
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0E3D34),
    onPrimaryContainer = Color.White,
    secondary = Sky,
    onSecondary = Color.White,
    tertiary = Grape,
    onTertiary = Color.White,
    error = Coral,
    onError = Color.White,
    errorContainer = Color(0xFF4A1E12),
    onErrorContainer = Color.White,
    background = NavyDeep,
    onBackground = Color.White,
    surface = Navy,
    onSurface = Color.White,
    surfaceVariant = NavySurface,
    onSurfaceVariant = Color.White.copy(alpha = 0.75f),
    outline = Color(0xFF3A4A66),
    // Snackbar در تیره: پس‌زمینه تیره + متن و اکشن سفید (پیش‌فرض متریال
    // برعکس است و متن تیره روی روشن می‌داد).
    inverseSurface = NavySurface,
    inverseOnSurface = Color.White,
    inversePrimary = Color.White,
)

/**
 * رنگ‌بندی‌ها — ۶ پالت، هر کدام روشن و تیره خودش را دارد.
 * قرارداد کنتراست: در روشن، primary تیره است (متن سفید روی آن خواناست و
 * خودش روی پس‌زمینه روشن خواناست). در تیره همه متن‌ها سفیدند (خواسته سید
 * ۲۰۲۶-۱۱-۱۹) و پرشده‌های پشت متن با saddPrimaryButtonBrush و
 * saddSelectedContainerColor تیره‌تر می‌شوند تا سفید خوانا بماند.
 * رنگ خطا در همه پالت‌ها یکی است تا معنای خطا ثابت بماند.
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
        // قانون سید (۲۰۲۶-۱۱-۱۹): در تیره همه متن‌ها سفیدند؛ پارامترهای
        // on* تیره عمداً نادیده گرفته می‌شوند تا هیچ متن رنگی/کم‌رنگی در
        // هیچ پالتی نماند. پرشده‌های پشت متن (دکمه/چیپ انتخاب‌شده) با
        // saddPrimaryButtonBrush و saddSelectedContainerColor تیره‌تر
        // می‌شوند تا متن سفید روی آن‌ها خوانا بماند.
        darkColorScheme(
            primary = primary, onPrimary = Color.White,
            primaryContainer = primaryContainer, onPrimaryContainer = Color.White,
            secondary = secondary, onSecondary = Color.White,
            tertiary = tertiary, onTertiary = Color.White,
            error = Coral, onError = Color.White,
            errorContainer = Color(0xFF4A1E12), onErrorContainer = Color.White,
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            inverseSurface = surfaceVariant,
            inverseOnSurface = Color.White,
            inversePrimary = Color.White,
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
            background = Color(0xFF120A06), onBackground = Color.White,
            surface = Color(0xFF1C120B), onSurface = Color.White,
            surfaceVariant = Color(0xFF2E1F14), onSurfaceVariant = Color.White.copy(alpha = 0.75f),
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
            background = Color(0xFF050F08), onBackground = Color.White,
            surface = Color(0xFF0A1A10), onSurface = Color.White,
            surfaceVariant = Color(0xFF142B1D), onSurfaceVariant = Color.White.copy(alpha = 0.75f),
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
            background = Color(0xFF040E18), onBackground = Color.White,
            surface = Color(0xFF081A2C), onSurface = Color.White,
            surfaceVariant = Color(0xFF112A42), onSurfaceVariant = Color.White.copy(alpha = 0.75f),
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
            background = Color(0xFF0B0618), onBackground = Color.White,
            surface = Color(0xFF150C28), onSurface = Color.White,
            surfaceVariant = Color(0xFF221838), onSurfaceVariant = Color.White.copy(alpha = 0.75f),
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
            background = Color(0xFF160812), onBackground = Color.White,
            surface = Color(0xFF220E1A), onSurface = Color.White,
            surfaceVariant = Color(0xFF331A27), onSurfaceVariant = Color.White.copy(alpha = 0.75f),
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

/** شعاع Liquid Glass: کارت ۲۴dp، کارت hero ‏۲۸dp، دکمه قرصی ۲۸dp، چیپ ۱۲dp، دیالوگ ۲۸dp، فیلد متنی ۲۰dp. */
object SaddRadius {
    val Card = 24.dp
    val Hero = 28.dp
    val Button = 28.dp
    val Chip = 12.dp
    val Dialog = 28.dp
    val TextField = 20.dp
}

/**
 * توکن‌های شیشه‌ای Liquid Glass («سد»):
 * - سطح کارت: رنگ سطح پالت با آلفای ۰٫۵۲ — پس‌زمینه ساده از پشت کارت
 *   دیده می‌شود تا کارت واقعاً مات و شیشه‌ای خوانده شود؛ متن با آلفای
 *   ۱۰۰٪ می‌ماند تا خوانایی حفظ شود.
 * - ظرف دیالوگ: surface با آلفای ۰٫۸۶ — کمی مات‌تر از کارت تا روی scrim
 *   دیالوگ هم خوانا بماند.
 * - حاشیه درخشان (specular): ۱dp با گرادیان عمودی — لبه بالا روشن‌تر
 *   (سفید کم‌آلفا) و پایین کم‌رنگ‌تر (primary پالت)؛ مثل لبه شیشه واقعی.
 * - برق بالا (sheen): گرادیان سفید از بالا تا ۴۵٪ ارتفاع، روی کارت و
 *   دکمه و چیپ کشیده می‌شود.
 * - حاشیه مویی (hairline) و BorderAlpha برای سازگاری API مانده‌اند؛
 *   کال‌سایت‌های قدیمی همان رنگ قبلی را می‌گیرند.
 * این‌ها فقط توکن‌اند؛ اعمال‌شان در کامپوننت‌های مشترک (Common.kt) است.
 */
object SaddGlass {
    val CardContainerAlpha = 0.52f
    val DialogContainerAlpha = 0.86f
    /** سطح تقریباً مات برای متن‌های طولانی (دیالوگ بازگشایی Strict): خوانایی بر شیشه مقدم است. */
    val OpaqueContainerAlpha = 0.97f
    val ButtonContainerAlpha = 0.85f
    val BorderAlpha = 0.12f
    val HairlineAlpha = 0.40f
    val SpecularAlpha = 0.75f
    val SheenAlpha = 0.28f
    val BorderWidth = 1.dp
}

/** آیا پالت جاری تیره است؟ از روشنایی رنگ پس‌زمینه فهمیده می‌شود تا به حالت تم وابسته نباشد. */
@Composable
fun saddIsDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * قانون متن سفید سید (۲۰۲۶-۱۱-۱۹): در حالت تیره هر متنی سفید خالص است؛
 * در روشن، متن تأکیدی همان primary پالت می‌ماند. همه جاهایی که قبلاً متن
 * را primary می‌کردند از این توکن می‌گذرند.
 */
@Composable
fun saddPrimaryTextColor(): Color =
    if (saddIsDark()) Color.White else MaterialTheme.colorScheme.primary

/** متن خطا: در تیره سفید (قانون متن سفید سید)، در روشن همان قرمز خطا. */
@Composable
fun saddErrorTextColor(): Color =
    if (saddIsDark()) Color.White else MaterialTheme.colorScheme.error

/** متن با رنگ تأکید دلخواه (مثل دسته بازی‌ها): در تیره سفید، در روشن خود تأکید. */
@Composable
fun saddAccentTextColor(accent: Color): Color =
    if (saddIsDark()) Color.White else accent

/**
 * رنگ پرشده پشت متن سفید (چیپ انتخاب‌شده، دایره مرحله انجام‌شده):
 * در تیره، primary پاستلی به سمت مشکی تیره می‌شود تا متن/آیکون سفید روی
 * آن خوانا بماند؛ در روشن primary خودش تیره است و دست نمی‌خورد.
 */
@Composable
fun saddSelectedContainerColor(): Color {
    val primary = MaterialTheme.colorScheme.primary
    return if (saddIsDark()) lerp(primary, Color.Black, 0.52f) else primary
}

/**
 * قلم‌موی دکمه پرشده اصلی و قطعه انتخاب‌شده کنترل قطعه‌ای:
 * در تیره گرادیان primary تیره‌شده به سمت مشکی (متن سفید خوانا می‌ماند)،
 * در روشن همان گرادیان قبلی (روشن‌شده به سمت سفید تا primary).
 */
@Composable
fun saddPrimaryButtonBrush(): Brush {
    val primary = MaterialTheme.colorScheme.primary
    return if (saddIsDark()) {
        Brush.verticalGradient(
            colors = listOf(
                lerp(primary, Color.Black, 0.40f),
                lerp(primary, Color.Black, 0.58f),
            ),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(lerp(primary, Color.White, 0.22f), primary),
        )
    }
}

/** رنگ حاشیه مویی: primary پالت با آلفای ۰٫۴۰ — در هر ۶ پالت، روشن و تیره (سازگاری با کال‌سایت‌های قدیمی). */
@Composable
fun saddHairlineColor(): Color =
    MaterialTheme.colorScheme.primary.copy(alpha = SaddGlass.HairlineAlpha)

/** رنگ حاشیه شیشه‌ای — همان حاشیه مویی است (سازگاری با کال‌سایت‌های قدیمی). */
@Composable
fun saddGlassBorderColor(): Color = saddHairlineColor()

/** رنگ ظرف دیالوگ شیشه‌ای: surface با آلفای ۰٫۸۶. */
@Composable
fun saddGlassDialogContainerColor(): Color =
    MaterialTheme.colorScheme.surface.copy(alpha = SaddGlass.DialogContainerAlpha)

/**
 * رنگ سطح شیشه‌ای Liquid Glass برای کارت و پنل و فیلد:
 * در تیره، surfaceVariant کمی به سمت سفید روشن شده و نیمه‌شفاف است تا
 * پس‌زمینه ساده از پشتش دیده شود؛ در روشن، سفید نیمه‌شفاف.
 */
@Composable
fun saddGlassSurfaceColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (saddIsDark()) {
        lerp(scheme.surfaceVariant, Color.White, 0.14f)
            .copy(alpha = SaddGlass.CardContainerAlpha)
    } else {
        Color.White.copy(alpha = 0.62f)
    }
}

/** رنگ یکدست حاشیه درخشان (برای جاهایی که BorderStroke لازم است): سفید آمیخته با primary پالت. */
@Composable
fun saddSpecularBorderColor(): Color {
    val scheme = MaterialTheme.colorScheme
    return if (saddIsDark()) {
        lerp(Color.White, scheme.primary, 0.20f).copy(alpha = 0.65f)
    } else {
        lerp(Color.White, scheme.primary, 0.15f).copy(alpha = 0.90f)
    }
}

/**
 * قلم‌موی حاشیه درخشان: گرادیان عمودی سه‌توقفه که لبه بالا را خیلی روشن،
 * میان را ملایم و لبه پایین را کم‌رنگ می‌کند — مثل نور روی لبه شیشه.
 */
@Composable
fun saddSpecularBorderBrush(): Brush {
    val scheme = MaterialTheme.colorScheme
    return if (saddIsDark()) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.White.copy(alpha = SaddGlass.SpecularAlpha),
                0.35f to Color.White.copy(alpha = 0.18f),
                1f to scheme.primary.copy(alpha = 0.20f),
            ),
        )
    } else {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.White.copy(alpha = 0.95f),
                0.35f to Color.White.copy(alpha = 0.55f),
                1f to scheme.primary.copy(alpha = 0.30f),
            ),
        )
    }
}

/** برق بالای شیشه: گرادیان سفید از لبه بالا تا حدود نیمه ارتفاع که به شفاف می‌رسد. */
@Composable
fun saddGlassSheenBrush(): Brush {
    val alpha = if (saddIsDark()) SaddGlass.SheenAlpha else 0.35f
    return Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color.White.copy(alpha = alpha),
            0.45f to Color.Transparent,
        ),
    )
}

/**
 * پس‌زمینه ساده «سد» (انتخاب سید ۲۰۲۶-۱۱-۱۹): گرادیان عمودی تک‌رنگ از
 * رنگ پس‌زمینه پالت در بالا تا سایه کمی روشن‌ترِ همان خانواده رنگی در
 * پایین (آمیخته با surfaceVariant همان پالت). بدون هیچ لکه رنگی، بدون
 * بلور و بدون نقش — آرام و ساده تا متن سفید روی آن همیشه خوانا بماند.
 * در هر ۶ پالت و هر دو حالت روشن/تیره همین منطق کار می‌کند؛ صفحه بلاک
 * هم همین را با پالت تیره می‌گیرد.
 */
@Composable
fun SaddBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val baseBrush = Brush.verticalGradient(
        colors = listOf(
            scheme.background,
            lerp(scheme.background, scheme.surfaceVariant, 0.65f),
        ),
    )
    Box(modifier = modifier.fillMaxSize().background(baseBrush)) {
        content()
    }
}

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
    val scheme = colorSchemeFor(colorTheme, dark)
    // قانون متن سفید سید: رنگ پیش‌فرض هر متنی که رنگ صریح ندارد onBackground
    // است (در تیره سفید خالص، در روشن نزدیک‌مشکی) — هیچ متنی با رنگ پیش‌فرض
    // کامپوز روی پس‌زمینه نمی‌نشیند.
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalContentColor provides scheme.onBackground,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = SaddTypography,
            shapes = SaddShapes,
            content = content,
        )
    }
}
