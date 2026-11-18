package com.sadd.blocker.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.sadd.blocker.core.ProtectionGuardDetector

/**
 * پرده رهگیری نگهبان — لایه TYPE_ACCESSIBILITY_OVERLAY روی صفحه محافظت‌شده.
 *
 * وقتی نگهبان صفحه اطلاعات برنامه «سد»، دیالوگ حذف نصب/توقف اجباری، یا
 * صفحه غیرفعال‌سازی مدیر دستگاه را تشخیص می‌دهد، فقط BACK زدن کافی نیست:
 * یک ضربه خیلی سریع روی «حذف نصب»/«تایید» می‌تواند قبل از BACK ثبت شود.
 * این پرده همان لحظه کل صفحه را می‌پوشاند و لمس‌ها را می‌خورد — ضربه روی
 * دکمه زیرین به دکمه نمی‌رسد، به پرده می‌رسد. روی پرده یک دکمه
 * «بازگشایی محافظت» هست که صفحه بازگشایی Strict (تایپ متن یا انتظار)
 * را باز می‌کند؛ بعد از بازگشایی موفق، پنجره ۵دقیقه‌ای باز می‌شود و پرده
 * برداشته می‌شود. اگر کاربر با دکمه بازگشت سیستم از صفحه محافظت‌شده
 * بیرون برود، سرویس در رویداد بعدی پرده را برمی‌دارد.
 *
 * قوانین سفت:
 * - پرده فقط وقتی نشان داده می‌شود که نگهبان مسلح باشد و صفحه
 *   محافظت‌شده واقعاً جلوی چشم باشد؛ برای صفحه‌های خود «سد» هرگز نمی‌آید
 *   (سرویس رویدادهای پکیج خود را از اول رد می‌کند).
 * - TYPE_ACCESSIBILITY_OVERLAY فقط از داخل AccessibilityService مجاز است
 *   و نیازی به مجوز SYSTEM_ALERT_WINDOW ندارد.
 * - FLAG_NOT_FOCUSABLE یعنی دکمه بازگشت سیستم همچنان به پنجره زیرین
 *   (تنظیمات) می‌رسد؛ پرده فقط لمس را می‌خورد، نه بازگشت را.
 * - همه‌چیز fail-open: هر خطایی یعنی پرده نشان داده نمی‌شود، نه قفل گوشی.
 */
class GuardOverlay(context: Context) {

    private val appContext: Context = context.applicationContext
    private val windowManager: WindowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var overlayView: View? = null

    @Volatile
    private var currentHit: ProtectionGuardDetector.GuardHit? = null

    val isShowing: Boolean get() = overlayView != null

    /**
     * نشان دادن پرده روی صفحه محافظت‌شده. اگر همین صفحه همین حالا پرده
     * دارد، دوباره ساخته نمی‌شود. [onUnlockClick] با ضربه روی دکمه
     * «بازگشایی محافظت» صدا زده می‌شود.
     */
    fun show(hit: ProtectionGuardDetector.GuardHit, reason: String, onUnlockClick: () -> Unit) {
        mainHandler.post {
            try {
                if (overlayView != null && currentHit == hit) return@post
                hideNow()
                val view = buildView(reason, onUnlockClick)
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT,
                )
                windowManager.addView(view, params)
                overlayView = view
                currentHit = hit
            } catch (_: Exception) {
                // fail-open: اگر پرده اضافه نشد، نگهبان با BACK ادامه می‌دهد.
                overlayView = null
                currentHit = null
            }
        }
    }

    /** برداشتن پرده، اگر هست. */
    fun hide() {
        mainHandler.post { hideNow() }
    }

    private fun hideNow() {
        val view = overlayView ?: return
        overlayView = null
        currentHit = null
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {
            // نادیده: پرده از قبل برداشته شده است.
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            appContext.resources.displayMetrics,
        ).toInt()
    }

    private fun buildView(reason: String, onUnlockClick: () -> Unit): View {
        val root = FrameLayout(appContext).apply {
            // لمس را می‌خورد: ضربه روی دکمه زیرین (حذف نصب/تایید) به پرده می‌رسد.
            isClickable = true
            isFocusable = false
            setBackgroundColor(Color.parseColor("#CC071224"))
            setOnClickListener { /* فقط لمس خورده می‌شود؛ کاری نمی‌کند. */ }
        }
        val card = LinearLayout(appContext).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#131E33"))
                cornerRadius = dp(28).toFloat()
            }
        }
        val title = TextView(appContext).apply {
            text = "محافظت فعال است"
            setTextColor(Color.parseColor("#7CFFD9"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val body = TextView(appContext).apply {
            text = reason
            setTextColor(Color.parseColor("#F7F8FA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(20))
        }
        val unlockButton = Button(appContext).apply {
            text = "بازگشایی محافظت"
            setTextColor(Color.parseColor("#071224"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#7CFFD9"))
                cornerRadius = dp(28).toFloat()
            }
            setPadding(dp(24), dp(12), dp(24), dp(12))
            setOnClickListener { onUnlockClick() }
        }
        val hint = TextView(appContext).apply {
            text = "برای ادامه، اول محافظت را با تایپ متن یا انتظار بازگشایی کن."
            setTextColor(Color.parseColor("#B9C6DA"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        card.addView(title)
        card.addView(body)
        card.addView(unlockButton)
        card.addView(hint)
        val cardParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        ).apply {
            leftMargin = dp(24)
            rightMargin = dp(24)
        }
        root.addView(card, cardParams)
        return root
    }
}
