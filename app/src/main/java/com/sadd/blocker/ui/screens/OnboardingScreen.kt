package com.sadd.blocker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.sadd.blocker.ui.OnboardingViewModel
import com.sadd.blocker.ui.theme.SaddRadius
import com.sadd.blocker.ui.theme.SaddSpacing
import kotlinx.coroutines.launch

/**
 * ویزارد راه‌اندازی اول‌نصب — دقیقاً طبق موکاپ تاییدشده:
 * سربرگ «راه‌اندازی سد» + خرس، نوار پیشرفت «مرحله X از ۶»، کارت مرحله
 * فعلی با توضیح و دکمه بزرگ، فهرست ۶ مرحله با وضعیت، جعبه راهنمای
 * restricted settings برای دسترسی‌پذیری، و لینک «بعداً انجام می‌دهم».
 * وضعیت‌ها زنده‌اند (ON_RESUME و بعد از هر نتیجه لانچر تازه می‌شوند)؛
 * وقتی مرحله فعلی انجام شود، ویزارد خودکار به مرحله بعد می‌رود.
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
                        "برای اینکه سد درست کار کند، این ۶ مجوز را بده.",
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

        if (currentStep != null) {
            item {
                SaddCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
                    ) {
                        Text(
                            currentStep.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            currentStep.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                Spacer(Modifier.height(SaddSpacing.Medium))
            }
        }

        if (currentStep?.key == "accessibility") {
            item {
                RestrictedSettingsHint()
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
            SectionTitle("مراحل راه‌اندازی")
        }
        itemsIndexed(steps, key = { _, step -> step.key }) { index, step ->
            val isCurrent = index == uiState.currentStepIndex && !uiState.allGranted
            SaddCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .then(
                        if (isCurrent) {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(SaddRadius.Card),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable { viewModel.selectStep(index) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StepStatusIcon(granted = step.granted, isCurrent = isCurrent)
                    Spacer(Modifier.size(SaddSpacing.Small))
                    Text(
                        step.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        when {
                            step.granted -> "انجام شد"
                            isCurrent -> "فعلی"
                            else -> "در انتظار"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (step.granted || isCurrent) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(SaddSpacing.Large))
            Text(
                "$grantedCount از ${steps.size} مجوز فعال شده است.",
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

/** برچسب دکمه مرحله فعلی. */
private fun ctaLabel(key: String): String {
    return when (key) {
        "notification" -> "درخواست مجوز"
        "vpn" -> "تایید VPN"
        else -> "فعال کردن"
    }
}

/**
 * راهنمای restricted settings — فقط برای مرحله دسترسی‌پذیری.
 * اندروید برای برنامه‌های نصب‌شده دستی (نه از فروشگاه) دکمه
 * دسترسی‌پذیری را قفل می‌کند تا وقتی این گزینه فعال نشود؛ هیچ کدی
 * نمی‌تواند دورش بزند، پس راهنما را همان‌جا به کاربر نشان می‌دهیم.
 */
@Composable
private fun RestrictedSettingsHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(SaddRadius.Card),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(SaddSpacing.Small))
            Text(
                "نکته: برای برنامه‌های نصب‌شده دستی، ابتدا باید در تنظیمات گوشی، " +
                    "از صفحه اطلاعات «سد»، گزینه «Allow restricted settings» را فعال کنید؛ " +
                    "در غیر این صورت دکمه دسترسی‌پذیری خاکستری می‌ماند.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
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
