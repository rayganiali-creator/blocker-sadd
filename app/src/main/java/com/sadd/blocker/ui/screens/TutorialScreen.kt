package com.sadd.blocker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sadd.blocker.R
import com.sadd.blocker.domain.TUTORIAL_PAGES
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.ChipPink
import com.sadd.blocker.ui.theme.ChipPurple
import com.sadd.blocker.ui.theme.ChipYellow
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddPrimaryTextColor

/**
 * آموزش آشنایی اول‌نصب — کارت‌های صفحه‌ای به سبک Liquid Glass، یک
 * کارت برای هر بخش اپ. فقط یک‌بار و داخل جریان راه‌اندازی نشان داده
 * می‌شود (پرچم tutorial_done در DataStore). «رد کردن» و «شروع» هر دو
 * آموزش را تمام می‌کنند؛ ثبت پرچم و رفتن به داشبورد با onFinish است.
 */
@Composable
fun TutorialContent(onFinish: () -> Unit) {
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    val page = TUTORIAL_PAGES[pageIndex]
    val isLast = pageIndex == TUTORIAL_PAGES.lastIndex

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = SaddSpacing.Large),
    ) {
        Spacer(Modifier.height(SaddSpacing.Large))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.sadd_bear),
                contentDescription = "خرس سد",
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.size(SaddSpacing.Small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "آشنایی با سد",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "بخش‌های برنامه را یک‌به‌یک معرفی می‌کنم.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Large))

        SaddCard(modifier = Modifier.fillMaxWidth(), hero = true) {
            Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, color) = tutorialVisual(page.title)
                    SaddIconChip(icon = icon, color = color)
                    Spacer(Modifier.width(SaddSpacing.Medium))
                    Text(
                        page.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${pageIndex + 1} از ${TUTORIAL_PAGES.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = saddPrimaryTextColor(),
                    )
                }
                Spacer(Modifier.height(SaddSpacing.Medium))
                Text(
                    page.description,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Medium))

        // نقطه‌های پیشرفت صفحه‌ها.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            TUTORIAL_PAGES.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier.size(if (index == pageIndex) 10.dp else 7.dp)
                        .background(
                            if (index == pageIndex) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            CircleShape,
                        ),
                )
                if (index != TUTORIAL_PAGES.lastIndex) {
                    Spacer(Modifier.width(6.dp))
                }
            }
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(SaddSpacing.Large))

        Row(modifier = Modifier.fillMaxWidth()) {
            SaddButton(
                text = "قبلی",
                onClick = { if (pageIndex > 0) pageIndex-- },
                style = SaddButtonStyle.SECONDARY,
                enabled = pageIndex > 0,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(SaddSpacing.Small))
            SaddButton(
                text = if (isLast) "شروع" else "بعدی",
                onClick = { if (isLast) onFinish() else pageIndex++ },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(SaddSpacing.Small))
        SaddButton(
            text = "رد کردن",
            onClick = onFinish,
            style = SaddButtonStyle.GHOST,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(SaddSpacing.Large))
    }
}

/** آیکون و رنگ چیپ هر صفحه آموزش — همان آیکون‌های نوار پایین اپ. */
private fun tutorialVisual(title: String): Pair<ImageVector, Color> {
    return when (title) {
        "داشبورد" -> Icons.Filled.Home to ChipMint
        "اپ‌ها" -> Icons.Filled.Apps to ChipPurple
        "وب" -> Icons.Filled.Language to ChipYellow
        "کلمات" -> Icons.Filled.TextFields to ChipPink
        "سشن‌ها" -> Icons.Filled.Timer to ChipMint
        "بازی‌ها" -> Icons.Filled.SportsEsports to ChipPurple
        "حالت بلاک" -> Icons.Filled.Settings to ChipYellow
        else -> Icons.Filled.Info to ChipYellow
    }
}
