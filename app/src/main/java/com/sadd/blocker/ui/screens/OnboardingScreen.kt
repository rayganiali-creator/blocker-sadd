package com.sadd.blocker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.R
import com.sadd.blocker.domain.OnboardingSteps
import com.sadd.blocker.ui.OnboardingViewModel
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.ChipPink
import com.sadd.blocker.ui.theme.ChipPurple
import com.sadd.blocker.ui.theme.ChipYellow
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddGlassSurfaceColor
import com.sadd.blocker.ui.theme.saddSpecularBorderColor
import kotlinx.coroutines.launch

/**
 * ویزارد راه‌اندازی اول‌نصب — دقیقاً طبق موکاپ تاییدشده:
 * سربرگ «راه‌اندازی سد» + خرس، نوار پیشرفت «مرحله X از ۵»، و کارت
 * مرحله فعلی با توضیح و دکمه بزرگ.
 *
 * نمایش یکی‌یکی است: فقط کارت مرحله فعلی کامل دیده می‌شود؛ مرحله‌های
 * انجام‌شده فقط به‌صورت یک ردیف کوچک جمع‌شده با تیک سبز بالای کارت
 * فعلی می‌آیند؛ مرحله‌های آینده پنهان‌اند تا وقتی نوبتشان برسد.
 * وقتی مرحله فعلی انجام شود، ویزارد خودکار به مرحله بعد می‌رود
 * (وضعیت‌ها زنده‌اند: ON_RESUME و بعد از هر نتیجه لانچر تازه می‌شوند).
 * مرحله دسترسی‌پذیری یک دنباله سه‌مرحله‌ای است، دقیقاً طبق ترتیبی که
 * روی گوشی سید واقعاً اتفاق می‌افتد: ۱) زدن کلید خاکستری «سد» در
 * تنظیمات دسترسی‌پذیری و بستن پیام، ۲) روشن‌کردن Allow restricted
 * settings در اطلاعات برنامه، ۳) برگشت و فعال‌کردن «سد». هر مرحله
 * دکمه بزرگ خودش و دکمه «انجام شد، ادامه» دارد؛ تایید دستی مرحله‌های
 * ۱ و ۲ فقط «تأیید شما»ست تا وقتی سرویس واقعاً فعال شود.
 * اندروید برای برنامه‌های نصب‌شده دستی کلید دسترسی‌پذیری را قفل می‌کند
 * تا وقتی این اجازه داده نشود؛ هیچ کدی نمی‌تواند دورش بزند.
 *
 * تیک هیچ مرحله‌ای جعل نمی‌شود: یا سیستم واقعاً آن را تایید کرده،
 * یا کاربر «انجامش دادم» را زده و «تأیید شما» نشان داده می‌شود.
 */
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onFinish: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launchers = rememberPermissionLaunchers { viewModel.refresh() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val steps = uiState.steps
    val currentStep = steps.getOrNull(uiState.currentStepIndex)
    val grantedCount = steps.count { it.granted }
    // مرحله فعلی دنباله دسترسی‌پذیری (۱ تا ۳)؛ تایید دستی مرحله‌ها تا
    // وقتی سرویس واقعاً فعال نشده، در فهرست «تأیید شما» نشان داده می‌شود.
    val accessibilityPhase = uiState.accessibilityPhase
    val progress = when {
        steps.isEmpty() -> 0f
        uiState.allGranted -> 1f
        else -> (uiState.currentStepIndex + 1) / steps.size.toFloat()
    }

    fun finish() {
        scope.launch {
            viewModel.completeOnboarding()
            onFinish()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = SaddSpacing.Large),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.sadd_bear),
                    contentDescription = "خرس سد",
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.size(SaddSpacing.Small))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "راه‌اندازی سد",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "برای اینکه سد درست کار کند، این ۵ مرحله را انجام بده.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(SaddSpacing.Medium))
            Text(
                if (uiState.allGranted) "همه مراحل انجام شد" else "مرحله ${uiState.currentStepIndex + 1} از ${steps.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(SaddSpacing.Small))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(SaddSpacing.Large))
        }

        // نمایش یکی‌یکی: مرحله‌های انجام‌شده فقط ردیف کوچک جمع‌شده با تیک‌اند؛
        // مرحله‌های آینده پنهان‌اند. ردیف‌های جمع‌شده قابل‌زدن نیستند —
        // مرحله انجام‌شده کاری برای انجام ندارد و selectStep هم مرحله
        // انجام‌شده را انتخاب نمی‌کند.
        val completedSteps = steps.filter { it.granted }
        if (completedSteps.isNotEmpty()) {
            item {
                // مرحله‌های انجام‌شده داخل یک کارت Liquid Glass جمع می‌شوند (فقط ظاهری).
                SaddCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                        completedSteps.forEach { step ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                StepStatusIcon(granted = true, isCurrent = false)
                                Spacer(Modifier.size(SaddSpacing.Small))
                                Text(
                                    step.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "انجام شد",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(SaddSpacing.Small))
            }
        }

        if (currentStep != null) {
            item {
                SaddCard(modifier = Modifier.fillMaxWidth(), hero = true) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // چیپ آیکون مرحله فعلی (فقط ظاهری؛ جریان مرحله‌ها عوض نمی‌شود).
                            val (stepIcon, stepColor) = stepVisual(currentStep.key)
                            SaddIconChip(icon = stepIcon, color = stepColor)
                            Spacer(Modifier.width(SaddSpacing.Medium))
                            Text(
                                currentStep.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            currentStep.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (currentStep.key == "accessibility") {
                            Spacer(Modifier.height(SaddSpacing.Medium))
                            Text(
                                "روی گوشی تو ترتیب کار این‌طوری است: اول کلید خاکستری «سد» را " +
                                    "می‌زنی و پیام را می‌بندی؛ بعد در اطلاعات برنامه Allow restricted " +
                                    "settings را روشن می‌کنی؛ بعد برمی‌گردی و کلید حالا روشن شده و " +
                                    "«سد» را فعال می‌کنی.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(SaddSpacing.Medium))
                            AccessibilityPhases(
                                currentPhase = accessibilityPhase,
                                onOpenAccessibilitySettings = {
                                    requestPermission(context, currentStep, launchers) {
                                        viewModel.refresh()
                                    }
                                },
                                onOpenAppInfo = { openAppInfoPage(context) },
                                onConfirmPhase = { viewModel.confirmAccessibilityPhase(it) },
                                onRefresh = { viewModel.refresh() },
                            )
                        } else {
                            Spacer(Modifier.height(SaddSpacing.Medium))
                            SaddButton(
                                text = ctaLabel(currentStep.key),
                                onClick = {
                                    requestPermission(context, currentStep, launchers) {
                                        viewModel.refresh()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(SaddSpacing.Medium))
            }
        }

        if (uiState.allGranted) {
            item {
                SaddCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            "همه مجوزها فعال شد. سد آماده است.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(SaddSpacing.Medium))
                        SaddButton(
                            text = "ورود به سد",
                            onClick = { finish() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Spacer(Modifier.height(SaddSpacing.Medium))
            }
        }

        item {
            Spacer(Modifier.height(SaddSpacing.Large))
            Text(
                "$grantedCount از ${steps.size} مرحله انجام شده است.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(SaddSpacing.Small))
            SaddButton(
                text = "بعداً انجام می‌دهم",
                onClick = { finish() },
                style = SaddButtonStyle.GHOST,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(SaddSpacing.Large))
        }
    }
}

/** آیکون و رنگ چیپ هر مرحله ویزارد (فقط نمایشی). */
private fun stepVisual(key: String): Pair<ImageVector, Color> {
    return when (key) {
        OnboardingSteps.KEY_NOTIFICATION -> Icons.Filled.Notifications to ChipPurple
        OnboardingSteps.KEY_ACCESSIBILITY -> Icons.Filled.Settings to ChipMint
        OnboardingSteps.KEY_USAGE -> Icons.Filled.Info to ChipYellow
        OnboardingSteps.KEY_BATTERY -> Icons.Filled.Timer to ChipPink
        OnboardingSteps.KEY_ALARM -> Icons.Filled.Schedule to ChipMint
        else -> Icons.Filled.Info to ChipYellow
    }
}

/** برچسب دکمه مرحله فعلی. */
private fun ctaLabel(key: String): String {
    return when (key) {
        "notification" -> "درخواست مجوز"
        else -> "فعال کردن"
    }
}

/**
 * دنباله سه‌مرحله‌ای دسترسی‌پذیری، دقیقاً طبق ترتیب واقعی گوشی سید:
 * ۱) زدن کلید خاکستری «سد» و بستن پیام، ۲) روشن‌کردن Allow restricted
 * settings در اطلاعات برنامه، ۳) برگشت و فعال‌کردن «سد».
 * هر مرحله دکمه بزرگ خودش را دارد؛ تایید دستی مرحله‌های ۱ و ۲ فقط
 * «تأیید شما»ست تا وقتی سرویس واقعاً فعال شود. مرحله ۳ دکمه تایید
 * دستی ندارد — انجام‌شدنش با فعال‌شدن واقعی سرویس خودکار تشخیص داده
 * می‌شود (تازه‌سازی موقع برگشت به اپ).
 */
@Composable
private fun AccessibilityPhases(
    currentPhase: Int,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onConfirmPhase: (Int) -> Unit,
    onRefresh: () -> Unit,
) {
    Column {
        AccessibilityPhaseBlock(
            number = "۱",
            phase = 1,
            currentPhase = currentPhase,
            title = "کلید خاکستری را بزن و پیام را ببند",
            text = "در تنظیمات دسترسی‌پذیری، «سد» را پیدا کن و روی کلید خاکستری آن بزن. " +
                "پیامی می‌آید؛ آن را ببند و برگرد.",
            buttonText = "رفتن به تنظیمات دسترسی‌پذیری",
            onButtonClick = onOpenAccessibilitySettings,
            showConfirm = true,
            onConfirm = { onConfirmPhase(1) },
            onRefresh = onRefresh,
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        AccessibilityPhaseBlock(
            number = "۲",
            phase = 2,
            currentPhase = currentPhase,
            title = "Allow restricted settings را روشن کن",
            text = "در صفحه اطلاعات برنامه، روی ⋮ (سه‌نقطه بالای صفحه) بزن و " +
                "«Allow restricted settings» را روشن کن، بعد برگرد به «سد».",
            buttonText = "رفتن به اطلاعات برنامه",
            onButtonClick = onOpenAppInfo,
            showConfirm = true,
            onConfirm = { onConfirmPhase(2) },
            onRefresh = onRefresh,
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        AccessibilityPhaseBlock(
            number = "۳",
            phase = 3,
            currentPhase = currentPhase,
            title = "«سد» را در دسترسی‌پذیری فعال کن",
            text = "حالا کلید «سد» روشن شده؛ آن را فعال کن. وقتی فعال شود، " +
                "ویزارد خودش جلو می‌رود.",
            buttonText = "بازگشت به دسترسی‌پذیری",
            onButtonClick = onOpenAccessibilitySettings,
            showConfirm = false,
            onConfirm = {},
            onRefresh = onRefresh,
        )
    }
}

/**
 * یک مرحله از دنباله دسترسی‌پذیری: دایره شماره (یا تیک برای انجام‌شده)،
 * عنوان، توضیح، دکمه بزرگ و — برای مرحله‌های ۱ و ۲ — دکمه
 * «انجام شد، ادامه». مرحله‌های آینده دکمه ندارند؛ مرحله‌های
 * انجام‌شده «تأیید شما» نشان می‌دهند تا وقتی سرویس واقعاً فعال شود.
 */
@Composable
private fun AccessibilityPhaseBlock(
    number: String,
    phase: Int,
    currentPhase: Int,
    title: String,
    text: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    showConfirm: Boolean,
    onConfirm: () -> Unit,
    onRefresh: () -> Unit,
) {
    val done = OnboardingSteps.isAccessibilityPhaseDone(phase, currentPhase, currentPhase == 0)
    val isCurrent = phase == currentPhase
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp)
                    .background(
                        if (done) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            saddGlassSurfaceColor()
                        },
                        CircleShape,
                    )
                    .border(
                        1.dp,
                        if (done) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            saddSpecularBorderColor()
                        },
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (done) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        number,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Spacer(Modifier.size(SaddSpacing.Small))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (done) {
                Text(
                    "تأیید شما",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(SaddSpacing.Small))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isCurrent) {
            Spacer(Modifier.height(SaddSpacing.Small))
            SaddButton(
                text = buttonText,
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(SaddSpacing.Small))
            if (showConfirm) {
                SaddButton(
                    text = "انجام شد، ادامه",
                    onClick = onConfirm,
                    style = SaddButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                SaddButton(
                    text = "بررسی دوباره",
                    onClick = onRefresh,
                    style = SaddButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** آیکون وضعیت مرحله: تیک برای انجام‌شده، دایره توپر برای فعلی، دایره خالی برای در انتظار. */
@Composable
private fun StepStatusIcon(granted: Boolean, isCurrent: Boolean) {
    when {
        granted -> Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        isCurrent -> Box(
            modifier = Modifier.size(24.dp)
                .padding(2.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        else -> Box(
            modifier = Modifier.size(24.dp)
                .padding(2.dp)
                .border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
        )
    }
}
