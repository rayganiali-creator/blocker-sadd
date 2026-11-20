package com.sadd.blocker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadd.blocker.core.SectionDetector
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.system.InstalledAppInfo
import com.sadd.blocker.data.system.InstalledAppsProvider
import com.sadd.blocker.domain.StrictCooldownRequest
import com.sadd.blocker.domain.StrictPendingKind
import com.sadd.blocker.domain.StrictRuleAdd
import com.sadd.blocker.domain.StrictRuleChange
import com.sadd.blocker.domain.StrictUnlock
import com.sadd.blocker.domain.StrictUnlockMethod
import com.sadd.blocker.domain.model.SectionRule
import com.sadd.blocker.domain.repository.SectionRuleRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.usecase.StrictSessionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SectionRulesUiState(
    val rules: List<SectionRule> = emptyList(),
    val installed: List<InstalledAppInfo> = emptyList(),
    val message: String? = null,
    /** دیالوگ بازگشایی Strict برای تغییر قانون محافظت‌شده باز است. */
    val strictUnlockVisible: Boolean = false,
    /** روش بازگشایی Strict (تایپ متن / انتظار) برای دیالوگ. */
    val strictUnlockMethod: StrictUnlockMethod = StrictUnlockMethod.PASSAGE,
    /** مدت انتظار بازگشایی Strict (ساعت) برای دیالوگ. */
    val strictCooldownHours: Int = StrictUnlock.DEFAULT_COOLDOWN_HOURS,
    /** درخواست انتظار ثبت‌شده (اگر هست) برای دیالوگ. */
    val strictCooldownRequest: StrictCooldownRequest? = null,
)

/**
 * ViewModel بخش‌های اپ — پیش‌فرض‌ها (۲۷ بخش: کل تلگرام/اینستاگرام/یوتیوب/
 * واتساپ/تیک‌تاک/ایکس/فیسبوک/اسنپ‌چت، جستجوی هر کدام، اکسپلور و ریلز
 * اینستاگرام، شورتس یوتیوب، وضعیت واتساپ، For You تیک‌تاک، خانه ایکس،
 * ریلز و واچ فیسبوک، اسپات‌لایت اسنپ‌چت) در اولین باز شدن seed می‌شوند؛
 * هر بخش تاگل دارد. کاربر می‌تواند هر اپ نصب‌شده دیگری را هم اضافه کند:
 * «خود اپ» (قطعی، تطابق پکیج) یا یک بخش سفارشی با متن نشانه (بهترین تلاش).
 * مثل بقیه صفحه‌های قوانین، در سشن Strict فعال نمی‌توان قانون فعال را
 * غیرفعال کرد (H-2) و نمی‌توان قانون جدید اضافه کرد — مگر بعد از بازگشایی Strict.
 * قانون‌های دستی کاربر («خود اپ» و بخش سفارشی) را می‌توان حذف کرد؛
 * حذف قانون فعال در سشن Strict هم همان گیت بازگشایی را دارد.
 */
@HiltViewModel
class SectionRulesViewModel @Inject constructor(
    private val repository: SectionRuleRepository,
    private val installedAppsProvider: InstalledAppsProvider,
    sessionRepository: SessionRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SectionRulesUiState())
    val uiState: StateFlow<SectionRulesUiState> = _uiState.asStateFlow()
    private val strictGuard = StrictSessionGuard(sessionRepository)
    private var lastRemoved: SectionRule? = null
    /** تغییر قانونِ در انتظار بازگشایی Strict (فقط در حافظه؛ کد آن در DataStore است). */
    private var pendingStrictRuleChange: StrictRuleChange? = null
    /** افزودن قانونِ در انتظار بازگشایی Strict (فقط در حافظه؛ کد آن در DataStore است). */
    private var pendingRuleAdd: StrictRuleAdd? = null

    init {
        viewModelScope.launch {
            // مهاجرت یک‌باره «پیش‌فرض‌ها خاموش» قبل از seed: نصب‌های
            // موجود هم همه پیش‌فرض‌ها را خاموش می‌گیرند (سرویس هم همین
            // مهاجرت را اجرا می‌کند؛ idempotent است).
            try {
                settingsStore.migratePresetsDefaultOffIfNeeded {
                    repository.disableAllPresets()
                }
            } catch (_: Exception) {
                // fail-open: مهاجرت خراب نمی‌کند.
            }
            repository.ensurePresets()
        }
        viewModelScope.launch {
            val installed = withContext(Dispatchers.IO) { installedAppsProvider.load() }
            _uiState.update { it.copy(installed = installed) }
        }
        viewModelScope.launch {
            repository.observe().collect { list ->
                _uiState.update { it.copy(rules = list) }
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
    }

    fun setEnabled(packageName: String, sectionKey: String, enabled: Boolean) {
        viewModelScope.launch {
            // H-2: در سشن Strict نمی‌توان قانون فعال را غیرفعال کرد —
            // مگر بعد از بازگشایی Strict (تایپ متن / انتظار).
            if (!enabled && strictGuard.isStrictSessionActive()) {
                requestStrictUnlock(
                    StrictRuleChange.toggle(
                        StrictRuleChange.SCOPE_SECTION,
                        "$packageName#$sectionKey",
                        false,
                    ),
                )
                return@launch
            }
            repository.setEnabled(packageName, sectionKey, enabled)
        }
    }

    /**
     * «خود اپ» را برای یک اپ نصب‌شده اضافه می‌کند (قطعی: باز شدن اپ
     * وسط سشن بلاک می‌شود). اگر آن اپ پیش‌فرض «خود اپ» را دارد،
     * تکراری ساخته نمی‌شود.
     */
    fun addWholeApp(packageName: String, appLabel: String) {
        viewModelScope.launch {
            if (SectionDetector.PRESETS.any { preset ->
                    preset.packageName == packageName &&
                        SectionDetector.isWholeAppSection(preset.sectionKey)
                }
            ) {
                _uiState.update { it.copy(message = "«خود $appLabel» از قبل وجود دارد؛ همان را فعال کن.") }
                return@launch
            }
            // وسط سشن Strict، افزودن قانون هم تغییر است و فقط بعد از
            // بازگشایی Strict (تایپ متن / انتظار) انجام می‌شود.
            if (strictGuard.isStrictSessionActive()) {
                pendingRuleAdd = StrictRuleAdd(StrictRuleChange.SCOPE_SECTION, packageName, appLabel)
                _uiState.update { it.copy(strictUnlockVisible = true) }
                return@launch
            }
            applyAddWholeApp(packageName, appLabel)
        }
    }

    /** افزودن «خود اپ» (مسیر آزاد و مسیر بعد از بازگشایی Strict). */
    private suspend fun applyAddWholeApp(packageName: String, appLabel: String) {
        val added = repository.addRule(
            packageName,
            SectionDetector.SECTION_CUSTOM_APP,
            "خود $appLabel",
        )
        _uiState.update {
            it.copy(
                message = if (added) "«خود $appLabel» اضافه شد." else "«خود $appLabel» از قبل وجود دارد.",
            )
        }
    }

    /**
     * بخش سفارشی برای یک اپ نصب‌شده اضافه می‌کند: نام بخش را کاربر
     * می‌نویسد و متن نشانه (مثلاً «For You») را؛ وقتی آن متن روی
     * صفحه آن اپ دیده شود، وسط سشن بلاک می‌شود. بهترین تلاش است:
     * نشانه کوتاه یا عمومی بیش‌بلاک می‌کند و نشانه‌ای که در UI آن
     * اپ نیست هرگز بلاک نمی‌کند.
     */
    fun addCustomSection(packageName: String, appLabel: String, name: String, signal: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) {
                _uiState.update { it.copy(message = "نام بخش را بنویس.") }
                return@launch
            }
            if (SectionDetector.customSectionKey(signal) == null) {
                _uiState.update { it.copy(message = "متن نشانه باید دست‌کم ۲ حرف باشد.") }
                return@launch
            }
            // وسط سشن Strict، افزودن قانون هم تغییر است و فقط بعد از
            // بازگشایی Strict (تایپ متن / انتظار) انجام می‌شود.
            // اعتبارسنجی بالا فقط خواندنی است و قبل از گیت می‌ماند تا
            // ورودی نامعتبر، هزینه بازگشایی Strict را نگیرد.
            if (strictGuard.isStrictSessionActive()) {
                pendingRuleAdd = StrictRuleAdd(
                    StrictRuleChange.SCOPE_SECTION,
                    packageName,
                    "$trimmedName|$signal",
                )
                _uiState.update { it.copy(strictUnlockVisible = true) }
                return@launch
            }
            applyAddCustomSection(packageName, appLabel, trimmedName, signal)
        }
    }

    /** افزودن بخش سفارشی (مسیر آزاد و مسیر بعد از بازگشایی Strict). */
    private suspend fun applyAddCustomSection(
        packageName: String,
        appLabel: String,
        trimmedName: String,
        signal: String,
    ) {
        val sectionKey = SectionDetector.customSectionKey(signal) ?: return
        val added = repository.addRule(packageName, sectionKey, trimmedName)
        _uiState.update {
            it.copy(
                message = if (added) {
                    "بخش «$trimmedName» برای $appLabel اضافه شد."
                } else {
                    "این بخش از قبل وجود دارد."
                },
            )
        }
    }

    /**
     * حذف یک قانون بخش دستی («خود اپ» یا بخش سفارشی)؛ پیش‌فرض‌ها از
     * UI صدا زده نمی‌شوند.
     *
     * رفتار بخش‌های باقی‌مانده بعد از حذف «خود اپ»: هیچ قانون یتیمی
     * به‌جای کل اپ بلاک نمی‌کند — سرویس (BlockAccessibilityService)
     * اول قانون «کل اپ» را چک می‌کند و اگر نباشد، هر قانون بخش را
     * جداگانه روی نشانه خودش می‌سنجد؛ پس بخش‌های همان پکیج فقط روی
     * نشانه خودشان کار می‌کنند، نه روی کل اپ.
     */
    fun removeSectionRule(packageName: String, sectionKey: String) {
        viewModelScope.launch {
            val current = _uiState.value.rules.firstOrNull {
                it.packageName == packageName && it.sectionKey == sectionKey
            }
            // H-2: در سشن Strict نمی‌توان قانون فعال را حذف کرد —
            // مگر بعد از بازگشایی Strict (تایپ متن / انتظار).
            // دقیقاً همان الگوی AppsViewModel.remove.
            if (current?.enabled == true && strictGuard.isStrictSessionActive()) {
                requestStrictUnlock(
                    StrictRuleChange.remove(
                        StrictRuleChange.SCOPE_SECTION,
                        "$packageName#$sectionKey",
                    ),
                )
                return@launch
            }
            applyRemoveSectionRule(current, packageName, sectionKey)
        }
    }

    /** حذف واقعی قانون بخش (مسیر آزاد و مسیر بعد از بازگشایی Strict). */
    private suspend fun applyRemoveSectionRule(
        rule: SectionRule?,
        packageName: String,
        sectionKey: String,
    ) {
        lastRemoved = rule
        repository.remove(packageName, sectionKey)
        // پیام باید به «حذف شد.» ختم شود تا اسنک‌بار دکمه «بازگردانی» نشان دهد.
        _uiState.update { it.copy(message = "«${rule?.labelFa ?: sectionKey}» حذف شد.") }
    }

    /** بازگردانی آخرین بخش حذف‌شده (مثل undoRemove در AppsViewModel). */
    fun undoRemove() {
        viewModelScope.launch {
            val removed = lastRemoved ?: return@launch
            lastRemoved = null
            repository.addRule(removed.packageName, removed.sectionKey, removed.labelFa)
            // addRule همیشه با enabled=true درج می‌کند؛ وضعیت قبلی را برگردان.
            if (!removed.enabled) {
                repository.setEnabled(removed.packageName, removed.sectionKey, false)
            }
            _uiState.update { it.copy(message = "«${removed.labelFa}» برگردانده شد.") }
        }
    }

    /** درخواست بازگشایی Strict برای یک تغییر قانون (دیالوگ باز می‌شود). */
    private fun requestStrictUnlock(change: StrictRuleChange) {
        pendingStrictRuleChange = change
        _uiState.update { it.copy(strictUnlockVisible = true) }
    }

    /**
     * اعمال افزودن قانون یا تغییر قانون بعد از بازگشایی موفق در دیالوگ.
     * در روش انتظار، درخواست باید برای همین کار ثبت شده و کامل گذشته باشد.
     */
    fun confirmStrictUnlocked() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val state = _uiState.value
            val add = pendingRuleAdd
                ?: state.strictCooldownRequest?.takeIf { it.pendingKind == StrictPendingKind.RULE_ADD }?.pendingRuleAdd
            if (add != null) {
                if (state.strictUnlockMethod == StrictUnlockMethod.COOLDOWN) {
                    val request = state.strictCooldownRequest
                    if (request == null || !StrictUnlock.isCooldownElapsed(request, now) ||
                        request.pendingRuleAdd != add
                    ) {
                        if (request != null && request.pendingRuleAdd != add) {
                            pendingRuleAdd = null
                            _uiState.update {
                                it.copy(
                                    strictUnlockVisible = false,
                                    message = "این انتظار برای تغییر دیگری ثبت شده است؛ برای این تغییر دوباره درخواست بده.",
                                )
                            }
                        }
                        return@launch
                    }
                }
                pendingRuleAdd = null
                _uiState.update { it.copy(strictUnlockVisible = false) }
                if (state.strictCooldownRequest?.pendingKind == StrictPendingKind.RULE_ADD) {
                    settingsStore.clearStrictCooldownRequest()
                }
                applyRuleAdd(add)
                return@launch
            }
            val change = pendingStrictRuleChange
                ?: state.strictCooldownRequest?.takeIf { it.pendingKind == StrictPendingKind.RULE_CHANGE }?.pendingRuleChange
            if (state.strictUnlockMethod == StrictUnlockMethod.COOLDOWN) {
                val request = state.strictCooldownRequest
                if (request == null || !StrictUnlock.isCooldownElapsed(request, now) ||
                    request.pendingRuleChange != change || change == null
                ) {
                    if (request != null && request.pendingRuleChange != change) {
                        pendingStrictRuleChange = null
                        _uiState.update {
                            it.copy(
                                strictUnlockVisible = false,
                                message = "این انتظار برای تغییر دیگری ثبت شده است؛ برای این تغییر دوباره درخواست بده.",
                            )
                        }
                    }
                    return@launch
                }
            }
            pendingStrictRuleChange = null
            _uiState.update { it.copy(strictUnlockVisible = false) }
            if (change == null) return@launch
            if (state.strictCooldownRequest?.pendingKind == StrictPendingKind.RULE_CHANGE) {
                settingsStore.clearStrictCooldownRequest()
            }
            applyStrictRuleChange(change)
        }
    }

    /** ثبت درخواست انتظار برای افزودن قانون یا تغییر قانون (از دیالوگ بازگشایی). */
    fun recordStrictCooldownRequest() {
        val add = pendingRuleAdd
        if (add != null) {
            val existing = _uiState.value.strictCooldownRequest
            // درخواست تکراری همان افزودن: ساعت از اول شمرده نمی‌شود.
            if (existing?.pendingKind == StrictPendingKind.RULE_ADD &&
                existing.pendingRuleAdd == add
            ) return
            viewModelScope.launch {
                settingsStore.recordStrictCooldownRequest(
                    kind = StrictPendingKind.RULE_ADD,
                    value = StrictRuleAdd.encode(add),
                    durationHours = _uiState.value.strictCooldownHours,
                )
            }
            return
        }
        val pending = pendingStrictRuleChange ?: return
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
    }

    fun cancelStrictUnlock() {
        pendingStrictRuleChange = null
        pendingRuleAdd = null
        _uiState.update { it.copy(strictUnlockVisible = false) }
    }

    /**
     * افزودن قانونِ بازگشایی‌شده (فقط scope همین صفحه).
     * «خود اپ» با value بدون «|» (همان برچسب اپ) و بخش سفارشی با
     * value به‌صورت «نام|نشانه» از هم جدا می‌شوند؛ برچسب اپ برای
     * بخش سفارشی موقع اعمال، از لیست اپ‌های نصب‌شده بازسازی می‌شود.
     */
    private suspend fun applyRuleAdd(add: StrictRuleAdd) {
        if (add.scope != StrictRuleChange.SCOPE_SECTION) return
        val separator = add.value.indexOf('|')
        if (separator < 0) {
            applyAddWholeApp(add.id, add.value)
        } else {
            val name = add.value.substring(0, separator)
            val signal = add.value.substring(separator + 1)
            val appLabel = _uiState.value.installed
                .firstOrNull { it.packageName == add.id }?.label ?: add.id
            applyAddCustomSection(add.id, appLabel, name, signal)
        }
    }

    /** اعمال تغییر قانونِ بازگشایی‌شده (فقط scope همین صفحه). */
    private suspend fun applyStrictRuleChange(change: StrictRuleChange) {
        if (change.scope != StrictRuleChange.SCOPE_SECTION) return
        val parts = change.id.split("#")
        if (parts.size != 2) return
        when (change.action) {
            StrictRuleChange.ACTION_TOGGLE -> {
                repository.setEnabled(parts[0], parts[1], change.value.toBooleanStrictOrNull() ?: return)
            }
            StrictRuleChange.ACTION_REMOVE -> {
                val current = _uiState.value.rules.firstOrNull {
                    it.packageName == parts[0] && it.sectionKey == parts[1]
                }
                applyRemoveSectionRule(current, parts[0], parts[1])
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
