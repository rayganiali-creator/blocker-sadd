package com.sadd.blocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sadd.blocker.core.KeywordMatcher
import com.sadd.blocker.core.ProtectionGuardDetector
import com.sadd.blocker.core.SectionDetector
import com.sadd.blocker.core.TextRedactor
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.system.UninstallProtection
import com.sadd.blocker.domain.UninstallProtectionGate
import com.sadd.blocker.domain.model.BlockEventKind
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.model.SectionRule
import com.sadd.blocker.domain.repository.BlockEventRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.SectionRuleRepository
import com.sadd.blocker.domain.repository.SessionRepository
import com.sadd.blocker.domain.repository.WebsiteRepository
import com.sadd.blocker.ui.screens.BlockedActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * موتور بلاک بدون VPN — AccessibilityService.
 *
 * سه مسیر بلاک، همه فقط وسط سشن فعال:
 * 1. تایپ کلمه ممنوعه: روی TYPE_VIEW_TEXT_CHANGED متن فیلد قابل‌ویرایش
 *    خوانده می‌شود؛ اگر کلمه‌ای whole-word مچ شود، بعد از تاخیر قانون،
 *    همان کلمه از فیلد حذف می‌شود (ACTION_SET_TEXT؛ اگر نشد backspace)
 *    و صفحه بلاک نشان داده می‌شود.
 * 2. بخش‌های اپ: روی تغییر پنجره، اگر بخش فعالِ قانون (کل تلگرام/
 *    اینستاگرام/یوتیوب/واتساپ/تیک‌تاک/ایکس/فیسبوک/اسنپ‌چت، جستجوی
 *    هر کدام، اکسپلور و ریلز اینستاگرام، شورتس یوتیوب، وضعیت واتساپ،
 *    For You تیک‌تاک، خانه ایکس، ریلز و واچ فیسبوک، اسپات‌لایت
 *    اسنپ‌چت، یا «خود اپ»/بخش سفارشی هر اپ نصب‌شده) روی صفحه باشد،
 *    بعد از تاخیر سراسری BACK زده می‌شود و صفحه بلاک می‌آید.
 *    «کل اپ» بقیه بخش‌های همان اپ را دربر می‌گیرد: اگر مچ شد،
 *    بخش‌های دیگر آن اپ همان دور رد می‌شوند تا دو BACK پشت سر هم
 *    زمان‌بندی نشود.
 * 3. مرورگر (کروم، فایرفاکس، سامسونگ اینترنت، اج، Brave): متن
 *    omnibox/جعبه جستجو خوانده می‌شود؛ دامنه بلاک‌شده یا کلمه
 *    ممنوعه → بعد از تاخیر BACK + صفحه بلاک. حالت Incognito نیازی
 *    به تشخیص جدا ندارد — متن نوار آدرس در تب خصوصی هم از همان
 *    مسیر خوانده می‌شود.
 *
 * اصول سفت (از نسخه ۲٫۰٫۱۰):
 * - همه‌چیز داخل try/catch؛ خطا هرگز گوشی را قفل نمی‌کند (fail-open).
 * - فقط متن فیلد ورودی خوانده می‌شود؛ محتوای صفحه (فید/مقاله/چت) برای
 *   بلاک استفاده نمی‌شود — لیست و فید به‌تنهایی هرگز مچ نمی‌سازند.
 * - متن خوانده‌شده هرگز لاگ نمی‌شود و به سرور نمی‌رود؛ فقط نوع بلاک و
 *   قانون منطبق (بدون متن کاربر) در block_events ثبت می‌شود.
 * - رویدادهای اپ خود «سد» نادیده گرفته می‌شوند — هم package رویداد و هم
 *   package پنجره فعال چک می‌شود، چون رویداد تایپ ممکن است از پکیج
 *   کیبورد (IME) بیاید در حالی که متن مال اپ پیش‌زمینه است.
 * - هرگز HOME یا قفل گوشی فراخوانی نمی‌شود؛ فقط BACK و صفحه بلاک.
 *
 * محدودیت صادقانه مرورگر (بدون VPN): هیچ مرورگری موظف نیست متن
 * نوار آدرس را از طریق Accessibility در اختیار بگذارد؛ در نسخه‌ها یا
 * مرورگرهایی که آن را نمی‌دهند، بلاک دامنه کار نمی‌کند. کروم مسیر
 * تأییدشده است؛ فایرفاکس، سامسونگ اینترنت، اج و Brave به همان روش
 * خوانده می‌شوند ولی بهترین تلاش‌اند. این جایگزین قطعی ندارد مگر
 * VPN که سید نخواست.
 *
 * حالت Strict: از سشن فعال و SettingsStore خوانده و در رویداد بلاک
 * ثبت می‌شود. معیار تطابق و تاخیر در هر دو حالت یکی است — Strict
 * بودنش در لایه سشن/قانون اعمال می‌شود (قانون فعال وسط سشن Strict
 * غیرفعال/حذف نمی‌شود؛ پایان سشن Strict PIN می‌خواهد) — نه با عوض
 * کردن معیار مچ، تا رفتار موتور قابل پیش‌بینی بماند.
 *
 * نگهبان حذف نصب / توقف اجباری / غیرفعال‌سازی مدیر دستگاه
 * (ProtectionGuardDetector + GuardOverlay): مستقل از سشن فعال است و وقتی
 * مسلح است کار می‌کند — مسلح یعنی تنظیم محافظت روشن باشد یا مدیر دستگاه
 * واقعاً فعال باشد (فعال‌سازی دستی از تنظیمات سیستم تنظیم داخل اپ را
 * عوض نمی‌کند). روی تغییر پنجره و تغییر محتوای پنجره، صفحه اطلاعات برنامه
 * «سد» در تنظیمات، دیالوگ حذف نصب/توقف اجباری، فهرست «Device admin apps»
 * یا دیالوگ غیرفعال‌کردن مدیر دستگاه را از روی متن صفحه تشخیص می‌دهد؛
 * همان لحظه پرده رهگیری (TYPE_ACCESSIBILITY_OVERLAY) را نشان می‌دهد تا
 * ضربه سریع روی دکمه زیرین ثبت نشود، BACK می‌زند و صفحه بازگشایی Strict
 * را باز می‌کند. محدودیت صادقانه: فقط وقتی Accessibility روشن است کار
 * می‌کند؛ صفحه غیرفعال‌سازی مدیر دستگاه یا خودِ سرویس Accessibility را
 * نمی‌توان مسدود کرد — فقط رهگیری مسیر رسیدن به آن‌ها ممکن است، و اگر
 * سرویس خاموش شود نگهبان هم خاموش می‌شود.
 *
 * توجه: فایل res/xml/accessibility_service_config.xml باید
 * typeWindowContentChanged را هم شامل شود تا تغییر محتوای پنجره
 * (بدون عوض شدن پنجره) به سرویس برسد؛ بدون آن، تشخیص بخش فقط روی
 * TYPE_WINDOW_STATE_CHANGED و TYPE_VIEW_TEXT_CHANGED کار می‌کند.
 */
@AndroidEntryPoint
class BlockAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var keywordRepository: KeywordRepository

    @Inject
    lateinit var websiteRepository: WebsiteRepository

    @Inject
    lateinit var sectionRuleRepository: SectionRuleRepository

    @Inject
    lateinit var blockEventRepository: BlockEventRepository

    @Inject
    lateinit var settingsStore: SettingsStore

    @Inject
    lateinit var uninstallProtection: UninstallProtection

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** پرده رهگیری نگهبان — ضربه سریع روی «حذف نصب»/«تایید» را می‌خورد. */
    private val guardOverlay by lazy { GuardOverlay(this) }

    /** کش وضعیت زنده مدیر دستگاه برای مسلح بودن نگهبان (۵ ثانیه). */
    @Volatile
    private var adminActiveCache: Boolean? = null

    @Volatile
    private var adminActiveCacheAtMs: Long = 0L

    /** عکس لحظه‌ای قوانین + وضعیت سشن؛ حداکثر ۵ ثانیه کش می‌شود. */
    private data class Snapshot(
        val sessionActive: Boolean,
        val strict: Boolean,
        val globalDelaySeconds: Int,
        val keywords: List<KeywordRule>,
        val websites: List<BlockedWebsite>,
        val sections: List<SectionRule>,
    )

    @Volatile
    private var snapshotCache: Snapshot? = null

    @Volatile
    private var snapshotAtMs: Long = 0L

    /** ضد حلقه: کلید بلاک → آخرین زمان اجرا (میلی‌ثانیه). */
    private val cooldownUntil = ConcurrentHashMap<String, Long>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return
            val type = event.eventType
            if (type != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                type != AccessibilityEvent.TYPE_VIEW_CLICKED
            ) {
                return
            }
            val rootForPackage = activeRootOrNull()
            // package پنجره فعال معتبر است، نه package رویداد (ممکن است IME باشد).
            val foregroundPkg = try {
                rootForPackage?.packageName?.toString()
            } catch (_: Exception) {
                null
            } ?: event.packageName?.toString() ?: return
            try {
                rootForPackage?.recycle()
            } catch (_: Exception) {
                // نادیده
            }
            if (TextRedactor.isOwnPackage(foregroundPkg, packageName)) return
            if (type == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                val text = readEventText(event)
                if (text.isBlank()) return
                scope.launch {
                    try {
                        evaluateText(foregroundPkg, text)
                    } catch (_: Exception) {
                        // fail-open
                    }
                }
            } else {
                val windowClass = event.className?.toString()
                scope.launch {
                    try {
                        // نگهبان حذف نصب/توقف اجباری: قبل از چک سشن —
                        // مستقل از سشن فعال است و فقط به «محافظت روشن» وابسته است.
                        evaluateGuard(foregroundPkg, windowClass)
                    } catch (_: Exception) {
                        // fail-open
                    }
                    try {
                        // ریشه تازه خوانده می‌شود؛ گره زمان رویداد تا اینجا کهنه شده است.
                        evaluateWindow(foregroundPkg, windowClass)
                    } catch (_: Exception) {
                        // fail-open
                    }
                }
            }
        } catch (_: Exception) {
            // fail-open: خطا در تشخیص = عدم بلاک.
        }
    }

    override fun onInterrupt() {
        // وقفه سیستمی؛ سشن از Room بازسازی می‌شود، نه از حافظه.
    }

    override fun onDestroy() {
        try {
            guardOverlay.hide()
        } catch (_: Exception) {
            // نادیده
        }
        scope.cancel()
        super.onDestroy()
    }

    // ---------- خواندن رویداد ----------

    private fun activeRootOrNull(): AccessibilityNodeInfo? {
        return try {
            rootInActiveWindow
        } catch (_: Exception) {
            null
        }
    }

    /** متن رویداد تایپ: اول از گره منبع، وگرنه از خود رویداد. گره بازیافت می‌شود. */
    private fun readEventText(event: AccessibilityEvent): String {
        val source = try {
            event.source
        } catch (_: Exception) {
            null
        }
        if (source != null) {
            try {
                val text = source.text?.toString()
                if (!text.isNullOrBlank()) return text
            } catch (_: Exception) {
                // ادامه با متن رویداد
            } finally {
                try {
                    source.recycle()
                } catch (_: Exception) {
                    // نادیده
                }
            }
        }
        return try {
            event.text.joinToString(" ") { it.toString() }
        } catch (_: Exception) {
            ""
        }
    }

    // ---------- عکس لحظه‌ای ----------

    private suspend fun loadSnapshot(forceRefresh: Boolean): Snapshot {
        val now = System.currentTimeMillis()
        val cached = snapshotCache
        if (!forceRefresh && cached != null && now - snapshotAtMs < SNAPSHOT_TTL_MS) {
            return cached
        }
        val fresh = try {
            val session = sessionRepository.getActive(now)
            val globalDelay = settingsStore.globalDelaySeconds.first()
            Snapshot(
                sessionActive = session != null,
                // سخت‌گیرانه بودن فقط از خود سشن فعال می‌آید — حالتی که
                // موقع شروع سشن انتخاب شده؛ حالت ذخیره‌شده فقط پیش‌فرضِ
                // پیشنهادی در دیالوگ شروع است و روی سشنِ در حال اجرا
                // اثری ندارد.
                strict = session?.strict == true,
                globalDelaySeconds = globalDelay,
                keywords = keywordRepository.list().filter { it.enabled },
                websites = websiteRepository.list().filter { it.enabled },
                sections = sectionRuleRepository.getEnabled(),
            )
        } catch (_: Exception) {
            // fail-open: اگر قوانین خوانده نشدند، هیچ بلاکی صادر نمی‌شود.
            Snapshot(
                sessionActive = false,
                strict = false,
                globalDelaySeconds = 5,
                keywords = emptyList(),
                websites = emptyList(),
                sections = emptyList(),
            )
        }
        snapshotCache = fresh
        snapshotAtMs = now
        return fresh
    }

    // ---------- ارزیابی متن تایپ‌شده ----------

    private suspend fun evaluateText(packageName: String, text: String) {
        val snapshot = loadSnapshot(forceRefresh = false)
        if (!snapshot.sessionActive) return
        if (SectionDetector.isBrowserPackage(packageName)) {
            evaluateBrowserText(packageName, text, snapshot)
            return
        }
        val matched = TextRedactor.findMatchedKeyword(
            text,
            snapshot.keywords.map { it.rawText },
        ) ?: return
        val rule = snapshot.keywords.firstOrNull { it.rawText == matched } ?: return
        val delaySeconds = TextRedactor.resolveDelay(rule.delaySeconds, snapshot.globalDelaySeconds)
        scheduleBlock(
            key = "keyword:$packageName:${rule.id}",
            delaySeconds = delaySeconds,
        ) {
            actOnKeyword(packageName, rule, delaySeconds, snapshot.strict)
        }
    }

    /** متن omnibox/جعبه جستجوی مرورگر: اول دامنه، بعد کلمه. */
    private suspend fun evaluateBrowserText(
        packageName: String,
        text: String,
        snapshot: Snapshot,
    ) {
        val host = TextRedactor.extractHostCandidate(text)
        if (host != null) {
            val domainRules = snapshot.websites.map {
                TextRedactor.DomainRule(it.host, it.matchSubdomains)
            }
            val matchedDomain = TextRedactor.findMatchedDomain(host, domainRules)
            if (matchedDomain != null) {
                val site = snapshot.websites.firstOrNull { it.host == matchedDomain.host }
                val delaySeconds = TextRedactor.resolveDelay(
                    site?.delaySeconds,
                    snapshot.globalDelaySeconds,
                )
                scheduleBlock(
                    key = "website:$packageName:${site?.id}",
                    delaySeconds = delaySeconds,
                ) {
                    actOnBrowser(packageName, "سایت «${matchedDomain.host}» بلاک شد", delaySeconds, snapshot.strict)
                }
                return
            }
        }
        val matched = TextRedactor.findMatchedKeyword(
            text,
            snapshot.keywords.map { it.rawText },
        ) ?: return
        val rule = snapshot.keywords.firstOrNull { it.rawText == matched } ?: return
        val delaySeconds = TextRedactor.resolveDelay(rule.delaySeconds, snapshot.globalDelaySeconds)
        scheduleBlock(
            key = "keyword:$packageName:${rule.id}",
            delaySeconds = delaySeconds,
        ) {
            actOnKeyword(packageName, rule, delaySeconds, snapshot.strict)
        }
    }

    // ---------- نگهبان حذف نصب / توقف اجباری ----------

    /**
     * نگهبان: مستقل از سشن فعال؛ وقتی مسلح است (تنظیم محافظت روشن باشد
     * یا مدیر دستگاه واقعاً فعال باشد — فعال‌سازی دستی از تنظیمات سیستم
     * تنظیم داخل اپ را عوض نمی‌کند، پس فقط به تنظیم تکیه کردن یعنی
     * نگهبان در وضعیت واقعی کاربر بی‌صدا خاموش می‌ماند) و پنجره بازگشایی
     * ۵دقیقه‌ای باز نیست. روی تغییر پنجره و تغییر محتوای پنجره، صفحه
     * اطلاعات برنامه «سد» در تنظیمات، دیالوگ حذف نصب/توقف اجباری، فهرست
     * «Device admin apps» یا دیالوگ غیرفعال‌کردن مدیر دستگاه را از روی
     * متن صفحه تشخیص می‌دهد؛ همان لحظه پرده رهگیری را نشان می‌دهد تا
     * ضربه سریع روی دکمه زیرین ثبت نشود، BACK می‌زند و صفحه بازگشایی
     * Strict را باز می‌کند. اگر صفحه محافظت‌شده دیگر جلوی چشم نباشد
     * (مثلاً کاربر با بازگشت بیرون رفته باشد)، پرده برداشته می‌شود.
     * متن خود اپ هرگز نگهبان را فعال نمی‌کند (هم چک پکیج پیش‌زمینه در
     * onAccessibilityEvent و هم چک داخل detector). ضد حلقه ۱۵ ثانیه‌ای
     * فقط برای BACK و صفحه بازگشایی است؛ پرده خودش idempotent است و با
     * هر رویداد دوباره نشان داده نمی‌شود.
     */
    private suspend fun evaluateGuard(packageName: String, windowClassName: String?) {
        try {
            if (!isGuardArmed()) {
                guardOverlay.hide()
                return
            }
            val now = System.currentTimeMillis()
            val unlockUntil = settingsStore.protectionUnlockUntilMs.first()
            if (ProtectionGuardDetector.isGuardUnlockWindowActive(now, unlockUntil)) {
                guardOverlay.hide()
                return
            }
            // پیش‌فیلتر ارزان: فقط پکیج‌های تنظیمات/نصب‌کننده/لانچر درخت
            // را می‌خوانند — تغییر محتوای بقیه اپ‌ها نگهبان را بیدار نمی‌کند.
            if (!ProtectionGuardDetector.isGuardedPackage(packageName)) {
                guardOverlay.hide()
                return
            }
            val root = activeRootOrNull() ?: return
            val signals = collectSignals(root, windowClassName)
            val hit = ProtectionGuardDetector.detect(
                ProtectionGuardDetector.GuardSignals(
                    packageName = packageName,
                    texts = signals.texts,
                    contentDescriptions = signals.contentDescriptions,
                    windowClassName = windowClassName,
                ),
                ownPackageName = this.packageName,
            )
            if (hit == null) {
                guardOverlay.hide()
                return
            }
            val reason = guardReason(hit)
            guardOverlay.show(hit, reason) { showGuardUnlock(hit) }
            val key = "guard:$hit"
            val last = cooldownUntil[key] ?: 0L
            if (now - last < COOLDOWN_MS) return
            cooldownUntil[key] = now
            logBlock(
                BlockEventKind.GUARD_BLOCKED,
                "guard:$hit pkg=$packageName",
            )
            performGlobalAction(GLOBAL_ACTION_BACK)
            showGuardUnlock(hit)
        } catch (_: Exception) {
            // fail-open: خطا در نگهبان = عدم مداخله.
        }
    }

    /** نگهبان مسلح است؟ تنظیم روشن، یا مدیر دستگاه واقعاً فعال. */
    private suspend fun isGuardArmed(): Boolean {
        return try {
            val enabled = settingsStore.uninstallProtectionEnabled.first()
            if (enabled) return true
            val now = System.currentTimeMillis()
            val cached = adminActiveCache
            if (cached != null && now - adminActiveCacheAtMs < SNAPSHOT_TTL_MS) return cached
            val active = runCatching { uninstallProtection.isAdminActive() }.getOrDefault(false)
            adminActiveCache = active
            adminActiveCacheAtMs = now
            UninstallProtectionGate.isGuardArmed(enabled, active)
        } catch (_: Exception) {
            false
        }
    }

    private fun guardReason(hit: ProtectionGuardDetector.GuardHit): String {
        return when (hit) {
            ProtectionGuardDetector.GuardHit.APP_INFO ->
                "برای ادامه، اول محافظت را بازگشایی کن"
            ProtectionGuardDetector.GuardHit.UNINSTALL_DIALOG ->
                "حذف نصب «سد» نیازمند بازگشایی محافظت است"
            ProtectionGuardDetector.GuardHit.FORCE_STOP_DIALOG ->
                "توقف اجباری «سد» نیازمند بازگشایی محافظت است"
            ProtectionGuardDetector.GuardHit.DEVICE_ADMIN_LIST ->
                "فهرست مدیر دستگاه نیازمند بازگشایی محافظت است"
            ProtectionGuardDetector.GuardHit.DEACTIVATE_ADMIN_DIALOG ->
                "غیرفعال‌کردن مدیر دستگاه نیازمند بازگشایی محافظت است"
        }
    }

    /** صفحه بازگشایی Strict برای نگهبان (BlockedActivity با extra guardUnlock). */
    private fun showGuardUnlock(hit: ProtectionGuardDetector.GuardHit) {
        try {
            val reason = guardReason(hit)
            val intent = Intent(this, BlockedActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("reason", reason)
            intent.putExtra("guardUnlock", true)
            startActivity(intent)
        } catch (_: Exception) {
            // fail-open: اگر صفحه بازگشایی باز نشد، گوشی قفل نمی‌شود.
        }
    }

    // ---------- ارزیابی پنجره (بخش‌ها + مرورگر) ----------

    private suspend fun evaluateWindow(
        packageName: String,
        windowClassName: String?,
    ) {
        val snapshot = loadSnapshot(forceRefresh = false)
        if (!snapshot.sessionActive) return
        val root = activeRootOrNull() ?: return
        val signals = collectSignals(root, windowClassName)
        if (SectionDetector.isBrowserPackage(packageName)) {
            evaluateBrowserWindow(packageName, signals, snapshot)
            return
        }
        // «کل اپ» بقیه بخش‌های همان اپ را دربر می‌گیرد: اگر مچ شد، فقط
        // همان یک بلاک زمان‌بندی می‌شود (یک BACK، نه چند BACK پشت سر هم).
        // جدا از ترتیب لیست قوانین چک می‌شود تا وابسته به ORDER BY نباشد.
        val wholeAppRule = snapshot.sections.firstOrNull { rule ->
            rule.packageName == packageName &&
                SectionDetector.isWholeAppSection(rule.sectionKey) &&
                SectionDetector.isSectionShowing(packageName, rule.sectionKey, signals)
        }
        if (wholeAppRule != null) {
            scheduleBlock(
                key = "section:$packageName:${wholeAppRule.sectionKey}",
                delaySeconds = snapshot.globalDelaySeconds,
            ) {
                actOnSection(packageName, wholeAppRule, snapshot.globalDelaySeconds, snapshot.strict)
            }
            return
        }
        for (rule in snapshot.sections) {
            if (rule.packageName != packageName) continue
            if (!SectionDetector.isSectionShowing(packageName, rule.sectionKey, signals)) continue
            val delaySeconds = snapshot.globalDelaySeconds
            scheduleBlock(
                key = "section:$packageName:${rule.sectionKey}",
                delaySeconds = delaySeconds,
            ) {
                actOnSection(packageName, rule, delaySeconds, snapshot.strict)
            }
        }
    }

    /** مرورگر روی تغییر پنجره: omnibox فعلی را از فیلدهای ویرایشی بخوان. */
    private suspend fun evaluateBrowserWindow(
        packageName: String,
        signals: SectionDetector.SectionSignals,
        snapshot: Snapshot,
    ) {
        for (editableText in signals.editableTexts) {
            if (editableText.isBlank()) continue
            evaluateBrowserText(packageName, editableText, snapshot)
        }
    }

    // ---------- زمان‌بندی بلاک (تاخیر + راستی‌آزمایی دوباره) ----------

    /**
     * بلاک را بعد از [delaySeconds] اجرا می‌کند؛ موقع اجرا دوباره چک
     * می‌شود که سشن هنوز فعال باشد. ضد حلقه: همان کلید تا ۱۵ ثانیه
     * دوباره زمان‌بندی نمی‌شود (صفحه بلاک خودش تغییر پنجره می‌سازد).
     */
    private fun scheduleBlock(
        key: String,
        delaySeconds: Int,
        act: suspend () -> Unit,
    ) {
        val now = System.currentTimeMillis()
        val last = cooldownUntil[key] ?: 0L
        if (now - last < COOLDOWN_MS) return
        cooldownUntil[key] = now
        scope.launch {
            try {
                if (delaySeconds > 0) delay(delaySeconds * 1000L)
                val fresh = loadSnapshot(forceRefresh = true)
                if (!fresh.sessionActive) return@launch
                act()
            } catch (_: Exception) {
                // fail-open
            }
        }
    }

    // ---------- اقدام‌ها ----------

    /** حذف کلمه منطبق از فیلد، بعد صفحه بلاک. */
    private suspend fun actOnKeyword(
        packageName: String,
        rule: KeywordRule,
        delaySeconds: Int,
        strict: Boolean,
    ) {
        val root = activeRootOrNull() ?: return
        val node = findEditableNodeMatching(root, rule.rawText) ?: return
        try {
            val currentText = try {
                node.text?.toString()
            } catch (_: Exception) {
                null
            } ?: return
            // راستی‌آزمایی دوباره: کلمه هنوز در فیلد هست؟
            if (TextRedactor.findMatchedKeyword(currentText, listOf(rule.rawText)) == null) return
            val redacted = TextRedactor.removeMatchedRule(currentText, rule.rawText)
            if (redacted == currentText) return
            val done = trySetText(node, redacted)
            if (!done) {
                tryCutMatchedWord(node, currentText, rule.rawText)
            }
            logBlock(
                BlockEventKind.KEYWORD_TYPED,
                "keyword_rule_id=${rule.id} pkg=$packageName strict=$strict",
            )
            showBlocked("کلمه «${rule.rawText}» بلاک شد", rule.category, delaySeconds)
        } catch (_: Exception) {
            // fail-open
        } finally {
            try {
                node.recycle()
            } catch (_: Exception) {
                // نادیده
            }
        }
    }

    /** ورود به بخش بلاک‌شده: BACK + صفحه بلاک. */
    private suspend fun actOnSection(
        packageName: String,
        rule: SectionRule,
        delaySeconds: Int,
        strict: Boolean,
    ) {
        val root = activeRootOrNull() ?: return
        try {
            // راستی‌آزمایی دوباره: اپ قانون هنوز پیش‌زمینه است؟
            // برای «کل اپ» سیگنال‌ها خالی هم مچ می‌شوند، پس بدون این چک
            // بلاک دیرکرده می‌توانست روی صفحه دیگری (مثلاً خانه) BACK بزند؛
            // اگر کاربر خودش وسط تاخیر از اپ بیرون رفته باشد، بلاک لغو می‌شود.
            val currentPkg = try {
                root.packageName?.toString()
            } catch (_: Exception) {
                null
            }
            if (currentPkg != packageName) return
            // راستی‌آزمایی دوباره: بخش هنوز روی صفحه است؟
            val signals = collectSignals(root, null)
            if (!SectionDetector.isSectionShowing(packageName, rule.sectionKey, signals)) return
            performGlobalAction(GLOBAL_ACTION_BACK)
            logBlock(
                BlockEventKind.SECTION_OPENED,
                "section=${rule.sectionKey} pkg=$packageName strict=$strict",
            )
            showBlocked("بخش «${rule.labelFa}» بلاک شد", null, delaySeconds)
        } catch (_: Exception) {
            // fail-open
        }
    }

    /** دامنه/کلمه در مرورگر: BACK + صفحه بلاک. */
    private suspend fun actOnBrowser(
        packageName: String,
        reason: String,
        delaySeconds: Int,
        strict: Boolean,
    ) {
        try {
            performGlobalAction(GLOBAL_ACTION_BACK)
            logBlock(
                BlockEventKind.WEBSITE_OPENED,
                "pkg=$packageName strict=$strict",
            )
            showBlocked(reason, null, delaySeconds)
        } catch (_: Exception) {
            // fail-open
        }
    }

    // ---------- ابزار گره‌ها ----------

    /** اولین گره قابل‌ویرایش که متن آن با قانون مچ شود. گره برگردانده‌شده باید بازیافت شود. */
    private fun findEditableNodeMatching(
        root: AccessibilityNodeInfo,
        ruleRaw: String,
    ): AccessibilityNodeInfo? {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        var visited = 0
        try {
            while (stack.isNotEmpty() && visited < MAX_NODES) {
                val node = stack.removeFirst()
                visited++
                var matched = false
                try {
                    if (node.isEditable) {
                        val text = node.text?.toString()
                        if (text != null && KeywordMatcher.matches(text, ruleRaw)) {
                            matched = true
                        }
                    }
                    if (!matched) {
                        for (i in 0 until node.childCount) {
                            try {
                                node.getChild(i)?.let { stack.add(it) }
                            } catch (_: Exception) {
                                // نادیده
                            }
                        }
                    }
                } catch (_: Exception) {
                    // نادیده
                }
                if (matched) return node
                try {
                    node.recycle()
                } catch (_: Exception) {
                    // نادیده
                }
            }
        } catch (_: Exception) {
            // fail-open
        } finally {
            for (leftover in stack) {
                try {
                    leftover.recycle()
                } catch (_: Exception) {
                    // نادیده
                }
            }
        }
        return null
    }

    /** جمع‌آوری سیگنال‌های بخش از درخت پنجره (متن‌ها، توضیح‌ها، فیلد ویرایشی). */
    private fun collectSignals(
        root: AccessibilityNodeInfo,
        windowClassName: String?,
    ): SectionDetector.SectionSignals {
        val texts = ArrayList<String>()
        val descriptions = ArrayList<String>()
        val editableTexts = ArrayList<String>()
        var hasEditable = false
        var hint: String? = null
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        var visited = 0
        try {
            while (stack.isNotEmpty() && visited < MAX_NODES) {
                val node = stack.removeFirst()
                visited++
                try {
                    node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
                    node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let {
                        descriptions.add(it)
                    }
                    if (node.isEditable) {
                        hasEditable = true
                        node.text?.toString()?.let { editableTexts.add(it) }
                        if (hint == null) hint = node.hintText?.toString()
                    }
                    for (i in 0 until node.childCount) {
                        try {
                            node.getChild(i)?.let { stack.add(it) }
                        } catch (_: Exception) {
                            // نادیده
                        }
                    }
                } catch (_: Exception) {
                    // نادیده
                } finally {
                    try {
                        node.recycle()
                    } catch (_: Exception) {
                        // نادیده
                    }
                }
            }
        } catch (_: Exception) {
            // fail-open: سیگنال ناقص برمی‌گردد، تصمیم محافظه‌کارانه می‌گیرد.
        } finally {
            for (leftover in stack) {
                try {
                    leftover.recycle()
                } catch (_: Exception) {
                    // نادیده
                }
            }
        }
        return SectionDetector.SectionSignals(
            windowTitle = null,
            windowClassName = windowClassName,
            texts = texts,
            contentDescriptions = descriptions,
            hasEditableTextField = hasEditable,
            editableHint = hint,
            editableTexts = editableTexts,
        )
    }

    /** جایگزینی کل متن فیلد؛ false یعنی ACTION_SET_TEXT پشتیبانی نشد. */
    private fun trySetText(node: AccessibilityNodeInfo, newText: String): Boolean {
        return try {
            val args = Bundle()
            args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText,
            )
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * حذف کلمه منطبق با انتخاب + CUT (fallback وقتی ACTION_SET_TEXT نشد).
     * سرویس Accessibility متد dispatchKeyEvent ندارد (متد Activity/View
     * است)، پس دکمه backspace را نمی‌توان مستقیم فرستاد؛ انتخاب بازه
     * کلمه و CUT همان کار را با APIهای دسترس‌پذیری می‌کند.
     */
    private fun tryCutMatchedWord(node: AccessibilityNodeInfo, text: String, ruleRaw: String): Boolean {
        val range = TextRedactor.firstMatchRange(text, ruleRaw) ?: return false
        return try {
            val selArgs = Bundle()
            selArgs.putInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,
                range.first,
            )
            selArgs.putInt(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,
                range.second,
            )
            if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs)) return false
            node.performAction(AccessibilityNodeInfo.ACTION_CUT)
        } catch (_: Exception) {
            false
        }
    }

    // ---------- خروجی ----------

    private suspend fun logBlock(kind: String, detail: String) {
        try {
            blockEventRepository.log(kind, detail, System.currentTimeMillis())
        } catch (_: Exception) {
            // ثبت گزارش هرگز بلاک را خراب نمی‌کند.
        }
    }

    private fun showBlocked(reason: String, category: String?, delaySeconds: Int?) {
        try {
            val intent = Intent(this, BlockedActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("reason", reason)
            if (category != null) intent.putExtra("category", category)
            if (delaySeconds != null) intent.putExtra("delaySeconds", delaySeconds)
            startActivity(intent)
        } catch (_: Exception) {
            // fail-open: اگر صفحه بلاک باز نشد، گوشی قفل نمی‌شود.
        }
    }

    companion object {
        /** کش عکس قوانین: ۵ ثانیه (رویداد تایپ با هر کلید می‌آید). */
        private const val SNAPSHOT_TTL_MS = 5_000L

        /** ضد حلقه بلاک تکراری: ۱۵ ثانیه برای هر کلید. */
        private const val COOLDOWN_MS = 15_000L

        /** سقف گره‌های خوانده‌شده در هر پیمایش درخت (جلوگیری از هنگ). */
        private const val MAX_NODES = 800
    }
}
