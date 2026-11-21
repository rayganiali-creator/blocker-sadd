package com.sadd.blocker.service

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
import com.sadd.blocker.core.KeyboardBlocker
import com.sadd.blocker.domain.model.BlockEventKind
import com.sadd.blocker.domain.repository.BlockEventRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * «کیبورد هوشمند» سد — کیبورد خود سد (درخواست سید ۲۰۲۶-۱۱-۲۱).
 *
 * مثل هر کیبوردی تایپ می‌کند (فارسی، انگلیسی با شیفت، صفحه اعداد و
 * نمادها، بک‌اسپیس با تکرار، فاصله، اینتر، کلید تعویض زبان). وسط
 * سشن فعال، بعد از هر commit تصمیم خالص [KeyboardBlocker.decide]
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
 * هماهنگی با BlockAccessibilityService: وقتی این کیبورد، کیبورد
 * فعلی سیستم است، سرویس Accessibility بخش کلمه‌ای متن تایپ‌شده را
 * رد می‌کند (تا کلمه دو بار هندل نشود)؛ بلاک اپ/بخش و دامنه مرورگر
 * سر جایشان می‌مانند.
 *
 * محدودیت صادقانه: اندروید اجازه نمی‌دهد سد کیبورد قبلی را غیرفعال
 * کند یا جلوی برگشتن به آن را بگیرد — فعال‌سازی و انتخاب این کیبورد
 * یک بار در تنظیمات سیستم با دست کاربر انجام می‌شود.
 */
@AndroidEntryPoint
class SaddKeyboardService : InputMethodService() {

    @Inject
    lateinit var sessionRepository: SessionRepository

    @Inject
    lateinit var keywordRepository: KeywordRepository

    @Inject
    lateinit var blockEventRepository: BlockEventRepository

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

    private enum class KbLayout { FA, EN, SYMBOLS }

    private enum class KeyKind { TEXT, BACKSPACE, SHIFT, SPACE, ENTER, SYMBOLS, LETTERS, LANG }

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
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setBackgroundColor(KEYBOARD_BG)
        container.setPadding(dp(4), dp(6), dp(4), dp(6))
        root = container
        rebuildRows()
        return container
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
            container.removeAllViews()
            val direction = if (layout == KbLayout.FA) {
                View.LAYOUT_DIRECTION_RTL
            } else {
                View.LAYOUT_DIRECTION_LTR
            }
            container.layoutDirection = direction
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

    private fun rowsFor(current: KbLayout): List<List<KbKey>> {
        return when (current) {
            KbLayout.FA -> listOf(
                listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ").map { KbKey(it) },
                listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ").map { KbKey(it) },
                listOf("ظ", "ط", "ز", "ر", "ذ", "د", "پ", "و", "ئ").map { KbKey(it) } +
                    KbKey("⌫", KeyKind.BACKSPACE),
                listOf(
                    KbKey("۱۲۳", KeyKind.SYMBOLS),
                    KbKey("،"),
                    KbKey("", KeyKind.SPACE, weight = 3f),
                    KbKey("."),
                    KbKey("EN", KeyKind.LANG),
                    KbKey(enterLabel(), KeyKind.ENTER),
                ),
            )
            KbLayout.EN -> listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map { KbKey(it) },
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").map { KbKey(it) },
                listOf(KbKey("⇧", KeyKind.SHIFT)) +
                    listOf("z", "x", "c", "v", "b", "n", "m").map { KbKey(it) } +
                    KbKey("⌫", KeyKind.BACKSPACE),
                listOf(
                    KbKey("123", KeyKind.SYMBOLS),
                    KbKey(","),
                    KbKey("", KeyKind.SPACE, weight = 3f),
                    KbKey("."),
                    KbKey("فا", KeyKind.LANG),
                    KbKey(enterLabel(), KeyKind.ENTER),
                ),
            )
            KbLayout.SYMBOLS -> listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { KbKey(it) },
                listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")").map { KbKey(it) },
                listOf("-", "_", "=", "+", "[", "]", "{", "}", ":", ";").map { KbKey(it) },
                listOf("آ", "ء", "ـ", "؟", "،", "؛", "«", "»", "'", "\"", "/", "\\").map { KbKey(it) },
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
        btn.setTextColor(0xFFFFFFFF.toInt())
        btn.textSize = if (key.kind == KeyKind.TEXT && key.label.length == 1) 19f else 14f
        btn.background = keyBackground()
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

    private fun keyBackground(): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.shape = GradientDrawable.RECTANGLE
        drawable.cornerRadius = dp(8).toFloat()
        drawable.setColor(KEY_BG)
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

    // ---------- رفتار کلیدها ----------

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
                KeyKind.LANG -> {
                    letterLayout = if (letterLayout == KbLayout.FA) KbLayout.EN else KbLayout.FA
                    layout = letterLayout
                    rebuildRows()
                }
                KeyKind.BACKSPACE -> doBackspace()
            }
        } catch (_: Exception) {
            // fail-open: خطای یک کلید، کیبورد را نمی‌خواباند.
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
        private const val KEYBOARD_BG = 0xFF14142B.toInt()
        private const val KEY_BG = 0xFF2A2A4A.toInt()
    }
}
