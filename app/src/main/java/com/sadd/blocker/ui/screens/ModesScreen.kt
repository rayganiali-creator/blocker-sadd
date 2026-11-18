package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictRuleChange
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.ui.ModesViewModel
import com.sadd.blocker.ui.theme.SaddSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** تغییر Strict در انتظار بازگشایی (فقط در روش تایپ متن، در حافظه نگه داشته می‌شود). */
private sealed interface PendingStrictChange {
    data class Method(val method: StrictUnlockMethod) : PendingStrictChange
    data class Hours(val hours: Int) : PendingStrictChange
}

data class StrictSettingsUiState(
    val blockMode: BlockMode = BlockMode.NORMAL,
    val unlockMethod: StrictUnlockMethod = StrictUnlockMethod.PASSAGE,
    val cooldownHours: Int = StrictUnlock.DEFAULT_COOLDOWN_HOURS,
    val cooldownRequest: StrictCooldownRequest? = null,
    val strictSessionActive: Boolean = false,
    val dialogVisible: Boolean = false,
    val message: String? = null,
)

/**
 * ViewModel تنظیمات بازگشایی Strict (ModesScreen).
 *
 * قانون اصلی: تغییر روش بازگشایی یا مدت انتظار، وقتی سشن Strict فعالی هست،
 * خودش نیازمند بازگشایی با روش فعلی است؛ وگرنه (سشن Strict فعالی نیست) آزاد است.
 * - روش فعلی PASSAGE: تغییر در حافظه می‌ماند و دیالوگ تایپ متن باز می‌شود؛
 *   با تطابق کامل، تغییر اعمال می‌شود.
 * - روش فعلی COOLDOWN: درخواست با زمان دیواری در DataStore ثبت می‌شود
 *   (درخواست جدید جای قبلی را می‌گیرد و زمان از اول شمرده می‌شود) و دیالوگ
 *   شمارش معکوس را نشان می‌دهد؛ با پایان انتظار، تغییر اعمال می‌شود.
 */
@HiltViewModel
class StrictSettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StrictSettingsUiState())
    val uiState: StateFlow<StrictSettingsUiState> = _uiState.asStateFlow()
    private var pendingChange: PendingStrictChange? = null

    init {
        viewModelScope.launch {
            settingsStore.blockMode.collect { mode ->
                _uiState.update { it.copy(blockMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictUnlockMethod.collect { method ->
                _uiState.update { it.copy(unlockMethod = method) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictCooldownHours.collect { hours ->
                _uiState.update { it.copy(cooldownHours = hours) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictCooldownRequest.collect { request ->
                _uiState.update { it.copy(cooldownRequest = request) }
            }
        }
        viewModelScope.launch {
            sessionRepository.observeActive().collect { active ->
                _uiState.update { it.copy(strictSessionActive = active?.strict == true) }
            }
        }
    }

    /** درخواست تغییر روش بازگشایی. */
    fun requestUnlockMethod(method: StrictUnlockMethod) {
        val state = _uiState.value
        if (method == state.unlockMethod) return
        viewModelScope.launch {
            if (!state.strictSessionActive) {
                settingsStore.setStrictUnlockMethod(method)
                settingsStore.clearStrictCooldownRequest()
                return@launch
            }
            if (state.unlockMethod == StrictUnlockMethod.PASSAGE) {
                pendingChange = PendingStrictChange.Method(method)
                _uiState.update { it.copy(dialogVisible = true) }
            } else {
                // درخواست تکراری همان تغییر: ساعت از اول شمرده نمی‌شود.
                val existing = state.cooldownRequest
                if (existing != null && existing.pendingKind == StrictPendingKind.METHOD &&
                    existing.pendingMethod == method
                ) {
                    _uiState.update { it.copy(dialogVisible = true) }
                    return@launch
                }
                settingsStore.recordStrictCooldownRequest(
                    kind = StrictPendingKind.METHOD,
                    value = method.name,
                    durationHours = state.cooldownHours,
                )
                _uiState.update { it.copy(dialogVisible = true) }
            }
        }
    }

    /** درخواست تغییر مدت انتظار. */
    fun requestCooldownHours(hours: Int) {
        val state = _uiState.value
        if (hours == state.cooldownHours) return
        viewModelScope.launch {
            if (!state.strictSessionActive) {
                settingsStore.setStrictCooldownHours(hours)
                settingsStore.clearStrictCooldownRequest()
                return@launch
            }
            if (state.unlockMethod == StrictUnlockMethod.PASSAGE) {
                pendingChange = PendingStrictChange.Hours(hours)
                _uiState.update { it.copy(dialogVisible = true) }
            } else {
                val existing = state.cooldownRequest
                if (existing != null && existing.pendingKind == StrictPendingKind.HOURS &&
                    existing.pendingHours == hours
                ) {
                    _uiState.update { it.copy(dialogVisible = true) }
                    return@launch
                }
                settingsStore.recordStrictCooldownRequest(
                    kind = StrictPendingKind.HOURS,
                    value = hours.toString(),
                    durationHours = state.cooldownHours,
                )
                _uiState.update { it.copy(dialogVisible = true) }
            }
        }
    }

    /**
     * ثبت درخواست انتظار از دیالوگ (وقتی دیالوگ با درخواستی باز نشده باشد).
     * در جریان معمول ModesScreen درخواست موقع ضربه روی گزینه ثبت می‌شود،
     * پس این تابع بیشتر تور ایمنی است.
     */
    fun recordCooldownRequest() {
        val pending = pendingChange ?: return
        viewModelScope.launch {
            val state = _uiState.value
            when (pending) {
                is PendingStrictChange.Method -> {
                    settingsStore.recordStrictCooldownRequest(
                        kind = StrictPendingKind.METHOD,
                        value = pending.method.name,
                        durationHours = state.cooldownHours,
                    )
                }
                is PendingStrictChange.Hours -> {
                    settingsStore.recordStrictCooldownRequest(
                        kind = StrictPendingKind.HOURS,
                        value = pending.hours.toString(),
                        durationHours = state.cooldownHours,
                    )
                }
            }
        }
    }

    /** بازگشایی موفق در دیالوگ: تغییر در انتظار اعمال می‌شود. */
    fun onUnlocked() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.unlockMethod == StrictUnlockMethod.PASSAGE) {
                val pending = pendingChange
                pendingChange = null
                when (pending) {
                    is PendingStrictChange.Method -> {
                        settingsStore.setStrictUnlockMethod(pending.method)
                    }
                    is PendingStrictChange.Hours -> {
                        settingsStore.setStrictCooldownHours(pending.hours)
                    }
                    null -> Unit
                }
                _uiState.update {
                    it.copy(dialogVisible = false, message = "تنظیم Strict تغییر کرد.")
                }
            } else {
                applyRecordedRequestIfElapsed()
                _uiState.update { it.copy(dialogVisible = false) }
            }
        }
    }

    /**
     * اعمال تغییر ثبت‌شده در درخواست انتظار، اگر زمان کامل گذشته باشد.
     * درخواست پایان سشن (END_SESSION) و تغییر قانون (RULE_CHANGE) اینجا
     * اعمال نمی‌شوند؛ مال داشبورد/صفحه سشن‌ها و صفحه قانون مربوطه‌اند و
     * دست نخورده می‌مانند.
     */
    private suspend fun applyRecordedRequestIfElapsed(): Boolean {
        val request = _uiState.value.cooldownRequest ?: return false
        if (!StrictUnlock.isCooldownElapsed(request, System.currentTimeMillis())) {
            return false
        }
        when (request.pendingKind) {
            StrictPendingKind.METHOD -> {
                request.pendingMethod?.let { settingsStore.setStrictUnlockMethod(it) }
            }
            StrictPendingKind.HOURS -> {
                request.pendingHours?.let { settingsStore.setStrictCooldownHours(it) }
            }
            StrictPendingKind.END_SESSION -> return false
            StrictPendingKind.RULE_CHANGE -> return false
        }
        settingsStore.clearStrictCooldownRequest()
        return true
    }

    /** اعمال تغییرِ در انتظارِ پایان‌یافته، از کارت شمارش معکوس خود صفحه. */
    fun applyElapsedCooldownChange() {
        viewModelScope.launch {
            if (applyRecordedRequestIfElapsed()) {
                _uiState.update { it.copy(message = "تنظیم Strict تغییر کرد.") }
            }
        }
    }

    fun dismissDialog() {
        pendingChange = null
        _uiState.update { it.copy(dialogVisible = false) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

/** توضیح فارسی کاری که درخواست انتظار برای آن ثبت شده است. */
private fun strictPendingDescription(request: StrictCooldownRequest): String {
    return when (request.pendingKind) {
        StrictPendingKind.END_SESSION -> "پایان سشن Strict"
        StrictPendingKind.METHOD -> when (request.pendingMethod) {
            StrictUnlockMethod.PASSAGE -> "تغییر روش بازگشایی به «تایپ متن طولانی»"
            StrictUnlockMethod.COOLDOWN -> "تغییر روش بازگشایی به «انتظار»"
            null -> "تغییر روش بازگشایی"
        }
        StrictPendingKind.HOURS -> {
            val hours = request.pendingHours ?: request.pendingValue
            "تغییر مدت انتظار به $hours ساعت"
        }
        StrictPendingKind.RULE_CHANGE -> {
            val change = request.pendingRuleChange
            if (change != null) {
                val actionText = when (change.action) {
                    StrictRuleChange.ACTION_TOGGLE -> if (change.value == "true") "فعال‌کردن" else "غیرفعال‌کردن"
                    StrictRuleChange.ACTION_REMOVE -> "حذف"
                    StrictRuleChange.ACTION_DELAY -> "تغییر تأخیر"
                    else -> "تغییر"
                }
                "$actionText قانون (${change.id})"
            } else {
                "تغییر قانون"
            }
        }
    }
}

/**
 * حالت بلاک.
 * Normal: صفحه مداخله با فرصت تجدیدنظر و خروج کنترل‌شده.
 * Strict: وسط سشن فعال نمی‌توان قانون را غیرفعال یا حذف کرد و پایان سشن
 * و تغییر تنظیمات Strict فقط با بازگشایی Strict (تایپ متن طولانی یا انتظار)
 * انجام می‌شود. شروع سشن Strict همیشه آزاد است.
 * صادقانه: بلاک واقعی (تشخیص نیت و بلاک وب با Accessibility)
 * هنوز وصل نشده و در فاز بعدی می‌آید؛ حالت‌ها فعلاً نحوه شروع سشن
 * و قفل قوانین وسط سشن را تعیین می‌کنند.
 * محدودیت صادقانه: اپ معمولی نمی‌تواند جلوی حذف نصب یا force-stop را قطعی بگیرد.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModesScreen(
    viewModel: ModesViewModel = hiltViewModel(),
    strictViewModel: StrictSettingsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strictState by strictViewModel.uiState.collectAsStateWithLifecycle()

    // تیکر شمارش معکوس کارت «تغییر در انتظار» — فقط وقتی درخواستی هست می‌دود.
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val countdownActive = strictState.cooldownRequest != null && strictState.strictSessionActive
    LaunchedEffect(countdownActive) {
        if (!countdownActive) return@LaunchedEffect
        while (true) {
            delay(1000)
            nowMs = System.currentTimeMillis()
        }
    }

    if (strictState.dialogVisible) {
        StrictUnlockDialog(
            method = strictState.unlockMethod,
            cooldownHours = strictState.cooldownHours,
            cooldownRequest = strictState.cooldownRequest,
            onRecordCooldownRequest = { strictViewModel.recordCooldownRequest() },
            onUnlocked = { strictViewModel.onUnlocked() },
            onDismiss = { strictViewModel.dismissDialog() },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SaddSpacing.Large),
    ) {
        SaddTopBar(
            "حالت بلاک",
            "حالت سشن‌های بعدی؛ سشن فعال با حالت خودش ادامه می‌یابد.",
            onBack = onBack,
        )
        MessageBar(message = strictState.message, onDismiss = { strictViewModel.dismissMessage() })
        ModeCard(
            selected = uiState.blockMode == BlockMode.NORMAL,
            title = "Normal — عادی",
            description = "صفحه مداخله نشان داده می‌شود، تاخیر تنظیم‌شده اعمال می‌شود " +
                "و می‌توانی با دکمه بازگشت از اقدام صرف‌نظر کنی.",
            onClick = { viewModel.setBlockMode(BlockMode.NORMAL) },
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        ModeCard(
            selected = uiState.blockMode == BlockMode.STRICT,
            title = "Strict — سخت‌گیرانه",
            description = "وسط سشن فعال نمی‌توان قانون را غیرفعال یا حذف کرد و پایان سشن " +
                "فقط با بازگشایی Strict (تایپ متن طولانی یا انتظار) انجام می‌شود. " +
                "Strict جلوی حذف نصب یا force-stop را نمی‌گیرد؛ " +
                "اپ معمولی چنین قدرتی در اندروید ندارد.",
            onClick = { viewModel.setBlockMode(BlockMode.STRICT) },
        )
        if (uiState.blockMode == BlockMode.STRICT) {
            Spacer(Modifier.height(SaddSpacing.Large))
            SectionTitle("بازگشایی Strict")
            SaddCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                    Text(
                        "پایان سشن Strict و تغییر تنظیمات Strict فقط با بازگشایی انجام می‌شود. " +
                            "شروع سشن Strict همیشه آزاد است؛ قفل فقط برای پایان و تغییر است.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(SaddSpacing.Medium))
                    ModeCard(
                        selected = strictState.unlockMethod == StrictUnlockMethod.PASSAGE,
                        title = "تایپ متن طولانی",
                        description = "یک متن طولانی (حدود ۳۰۰ کاراکتر) را دقیقاً تایپ می‌کنی؛ " +
                            "سخن امامان یا جمله انگیزشی. تا کامل تایپ نشود، بازگشایی نمی‌شود.",
                        onClick = { strictViewModel.requestUnlockMethod(StrictUnlockMethod.PASSAGE) },
                    )
                    Spacer(Modifier.height(SaddSpacing.Small))
                    ModeCard(
                        selected = strictState.unlockMethod == StrictUnlockMethod.COOLDOWN,
                        title = "انتظار",
                        description = "درخواست ثبت می‌شود و باید مدت انتخاب‌شده کامل بگذرد؛ " +
                            "بستن اپ یا ری‌استارت گوشی زمان را صفر نمی‌کند.",
                        onClick = { strictViewModel.requestUnlockMethod(StrictUnlockMethod.COOLDOWN) },
                    )
                    Spacer(Modifier.height(SaddSpacing.Medium))
                    Text(
                        "مدت انتظار (در روش انتظار):",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(SaddSpacing.Small))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
                        verticalArrangement = Arrangement.spacedBy(SaddSpacing.Small),
                    ) {
                        for (hours in StrictUnlock.COOLDOWN_HOUR_OPTIONS) {
                            FilterChip(
                                selected = strictState.cooldownHours == hours,
                                onClick = { strictViewModel.requestCooldownHours(hours) },
                                label = { Text("$hours ساعت") },
                            )
                        }
                    }
                    if (!strictState.strictSessionActive) {
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            "سشن Strict فعالی نیست؛ تغییر این تنظیمات آزاد است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            val request = strictState.cooldownRequest
            if (request != null && strictState.strictSessionActive) {
                Spacer(Modifier.height(SaddSpacing.Medium))
                val remaining = StrictUnlock.cooldownRemainingMs(request, nowMs)
                SaddCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large)) {
                        Text(
                            "تغییر در انتظار",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(SaddSpacing.Small))
                        Text(
                            strictPendingDescription(request),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(SaddSpacing.Small))
                        if (remaining > 0L) {
                            Text(
                                StrictUnlock.formatCooldownRemaining(remaining),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "باقی‌مانده تا پایان انتظار",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                "زمان انتظار تمام شد.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (request.pendingKind != StrictPendingKind.END_SESSION) {
                                Spacer(Modifier.height(SaddSpacing.Medium))
                                SaddButton(
                                    text = "اعمال تغییر",
                                    onClick = { strictViewModel.applyElapsedCooldownChange() },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Spacer(Modifier.height(SaddSpacing.Small))
                                Text(
                                    "برای پایان سشن، از داشبورد یا صفحه سشن‌ها اقدام کن.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(SaddSpacing.Large))
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "مکانیزم بلاک واقعی (تشخیص نیت و بلاک وب با Accessibility) هنوز وصل نشده " +
                    "و در فاز بعدی می‌آید؛ تا آن زمان، حالت‌ها فقط شروع سشن و قفل قوانین را تعیین می‌کنند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(SaddSpacing.Large),
            )
        }
        Spacer(Modifier.height(SaddSpacing.Large))
    }
}

@Composable
private fun ModeCard(selected: Boolean, title: String, description: String, onClick: () -> Unit) {
    SaddCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.size(SaddSpacing.Small))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
