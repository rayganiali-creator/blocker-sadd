package com.sadd.blocker.domain

import com.sadd.blocker.domain.model.BlockedApp
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private fun sampleData(): BackupData {
        return BackupData(
            apps = listOf(BlockedApp("com.example.game", "بازی نمونه", true, 5)),
            websites = listOf(BlockedWebsite(0, "example.com", true, true, null)),
            keywords = listOf(KeywordRule(0, "قمار", "قمار", "عمومی", false, true, null)),
            schedules = listOf(Schedule(0, "SAT,SUN", 480, 1320, false, "all", true)),
        )
    }

    @Test
    fun roundTripPreservesData() {
        val data = sampleData()
        val json = BackupCodec.encode(data, 1_700_000_000_000L)
        val result = BackupCodec.decode(json)
        assertTrue(result is BackupDecodeResult.Success)
        val decoded = (result as BackupDecodeResult.Success).data
        assertEquals(1, decoded.apps.size)
        assertEquals("com.example.game", decoded.apps[0].packageName)
        assertEquals("بازی نمونه", decoded.apps[0].label)
        assertEquals(5, decoded.apps[0].delaySeconds)
        assertEquals("example.com", decoded.websites[0].host)
        assertEquals("قمار", decoded.keywords[0].normalizedText)
        assertEquals("SAT,SUN", decoded.schedules[0].daysOfWeek)
        assertEquals(480, decoded.schedules[0].startMinute)
        assertEquals(0, result.skipped)
    }

    @Test
    fun malformedJsonFails() {
        assertTrue(BackupCodec.decode("") is BackupDecodeResult.Failure)
        assertTrue(BackupCodec.decode("{not json") is BackupDecodeResult.Failure)
        assertTrue(BackupCodec.decode("[1,2,3]") is BackupDecodeResult.Failure)
        assertTrue(BackupCodec.decode("{\"version\":1") is BackupDecodeResult.Failure)
    }

    @Test
    fun wrongVersionFails() {
        assertTrue(BackupCodec.decode("{\"version\":2,\"apps\":[]}") is BackupDecodeResult.Failure)
        assertTrue(BackupCodec.decode("{\"apps\":[]}") is BackupDecodeResult.Failure)
    }

    @Test
    fun specialCharactersSurviveRoundTrip() {
        val data = BackupData(
            apps = emptyList(),
            websites = emptyList(),
            keywords = listOf(KeywordRule(0, "گفت: \"سلام\"", "گفت: \"سلام\"", "عمومی", true, true, null)),
            schedules = emptyList(),
        )
        val json = BackupCodec.encode(data, 0L)
        val result = BackupCodec.decode(json)
        assertTrue(result is BackupDecodeResult.Success)
        val decoded = (result as BackupDecodeResult.Success).data
        assertEquals("گفت: \"سلام\"", decoded.keywords[0].rawText)
    }

    @Test
    fun duplicateHostsAndBadItemsSkipped() {
        val json = "{" +
            "\"version\":1," +
            "\"websites\":[" +
            "{\"host\":\"example.com\"}," +
            "{\"host\":\"https://www.EXAMPLE.com/\"}," +
            "{\"host\":\"not-a-domain\"}" +
            "]," +
            "\"keywords\":[" +
            "{\"rawText\":\"قمار\"}," +
            "{\"rawText\":\"قمار\"}," +
            "{\"rawText\":\"\"}" +
            "]," +
            "\"schedules\":[" +
            "{\"daysOfWeek\":\"SAT\",\"startMinute\":900,\"endMinute\":800}" +
            "]}"
        val result = BackupCodec.decode(json)
        assertTrue(result is BackupDecodeResult.Success)
        val success = result as BackupDecodeResult.Success
        assertEquals(1, success.data.websites.size)
        assertEquals(1, success.data.keywords.size)
        assertEquals(0, success.data.schedules.size)
        assertTrue(success.skipped >= 4)
    }

    @Test
    fun emptyBackupRoundTrips() {
        val json = BackupCodec.encode(BackupData(emptyList(), emptyList(), emptyList(), emptyList()), 0L)
        val result = BackupCodec.decode(json)
        assertTrue(result is BackupDecodeResult.Success)
        val data = (result as BackupDecodeResult.Success).data
        assertTrue(data.apps.isEmpty())
        assertTrue(data.keywords.isEmpty())
    }

    @Test
    fun keywordNormalizationRecomputedOnDecode() {
        val json = "{\"version\":1,\"keywords\":[{\"rawText\":\"قمار\",\"normalizedText\":\"WRONG\"}]}"
        val result = BackupCodec.decode(json)
        assertTrue(result is BackupDecodeResult.Success)
        val data = (result as BackupDecodeResult.Success).data
        assertEquals("قمار", data.keywords[0].normalizedText)
    }
}
