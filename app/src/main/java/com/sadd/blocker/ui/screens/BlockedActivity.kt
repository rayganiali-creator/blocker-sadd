package com.sadd.blocker.ui.screens

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadd.blocker.R
import com.sadd.blocker.core.ProtectionGuardDetector
import com.sadd.blocker.data.datastore.AppTheme
import com.sadd.blocker.data.datastore.ColorTheme
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
import com.sadd.blocker.ui.games.GameHost
import com.sadd.blocker.ui.games.GameInfo
import com.sadd.blocker.ui.games.GamesGrid
import com.sadd.blocker.ui.theme.BlockerTheme
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.Mint
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.saddHairlineColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * صفحه بلاک — همیشه تیره سرمه‌ای، خرس «سد» به‌عنوان ماسکوت (حفظ هویت نسخه قبلی).
 * طبق موکاپ تاییدشده: خرس، دلیل بلاک، دسته و فقط دکمه «بازگشت».
 * شمارش معکوس تاخیر و منطق مداخله در فاز ۴ وصل می‌شود؛
 * هرگز HOME یا قفل گوشی فراخوانی نمی‌شود — فقط بازگشت (fail-open).
 *
 * حالت نگهبان (extra guardUnlock=true): به‌جای صفحه بلاک، دیالوگ بازگشایی
 * Strict نشان داده می‌شود تا کاربر با تایپ متن طولانی یا انتظار کامل،
 * پنجره ۵دقیقه‌ای بازگشایی محافظت را باز کند و بعد از تنظیمات ادامه بدهد.
 * بازگشایی موفق پنجره را تنظیم می‌کند، درخواست انتظار نگهبان را پاک می‌کند
 * و صفحه را می‌بندد؛ لغو هم صفحه را می‌بندد (محافظت روشن می‌ماند).
 */
class BlockedActivity : ComponentActivity() {
    // M-8: با launchMode=singleTop، بازفرستادن intent باید دلیل بلاک را به‌روز کند.
    private val reasonState = mutableStateOf("قانون فعال")
    private val categoryState = mutableStateOf<String?>(null)
    private val delayState = mutableStateOf<Int?>(null)
    private val guardUnlockState = mutableStateOf(false)
    private val guardMethodState = mutableStateOf(StrictUnlockMethod.PASSAGE)
    private val guardHoursState = mutableStateOf(StrictUnlock.DEFAULT_COOLDOWN_HOURS)
    private val guardRequestState = mutableStateOf<StrictCooldownRequest?>(null)
    /** رنگ‌بندی انتخاب‌شده؛ صفحه بلاک همیشه تیره است، ولی با پالت انتخاب‌شده. */
    private val colorThemeState = mutableStateOf(ColorTheme.SADD)

    // Hilt ندارد؛ SettingsStore مستقیم ساخته می‌شود.
    private val settingsStore by lazy { SettingsStore(applicationContext) }

    // اسکوپ خود اکتیویتی — lifecycle-runtime-ktx وابستگی پروژه نیست،
    // پس lifecycleScope در دسترس نیست؛ در onDestroy لغو می‌شود.
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyIntent(intent)
        activityScope.launch {
            try {
                colorThemeState.value = settingsStore.colorTheme.first()
            } catch (_: Exception) {
                // fail-open: پالت پیش‌فرض «سد» می‌ماند.
            }
        }
        setContent {
            BlockerTheme(theme = AppTheme.DARK, colorTheme = colorThemeState.value) {
                if (guardUnlockState.value) {
                    StrictUnlockDialog(
                        method = guardMethodState.value,
                        cooldownHours = guardHoursState.value,
                        cooldownRequest = guardRequestState.value,
                        onRecordCooldownRequest = {
                            activityScope.launch {
                                settingsStore.recordStrictCooldownRequest(
                                    kind = StrictPendingKind.GUARD_UNLOCK,
                                    value = "",
                                    durationHours = guardHoursState.value,
                                )
                                refreshGuardState()
                            }
                        },
                        onUnlocked = {
                            activityScope.launch {
                                val now = System.currentTimeMillis()
                                settingsStore.setProtectionUnlockUntil(
                                    now + ProtectionGuardDetector.PROTECTION_UNLOCK_WINDOW_MS,
                                )
                                // فقط درخواست نگهبان پاک می‌شود؛ درخواست دیگری
                                // (مثلاً پایان سشن) دست نخورده می‌ماند.
                                val request = settingsStore.strictCooldownRequest.first()
                                if (request?.pendingKind == StrictPendingKind.GUARD_UNLOCK) {
                                    settingsStore.clearStrictCooldownRequest()
                                }
                                Toast.makeText(
                                    this@BlockedActivity,
                                    "۵ دقیقه فرصت داری — حالا می‌توانی ادامه بدهی",
                                    Toast.LENGTH_LONG,
                                ).show()
                                finish()
                            }
                        },
                        onDismiss = { finish() },
                    )
                } else {
                    BlockedContent(
                        reason = reasonState.value,
                        category = categoryState.value,
                        delaySeconds = delayState.value,
                        onBack = { finish() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntent(intent)
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun applyIntent(intent: Intent?) {
        reasonState.value = intent?.getStringExtra("reason") ?: "قانون فعال"
        categoryState.value = intent?.getStringExtra("category")
        val delay = intent?.getIntExtra("delaySeconds", -1) ?: -1
        delayState.value = if (delay >= 0) delay else null
        guardUnlockState.value = intent?.getBooleanExtra("guardUnlock", false) ?: false
        if (guardUnlockState.value) refreshGuardState()
    }

    /**
     * خواندن روش/مدت بازگشایی و درخواست انتظار نگهبان از DataStore.
     * فقط درخواست GUARD_UNLOCK نشان داده می‌شود؛ درخواست دیگری (مثلاً
     * پایان سشن) اینجا null حساب می‌شود تا شمارش معکوس اشتباهی نیاید.
     */
    private fun refreshGuardState() {
        activityScope.launch {
            try {
                guardMethodState.value = settingsStore.strictUnlockMethod.first()
                guardHoursState.value = settingsStore.strictCooldownHours.first()
                guardRequestState.value = settingsStore.strictCooldownRequest.first()
                    ?.takeIf { it.pendingKind == StrictPendingKind.GUARD_UNLOCK }
            } catch (_: Exception) {
                // fail-open: پیش‌فرض‌ها می‌مانند و دیالوگ با آن‌ها باز می‌شود.
            }
        }
    }
}

@Composable
fun BlockedContent(
    reason: String,
    category: String? = null,
    delaySeconds: Int? = null,
    onBack: () -> Unit,
) {
    // بازی انتخاب‌شده از بخش «وقتی موج میل می‌آید»؛ بستن بازی فقط به همین
    // صفحه برمی‌گردد و هرگز اکتیویتی را نمی‌بندد (بازگشت به اپ بلاک‌شده ممکن نیست).
    var selectedGame by remember { mutableStateOf<GameInfo?>(null) }
    val openGame = selectedGame
    if (openGame != null) {
        GameHost(game = openGame, onClose = { selectedGame = null })
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.sadd_bear),
            contentDescription = "خرس سد",
            modifier = Modifier.size(160.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "سد",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Mint,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "این محتوا بلاک شد",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        // کارت Detox دلیل بلاک: دلیل، دسته و تاخیر داخل یک کارت hero.
        SaddCard(modifier = Modifier.fillMaxWidth(), hero = true) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                if (category != null) {
                    Spacer(Modifier.height(12.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("دسته: $category") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        border = BorderStroke(SaddGlass.BorderWidth, saddHairlineColor()),
                    )
                }
                if (delaySeconds != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "تاخیر مداخله: $delaySeconds ثانیه",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "یک لحظه مکث کن — این انتخاب آگاهانه توست، نه یک اتفاق.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        SaddButton(
            text = "بازگشت",
            onClick = onBack,
            modifier = Modifier.padding(top = 24.dp),
        )
        // بخش بازی‌های کوچک: فقط داخل خود «سد» و فقط در محتوای عادی بلاک
        // (حالت نگهبان این صفحه را اصلاً نشان نمی‌دهد). بازی‌ها بلاک را
        // ضعیف نمی‌کنند؛ بستن هر بازی به همین صفحه برمی‌گردد.
        Spacer(Modifier.height(SaddSpacing.XLarge))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaddIconChip(icon = Icons.Filled.PlayArrow, color = ChipMint)
            Spacer(Modifier.width(SaddSpacing.Medium))
            Text(
                "وقتی موج میل می‌آید",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(SaddSpacing.Medium))
        GamesGrid(onGameClick = { selectedGame = it })
        Spacer(Modifier.height(SaddSpacing.XLarge))
    }
}
