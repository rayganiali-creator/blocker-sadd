package com.sadd.blocker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.R
import com.sadd.blocker.ui.DashboardRule
import com.sadd.blocker.ui.DashboardRuleKind
import com.sadd.blocker.ui.DashboardViewModel
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.ChipPink
import com.sadd.blocker.ui.theme.ChipPurple
import com.sadd.blocker.ui.theme.ChipYellow
import com.sadd.blocker.ui.theme.Mint
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddGlassSurfaceColor
import com.sadd.blocker.ui.theme.saddPrimaryTextColor
import com.sadd.blocker.ui.theme.saddSpecularBorderColor

/**
 * داشبورد — طبق موکاپ تاییدشده v2: سرتیتر با خرس «سد» و آیکون‌های
 * تنظیمات/اعلان، حلقه درخشان سشن، دسترسی سریع، دو کارت آمار واقعی
 * (قوانین فعال و بلاک‌های امروز از Room) و ردیف‌های
 * تاگل قوانین فعال که مستقیم روی همان قوانین واقعی کار می‌کنند.
 * شروع سشن از اینجا به صفحه سشن‌ها منتقل شده است.
 * همه اعداد از Room و سیستم خوانده می‌شوند، نه نمایشی.
 * تیکر فقط وقتی صفحه دیده می‌شود می‌دود (ON_RESUME روشن، ON_PAUSE خاموش).
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.refreshPermissions()
                    viewModel.setTicking(true)
                }
                Lifecycle.Event.ON_PAUSE -> viewModel.setTicking(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (uiState.strictUnlockVisible) {
        StrictUnlockDialog(
            method = uiState.strictUnlockMethod,
            cooldownHours = uiState.strictCooldownHours,
            cooldownRequest = uiState.strictCooldownRequest,
            onRecordCooldownRequest = { viewModel.recordStrictCooldownRequest() },
            onUnlocked = { viewModel.confirmStrictUnlocked() },
            onDismiss = { viewModel.cancelStrictUnlock() },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SaddSpacing.Large),
    ) {
        // سرتیتر موکاپ: خرس + «سد» سمت راست، زنگ و چرخ‌دنده سمت چپ (در RTL).
        // مسیر تنظیمات اختصاصی وجود ندارد، پس چرخ‌دنده به عیب‌یابی می‌رود.
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.sadd_bear),
                    contentDescription = "خرس سد",
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.size(SaddSpacing.Small))
                Text(
                    "سد",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onNavigate("diagnostics") }) {
                    Icon(Icons.Filled.Notifications, contentDescription = "اعلان‌ها و عیب‌یابی")
                }
                IconButton(onClick = { onNavigate("diagnostics") }) {
                    Icon(Icons.Filled.Settings, contentDescription = "تنظیمات")
                }
            }
        }
        Spacer(Modifier.height(SaddSpacing.Large))
        MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })

        SaddCard(modifier = Modifier.fillMaxWidth(), hero = true) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val progress = if (uiState.totalMs > 0) {
                    (uiState.remainingMs.toFloat() / uiState.totalMs.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                SessionRing(progress = progress, remainingText = uiState.remainingText, active = uiState.sessionActive)
                Spacer(Modifier.height(SaddSpacing.Small))
                Text(
                    if (uiState.sessionActive) "محافظت فعال است" else "سشنی فعال نیست",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = saddPrimaryTextColor(),
                )
                Text(
                    if (uiState.sessionActive) "زمان باقی‌مانده جلسه" else "بلاک فقط وسط سشن فعال کار می‌کند",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (uiState.sessionActive && uiState.strict) {
                    Spacer(Modifier.height(SaddSpacing.Small))
                    AssistChip(
                        onClick = {},
                        label = { Text("\u2066Strict\u2069 — سخت‌گیرانه") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = saddGlassSurfaceColor(),
                            labelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = BorderStroke(SaddGlass.BorderWidth, saddSpecularBorderColor()),
                    )
                }
                Spacer(Modifier.height(SaddSpacing.Medium))
                if (uiState.sessionActive) {
                    SaddButton(
                        text = "پایان سشن",
                        onClick = { viewModel.requestEndSession() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(SaddSpacing.Small))
                    SaddButton(
                        text = "جزئیات سشن",
                        onClick = { onNavigate("sessions") },
                        style = SaddButtonStyle.SECONDARY,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    SaddButton(
                        text = "شروع جلسه",
                        onClick = { onNavigate("sessions") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Spacer(Modifier.height(SaddSpacing.Large))
        Row(horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small)) {
            QuickTile(
                icon = Icons.Filled.PlayArrow,
                color = ChipMint,
                label = "شروع جلسه",
                onClick = { onNavigate("sessions") },
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                icon = Icons.Filled.Language,
                color = ChipPurple,
                label = "وب‌سایت‌ها",
                onClick = { onNavigate("websites") },
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                icon = Icons.Filled.Apps,
                color = ChipYellow,
                label = "اپ‌ها",
                onClick = { onNavigate("apps") },
                modifier = Modifier.weight(1f),
            )
            QuickTile(
                icon = Icons.Filled.TextFields,
                color = ChipPink,
                label = "کلمات",
                onClick = { onNavigate("rules") },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(SaddSpacing.Large))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
        ) {
            StatCard(
                label = "قوانین فعال",
                value = uiState.activeRulesCount.toString(),
                icon = Icons.Filled.Shield,
                color = ChipMint,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "بلاک امروز",
                value = uiState.blocksTodayCount.toString(),
                icon = Icons.Filled.Timer,
                color = ChipPurple,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(SaddSpacing.Small))
        // همه موارد بلاک‌شده (کلمه/اپ/وب‌سایت) در یک لیست واحد کشویی:
        // پیش‌فرض بسته است تا داشبورد جمع‌وجور بماند؛ سرتیتر با تعداد
        // کل و شورون، کل لیست را با هم باز و بسته می‌کند.
        CollapsibleBlockedItems(
            rules = uiState.activeRules,
            onToggle = { rule -> viewModel.toggleRule(rule) },
        )

        Spacer(Modifier.height(SaddSpacing.Large))
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                // کارت مجوزها به سبک Liquid Glass: چیپ آیکون سپر + متن وضعیت.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SaddIconChip(icon = Icons.Filled.Shield, color = ChipMint)
                    Spacer(Modifier.size(SaddSpacing.Small))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (uiState.missingPermissions > 0) {
                                "${uiState.missingPermissions} مجوز نیازمند توجه است"
                            } else {
                                "همه مجوزها فعال‌اند"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "بدون مجوزها، بلاک کار نمی‌کند.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(SaddSpacing.Small))
                SaddButton(
                    text = "مجوزها و عیب‌یابی",
                    onClick = { onNavigate("diagnostics") },
                    style = SaddButtonStyle.SECONDARY,
                )
            }
        }

        SectionTitle("دسترسی سریع")
        val quickItems = listOf(
            "زمان‌بندی" to "scheduling",
            "حالت بلاک" to "modes",
            "تاخیر" to "delay",
            "PIN و امنیت" to "pin",
            "بکاپ و ریستور" to "backup",
            "درباره" to "about",
        )
        for (row in quickItems.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
            ) {
                for ((label, route) in row) {
                    SaddButton(
                        text = label,
                        onClick = { onNavigate(route) },
                        style = SaddButtonStyle.SECONDARY,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(SaddSpacing.Large))
    }
}

/** کاشی دسترسی سریع به سبک Liquid Glass: چیپ آیکون رنگی + برچسب. */
@Composable
private fun QuickTile(icon: ImageVector, color: Color, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SaddCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = SaddSpacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SaddIconChip(icon = icon, color = color)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** کارت آمار به سبک Liquid Glass: چیپ آیکون رنگی + عدد و برچسب. */
@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    SaddCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaddIconChip(icon = icon, color = color)
            Spacer(Modifier.size(SaddSpacing.Small))
            Column(modifier = Modifier.weight(1f)) {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/**
 * لیست واحد و کشویی همه موارد بلاک‌شده (کلمه/اپ/وب‌سایت با هم):
 * سرتیتر «موارد بلاک‌شده (تعداد)» + شورون، با ضربه کل لیست را با
 * انیمیشن فنری باز و بسته می‌کند. پیش‌فرض بسته است؛ وضعیت باز/بسته
 * فقط در حافظه UI می‌ماند (rememberSaveable). ردیف‌ها و حالت خالی
 * دقیقاً همان قبلی‌اند — فقط نمایش عوض شده است.
 */
@Composable
private fun CollapsibleBlockedItems(
    rules: List<DashboardRule>,
    onToggle: (DashboardRule) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "blockedItemsChevron",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable { expanded = !expanded }
            .padding(top = SaddSpacing.Large, bottom = SaddSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "موارد بلاک‌شده (${rules.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = if (expanded) "بستن لیست" else "باز کردن لیست",
            modifier = Modifier.rotate(chevronRotation),
            tint = saddPrimaryTextColor(),
        )
    }
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        ) + fadeIn(animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f)),
        exit = shrinkVertically(
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        ) + fadeOut(animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (rules.isEmpty()) {
                EmptyState("هنوز قانون فعالی نیست. از کلمات، اپ‌ها یا وب‌سایت‌ها قانون اضافه کن.")
            } else {
                for (rule in rules) {
                    ActiveRuleRow(rule = rule, onToggle = { onToggle(rule) })
                }
            }
        }
    }
}

/** ردیف تاگل یک قانون فعال واقعی (کلمه / اپ / وب‌سایت) — کارت سوییچ Liquid Glass با چیپ آیکون رنگی. */
@Composable
private fun ActiveRuleRow(rule: DashboardRule, onToggle: () -> Unit) {
    val icon = when (rule.kind) {
        DashboardRuleKind.KEYWORD -> Icons.Filled.TextFields
        DashboardRuleKind.APP -> Icons.Filled.Apps
        DashboardRuleKind.WEBSITE -> Icons.Filled.Language
    }
    val iconColor = when (rule.kind) {
        DashboardRuleKind.KEYWORD -> ChipPurple
        DashboardRuleKind.APP -> ChipYellow
        DashboardRuleKind.WEBSITE -> ChipMint
    }
    SaddToggleCard(
        label = rule.label,
        description = rule.detail,
        icon = icon,
        iconColor = iconColor,
        checked = rule.enabled,
        onCheckedChange = { onToggle() },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
private fun SessionRing(progress: Float, remainingText: String, active: Boolean) {
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    // حلقه گرادیانی Liquid Glass: از primary به tertiary و برگشت (مثل موکاپ).
    val arcBrush = Brush.sweepGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.primary,
        ),
    )
    // درخشش مینت دور حلقه فقط وقتی سشن فعال است (موکاپ v2).
    val ringModifier = if (active) {
        Modifier.shadow(elevation = 24.dp, shape = CircleShape, ambientColor = Mint, spotColor = Mint)
    } else {
        Modifier
    }
    Box(
        modifier = ringModifier.size(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        // هاله نورانی پشت حلقه: گرادیان شعاعی primary که به شفاف می‌رسد.
        Box(
            modifier = Modifier
                .size(230.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (active) 0.30f else 0.12f),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )
        // پس‌زمینه قفل: وکتور قفل بزرگ و کم‌رنگ پشت حلقه — در هر دو تم
        // خواناست و متن تایمر را نمی‌پوشاند؛ حلقه و پیشرفتش دست نخورده‌اند.
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.size(150.dp).alpha(0.10f),
            tint = MaterialTheme.colorScheme.primary,
        )
        Canvas(modifier = Modifier.size(180.dp)) {
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx()),
            )
            if (active) {
                // درخشش حلقه: دو کمان پهن و کم‌آلفا زیر کمان اصلی.
                drawArc(
                    brush = arcBrush,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 30.dp.toPx()),
                    alpha = 0.16f,
                )
                drawArc(
                    brush = arcBrush,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 20.dp.toPx()),
                    alpha = 0.28f,
                )
                drawArc(
                    brush = arcBrush,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx()),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (active) remainingText else "--:--:--",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text("باقی‌مانده", style = MaterialTheme.typography.bodySmall)
        }
    }
}
