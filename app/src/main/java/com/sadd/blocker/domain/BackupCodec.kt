package com.sadd.blocker.domain

import com.sadd.blocker.core.KeywordMatcher
import com.sadd.blocker.core.PersianNormalizer
import com.sadd.blocker.domain.model.BlockedApp
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.model.Schedule

/**
 * بکاپ و ریستور — خالص Kotlin، بدون وابستگی اندروید.
 * فرمت JSON نسخه‌دار (version=1) با writer و parser دست‌ساز؛
 * هیچ کتابخانه JSON خارجی لازم نیست و خروجی/ورودی کاملاً تست‌پذیر است.
 * decode سخت‌گیرانه است: JSON خراب یا نسخه ناشناس رد می‌شود،
 * ولی آیتم‌های معیوب تکی فقط skip می‌شوند (با شمارش) تا یک آیتم خراب
 * کل ریستور را خراب نکند.
 */
data class BackupData(
    val apps: List<BlockedApp>,
    val websites: List<BlockedWebsite>,
    val keywords: List<KeywordRule>,
    val schedules: List<Schedule>,
)

sealed class BackupDecodeResult {
    data class Success(val data: BackupData, val skipped: Int, val notes: List<String>) : BackupDecodeResult()
    data class Failure(val reason: String) : BackupDecodeResult()
}

/** مقادیر JSON — فقط همین زیرمجموعه استفاده می‌شود. */
sealed class JsonValue {
    data class Obj(val map: Map<String, JsonValue>) : JsonValue()
    data class Arr(val items: List<JsonValue>) : JsonValue()
    data class Str(val value: String) : JsonValue()
    data class Num(val value: Long) : JsonValue()
    data class Bool(val value: Boolean) : JsonValue()
    object Null : JsonValue()
}

/** تجزیه‌کننده بازگشتی JSON — کوچک و سخت‌گیرانه. */
class JsonParser(private val input: String) {
    private var pos = 0

    fun parse(): JsonValue? {
        skipWs()
        val v = parseValue()
        skipWs()
        if (v == null || pos != input.length) return null
        return v
    }

    private fun skipWs() {
        while (pos < input.length && input[pos].isWhitespace()) pos++
    }

    private fun parseValue(): JsonValue? {
        if (pos >= input.length) return null
        val c = input[pos]
        if (c == '{') return parseObject()
        if (c == '[') return parseArray()
        if (c == '"') {
            val s = parseString() ?: return null
            return JsonValue.Str(s)
        }
        if (c == 't') {
            if (input.startsWith("true", pos)) {
                pos += 4
                return JsonValue.Bool(true)
            }
            return null
        }
        if (c == 'f') {
            if (input.startsWith("false", pos)) {
                pos += 5
                return JsonValue.Bool(false)
            }
            return null
        }
        if (c == 'n') {
            if (input.startsWith("null", pos)) {
                pos += 4
                return JsonValue.Null
            }
            return null
        }
        return parseNumber()
    }

    private fun parseObject(): JsonValue? {
        pos++ // {
        val map = LinkedHashMap<String, JsonValue>()
        skipWs()
        if (pos < input.length && input[pos] == '}') {
            pos++
            return JsonValue.Obj(map)
        }
        while (true) {
            skipWs()
            val key = parseString() ?: return null
            skipWs()
            if (pos >= input.length || input[pos] != ':') return null
            pos++
            skipWs()
            val value = parseValue() ?: return null
            map[key] = value
            skipWs()
            if (pos >= input.length) return null
            if (input[pos] == ',') {
                pos++
                continue
            }
            if (input[pos] == '}') {
                pos++
                return JsonValue.Obj(map)
            }
            return null
        }
    }

    private fun parseArray(): JsonValue? {
        pos++ // [
        val items = ArrayList<JsonValue>()
        skipWs()
        if (pos < input.length && input[pos] == ']') {
            pos++
            return JsonValue.Arr(items)
        }
        while (true) {
            skipWs()
            val value = parseValue() ?: return null
            items.add(value)
            skipWs()
            if (pos >= input.length) return null
            if (input[pos] == ',') {
                pos++
                continue
            }
            if (input[pos] == ']') {
                pos++
                return JsonValue.Arr(items)
            }
            return null
        }
    }

    private fun parseString(): String? {
        if (pos >= input.length || input[pos] != '"') return null
        pos++
        val sb = StringBuilder()
        while (pos < input.length) {
            val c = input[pos]
            if (c == '"') {
                pos++
                return sb.toString()
            }
            if (c == '\\') {
                pos++
                if (pos >= input.length) return null
                val e = input[pos]
                if (e == 'u') {
                    if (pos + 4 >= input.length) return null
                    val hex = input.substring(pos + 1, pos + 5)
                    val code = hex.toIntOrNull(16) ?: return null
                    sb.append(code.toChar())
                    pos += 5
                } else {
                    if (e == 'n') sb.append('\n')
                    if (e == 'r') sb.append('\r')
                    if (e == 't') sb.append('\t')
                    if (e == '"' || e == '\\' || e == '/') sb.append(e)
                    if (e != 'n' && e != 'r' && e != 't' && e != '"' && e != '\\' && e != '/') return null
                    pos++
                }
                continue
            }
            sb.append(c)
            pos++
        }
        return null
    }

    private fun parseNumber(): JsonValue? {
        val start = pos
        if (pos < input.length && input[pos] == '-') pos++
        while (pos < input.length && input[pos].isDigit()) pos++
        if (pos == start || (pos == start + 1 && input[start] == '-')) return null
        val text = input.substring(start, pos)
        val n = text.toLongOrNull() ?: return null
        return JsonValue.Num(n)
    }
}

object BackupCodec {

    const val CURRENT_VERSION = 1

    fun encode(data: BackupData, exportedAtMs: Long): String {
        val sb = StringBuilder()
        sb.append("{\"version\":1,\"exportedAt\":")
        sb.append(exportedAtMs)
        sb.append(",\"apps\":[")
        for ((index, app) in data.apps.withIndex()) {
            if (index > 0) sb.append(',')
            sb.append("{\"packageName\":")
            sb.append(jsonString(app.packageName))
            sb.append(",\"label\":")
            sb.append(jsonString(app.label))
            sb.append(",\"enabled\":")
            sb.append(app.enabled)
            sb.append(",\"delaySeconds\":")
            sb.append(app.delaySeconds?.toString() ?: "null")
            sb.append('}')
        }
        sb.append("],\"websites\":[")
        for ((index, site) in data.websites.withIndex()) {
            if (index > 0) sb.append(',')
            sb.append("{\"host\":")
            sb.append(jsonString(site.host))
            sb.append(",\"matchSubdomains\":")
            sb.append(site.matchSubdomains)
            sb.append(",\"enabled\":")
            sb.append(site.enabled)
            sb.append(",\"delaySeconds\":")
            sb.append(site.delaySeconds?.toString() ?: "null")
            sb.append('}')
        }
        sb.append("],\"keywords\":[")
        for ((index, rule) in data.keywords.withIndex()) {
            if (index > 0) sb.append(',')
            sb.append("{\"rawText\":")
            sb.append(jsonString(rule.rawText))
            sb.append(",\"normalizedText\":")
            sb.append(jsonString(rule.normalizedText))
            sb.append(",\"category\":")
            sb.append(jsonString(rule.category))
            sb.append(",\"isPhrase\":")
            sb.append(rule.isPhrase)
            sb.append(",\"enabled\":")
            sb.append(rule.enabled)
            sb.append(",\"delaySeconds\":")
            sb.append(rule.delaySeconds?.toString() ?: "null")
            sb.append('}')
        }
        sb.append("],\"schedules\":[")
        for ((index, schedule) in data.schedules.withIndex()) {
            if (index > 0) sb.append(',')
            sb.append("{\"daysOfWeek\":")
            sb.append(jsonString(schedule.daysOfWeek))
            sb.append(",\"startMinute\":")
            sb.append(schedule.startMinute)
            sb.append(",\"endMinute\":")
            sb.append(schedule.endMinute)
            sb.append(",\"strict\":")
            sb.append(schedule.strict)
            sb.append(",\"ruleScope\":")
            sb.append(jsonString(schedule.ruleScope))
            sb.append(",\"enabled\":")
            sb.append(schedule.enabled)
            sb.append('}')
        }
        sb.append("]}")
        return sb.toString()
    }

    fun decode(json: String): BackupDecodeResult {
        val root = JsonParser(json).parse()
        if (root !is JsonValue.Obj) {
            return BackupDecodeResult.Failure("فایل بکاپ خراب است (JSON نامعتبر).")
        }
        val version = root.map["version"]
        if (version !is JsonValue.Num) {
            return BackupDecodeResult.Failure("نسخه بکاپ مشخص نیست.")
        }
        if (version.value != CURRENT_VERSION.toLong()) {
            return BackupDecodeResult.Failure("نسخه بکاپ (${version.value}) پشتیبانی نمی‌شود.")
        }
        val notes = ArrayList<String>()
        var skipped = 0
        val apps = decodeApps(root.map["apps"], object : SkippedCounter {
            override fun add(n: Int, note: String) {
                skipped += n
                notes.add(note)
            }
        })
        if (apps == null) {
            return BackupDecodeResult.Failure("بخش apps در بکاپ معتبر نیست.")
        }
        val websites = decodeWebsites(root.map["websites"], object : SkippedCounter {
            override fun add(n: Int, note: String) {
                skipped += n
                notes.add(note)
            }
        })
        if (websites == null) {
            return BackupDecodeResult.Failure("بخش websites در بکاپ معتبر نیست.")
        }
        val keywords = decodeKeywords(root.map["keywords"], object : SkippedCounter {
            override fun add(n: Int, note: String) {
                skipped += n
                notes.add(note)
            }
        })
        if (keywords == null) {
            return BackupDecodeResult.Failure("بخش keywords در بکاپ معتبر نیست.")
        }
        val schedules = decodeSchedules(root.map["schedules"], object : SkippedCounter {
            override fun add(n: Int, note: String) {
                skipped += n
                notes.add(note)
            }
        })
        if (schedules == null) {
            return BackupDecodeResult.Failure("بخش schedules در بکاپ معتبر نیست.")
        }
        return BackupDecodeResult.Success(
            BackupData(apps, websites, keywords, schedules),
            skipped,
            notes,
        )
    }

    private interface SkippedCounter {
        fun add(n: Int, note: String)
    }

    private fun decodeApps(value: JsonValue?, counter: SkippedCounter): List<BlockedApp>? {
        if (value == null) return emptyList()
        if (value !is JsonValue.Arr) return null
        val out = ArrayList<BlockedApp>()
        val seen = HashSet<String>()
        for (item in value.items) {
            if (item !is JsonValue.Obj) {
                counter.add(1, "یک اپ خراب skip شد.")
                continue
            }
            val pkg = (item.map["packageName"] as? JsonValue.Str)?.value ?: ""
            val label = (item.map["label"] as? JsonValue.Str)?.value ?: pkg
            if (pkg.isEmpty() || seen.contains(pkg)) {
                counter.add(1, "یک اپ تکراری یا بدون پکیج skip شد.")
                continue
            }
            seen.add(pkg)
            out.add(
                BlockedApp(
                    packageName = pkg,
                    label = label,
                    enabled = (item.map["enabled"] as? JsonValue.Bool)?.value ?: true,
                    delaySeconds = (item.map["delaySeconds"] as? JsonValue.Num)?.value?.toInt(),
                ),
            )
        }
        return out
    }

    private fun decodeWebsites(value: JsonValue?, counter: SkippedCounter): List<BlockedWebsite>? {
        if (value == null) return emptyList()
        if (value !is JsonValue.Arr) return null
        val out = ArrayList<BlockedWebsite>()
        val seen = HashSet<String>()
        for (item in value.items) {
            if (item !is JsonValue.Obj) {
                counter.add(1, "یک وب‌سایت خراب skip شد.")
                continue
            }
            val rawHost = (item.map["host"] as? JsonValue.Str)?.value ?: ""
            val host = KeywordMatcher.normalizeHost(rawHost)
            if (RuleValidation.validateWebsite(host).ok.not() || seen.contains(host)) {
                counter.add(1, "یک وب‌سایت نامعتبر یا تکراری skip شد.")
                continue
            }
            seen.add(host)
            out.add(
                BlockedWebsite(
                    host = host,
                    matchSubdomains = (item.map["matchSubdomains"] as? JsonValue.Bool)?.value ?: true,
                    enabled = (item.map["enabled"] as? JsonValue.Bool)?.value ?: true,
                    delaySeconds = (item.map["delaySeconds"] as? JsonValue.Num)?.value?.toInt(),
                ),
            )
        }
        return out
    }

    private fun decodeKeywords(value: JsonValue?, counter: SkippedCounter): List<KeywordRule>? {
        if (value == null) return emptyList()
        if (value !is JsonValue.Arr) return null
        val out = ArrayList<KeywordRule>()
        val seen = HashSet<String>()
        for (item in value.items) {
            if (item !is JsonValue.Obj) {
                counter.add(1, "یک کلمه خراب skip شد.")
                continue
            }
            val raw = (item.map["rawText"] as? JsonValue.Str)?.value ?: ""
            val normalized = PersianNormalizer.normalize(raw)
            if (normalized.isEmpty() || seen.contains(normalized)) {
                counter.add(1, "یک کلمه خالی یا تکراری skip شد.")
                continue
            }
            seen.add(normalized)
            out.add(
                KeywordRule(
                    rawText = raw,
                    normalizedText = normalized,
                    category = (item.map["category"] as? JsonValue.Str)?.value ?: "عمومی",
                    isPhrase = RuleValidation.isPhrase(normalized),
                    enabled = (item.map["enabled"] as? JsonValue.Bool)?.value ?: true,
                    delaySeconds = (item.map["delaySeconds"] as? JsonValue.Num)?.value?.toInt(),
                ),
            )
        }
        return out
    }

    private fun decodeSchedules(value: JsonValue?, counter: SkippedCounter): List<Schedule>? {
        if (value == null) return emptyList()
        if (value !is JsonValue.Arr) return null
        val out = ArrayList<Schedule>()
        for (item in value.items) {
            if (item !is JsonValue.Obj) {
                counter.add(1, "یک زمان‌بندی خراب skip شد.")
                continue
            }
            val start = (item.map["startMinute"] as? JsonValue.Num)?.value?.toInt() ?: -1
            val end = (item.map["endMinute"] as? JsonValue.Num)?.value?.toInt() ?: -1
            if (start < 0 || start > 1439 || end < 0 || end > 1439 || start >= end) {
                counter.add(1, "یک زمان‌بندی با ساعت نامعتبر skip شد.")
                continue
            }
            out.add(
                Schedule(
                    daysOfWeek = (item.map["daysOfWeek"] as? JsonValue.Str)?.value ?: "",
                    startMinute = start,
                    endMinute = end,
                    strict = (item.map["strict"] as? JsonValue.Bool)?.value ?: false,
                    ruleScope = (item.map["ruleScope"] as? JsonValue.Str)?.value ?: "all",
                    enabled = (item.map["enabled"] as? JsonValue.Bool)?.value ?: true,
                ),
            )
        }
        return out
    }

    private fun jsonString(s: String): String {
        val sb = StringBuilder()
        sb.append('"')
        for (c in s) {
            if (c == '"') sb.append("\\\"")
            if (c == '\\') sb.append("\\\\")
            if (c == '\n') sb.append("\\n")
            if (c == '\r') sb.append("\\r")
            if (c == '\t') sb.append("\\t")
            if (c == '"' || c == '\\' || c == '\n' || c == '\r' || c == '\t') continue
            if (c.code < 0x20) {
                sb.append("\\u%04x".format(c.code))
                continue
            }
            sb.append(c)
        }
        sb.append('"')
        return sb.toString()
    }
}
