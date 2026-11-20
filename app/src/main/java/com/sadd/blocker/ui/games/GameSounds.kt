package com.sadd.blocker.ui.games

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * صداهای کوتاه بازی‌ها، ساخته‌شده در لحظه با AudioTrack — بدون هیچ فایل
 * صوتی و بدون وابستگی تازه. همه صداها سینوسی با پوش کاهشی‌اند و صدای
 * ملایمی دارند. هر خطای صوتی بی‌صدا رد می‌شود (fail-open): بازی هرگز
 * به‌خاطر صدا کرش نمی‌کند.
 */
object GameSounds {

    private const val SAMPLE_RATE = 44100
    private const val AMPLITUDE = 0.22

    /**
     * ترکیدن بابل‌رپ بسته‌بندی (همان پلاستیک حباب‌داری که دور محصولات
     * می‌پیچند — گزارش سید ۲۰۲۶-۱۱-۲۰): پارگی غشا، نه بوق موسیقایی و
     * نه افت فرکانس طولانی. هر بار یکی از چند گونه با فرکانس مرکزی کمی
     * متفاوت پخش می‌شود تا کشیدن انگشت روی حباب‌ها طبیعی صدا بدهد.
     */
    fun pop() {
        play(popBuffers[Random.nextInt(popBuffers.size)])
    }

    /** ضربه/حرکت نرم: سینوس کوتاه ۶۰۰ هرتز. */
    fun blip() {
        play(blipBuffer)
    }

    /** نشستن/افتادن: سینوس بم ۱۲۰ هرتز. */
    fun thud() {
        play(thudBuffer)
    }

    /** موفقیت: دو نت هم‌زمان (۸۸۰ + ۱۳۲۰ هرتز) با پوش کاهشی بلندتر. */
    fun chime() {
        play(chimeBuffer)
    }

    /** نت تک با فرکانس داده‌شده (برای سلول‌های «پژواک» و تغییر فاز «نفس بکش»). */
    fun tone(freqHz: Float) {
        play(toneBuffer(freqHz))
    }

    private val popBuffers: List<ShortArray> by lazy {
        // پنج گونه: فرکانس مرکزی فیلتر میان‌گذر ±۱۵٪ دور ۱۲۰۰ هرتز.
        listOf(0.85, 0.925, 1.0, 1.075, 1.15).map { synthBubbleWrapPop(it) }
    }
    private val blipBuffer: ShortArray by lazy { synthTone(600f, 60) }
    private val thudBuffer: ShortArray by lazy { synthTone(120f, 130) }
    private val chimeBuffer: ShortArray by lazy { synthChime() }
    private val toneCache = HashMap<Int, ShortArray>()

    private fun toneBuffer(freqHz: Float): ShortArray {
        val key = freqHz.toInt()
        val cached = toneCache[key]
        if (cached != null) {
            return cached
        }
        val buffer = synthTone(freqHz, 180)
        toneCache[key] = buffer
        return buffer
    }

    /**
     * صدای ترکیدن بابل‌رپ بسته‌بندی — پارگی غشای پلاستیکی:
     * - کلیک نویزی ۳ تا ۵ میلی‌ثانیه اول (لحظه پاره‌شدن غشا)؛
     * - نویز میان‌گذرشده (فیلتر biquad، مرکز ~۱۲۰۰×[centerFactor]
     *   هرتز، یعنی بازه ۱۰۰۰ تا ۱۴۰۰ هرتز) با پوش کاهشی نمایی سریع
     *   (~۴۰ میلی‌ثانیه) — بدنه اصلی «پق» ترکیدن؛
     * - سینوس بم کوتاه (~۲۵۰×[centerFactor] هرتز، بازه ۲۲۰ تا ۲۸۰)
     *   با افت ~۲۵ میلی‌ثانیه — تنه هوا که از حباب بیرون می‌زند.
     * هیچ افت فرکانس (pitch sweep) طولانی‌ای نیست؛ مجموع ~۸۰ میلی‌ثانیه
     * است، مثل ترکیدن واقعی یک حباب بابل‌رپ.
     */
    private fun synthBubbleWrapPop(centerFactor: Double): ShortArray {
        val durationMs = 80
        val count = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(count)
        val noise = Random(1234)
        // فیلتر میان‌گذر RBJ (بهره قله 0dB): مرکز و Q ثابت هر گونه.
        val centerHz = 1200.0 * centerFactor
        val q = 1.2
        val w0 = 2.0 * PI * centerHz / SAMPLE_RATE
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = kotlin.math.cos(w0)
        val b0 = alpha / (1.0 + alpha)
        val b2 = -alpha / (1.0 + alpha)
        val a1 = -2.0 * cosW0 / (1.0 + alpha)
        val a2 = (1.0 - alpha) / (1.0 + alpha)
        val bodyHz = 250.0 * centerFactor
        var x1 = 0.0
        var x2 = 0.0
        var y1 = 0.0
        var y2 = 0.0
        val clickSamples = SAMPLE_RATE * 4 / 1000 // ~۴ میلی‌ثانیه
        for (i in 0 until count) {
            val ms = i * 1000.0 / SAMPLE_RATE
            val white = noise.nextDouble() * 2.0 - 1.0
            // میان‌گذر روی نویز سفید + پوش نمایی سریع (~۴۰ms).
            val filtered = b0 * white + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = white
            y2 = y1
            y1 = filtered
            val membrane = filtered * exp(-ms / 40.0)
            // کلیک پارگی فقط در چند میلی‌ثانیه اول.
            val click = if (i < clickSamples) {
                white * exp(-i / 40.0)
            } else {
                0.0
            }
            // تنه بم کوتاه (~۲۵ms افت).
            val body = sin(2.0 * PI * bodyHz * i / SAMPLE_RATE) * exp(-ms / 22.0)
            val value = (membrane * 1.4 + click * 0.7 + body * 0.45).coerceIn(-1.0, 1.0)
            samples[i] = (value * AMPLITUDE * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    private fun synthTone(freqHz: Float, durationMs: Int): ShortArray {
        val count = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val envelope = exp(-3.0 * t)
            val value = sin(2.0 * PI * freqHz * i / SAMPLE_RATE) * envelope
            samples[i] = (value * AMPLITUDE * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    private fun synthChime(): ShortArray {
        val durationMs = 320
        val count = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val envelope = exp(-3.5 * t)
            val value = (
                sin(2.0 * PI * 880.0 * i / SAMPLE_RATE) * 0.7 +
                    sin(2.0 * PI * 1320.0 * i / SAMPLE_RATE) * 0.3
                ) * envelope
            samples[i] = (value * AMPLITUDE * Short.MAX_VALUE).toInt().toShort()
        }
        return samples
    }

    /** پخش یک بافر PCM؛ هر خطا بی‌صدا رد می‌شود و ترک بعد از پایان آزاد می‌شود. */
    private fun play(samples: ShortArray) {
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.setNotificationMarkerPosition(samples.size)
            track.setPlaybackPositionUpdateListener(
                object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(track: AudioTrack?) {
                        runCatching { track?.release() }
                    }

                    override fun onPeriodicNotification(track: AudioTrack?) {
                        // عمداً خالی: فقط پایان پخش لازم است.
                    }
                },
            )
            track.play()
        }
    }
}
