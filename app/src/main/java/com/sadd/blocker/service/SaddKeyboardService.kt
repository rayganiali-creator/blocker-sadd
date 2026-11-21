package com.sadd.blocker.service

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.room.Room
import com.sadd.blocker.MainActivity
import com.sadd.blocker.core.KeyboardBlocker
import com.sadd.blocker.core.KeyboardLayouts
import com.sadd.blocker.core.KeyboardTheme
import com.sadd.blocker.core.KeyboardThemes
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.data.db.AppDatabase
import com.sadd.blocker.data.db.MIGRATION_1_2
import com.sadd.blocker.data.db.MIGRATION_2_3
import com.sadd.blocker.data.db.MIGRATION_3_4
import com.sadd.blocker.data.db.MIGRATION_4_5
import com.sadd.blocker.data.db.MIGRATION_5_6
import com.sadd.blocker.data.repository.RoomBlockEventRepository
import com.sadd.blocker.data.repository.RoomKeywordRepository
import com.sadd.blocker.data.repository.RoomSessionRepository
import com.sadd.blocker.domain.model.BlockEventKind
import com.sadd.blocker.domain.repository.BlockEventRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * «کیبورد هوشمند» سد — کیبورد خود سد (درخواست سید ۲۰۲۶-۱۱-۲۱).
 *
 * مثل هر کیبوردی تایپ می‌کند (فارسی، انگلیسی با شیفت، صفحه اعداد و
 * نمادها، بک‌اسپیس با تکرار، فاصله، اینتر). بالای کلیدها یک نوار
 * ابزار باریک به سبک کیبورد سامسونگ است: چرخ‌دنده تنظیمات (بازکردن
 * سد روی تب «کیبورد هوشمند»)، دکمه رنگ (چرخه تم‌های رنگی کیبورد،
 * انتخاب در DataStore ذخیره می‌شود) و کلید تعویض زبان فارسی/انگلیسی.
 * وسط سشن فعال، بعد از هر commit تصمیم خالص [KeyboardBlocker.decide]
 * گرفته می‌شود: اگر کلمه‌ای که همین الان تمام شده (فاصله/علائم/ارسال)
 * کلمه بلاک‌شده باشد، همان لحظه از فیلد پاک می‌شود — بدون صفحه بلاک
 * و بدون تاخیر تنظیم‌شده — و یک رویداد بلاک با همان نوع مسیر
 * Accessibility (KEYWORD_TYPED) ثبت می‌شود تا در «بلاک امروز» شمرده
 * شود. خارج از سشن هیچ چکی نیست و تایپ عادی است.
 *
 * Fail-open بالاتر از همه: هر استثنایی در چک بلاک، تایپ را خراب
 * نمی‌کند. متن خود سد (پکیج خودمان) و فیلدهای رمز عبور هرگز چک
 * نمی‌شوند — رمز نباید دست بخورد.
 *
 * وابستگی‌ها دستی ساخته می‌شوند (بدون Hilt): تزریق Hilt به
 * InputMethodService کلاسیک‌ترین دلیل کرش کیبوردهاست (گزارش سید
 * ۲۰۲۶-۱۱-۲۱: «کیبورده کار نمیکنه») — سیستم IME را خودش می‌سازد و
 * اگر تزریق در onCreate بیفتد، کیبورد اصلاً بالا نمی‌آید. اینجا
 * Room و Repositoryها با lazy از applicationContext ساخته می‌شوند و
 * ساخت نما هم در try/catch است: اگر ساخت کامل کیبورد افتاد، یک
 * کیبورد حداقلی (فاصله/بک‌اسپیس/اینتر) برمی‌گردد تا تایپ هرگز نخوابد.
 *
 * هماهنگی با BlockAccessibilityService: وقتی این کیبورد، کیبورد
 * فعلی سیستم است، سرویس Accessibility بخش کلمه‌ای متن تایپ‌شده را
 * رد می‌کند (تا کلمه دو بار هندل نشود)؛ بلاک اپ/بخش و دامنه مرورگر
 * سر جایشان می‌مانند.
 *
 * محدودیت صادقانه: اندروید اجازه نمی‌دهد سد کیبورد قبلی را غیرفعال
 * کند یا جلوی برگشتن به آن را بگیرد — فعال‌سازی و انتخاب این کیبورد
 * یک بار در تنظیمات سیستم با دست کاربر انجام می‌شود.
 */
class SaddKeyboardService : InputMethodService() {

    // وابستگی‌های دستی (بدون Hilt) — فقط وقتی لازم شدند ساخته می‌شوند
    // و هر خطایی در ساخت/خواندنشان تایپ را خراب نمی‌کند (fail-open).
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "blocker.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()
    }
    private val sessionRepository: SessionRepository by lazy {
        RoomSessionRepository(database.sessionDao())
    }
    private val keywordRepository: KeywordRepository by lazy {
        RoomKeywordRepository(database.keywordRuleDao())
    }
    private val blockEventRepository: BlockEventRepository by lazy {
        RoomBlockEventRepository(database.blockEventDao())
    }
    private val settingsStore: SettingsStore by lazy {
        SettingsStore(applicationContext)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val repeatHandler = Handler(Looper.getMainLooper())

    /** عکس لحظه‌ای قوانین برای چک کیبورد؛ حداکثر ۵ ثانیه کش می‌شود. */
    private data class Snapshot(
        val sessionActive: Boolean,
        val keywords: List<String>,
        val loadedAtMs: Long,
    )

    @Volatile
    private var snapshot: Snapshot? = null

    /** تم رنگی فعلی کیبورد؛ از DataStore خوانده و با دکمه رنگ عوض می‌شود. */
    @Volatile
    private var kbTheme: KeyboardTheme = KeyboardThemes.DEFAULT

    private enum class KbLayout { FA, EN, SYMBOLS }

    private enum class KeyKind { TEXT, BACKSPACE, SHIFT, SPACE, ENTER, SYMBOLS, LETTERS }

    private data class KbKey(
        val label: String,
        val kind: KeyKind = KeyKind.TEXT,
        val weight: Float = 1f,
    )

    /** چیدمان فعلی روی صفحه. */
    private var layout: KbLayout = KbLayout.FA

    /** آخرین چیدمان حروفی (فارسی/انگلیسی) — کلید «اب» از نمادها به همین برمی‌گردد. */
    private var letterLayout: KbLayout = KbLayout.FA

    /** شیفت انگلیسی (تاگل ساده: روشن/خاموش). */
    private var capsOn: Boolean = false

    private var root: LinearLayout? = null

    private val repeatRunnable = object : Runnable {
        override fun run() {
            doBackspace()
            repeatHandler.postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    // ---------- چرخه حیات IME ----------

    override fun onCreateInputView(): View {
        return try {
            val container = LinearLayout(this)
            container.orientation = LinearLayout.VERTICAL
            container.setBackgroundColor(kbTheme.background)
            container.setPadding(dp(4), dp(6), dp(4), dp(6))
            root = container
            rebuildRows()
            container
        } catch (_: Exception) {
            // fail-open: ساخت کامل کیبورد افتاد — کیبورد حداقلی برگردد
            // تا تایپ هرگز نخوابد.
            fallbackInputView()
        }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        try {
            val cls = info.inputType and InputType.TYPE_MASK_CLASS
            layout = if (cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE) {
                KbLayout.SYMBOLS
            } else {
                letterLayout
            }
            rebuildRows()
            warmSnapshot()
        } catch (_: Exception) {
            // fail-open: کیبورد با چیدمان قبلی می‌ماند.
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        repeatHandler.removeCallbacks(repeatRunnable)
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        repeatHandler.removeCallbacks(repeatRunnable)
        scope.cancel()
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    // ---------- ساخت نما ----------

    private fun rebuildRows() {
        val container = root ?: return
        try {
            container.setBackgroundColor(kbTheme.background)
            container.removeAllViews()
            val direction = if (layout == KbLayout.FA) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }
            container.layoutDirection = direction
            container.addView(makeToolbar())
            for (rowKeys in rowsFor(layout)) {
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.layoutDirection = direction
                for (key in rowKeys) {
                    row.addView(makeKey(key))
                }
                container.addView(row)
            }
        } catch (_: Exception) {
            // fail-open: نمای قبلی می‌ماند.
        }
    }

    /**
     * نوار ابزار باریک بالای کلیدها (به سبک کیبورد سامسونگ): چرخ‌دنده
     * تنظیمات، دکمه رنگ (چرخه تم‌ها) و تعویض زبان. همیشه چپ‌به‌راست
     * است تا جای دکمه‌ها ثابت بماند؛ هر اکشن fail-open است.
     */
    private fun makeToolbar(): LinearLayout {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.layoutDirection = View.LAYOUT_DIRECTION_LTR
        bar.setBackgroundColor(kbTheme.toolbarBackground)
        bar.setPadding(dp(2), dp(2), dp(2), dp(2))

        val settingsBtn = makeToolbarButton("⚙", "تنظیمات کیبورد هوشمند")
        settingsBtn.setOnClickListener { openKeyboardSettings() }
        bar.addView(settingsBtn)

        val themeBtn = makeToolbarButton("🎨", "رنگ کیبورد")
        themeBtn.setOnClickListener { cycleTheme() }
        bar.addView(themeBtn)

        // فاصله‌گیر تا کلید زبان به انتهای نوار برود.
        val spacer = View(this)
        spacer.layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f)
        bar.addView(spacer)

        val langLabel = if (letterLayout == KbLayout.FA) "EN" else "فا"
        val langBtn = makeToolbarButton(langLabel, "تعویض زبان")
        langBtn.setOnClickListener { switchLanguage() }
        bar.addView(langBtn)

        return bar
    }

    private fun makeToolbarButton(label: String, description: String): Button {
        val btn = Button(this)
        btn.text = label
        btn.contentDescription = description
        btn.isFocusable = false
        btn.isFocusableInTouchMode = false
        btn.setTextColor(kbTheme.toolbarText)
        btn.textSize = 15f
        btn.background = keyBackground(kbTheme.toolbarBackground)
        btn.setPadding(dp(10), 0, dp(10), 0)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(38),
        )
        params.setMargins(dp(2), dp(1), dp(2), dp(1))
        btn.layoutParams = params
        return btn
    }

    private fun rowsFor(current: KbLayout): List<List<KbKey>> {
        return when (current) {
            KbLayout.FA -> listOf(
                KeyboardLayouts.PERSIAN_ROWS[0].map { KbKey(it) },
                KeyboardLayouts.PERSIAN_ROWS[1].map { KbKey(it) },
                KeyboardLayouts.PERSIAN_ROWS[2].map { KbKey(it) } +
                    KbKey("⌫", KeyKind.BACKSPACE),
                listOf(
                    KbKey("۱۲۳", KeyKind.SYMBOLS),
                    KbKey("،"),
                    KbKey("", KeyKind.SPACE, weight = 3f),
                    KbKey("."),
                    KbKey(enterLabel(), KeyKind.ENTER),
                ),
            )
            KbLayout.EN -> listOf(
                KeyboardLayouts.ENGLISH_ROWS[0].map { KbKey(it) },
                KeyboardLayouts.ENGLISH_ROWS[1].map { KbKey(it) },
                listOf(KbKey("⇧", KeyKind.SHIFT)) +
                    KeyboardLayouts.ENGLISH_ROWS[2].map { KbKey(it) } +
                    KbKey("⌫", KeyKind.BACKSPACE),
                listOf(
                    KbKey("123", KeyKind.SYMBOLS),
                    KbKey(","),
                    KbKey("", KeyKind.SPACE, weight = 3f),
                    KbKey("."),
                    KbKey(enterLabel(), KeyKind.ENTER),
                ),
            )
            KbLayout.SYMBOLS -> listOf(
                KeyboardLayouts.SYMBOL_ROWS[0].map { KbKey(it) },
                KeyboardLayouts.SYMBOL_ROWS[1].map { KbKey(it) },
                KeyboardLayouts.SYMBOL_ROWS[2].map { KbKey(it) },
                KeyboardLayouts.SYMBOL_ROWS[3].map { KbKey(it) },
                listOf(
                    KbKey("اب", KeyKind.LETTERS),
                    KbKey("."),
                    KbKey("", KeyKind.SPACE, weight = 3f),
                    KbKey(","),
                    KbKey("⌫", KeyKind.BACKSPACE),
                    KbKey(enterLabel(), KeyKind.ENTER),
                ),
            )
        }
    }

    private fun makeKey(key: KbKey): Button {
        val btn = Button(this)
        btn.text = displayLabel(key)
        btn.isFocusable = false
        btn.isFocusableInTouchMode = false
        btn.setTextColor(kbTheme.keyText)
        btn.textSize = if (key.kind == KeyKind.TEXT && key.label.length == 1) 19f else 14f
        btn.background = keyBackground(kbTheme.keyBackground)
        btn.setPadding(0, 0, 0, 0)
        val params = LinearLayout.LayoutParams(0, dp(50), key.weight)
        params.setMargins(dp(2), dp(2), dp(2), dp(2))
        btn.layoutParams = params
        if (key.kind == KeyKind.BACKSPACE) {
            btn.setOnTouchListener { _, event -> onBackspaceTouch(event) }
        } else {
            btn.setOnClickListener { onKey(key) }
        }
        return btn
    }

    private fun displayLabel(key: KbKey): String {
        if (layout == KbLayout.EN && capsOn && key.kind == KeyKind.TEXT && key.label.length == 1) {
            return key.label.uppercase()
        }
        return key.label
    }

    private fun keyBackground(color: Int): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = dp(8).toFloat()
        drawable.setColor(color)
        return drawable
    }

    private fun enterLabel(): String {
        val info = currentInputEditorInfo ?: return "⏎"
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        return when (action) {
            EditorInfo.IME_ACTION_SEND -> "ارسال"
            EditorInfo.IME_ACTION_DONE -> "تمام"
            EditorInfo.IME_ACTION_SEARCH -> "جستجو"
            EditorInfo.IME_ACTION_GO -> "برو"
            EditorInfo.IME_ACTION_NEXT -> "بعدی"
            else -> "⏎"
        }
    }

    /**
     * کیبورد حداقلی وقتی ساخت کامل نما می‌افتد: فقط فاصله، بک‌اسپیس و
     * اینتر — تایپ هیچ‌وقت نمی‌خوابد.
     */
    private fun fallbackInputView(): View {
        val container = LinearLayout(this)
        container.orientation = LinearLayout.HORIZONTAL
        container.setBackgroundColor(kbTheme.background)
        val space = Button(this)
        space.text = "فاصله"
        space.setOnClickListener { commit(" ") }
        space.layoutParams = LinearLayout.LayoutParams(0, dp(50), 2f)
        val backspace = Button(this)
        backspace.text = "⌫"
        backspace.setOnClickListener { doBackspace() }
        backspace.layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f)
        val enter = Button(this)
        enter.text = "⏎"
        enter.setOnClickListener { onEnter() }
        enter.layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f)
        container.addView(space)
        container.addView(backspace)
        container.addView(enter)
        return container
    }

    // ---------- رفتار کلیدها و نوار ابزار ----------

    private fun onKey(key: KbKey) {
        try {
            when (key.kind) {
                KeyKind.TEXT -> {
                    val text = if (layout == KbLayout.EN && capsOn) {
                        key.label.uppercase()
                    } else {
                        key.label
                    }
                    commit(text)
                }
                KeyKind.SPACE -> commit(" ")
                KeyKind.ENTER -> onEnter()
                KeyKind.SHIFT -> {
                    capsOn = !capsOn
                    rebuildRows()
                }
                KeyKind.SYMBOLS -> {
                    layout = KbLayout.SYMBOLS
                    rebuildRows()
                }
                KeyKind.LETTERS -> {
                    layout = letterLayout
                    rebuildRows()
                }
                KeyKind.BACKSPACE -> doBackspace()
            }
        } catch (_: Exception) {
            // fail-open: خطای یک کلید، کیبورد را نمی‌خواباند.
        }
    }

    /** تعویض زبان از نوار ابزار (تنها جای تعویض زبان — ردیف پایین دیگر ندارد). */
    private fun switchLanguage() {
        try {
            letterLayout = if (letterLayout == KbLayout.FA) KbLayout.EN else KbLayout.FA
            layout = letterLayout
            rebuildRows()
        } catch (_: Exception) {
            // fail-open
        }
    }

    /** چرخه تم رنگی کیبورد؛ انتخاب همان لحظه اعمال و در DataStore ذخیره می‌شود. */
    private fun cycleTheme() {
        try {
            kbTheme = KeyboardThemes.next(kbTheme)
            rebuildRows()
            val key = kbTheme.key
            scope.launch {
                try {
                    settingsStore.setKeyboardTheme(key)
                } catch (_: Exception) {
                    // fail-open: تم روی صفحه عوض شده، ذخیره نشدنش تایپ را خراب نمی‌کند.
                }
            }
        } catch (_: Exception) {
            // fail-open
        }
    }

    /** چرخ‌دنده نوار ابزار: سد را روی تب «کیبورد هوشمند» باز می‌کند. */
    private fun openKeyboardSettings() {
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(MainActivity.EXTRA_OPEN_ROUTE, MainActivity.ROUTE_SMART_KEYBOARD)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                Toast.makeText(
                    this,
                    "تنظیمات باز نشد: ${e.message ?: e.javaClass.simpleName}",
                    Toast.LENGTH_SHORT,
                ).show()
            } catch (_: Exception) {
                // fail-open
            }
        }
    }

    private fun commit(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
        checkBlocking()
    }

    private fun onBackspaceTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                doBackspace()
                repeatHandler.removeCallbacks(repeatRunnable)
                repeatHandler.postDelayed(repeatRunnable, REPEAT_INITIAL_DELAY_MS)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                repeatHandler.removeCallbacks(repeatRunnable)
            }
        }
        return true
    }

    private fun doBackspace() {
        try {
            val ic = currentInputConnection ?: return
            val selected = ic.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                ic.commitText("", 1)
            } else {
                ic.deleteSurroundingText(1, 0)
            }
        } catch (_: Exception) {
            // fail-open
        }
    }

    private fun onEnter() {
        try {
            val ic = currentInputConnection ?: return
            val info = currentInputEditorInfo
            val action = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
            val multiline = ((info?.inputType ?: 0) and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
            if (!multiline && action != EditorInfo.IME_ACTION_NONE &&
                action != EditorInfo.IME_ACTION_UNSPECIFIED
            ) {
                // قبل از ارسال/تایید، کلمه جاری را تمام‌شده حساب کن تا
                // کلمه بلاک‌شده بدون فاصله هم پاک شود.
                val before = ic.getTextBeforeCursor(TEXT_WINDOW, 0)?.toString()
                val snap = snapshot
                if (!before.isNullOrEmpty() && snap != null && !isExemptEditor()) {
                    val range = KeyboardBlocker.decide(before + " ", snap.keywords, snap.sessionActive)
                    if (range != null) {
                        applyDelete(range.first, before.length)
                    }
                }
                ic.performEditorAction(action)
            } else {
                ic.commitText("\n", 1)
                checkBlocking()
            }
        } catch (_: Exception) {
            // fail-open
        }
    }

    // ---------- چک بلاک (fail-open) ----------

    /** فیلدهایی که هرگز چک نمی‌شوند: متن خود سد و فیلدهای رمز عبور. */
    private fun isExemptEditor(): Boolean {
        val info = currentInputEditorInfo ?: return false
        if (info.packageName == packageName) return true
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }

    private fun checkBlocking() {
        try {
            if (isExemptEditor()) return
            val ic = currentInputConnection ?: return
            val before = ic.getTextBeforeCursor(TEXT_WINDOW, 0)?.toString() ?: return
            if (before.isEmpty()) return
            val snap = snapshot
            if (snap != null && System.currentTimeMillis() - snap.loadedAtMs < SNAPSHOT_TTL_MS) {
                applyDecision(before, snap)
            } else {
                scope.launch {
                    val fresh = loadSnapshot() ?: return@launch
                    snapshot = fresh
                    // متن تازه خوانده می‌شود؛ اگر کاربر در این فاصله
                    // تایپ کرده باشد، تصمیم روی متن فعلی گرفته می‌شود.
                    val icNow = currentInputConnection ?: return@launch
                    val beforeNow = icNow.getTextBeforeCursor(TEXT_WINDOW, 0)?.toString() ?: return@launch
                    applyDecision(beforeNow, fresh)
                }
            }
        } catch (_: Exception) {
            // fail-open: خطای چک بلاک، تایپ را خراب نمی‌کند.
        }
    }

    private fun applyDecision(textBefore: String, snap: Snapshot) {
        try {
            val range = KeyboardBlocker.decide(textBefore, snap.keywords, snap.sessionActive) ?: return
            applyDelete(range.first, textBefore.length)
        } catch (_: Exception) {
            // fail-open
        }
    }

    /** حذف از [start] تا مکان‌نما، ثبت رویداد بلاک و پیام کوتاه. */
    private fun applyDelete(start: Int, textBeforeLength: Int) {
        val deleteCount = textBeforeLength - start
        if (deleteCount <= 0) return
        val ic = currentInputConnection ?: return
        ic.deleteSurroundingText(deleteCount, 0)
        scope.launch {
            try {
                blockEventRepository.log(
                    BlockEventKind.KEYWORD_TYPED,
                    "keyboard",
                    System.currentTimeMillis(),
                )
            } catch (_: Exception) {
                // fail-open: ثبت رویداد خراب شد، حذف انجام شده است.
            }
        }
        try {
            Toast.makeText(this, "کلمه بلاک‌شده حذف شد", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            // fail-open
        }
    }

    private fun warmSnapshot() {
        scope.launch {
            // تم رنگی ذخیره‌شده هم همین‌جا خوانده می‌شود؛ اگر با تم
            // فعلی فرق داشت، نما با تم تازه بازسازی می‌شود.
            try {
                val loaded = KeyboardThemes.themeByKey(settingsStore.keyboardTheme.first())
                if (loaded.key != kbTheme.key) {
                    kbTheme = loaded
                    rebuildRows()
                }
            } catch (_: Exception) {
                // fail-open: تم پیش‌فرض می‌ماند.
            }
            val fresh = loadSnapshot()
            if (fresh != null) snapshot = fresh
        }
    }

    private suspend fun loadSnapshot(): Snapshot? {
        return try {
            val now = System.currentTimeMillis()
            val session = sessionRepository.getActive(now)
            Snapshot(
                sessionActive = session != null,
                keywords = keywordRepository.list().filter { it.enabled }.map { it.rawText },
                loadedAtMs = now,
            )
        } catch (_: Exception) {
            // fail-open: اگر قوانین خوانده نشدند، تایپ عادی است.
            null
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val SNAPSHOT_TTL_MS = 5_000L
        private const val TEXT_WINDOW = 200
        private const val REPEAT_INITIAL_DELAY_MS = 400L
        private const val REPEAT_INTERVAL_MS = 55L
    }
}
