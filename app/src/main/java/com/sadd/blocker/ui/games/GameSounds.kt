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
     * ترکیدن حباب پلاستیکی (bubble wrap): کلیک تیز نویزی + افت سریع فرکانس
     * سینوس (۹۰۰→۲۰۰ هرتز) با پوش کاهشی تند؛ هر بار یکی از چند گونه با
     * زیروبمی کمی متفاوت پخش می‌شود تا کشیدن انگشت روی حباب‌ها طبیعی صدا بدهد.
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
        listOf(0.85, 0.95, 1.0, 1.1, 1.2).map { synthPlasticPop(it) }
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
     * صدای ترکیدن پلاستیک حباب‌دار: در ~۵ میلی‌ثانیه اول یک کلیک نویزی تیز
     * (صدای پاره‌شدن پلاستیک) و زیر آن سینوسی با افت سریع فرکانس از
     * ۹۰۰×[pitchFactor] تا ۲۰۰×[pitchFactor] هرتز و پوش کاهشی تند؛
     * مجموع حدود ۸۰ میلی‌ثانیه است، مثل ترکیدن واقعی حباب بسته‌بندی.
     */
    private fun synthPlasticPop(pitchFactor: Double): ShortArray {
        val durationMs = 80
        val count = SAMPLE_RATE * durationMs / 1000
        val samples = ShortArray(count)
        val noise = Random(1234)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = (900.0 - 700.0 * t) * pitchFactor
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val body = sin(phase) * exp(-5.5 * t)
            val click = if (i < 220) {
                (noise.nextDouble() * 2.0 - 1.0) * exp(-i / 55.0)
            } else {
                0.0
            }
            val value = (body * 0.8 + click * 0.6).coerceIn(-1.0, 1.0)
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
