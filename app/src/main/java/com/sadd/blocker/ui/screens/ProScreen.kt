package com.sadd.blocker.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.domain.JalaliDate
import com.sadd.blocker.domain.ProEntitlement
import com.sadd.blocker.domain.ProPlan
import com.sadd.blocker.domain.ProStatus
import com.sadd.blocker.ui.ProViewModel
import com.sadd.blocker.ui.theme.ChipYellow
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddPrimaryTextColor

/**
 * صفحه «سد پرو»: وضعیت فعلی، پلن‌ها (قیمت فروشگاه یا پشتیبان)، خرید و
 * بازیابی خرید، و فهرست امکانات پرو. هیچ فیلد کد فعال‌سازی‌ای در این
 * صفحه نیست — فعال‌سازی توسعه‌دهنده فقط با ژست مخفی روی تصویر خرس
 * داشبورد انجام می‌شود و هیچ‌جا به آن اشاره‌ای نشده است.
 */
@Composable
fun ProScreen(
    viewModel: ProViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SaddSpacing.Large),
    ) {
        SaddTopBar(title = "سد پرو", subtitle = "همه امکانات سد، بدون محدودیت", onBack = onBack)
        MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })

        // وضعیت فعلی پرو.
        SaddCard(modifier = Modifier.fillMaxWidth(), hero = true) {
            Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SaddIconChip(icon = Icons.Filled.Star, color = ChipYellow)
                    Spacer(Modifier.size(SaddSpacing.Small))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (val status = uiState.status) {
                                ProStatus.Free -> "نسخه رایگان"
                                is ProStatus.Trial -> "دوره آزمایشی فعال است"
                                is ProStatus.Active -> "سد پرو فعال است"
                                ProStatus.Lifetime -> "سد پرو مادام‌العمر فعال است"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = when (val status = uiState.status) {
                                ProStatus.Free ->
                                    "در نسخه رایگان از هر نوع قانون حداکثر ${JalaliDate.toPersianDigits(ProEntitlement.FREE_RULE_CAP.toString())} قانون فعال می‌شود."
                                is ProStatus.Trial ->
                                    "${JalaliDate.toPersianDigits(uiState.trialDaysLeft.toString())} روز از دوره آزمایشی مانده (تا ${JalaliDate.formatDate(status.trialEndsAtMs)})."
                                is ProStatus.Active ->
                                    "تا ${JalaliDate.formatDate(status.untilMs)} فعال است. خرید دوباره مدت را اضافه می‌کند."
                                ProStatus.Lifetime -> "همه امکانات برای همیشه باز است."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        SectionTitle("پلن‌ها")
        for (plan in ProPlan.entries) {
            val storeProduct = uiState.storeProducts[plan.productId]
            val priceText = storeProduct?.priceText
                ?: ProEntitlement.formatTomanFa(plan.fallbackPriceToman)
            val buying = uiState.buyingPlan == plan
            SaddCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                hero = plan.isLifetime,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (plan.isLifetime) "مادام‌العمر" else "پرو ${plan.labelFa}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = priceText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = saddPrimaryTextColor(),
                        )
                        if (plan.isLifetime) {
                            Text(
                                text = "یک‌بار پرداخت، برای همیشه.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    SaddButton(
                        text = if (buying) "در حال خرید…" else "خرید",
                        onClick = {
                            if (activity != null) viewModel.buy(activity, plan)
                        },
                        enabled = uiState.billingAvailable && uiState.buyingPlan == null && activity != null,
                        style = if (plan.isLifetime) SaddButtonStyle.PRIMARY else SaddButtonStyle.SECONDARY,
                    )
                }
            }
        }

        Spacer(Modifier.height(SaddSpacing.Small))
        SaddButton(
            text = if (uiState.restoring) "در حال بازیابی…" else "بازیابی خرید",
            onClick = { viewModel.restore() },
            enabled = uiState.billingAvailable && !uiState.restoring,
            style = SaddButtonStyle.SECONDARY,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionTitle("امکانات سد پرو")
        val features = listOf(
            "حالت سخت‌گیرانه (Strict) برای سشن‌ها",
            "بلاک بخش‌های اپ (جستجو، اکسپلور، ریلز و…)",
            "جلوگیری از حذف نصب و توقف اجباری",
            "زمان‌بندی خودکار سشن‌ها",
            "پشتیبان‌گیری و بازیابی قوانین",
            "همه رنگ‌بندی‌های برنامه",
            "قفل PIN",
            "قانون نامحدود (کلمه، اپ و وب‌سایت)",
        )
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium)) {
                for (feature in features) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = saddPrimaryTextColor(),
                            modifier = Modifier.size(SaddSpacing.Medium),
                        )
                        Spacer(Modifier.size(SaddSpacing.Small))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(SaddSpacing.Medium))
        Text(
            text = if (uiState.billingAvailable) {
                "پرداخت از طریق ${uiState.storeNameFa ?: "فروشگاه"} انجام می‌شود. خرید پلن زمانی قبل از پایان، مدت را به همان پایان اضافه می‌کند."
            } else {
                "خرید فقط در نسخه‌ای که از کافه‌بازار یا مایکت نصب شده باشد کار می‌کند. سه روز اول بعد از نصب، سد پرو خودکار فعال است."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(SaddSpacing.Large))
    }
}
