package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.core.SessionCalculator
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.system.PermissionHealthChecker
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictRuleChange
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
import com.sadd.blocker.domain.TimeFormat
import com.sadd.blocker.domain.model.BlockMode
import com.sadd.blocker.domain.model.BlockedApp
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.repository.BlockEventRepository
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import com.sadd.blocker.domain.usecase.EndSessionResult
import com.sadd.blocker.domain.usecase.EndSessionUseCase
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** نوع قانون در ردیف «قوانین فعال» داشبورد. */
enum class DashboardRuleKind {
    KEYWORD, APP, WEBSITE,
}

/** یک قانون فعال برای ردیف تاگل داشبورد — مستقیم از Room خوانده می‌شود. */
data class DashboardRule(
    val kind: DashboardRuleKind,
    /** برای کلمه/وب‌سایت: id؛ برای اپ: 0. */
    val id: Long = 0L,
    /** برای اپ: packageName؛ بقیه: خالی. */
    val packageName: String = "",
    val label: String,
    val detail: String,
    val enabled: Boolean = true,
)

data class DashboardUiState(
    val sessionActive: Boolean = false,
    val remainingText: String = "",
    val remainingMs: Long = 0L,
    val totalMs: Long = 0L,
    val strict: Boolean = false,
    val blockMode: BlockMode = BlockMode.NORMAL,
    val appsCount: Int = 0,
    val websitesCount: Int = 0,
    val keywordsCount: Int = 0,
    val activeRulesCount: Int = 0,
    /** قوانین فعال (کلمه + اپ + وب‌سایت) برای ردیف‌های تاگل داشبورد. */
    val activeRules: List<DashboardRule> = emptyList(),
    /**
     * بلاک‌های امروز: رویدادهای بلاک ثبت‌شده از نیمه‌شب امروز
     * (از جدول block_events که سرویس Accessibility می‌نویسد).
     */
    val blocksTodayCount: Int = 0,
    val missingPermissions: Int = 0,
    val message: String? = null,
    /** دیالوگ بازگشایی Strict برای پایان سشن Strict باز است. */
    val strictUnlockVisible: Boolean = false,
    /** روش بازگشایی Strict (تایپ متن / انتظار) برای دیالوگ. */
    val strictUnlockMethod: StrictUnlockMethod = StrictUnlockMethod.PASSAGE,
    /** مدت انتظار بازگشایی Strict (ساعت) برای دیالوگ. */
    val strictCooldownHours: Int = StrictUnlock.DEFAULT_COOLDOWN_HOURS,
    /** درخواست انتظار ثبت‌شده برای پایان سشن، اگر هست. */
    val strictCooldownRequest: StrictCooldownRequest? = null,
)

/**
 * ViewModel داشبورد — فاز ۲ + رفع H-1 و M-5.
 * باقی‌مانده سشن هر ثانیه از endEpochMs محاسبه می‌شود (نه تایمر در حافظه)؛
 * تعداد قوانین و سلامت مجوزها از Room و سیستم خوانده می‌شوند، نه نمایشی.
 * پایان سشن Strict فقط با بازگشایی Strict (تایپ متن / انتظار) انجام می‌شود.
 *
 * تیکر فقط وقتی صفحه دیده می‌شود می‌دود (M-5): صفحه با
 * [setTicking] آن را در onResume روشن و در onPause خاموش می‌کند.
 * تعداد قوانین با Flow تغییرمحور خوانده می‌شود، نه در هر تیک.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val appRepository: BlockedAppRepository,
    private val websiteRepository: WebsiteRepository,
    private val keywordRepository: KeywordRepository,
    private val blockEventRepository: BlockEventRepository,
    private val endSessionUseCase: EndSessionUseCase,
    private val permissionHealth: PermissionHealthChecker,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var tickCount = 0
    private var tickerJob: Job? = null
    private var latestApps: List<BlockedApp> = emptyList()
    private var latestWebsites: List<BlockedWebsite> = emptyList()
    private var latestKeywords: List<KeywordRule> = emptyList()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    /** تغییر قانونِ در انتظار بازگشایی Strict (فقط در حافظه؛ کد آن در DataStore است). */
    private var pendingStrictRuleChange: StrictRuleChange? = null

    init {
        viewModelScope.launch {
            settingsStore.blockMode.collect { mode ->
                _uiState.update { it.copy(blockMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictUnlockMethod.collect { method ->
                _uiState.update { it.copy(strictUnlockMethod = method) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictCooldownHours.collect { hours ->
                _uiState.update { it.copy(strictCooldownHours = hours) }
            }
        }
        viewModelScope.launch {
            settingsStore.strictCooldownRequest.collect { request ->
                _uiState.update { it.copy(strictCooldownRequest = request) }
            }
        }
        // تعداد قوانین: فقط موقع تغییر جدول به‌روز می‌شود، نه هر ثانیه.
        viewModelScope.launch {
            appRepository.observe().collect { list ->
                latestApps = list
                refreshRuleCounts()
            }
        }
        viewModelScope.launch {
            websiteRepository.observe().collect { list ->
                latestWebsites = list
                refreshRuleCounts()
            }
        }
        viewModelScope.launch {
            keywordRepository.observe().collect { list ->
                latestKeywords = list
                refreshRuleCounts()
            }
        }
        setTicking(true)
    }

    /** تعداد قوانین فعال + ردیف‌های تاگل را از سه لیست آخر بازسازی می‌کند. */
    private fun refreshRuleCounts() {
        val appsEnabled = latestApps.count { it.enabled }
        val websitesEnabled = latestWebsites.count { it.enabled }
        val keywordsEnabled = latestKeywords.count { it.enabled }
        val rules = buildList {
            for (rule in latestKeywords) {
                if (rule.enabled) {
                    add(
                        DashboardRule(
                            kind = DashboardRuleKind.KEYWORD,
                            id = rule.id,
                            label = rule.rawText,
                            detail = rule.category,
                        ),
                    )
                }
            }
            for (app in latestApps) {
                if (app.enabled) {
                    add(
                        DashboardRule(
                            kind = DashboardRuleKind.APP,
                            packageName = app.packageName,
                            label = app.label,
                            detail = app.packageName,
                        ),
                    )
                }
            }
            for (site in latestWebsites) {
                if (site.enabled) {
                    add(
                        DashboardRule(
                            kind = DashboardRuleKind.WEBSITE,
                            id = site.id,
                            label = site.host,
                            detail = "وب‌سایت",
                        ),
                    )
                }
            }
        }
        _uiState.update {
            it.copy(
                appsCount = appsEnabled,
                websitesCount = websitesEnabled,
                keywordsCount = keywordsEnabled,
                activeRulesCount = appsEnabled + websitesEnabled + keywordsEnabled,
                activeRules = rules,
            )
        }
    }

    /** تاگل یک قانون فعال از داشبورد — مستقیم روی همان Repository واقعی. */
    fun toggleRule(rule: DashboardRule) {
        viewModelScope.launch {
            // H-2: غیرفعال‌کردن قانون فعال در سشن Strict فقط بعد از بازگشایی Strict.
            if (rule.enabled && strictGuard.isStrictSessionActive()) {
                val scope = when (rule.kind) {
                    DashboardRuleKind.KEYWORD -> StrictRuleChange.SCOPE_KEYWORD
                    DashboardRuleKind.APP -> StrictRuleChange.SCOPE_APP
                    DashboardRuleKind.WEBSITE -> StrictRuleChange.SCOPE_WEBSITE
                }
                val id = when (rule.kind) {
                    DashboardRuleKind.APP -> rule.packageName
                    else -> rule.id.toString()
                }
                pendingStrictRuleChange = StrictRuleChange.toggle(scope, id, false)
                _uiState.update { it.copy(strictUnlockVisible = true) }
                return@launch
            }
            when (rule.kind) {
                DashboardRuleKind.KEYWORD -> keywordRepository.setEnabled(rule.id, !rule.enabled)
                DashboardRuleKind.APP -> appRepository.setEnabled(rule.packageName, !rule.enabled)
                DashboardRuleKind.WEBSITE -> websiteRepository.setEnabled(rule.id, !rule.enabled)
            }
        }
    }

    /** اعمال تغییر قانونِ بازگشایی‌شده از داشبورد (فقط scopeهای ردیف داشبورد). */
    private suspend fun applyStrictRuleChange(change: StrictRuleChange) {
        when (change.scope) {
            StrictRuleChange.SCOPE_KEYWORD -> {
                if (change.action != StrictRuleChange.ACTION_TOGGLE) return
                keywordRepository.setEnabled(
                    change.id.toLongOrNull() ?: return,
                    change.value.toBooleanStrictOrNull() ?: return,
                )
            }
            StrictRuleChange.SCOPE_APP -> {
                if (change.action != StrictRuleChange.ACTION_TOGGLE) return
                appRepository.setEnabled(change.id, change.value.toBooleanStrictOrNull() ?: return)
            }
            StrictRuleChange.SCOPE_WEBSITE -> {
                if (change.action != StrictRuleChange.ACTION_TOGGLE) return
                websiteRepository.setEnabled(
                    change.id.toLongOrNull() ?: return,
                    change.value.toBooleanStrictOrNull() ?: return,
                )
            }
        }
    }

    /** تیکر شمارش معکوس را روشن/خاموش می‌کند (M-5: فقط وقتی صفحه دیده می‌شود). */
    fun setTicking(active: Boolean) {
        tickerJob?.cancel()
        tickerJob = null
        if (!active) return
        tickerJob = viewModelScope.launch {
            while (true) {
                tickCount++
                refresh()
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            _uiState.update { it.copy(missingPermissions = permissionHealth.missingCount()) }
        }
    }

    /**
     * درخواست پایان سشن.
     * سشن غیر Strict مثل قبل مستقیم پایان می‌یابد. سشن Strict فقط با
     * بازگشایی Strict (تایپ متن / انتظار) پایان می‌یابد؛ دیالوگ بازگشایی
     * باز می‌شود و پایان واقعی در [confirmStrictUnlocked] انجام می‌شود.
     * PIN دیگر پایان سشن Strict را گیت نمی‌کند (جایگزین آن بازگشایی Strict است).
     */
    fun requestEndSession() {
        viewModelScope.launch {
            val active = sessionRepository.getActive(System.currentTimeMillis())
            if (active == null) {
                refresh()
                return@launch
            }
            if (!active.strict) {
                when (val result = endSessionUseCase.execute("user")) {
                    is EndSessionResult.Ended -> {
                        _uiState.update { it.copy(message = "سشن پایان یافت.") }
                    }
                    is EndSessionResult.NoActive -> Unit
                    else -> Unit
                }
            } else {
                _uiState.update { it.copy(strictUnlockVisible = true) }
            }
            refresh()
        }
    }

    /**
     * پایان سشن Strict یا اعمال تغییر قانون بعد از بازگشایی موفق در دیالوگ.
     * در روش انتظار، درخواست باید برای همین کار ثبت شده و کامل گذشته باشد؛
     * انتظارِ ثبت‌شده برای کاری دیگر (مثلاً تغییر روش) اینجا خرج نمی‌شود.
     * پایان سشن دوباره از مسیر واحد UseCase انجام می‌شود (H-1).
     */
    fun confirmStrictUnlocked() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val state = _uiState.value
            val change = pendingStrictRuleChange
                ?: state.strictCooldownRequest?.takeIf { it.pendingKind == StrictPendingKind.RULE_CHANGE }?.pendingRuleChange
            if (state.strictUnlockMethod == StrictUnlockMethod.COOLDOWN) {
                val request = state.strictCooldownRequest
                val kindMatches = if (change != null) {
                    request?.pendingRuleChange == change
                } else {
                    request?.pendingKind == StrictPendingKind.END_SESSION
                }
                if (request == null || !StrictUnlock.isCooldownElapsed(request, now) || !kindMatches) {
                    if (request != null && !kindMatches) {
                        pendingStrictRuleChange = null
                        _uiState.update {
                            it.copy(
                                strictUnlockVisible = false,
                                message = "این انتظار برای کار دیگری ثبت شده است؛ برای این کار دوباره درخواست بده.",
                            )
                        }
                    }
                    return@launch
                }
            }
            if (change != null) {
                pendingStrictRuleChange = null
                if (state.strictCooldownRequest?.pendingKind == StrictPendingKind.RULE_CHANGE) {
                    settingsStore.clearStrictCooldownRequest()
                }
                _uiState.update { it.copy(strictUnlockVisible = false, message = "قانون تغییر کرد.") }
                applyStrictRuleChange(change)
                refresh()
                return@launch
            }
            val active = sessionRepository.getActive(now)
            if (active == null) {
                _uiState.update {
                    it.copy(strictUnlockVisible = false, message = "سشنی فعال نیست.")
                }
                refresh()
                return@launch
            }
            when (endSessionUseCase.execute("user")) {
                is EndSessionResult.Ended -> {
                    if (state.strictCooldownRequest?.pendingKind == StrictPendingKind.END_SESSION) {
                        settingsStore.clearStrictCooldownRequest()
                    }
                    _uiState.update { it.copy(strictUnlockVisible = false, message = "سشن پایان یافت.") }
                }
                is EndSessionResult.NoActive -> {
                    _uiState.update { it.copy(strictUnlockVisible = false, message = "سشنی فعال نیست.") }
                }
            }
            refresh()
        }
    }

    /** ثبت درخواست انتظار برای پایان سشن Strict یا تغییر قانون (از دیالوگ بازگشایی). */
    fun recordStrictCooldownRequest() {
        val pending = pendingStrictRuleChange
        if (pending != null) {
            val existing = _uiState.value.strictCooldownRequest
            // درخواست تکراری همان تغییر: ساعت از اول شمرده نمی‌شود.
            if (existing?.pendingKind == StrictPendingKind.RULE_CHANGE &&
                existing.pendingRuleChange == pending
            ) return
            viewModelScope.launch {
                settingsStore.recordStrictCooldownRequest(
                    kind = StrictPendingKind.RULE_CHANGE,
                    value = StrictRuleChange.encode(pending),
                    durationHours = _uiState.value.strictCooldownHours,
                )
            }
            return
        }
        viewModelScope.launch {
            settingsStore.recordStrictCooldownRequest(
                kind = StrictPendingKind.END_SESSION,
                value = "",
                durationHours = _uiState.value.strictCooldownHours,
            )
        }
    }

    fun cancelStrictUnlock() {
        pendingStrictRuleChange = null
        _uiState.update { it.copy(strictUnlockVisible = false) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private suspend fun refresh() {
        val now = System.currentTimeMillis()
        val active = sessionRepository.getActive(now)
        val remaining = if (active != null) {
            SessionCalculator.remainingMs(active.endEpochMs, now)
        } else {
            0L
        }
        val total = if (active != null) {
            active.endEpochMs - active.startEpochMs
        } else {
            0L
        }
        // سلامت مجوزها فقط هر ۱۰ ثانیه چک می‌شود.
        val missing = if (tickCount % 10 == 1) {
            permissionHealth.missingCount()
        } else {
            _uiState.value.missingPermissions
        }
        // بلاک‌های امروز هم فقط هر ۱۰ ثانیه از رویدادهای بلاک خوانده می‌شود.
        val blocksToday = if (tickCount % 10 == 1) {
            blockEventRepository.countSinceNow(startOfTodayMs(now))
        } else {
            _uiState.value.blocksTodayCount
        }
        _uiState.update {
            it.copy(
                sessionActive = active != null && remaining > 0,
                remainingText = TimeFormat.formatRemainingMs(remaining),
                remainingMs = remaining,
                totalMs = total,
                strict = active?.strict ?: false,
                missingPermissions = missing,
                blocksTodayCount = blocksToday,
            )
        }
    }

    /** نیمه‌شب امروز به وقت دستگاه. */
    private fun startOfTodayMs(nowMs: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = nowMs
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
