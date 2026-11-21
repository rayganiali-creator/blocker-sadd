package com.sadd.blocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.sadd.blocker.core.BlockThrottle
import com.sadd.blocker.core.KeywordMatcher
import com.sadd.blocker.core.ProtectionGuardDetector
import com.sadd.blocker.core.SectionDetector
import com.sadd.blocker.core.TextRedactor
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.system.UninstallProtection
import com.sadd.blocker.domain.UninstallProtectionGate
import com.sadd.blocker.domain.model.BlockEventKind
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.model.DelayOptions
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
import javax.inject.Inject

/**
 * موتور بلاک بدون VPN — AccessibilityService.
 *
 * سه مسیر بلاک، همه فقط وسط سشن فعال:
 * 1. تایپ کلمه ممنوعه: روی TYPE_VIEW_TEXT_CHANGED متن فیلد قابل‌ویرایش
 *    خوانده می‌شود؛ اگر کلمه‌ای whole-word مچ شود، بعد از تاخیر قانون،
 *    همان کلمه از فیلد حذف می‌شود (ACTION_SET_TEXT؛ اگر نشد backspace)
 *    و صفحه بلاک نشان داده می‌شود.
 * 2. بخش‌های اپ: روی تغییر پنجره/محتوا/کلیک، اگر بخش فعالِ قانون روی
 *    صفحه باشد، بعد از تاخیر سراسری BACK زده می‌شود و صفحه بلاک
 *    می‌آید. بخش فقط وقتی «روی صفحه» است که کاربر واقعاً وارد آن شده
 *    باشد، نه صرف اینکه عنصرش در درخت دیده شود: جستجو فقط با فیلد
 *    جستجوی واقعاً فعال (فوکوس یا متن تایپ‌شده — تایپ در فیلد جستجو
 *    هم از مسیر TYPE_VIEW_TEXT_CHANGED ارزیابی می‌شود) و فید فقط با
 *    تب واقعاً انتخاب‌شده یا عنوان/کلاس پنجره. «کل اپ» (تلگرام/
 *    اینستاگرام/یوتیوب/واتساپ/تیک‌تاک/ایکس/فیسبوک/اسنپ‌چت/کروم، یا
 *    «خود اپ» هر اپ نصب‌شده) فقط تطابق پکیج می‌خواهد و بقیه بخش‌های
 *    همان اپ را دربر می‌گیرد. مرورگرها هم قانون بخش دارند (خود کروم،
 *    Incognito) و در evaluateWindow جدا از omnibox ارزیابی می‌شوند.
 *    «کل اپ» بقیه بخش‌های همان اپ را دربر می‌گیرد: اگر مچ شد،
 *    بخش‌های دیگر آن اپ همان دور رد می‌شوند تا دو BACK پشت سر هم
 *    زمان‌بندی نشود.
 * 3. مرورگر (کروم، فایرفاکس، سامسونگ اینترنت، اج، Brave): متن
 *    omnibox/جعبه جستجو خوانده می‌شود؛ دامنه بلاک‌شده یا کلمه
 *    ممنوعه → بعد از تاخیر BACK + صفحه بلاک. بلاک دامنه/کلمه در تب
 *    خصوصی به تشخیص Incognito وابسته نیست — متن نوار آدرس در تب
 *    خصوصی هم از همان مسیر خوانده می‌شود؛ قانون بخش Incognito
 *    کروم جداگانه از مسیر بخش ارزیابی می‌شود (بهترین تلاش).
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

    override fun onCreate() {
        super.onCreate()
        // مهاجرت یک‌باره تاخیر پیش‌فرض به فوری؛ سرویس ممکن است پیش از
        // MainActivity (مثلاً بعد از بوت) روشن شود.
        scope.launch {
            try {
                settingsStore.migrateDelayDefaultIfNeeded()
            } catch (_: Exception) {
                // fail-open: مهاجرت خراب نمی‌کند؛ پیش‌فرض کد ۰ است.
            }
            try {
                // مهاجرت یک‌باره «پیش‌فرض‌های بخش خاموش» (گزارش سید
                // ۲۰۲۶-۱۱-۲۰): نصب‌های موجود هم مثل نصب تازه، همه
                // پیش‌فرض‌ها را خاموش می‌گیرند؛ کاربر خودش روشن می‌کند.
                settingsStore.migratePresetsDefaultOffIfNeeded {
                    sectionRuleRepository.disableAllPresets()
                }
            } catch (_: Exception) {
                // fail-open: مهاجرت خراب نمی‌کند.
            }
        }
    }

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

    /** ضد بلاک تکراری: زمان‌بندیِ دوباره همان کلید تا اجرای بلاک رد می‌شود، و بعد از بلاک واقعی فقط ۱٫۵ ثانیه debounce است (BlockThrottle). */
    private val blockThrottle = BlockThrottle()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return
            val type = event.eventType
            if (type != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                type != AccessibilityEvent.TYPE_VIEW_CLICKED &&
                type != AccessibilityEvent.TYPE_VIEW_FOCUSED &&
                type != AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
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
                // سیگنال بخشِ تایپ‌شده هم‌زمان با خواندن متن گرفته می‌شود:
                // رویداد بعد از بازگشت از این callback بازیافت می‌شود، پس
                // گره منبع را نمی‌توان به کوروتین سپرد.
                val typedSectionSignals = readTextChangedSectionSignals(event)
                scope.launch {
                    try {
                        evaluateText(foregroundPkg, text)
                    } catch (_: Exception) {
                        // fail-open
                    }
                }
                // تایپ در فیلد جستجو، خودش شاهد فعال‌سازی بخش جستجو است:
                // بدون این مسیر، تایپ کردن هرگز رویداد پنجره نمی‌سازد و
                // بخش جستجو فقط با فوکوس (کلیک) دیده می‌شد. مرورگرها
                // از این مسیر بیرون‌اند — تایپ در omnibox کروم از مسیر
                // دامنه/کلمه می‌گذرد و «خود کروم» نباید با تایپ بلاک شود.
                if (typedSectionSignals != null && !SectionDetector.isBrowserPackage(foregroundPkg)) {
                    scope.launch {
                        try {
                            evaluateTypedSection(foregroundPkg, typedSectionSignals)
                        } catch (_: Exception) {
                            // fail-open
                        }
                    }
                }
            } else {
                val windowClass = event.className?.toString()
                // شاهد تعامل (کلیک/فوکوس) همین‌جا از گره منبع گرفته
                // می‌شود؛ رویداد بعد از بازگشت از callback بازیافت
                // می‌شود و نمی‌توان گره را به کوروتین سپرد.
                val interactionSignals = readInteractionSignals(event)
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
                        evaluateWindow(foregroundPkg, windowClass, interactionSignals)
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

    /**
     * سیگنال بخش از گره منبع رویداد تایپ: فقط وقتی گره، فیلد ویرایشی
     * جستجو ([SectionDetector.isSearchFieldTextChange] — مارکر در
     * hint/متن/توضیح) با متن غیرخالی است، سیگنال برمی‌گردد؛ وگرنه null
     * (تایپ در چت/کامنت/متن عادی، شاهد ورود به جستجو نیست). گره
     * همین‌جا بازیافت می‌شود؛ رویداد بعد از callback بازیافت می‌شود و
     * نمی‌توان آن را به کوروتین سپرد.
     */
    private fun readTextChangedSectionSignals(
        event: AccessibilityEvent,
    ): SectionDetector.SectionSignals? {
        val source = try {
            event.source
        } catch (_: Exception) {
            null
        } ?: return null
        try {
            val hint = try {
                source.hintText?.toString()
            } catch (_: Exception) {
                null
            }
            val text = try {
                source.text?.toString()
            } catch (_: Exception) {
                null
            }
            val desc = try {
                source.contentDescription?.toString()
            } catch (_: Exception) {
                null
            }
            val editable = try {
                source.isEditable
            } catch (_: Exception) {
                false
            }
            if (!SectionDetector.isSearchFieldTextChange(editable, hint, text, desc)) return null
            if (text.isNullOrBlank()) return null
            val focused = try {
                source.isFocused || source.isAccessibilityFocused
            } catch (_: Exception) {
                false
            }
            return SectionDetector.SectionSignals(
                hasEditableTextField = true,
                editableHint = hint,
                searchFieldFocused = focused,
                searchFieldHasText = true,
            )
        } catch (_: Exception) {
            return null
        } finally {
            try {
                source.recycle()
            } catch (_: Exception) {
                // نادیده
            }
        }
    }

    /**
     * شاهد تعامل از گره منبع رویداد کلیک/فوکوس — تنها شاهد معتبر بخش
     * جستجو و مرور خصوصی (گزارش سید ۲۰۲۶-۱۱-۲۰: اسکرول از کنار نوار
     * جستجو یا دیده شدن کلمه «incognito» در منو هرگز بلاک نمی‌کند).
     *
     * - TYPE_VIEW_CLICKED: متن/توضیح گره کلیک‌شده (و فرزندان مستقیمش)
     *   در clickedTexts/clickedContentDescriptions می‌نشیند؛ اگر
     *   [SectionDetector.isSearchFieldClick] روی گره درست باشد (فیلد
     *   ویرایشی جستجو در خود گره یا زیردرختش، یا نوار جستجوی
     *   غیرقابل‌ویرایش با مارکر) searchFieldClicked هم true می‌شود.
     *   گره checkable (تب ناوبری) هرگز شاهد نیست — کلیک روی تب
     *   «Search and explore» اینستاگرام ورود به اکسپلور است، نه جستجو
     *   (گزارش سید ۲۰۲۶-۱۱-۲۱).
     * - TYPE_VIEW_FOCUSED / TYPE_VIEW_ACCESSIBILITY_FOCUSED: اگر
     *   [SectionDetector.isSearchFieldFocus] روی گره فوکوس‌گرفته درست
     *   باشد، searchFieldFocused=true می‌شود.
     *
     * برای بقیه رویدادها null برمی‌گردد. گره‌ها همین‌جا بازیافت
     * می‌شوند؛ رویداد بعد از callback بازیافت می‌شود.
     */
    private fun readInteractionSignals(
        event: AccessibilityEvent,
    ): SectionDetector.SectionSignals? {
        val type = event.eventType
        val isClick = type == AccessibilityEvent.TYPE_VIEW_CLICKED
        val isFocus = type == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            type == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
        if (!isClick && !isFocus) return null
        val source = try {
            event.source
        } catch (_: Exception) {
            null
        } ?: return null
        try {
            val text = try {
                source.text?.toString()
            } catch (_: Exception) {
                null
            }
            val desc = try {
                source.contentDescription?.toString()
            } catch (_: Exception) {
                null
            }
            val hint = try {
                source.hintText?.toString()
            } catch (_: Exception) {
                null
            }
            val editable = try {
                source.isEditable
            } catch (_: Exception) {
                false
            }
            val checkable = try {
                source.isCheckable
            } catch (_: Exception) {
                false
            }
            if (isFocus) {
                val hasDescendant = hasEditableSearchDescendant(source)
                if (!SectionDetector.isSearchFieldFocus(
                        editable, checkable, hint, text, desc, hasDescendant,
                    )
                ) {
                    return null
                }
                return SectionDetector.SectionSignals(
                    hasEditableTextField = true,
                    editableHint = hint,
                    searchFieldFocused = true,
                )
            }
            // کلیک: متن/توضیح خود گره + فرزندان مستقیم (آیتم منو ممکن
            // است متن را در فرزند نگه دارد).
            val clickedTexts = ArrayList<String>()
            val clickedDescriptions = ArrayList<String>()
            text?.takeIf { it.isNotBlank() }?.let { clickedTexts.add(it) }
            desc?.takeIf { it.isNotBlank() }?.let { clickedDescriptions.add(it) }
            val searchClicked = SectionDetector.isSearchFieldClick(
                editable, checkable, hint, text, desc,
                hasEditableSearchDescendant(source),
            )
            try {
                for (i in 0 until source.childCount) {
                    val child = try {
                        source.getChild(i)
                    } catch (_: Exception) {
                        null
                    } ?: continue
                    try {
                        val childText = child.text?.toString()
                        val childDesc = child.contentDescription?.toString()
                        childText?.takeIf { it.isNotBlank() }?.let { clickedTexts.add(it) }
                        childDesc?.takeIf { it.isNotBlank() }?.let { clickedDescriptions.add(it) }
                    } catch (_: Exception) {
                        // نادیده
                    } finally {
                        try {
                            child.recycle()
                        } catch (_: Exception) {
                            // نادیده
                        }
                    }
                }
            } catch (_: Exception) {
                // نادیده: شاهد خود گره کافی است.
            }
            if (clickedTexts.isEmpty() && clickedDescriptions.isEmpty() && !searchClicked) {
                return null
            }
            return SectionDetector.SectionSignals(
                searchFieldClicked = searchClicked,
                clickedTexts = clickedTexts,
                clickedContentDescriptions = clickedDescriptions,
            )
        } catch (_: Exception) {
            return null
        } finally {
            try {
                source.recycle()
            } catch (_: Exception) {
                // نادیده
            }
        }
    }

    /**
     * آیا در زیردرخت [root] (نه خود ریشه) فیلد ویرایشی جستجو هست؟
     * نوار جستجوی اینستاگرام یک ظرف غیرقابل‌ویرایش است و فیلد واقعی
     * چند سطح پایین‌تر است؛ چکِ فقط فرزندان مستقیم آن را نمی‌دید و
     * جستجو هرگز بلاک نمی‌شد (گزارش سید ۲۰۲۶-۱۱-۲۱). پیمایش محدود
     * است (سقف گره) و همه گره‌های گرفته‌شده بازیافت می‌شوند؛ خود
     * [root] را صداکننده بازیافت می‌کند.
     */
    private fun hasEditableSearchDescendant(root: AccessibilityNodeInfo): Boolean {
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        try {
            for (i in 0 until root.childCount) {
                try {
                    root.getChild(i)?.let { stack.add(it) }
                } catch (_: Exception) {
                    // نادیده
                }
            }
        } catch (_: Exception) {
            return false
        }
        var visited = 0
        try {
            while (stack.isNotEmpty() && visited < MAX_SEARCH_SUBTREE_NODES) {
                val node = stack.removeFirst()
                visited++
                try {
                    val nodeEditable = try {
                        node.isEditable
                    } catch (_: Exception) {
                        false
                    }
                    if (nodeEditable) {
                        val nodeHint = try {
                            node.hintText?.toString()
                        } catch (_: Exception) {
                            null
                        }
                        val nodeText = try {
                            node.text?.toString()
                        } catch (_: Exception) {
                            null
                        }
                        val nodeDesc = try {
                            node.contentDescription?.toString()
                        } catch (_: Exception) {
                            null
                        }
                        if (SectionDetector.isSearchEditableNode(
                                nodeEditable, nodeHint, nodeText, nodeDesc,
                            )
                        ) {
                            return true
                        }
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
            // fail-open: شاهد زیردرخت پیدا نشد.
        } finally {
            for (leftover in stack) {
                try {
                    leftover.recycle()
                } catch (_: Exception) {
                    // نادیده
                }
            }
        }
        return false
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
                globalDelaySeconds = DelayOptions.DEFAULT_SECONDS,
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

    /**
     * بخش جستجو روی تایپ: کاربر دارد در فیلد جستجو تایپ می‌کند، پس بخش
     * جستجو فعال است — حتی اگر هیچ رویداد پنجره‌ای نیامده باشد. فقط
     * قانون‌های جستجوی همان پکیج چک می‌شوند («کل اپ» و فید و بخش
     * سفارشی از این مسیر نمی‌گذرند). راستی‌آزمایی دوباره در actOnSection
     * از درخت تازه می‌خواند: اگر فیلد تا موقع اجرا خالی شده باشد، بلاک
     * لغو می‌شود.
     */
    private suspend fun evaluateTypedSection(
        packageName: String,
        signals: SectionDetector.SectionSignals,
    ) {
        val snapshot = loadSnapshot(forceRefresh = false)
        if (!snapshot.sessionActive) return
        for (rule in snapshot.sections) {
            if (rule.packageName != packageName) continue
            if (!SectionDetector.isSearchSection(rule.sectionKey)) continue
            if (!SectionDetector.isSectionShowing(packageName, rule.sectionKey, signals)) continue
            val delaySeconds = snapshot.globalDelaySeconds
            scheduleBlock(
                key = "section:$packageName:${rule.sectionKey}",
                delaySeconds = delaySeconds,
            ) {
                actOnSection(packageName, rule, delaySeconds, snapshot.strict, evidence = signals)
            }
        }
    }

    /**
     * متن omnibox/جعبه جستجوی مرورگر: اول دامنه، بعد کلمه.
     */
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
     * Strict را باز می‌کند. نگهبان همیشه فوری عمل می‌کند (تاخیر صفر) و
     * تاخیر تنظیم‌شده کاربر روی آن اثری ندارد — مسیر scheduleBlock را نمی‌رود. اگر صفحه محافظت‌شده دیگر جلوی چشم نباشد
     * (مثلاً کاربر با بازگشت بیرون رفته باشد)، پرده برداشته می‌شود.
     * متن خود اپ هرگز نگهبان را فعال نمی‌کند (هم چک پکیج پیش‌زمینه در
     * onAccessibilityEvent و هم چک داخل detector). ضد حلقه فقط برای BACK و صفحه بازگشایی است — BlockThrottle با
     * debounce ۱٫۵ ثانیه‌ای بعد از هر شلیک؛ پرده خودش idempotent است و با
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
            if (!blockThrottle.trySchedule(key)) return
            logBlock(
                BlockEventKind.GUARD_BLOCKED,
                "guard:$hit pkg=$packageName",
            )
            performGlobalAction(GLOBAL_ACTION_BACK)
            showGuardUnlock(hit)
            blockThrottle.markFired(key)
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
        interactionSignals: SectionDetector.SectionSignals? = null,
    ) {
        val snapshot = loadSnapshot(forceRefresh = false)
        if (!snapshot.sessionActive) return
        val root = activeRootOrNull() ?: return
        val signals = mergeInteractionSignals(collectSignals(root, windowClassName), interactionSignals)
        if (SectionDetector.isBrowserPackage(packageName)) {
            // پیش‌تر همه پکیج‌های مرورگر فقط به evaluateBrowserWindow
            // می‌رفتند و قانون بخش (خود کروم، Incognito) هرگز ارزیابی
            // نمی‌شد؛ حالا بخش‌های مرورگر هم چک می‌شوند و بعد omnibox
            // (دامنه/کلمه) مثل قبل خوانده می‌شود.
            evaluateSectionRules(packageName, signals, windowClassName, snapshot)
            evaluateBrowserWindow(packageName, signals, snapshot)
            return
        }
        evaluateSectionRules(packageName, signals, windowClassName, snapshot)
    }

    /**
     * شاهد تعامل (کلیک/فوکوس/تایپ) را روی سیگنال‌های درخت می‌نشاند:
     * پرچم‌های تعامل OR می‌شوند و متن‌های کلیک‌شده اضافه می‌شوند تا
     * تصمیم بخش روی شاهد واقعی بماند، نه روی صرفِ دیده شدن عنصر.
     */
    private fun mergeInteractionSignals(
        base: SectionDetector.SectionSignals,
        interaction: SectionDetector.SectionSignals?,
    ): SectionDetector.SectionSignals {
        if (interaction == null) return base
        return base.copy(
            searchFieldClicked = base.searchFieldClicked || interaction.searchFieldClicked,
            searchFieldFocused = base.searchFieldFocused || interaction.searchFieldFocused,
            searchFieldHasText = base.searchFieldHasText || interaction.searchFieldHasText,
            clickedTexts = base.clickedTexts + interaction.clickedTexts,
            clickedContentDescriptions = base.clickedContentDescriptions +
                interaction.clickedContentDescriptions,
        )
    }

    /**
     * قانون‌های بخش یک پکیج روی سیگنال‌های پنجره فعلی.
     * «کل اپ» بقیه بخش‌های همان اپ را دربر می‌گیرد: اگر مچ شد، فقط
     * همان یک بلاک زمان‌بندی می‌شود (یک BACK، نه چند BACK پشت سر هم).
     * جدا از ترتیب لیست قوانین چک می‌شود تا وابسته به ORDER BY نباشد.
     */
    private suspend fun evaluateSectionRules(
        packageName: String,
        signals: SectionDetector.SectionSignals,
        windowClassName: String?,
        snapshot: Snapshot,
    ) {
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
                actOnSection(
                    packageName,
                    wholeAppRule,
                    snapshot.globalDelaySeconds,
                    snapshot.strict,
                    windowClassName,
                    evidence = signals,
                )
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
                actOnSection(packageName, rule, delaySeconds, snapshot.strict, windowClassName, evidence = signals)
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
     * می‌شود که سشن هنوز فعال باشد. ضد حلقه (BlockThrottle): همان کلید
     * تا وقتی بلاکش در صف است دوباره زمان‌بندی نمی‌شود؛ بعد از اجرای
     * واقعی بلاک فقط ۱٫۵ ثانیه debounce است تا انفجار رویدادهای صفحه
     * بلاک بلاک دوباره نسازد — تلاش تازه بعد از آن بی‌درنگ بلاک می‌شود.
     * اگر بلاک اجرا نشد (سشن تمام شد / کلمه پاک شد / بخش رفت)، هیچ
     * debounceای نمی‌ماند.
     */
    private fun scheduleBlock(
        key: String,
        delaySeconds: Int,
        act: suspend () -> Boolean,
    ) {
        if (!blockThrottle.trySchedule(key)) return
        scope.launch {
            try {
                if (delaySeconds > 0) delay(delaySeconds * 1000L)
                val fresh = loadSnapshot(forceRefresh = true)
                if (!fresh.sessionActive) {
                    blockThrottle.markAbandoned(key)
                    return@launch
                }
                val fired = act()
                if (fired) blockThrottle.markFired(key) else blockThrottle.markAbandoned(key)
            } catch (_: Exception) {
                blockThrottle.markAbandoned(key)
            }
        }
    }

    // ---------- اقدام‌ها ----------

    /** حذف کلمه منطبق از فیلد، بعد صفحه بلاک. true یعنی بلاک واقعاً شد. */
    private suspend fun actOnKeyword(
        packageName: String,
        rule: KeywordRule,
        delaySeconds: Int,
        strict: Boolean,
    ): Boolean {
        val root = activeRootOrNull() ?: return false
        val node = findEditableNodeMatching(root, rule.rawText) ?: return false
        try {
            val currentText = try {
                node.text?.toString()
            } catch (_: Exception) {
                null
            } ?: return false
            // راستی‌آزمایی دوباره: کلمه هنوز در فیلد هست؟
            if (TextRedactor.findMatchedKeyword(currentText, listOf(rule.rawText)) == null) return false
            val redacted = TextRedactor.removeMatchedRule(currentText, rule.rawText)
            if (redacted == currentText) return false
            val done = trySetText(node, redacted)
            if (!done) {
                tryCutMatchedWord(node, currentText, rule.rawText)
            }
            logBlock(
                BlockEventKind.KEYWORD_TYPED,
                "keyword_rule_id=${rule.id} pkg=$packageName strict=$strict",
            )
            // خروج از اپ/سایت بلاک‌شده، بعد رفتن به «سد»: بدون BACK، برگشت از
            // صفحه بلاک کاربر را به همان متن بلاک‌شده برمی‌گرداند.
            performGlobalAction(GLOBAL_ACTION_BACK)
            showBlocked("کلمه «${rule.rawText}» بلاک شد", rule.category, delaySeconds)
            return true
        } catch (_: Exception) {
            return false
        } finally {
            try {
                node.recycle()
            } catch (_: Exception) {
                // نادیده
            }
        }
    }

    /** ورود به بخش بلاک‌شده: BACK + صفحه بلاک. true یعنی بلاک واقعاً شد. */
    private suspend fun actOnSection(
        packageName: String,
        rule: SectionRule,
        delaySeconds: Int,
        strict: Boolean,
        windowClassName: String? = null,
        evidence: SectionDetector.SectionSignals? = null,
    ): Boolean {
        val root = activeRootOrNull() ?: return false
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
            if (currentPkg != packageName) return false
            // راستی‌آزمایی دوباره: بخش هنوز روی صفحه است؟ کلاس پنجره از
            // لحظه زمان‌بندی نگه داشته می‌شود تا بخشی که فقط با کلاس
            // پنجره مچ شده بود (مثلاً فید با کلاس اکتیویتی) در این چک
            // با کلاس null رد نشود. شاهد تعامل لحظه زمان‌بندی (کلیک/
            // فوکوس/تایپ) هم روی سیگنال تازه می‌نشیند — کلیک یک رویداد
            // لحظه‌ای است و در درخت تازه دیگر دیده نمی‌شود، ولی همان
            // کلیک دلیل بلاک است.
            val signals = mergeInteractionSignals(collectSignals(root, windowClassName), evidence)
            if (!SectionDetector.isSectionShowing(packageName, rule.sectionKey, signals)) return false
            performGlobalAction(GLOBAL_ACTION_BACK)
            logBlock(
                BlockEventKind.SECTION_OPENED,
                "section=${rule.sectionKey} pkg=$packageName strict=$strict",
            )
            showBlocked("بخش «${rule.labelFa}» بلاک شد", null, delaySeconds)
            return true
        } catch (_: Exception) {
            return false
        }
    }

    /** دامنه/کلمه در مرورگر: BACK + صفحه بلاک. true یعنی بلاک واقعاً شد. */
    private suspend fun actOnBrowser(
        packageName: String,
        reason: String,
        delaySeconds: Int,
        strict: Boolean,
    ): Boolean {
        try {
            performGlobalAction(GLOBAL_ACTION_BACK)
            logBlock(
                BlockEventKind.WEBSITE_OPENED,
                "pkg=$packageName strict=$strict",
            )
            showBlocked(reason, null, delaySeconds)
            return true
        } catch (_: Exception) {
            return false
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

    /**
     * جمع‌آوری سیگنال‌های بخش از درخت پنجره.
     *
     * علاوه بر متن‌ها/توضیح‌ها/فیلد ویرایشی، شواهد فعال‌سازی هم جمع
     * می‌شود — بخش فقط با همین شواهد بلاک می‌شود، نه با صرفِ دیده شدن:
     * - فیلد جستجو واقعاً فعال است؟ فیلد ویرایشی با hint جستجو که فوکوس
     *   دارد (isFocused یا isAccessibilityFocused) یا متن تایپ‌شده
     *   (غیرخالی) دارد. نوار جستجوی بالای صفحه اصلی (بدون فوکوس، خالی)
     *   هیچ‌کدام را true نمی‌کند.
     * - کدام گره‌ها واقعاً انتخاب/چک شده‌اند؟ متن/توضیح آن‌ها جدا نگه
     *   داشته می‌شود تا برچسب تبِ صرفاً دیده‌شده (روی همه صفحه‌ها)
     *   فید را بلاک نکند.
     */
    private fun collectSignals(
        root: AccessibilityNodeInfo,
        windowClassName: String?,
    ): SectionDetector.SectionSignals {
        val texts = ArrayList<String>()
        val descriptions = ArrayList<String>()
        val editableTexts = ArrayList<String>()
        val selectedTexts = ArrayList<String>()
        val selectedDescriptions = ArrayList<String>()
        var hasEditable = false
        var hint: String? = null
        var searchFieldFocused = false
        var searchFieldHasText = false
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)
        var visited = 0
        try {
            while (stack.isNotEmpty() && visited < MAX_NODES) {
                val node = stack.removeFirst()
                visited++
                try {
                    val nodeText = node.text?.toString()?.takeIf { it.isNotBlank() }
                    nodeText?.let { texts.add(it) }
                    val nodeDesc = node.contentDescription?.toString()?.takeIf { it.isNotBlank() }
                    nodeDesc?.let { descriptions.add(it) }
                    try {
                        if (node.isSelected || node.isChecked) {
                            nodeText?.let { selectedTexts.add(it) }
                            nodeDesc?.let { selectedDescriptions.add(it) }
                        }
                    } catch (_: Exception) {
                        // نادیده
                    }
                    if (node.isEditable) {
                        hasEditable = true
                        node.text?.toString()?.let { editableTexts.add(it) }
                        val nodeHint = try {
                            node.hintText?.toString()
                        } catch (_: Exception) {
                            null
                        }
                        if (hint == null) hint = nodeHint
                        // مارکر جستجو در هر سه منبع (hint/متن/توضیح) —
                        // فیلد جستجوی اینستاگرام مارکر را در hint نمی‌دهد
                        // (گزارش سید ۲۰۲۶-۱۱-۲۱).
                        if (SectionDetector.hasSearchMarker(nodeHint, nodeText, nodeDesc)) {
                            try {
                                if (node.isFocused || node.isAccessibilityFocused) {
                                    searchFieldFocused = true
                                }
                            } catch (_: Exception) {
                                // نادیده
                            }
                            // عمداً searchFieldHasText از متن ایستای فیلد
                            // پر نمی‌شود: فیلدی که فقط متن نگه داشته (متن
                            // قبلی یا hint گزارش‌شده به‌صورت متن) شاهد
                            // تعامل نیست — اسکرول از کنار نوار جستجو
                            // هرگز بلاک نمی‌کند (گزارش سید ۲۰۲۶-۱۱-۲۰).
                            // شاهد تایپ فقط از رویداد TYPE_VIEW_TEXT_CHANGED
                            // می‌آید (readTextChangedSectionSignals).
                        }
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
            searchFieldFocused = searchFieldFocused,
            searchFieldHasText = searchFieldHasText,
            selectedTexts = selectedTexts,
            selectedContentDescriptions = selectedDescriptions,
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

        /** سقف گره‌های خوانده‌شده در هر پیمایش درخت (جلوگیری از هنگ). */
        private const val MAX_NODES = 800

        /** سقف گره‌های پیمایش زیردرخت برای یافتن فیلد جستجو (کلیک/فوکوس). */
        private const val MAX_SEARCH_SUBTREE_NODES = 128
    }
}
