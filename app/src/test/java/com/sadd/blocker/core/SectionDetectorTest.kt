package com.sadd.blocker.core

import com.sadd.blocker.core.SectionDetector.SectionSignals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionDetectorTest {

    private fun searchField(hint: String) = SectionSignals(
        hasEditableTextField = true,
        editableHint = hint,
    )

    @Test
    fun telegramSearchDetectedWithEditableSearchField() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                SectionDetector.SECTION_TELEGRAM_SEARCH,
                searchField("Search"),
            ),
        )
    }

    @Test
    fun telegramSearchDetectedWithPersianHint() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                SectionDetector.SECTION_TELEGRAM_SEARCH,
                searchField("جستجو"),
            ),
        )
    }

    @Test
    fun telegramMainScreenWithNoEditableFieldIsNotSearch() {
        // صفحه اصلی فقط آیکون جستجو دارد — هرگز بلاک نمی‌شود.
        assertFalse(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                SectionDetector.SECTION_TELEGRAM_SEARCH,
                SectionSignals(
                    contentDescriptions = listOf("Search"),
                    texts = listOf("Telegram"),
                ),
            ),
        )
    }

    @Test
    fun telegramSearchRequiresEditableFieldEvenWithSearchTitle() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                SectionDetector.SECTION_TELEGRAM_SEARCH,
                SectionSignals(windowTitle = "Search"),
            ),
        )
    }

    @Test
    fun telegramSearchNotDetectedInOtherApps() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_TELEGRAM_SEARCH,
                searchField("Search"),
            ),
        )
    }

    @Test
    fun instagramSearchDetectedWithEditableSearchField() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_SEARCH,
                searchField("Search"),
            ),
        )
    }

    @Test
    fun instagramExploreDetectedByTitleWithoutEditableField() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_EXPLORE,
                SectionSignals(windowTitle = "Explore"),
            ),
        )
    }

    @Test
    fun instagramExploreDetectedByContentDescription() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_EXPLORE,
                SectionSignals(contentDescriptions = listOf("Search and explore")),
            ),
        )
    }

    @Test
    fun instagramExploreNotDetectedWhenSearchFieldIsOpen() {
        // وقتی فیلد جستجو باز است، صفحه «جستجو» است نه اکسپلور.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_EXPLORE,
                searchField("Search"),
            ),
        )
    }

    @Test
    fun instagramHomeWithNoSignalsIsNeitherSearchNorExplore() {
        val empty = SectionSignals()
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_SEARCH,
                empty,
            ),
        )
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_EXPLORE,
                empty,
            ),
        )
    }

    @Test
    fun youtubeSearchDetectedWithSearchYouTubeHint() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_SEARCH,
                searchField("Search YouTube"),
            ),
        )
    }

    @Test
    fun youtubeHomeWithSearchIconOnlyIsNotSearch() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_SEARCH,
                SectionSignals(contentDescriptions = listOf("Search")),
            ),
        )
    }

    @Test
    fun unknownSectionKeyNeverMatches() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                "no_such_section",
                searchField("Search"),
            ),
        )
    }

    @Test
    fun presetsCoverAllNineSections() {
        val keys = SectionDetector.PRESETS.map { it.sectionKey }.toSet()
        assertTrue(keys.contains(SectionDetector.SECTION_TELEGRAM_APP))
        assertTrue(keys.contains(SectionDetector.SECTION_TELEGRAM_SEARCH))
        assertTrue(keys.contains(SectionDetector.SECTION_INSTAGRAM_APP))
        assertTrue(keys.contains(SectionDetector.SECTION_INSTAGRAM_SEARCH))
        assertTrue(keys.contains(SectionDetector.SECTION_INSTAGRAM_EXPLORE))
        assertTrue(keys.contains(SectionDetector.SECTION_INSTAGRAM_REELS))
        assertTrue(keys.contains(SectionDetector.SECTION_YOUTUBE_APP))
        assertTrue(keys.contains(SectionDetector.SECTION_YOUTUBE_SEARCH))
        assertTrue(keys.contains(SectionDetector.SECTION_YOUTUBE_SHORTS))
        assertTrue(SectionDetector.PRESETS.size == 9)
    }

    // ---------- کل اپ ----------

    @Test
    fun wholeAppMatchesWithNoSignalsAtAll() {
        // خودِ پیش‌زمینه بودن اپ کافی است؛ سیگنالی لازم نیست.
        assertTrue(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                SectionDetector.SECTION_TELEGRAM_APP,
                SectionSignals(),
            ),
        )
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_APP,
                SectionSignals(),
            ),
        )
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_APP,
                SectionSignals(),
            ),
        )
    }

    @Test
    fun wholeAppNeverMatchesWrongPackage() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_TELEGRAM_APP,
                SectionSignals(),
            ),
        )
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_INSTAGRAM_APP,
                SectionSignals(),
            ),
        )
        assertFalse(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                SectionDetector.SECTION_YOUTUBE_APP,
                SectionSignals(),
            ),
        )
    }

    @Test
    fun isWholeAppSectionOnlyForWholeAppKeys() {
        assertTrue(SectionDetector.isWholeAppSection(SectionDetector.SECTION_TELEGRAM_APP))
        assertTrue(SectionDetector.isWholeAppSection(SectionDetector.SECTION_INSTAGRAM_APP))
        assertTrue(SectionDetector.isWholeAppSection(SectionDetector.SECTION_YOUTUBE_APP))
        assertFalse(SectionDetector.isWholeAppSection(SectionDetector.SECTION_TELEGRAM_SEARCH))
        assertFalse(SectionDetector.isWholeAppSection(SectionDetector.SECTION_INSTAGRAM_REELS))
        assertFalse(SectionDetector.isWholeAppSection(SectionDetector.SECTION_YOUTUBE_SHORTS))
        assertFalse(SectionDetector.isWholeAppSection("no_such_section"))
    }

    // ---------- ریلز اینستاگرام ----------

    @Test
    fun instagramReelsDetectedByContentDescription() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(contentDescriptions = listOf("Reels")),
            ),
        )
    }

    @Test
    fun instagramReelsDetectedByPersianText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(texts = listOf("ریلز")),
            ),
        )
    }

    @Test
    fun instagramReelsDetectedByWindowTitle() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(windowTitle = "Reels"),
            ),
        )
    }

    @Test
    fun instagramReelsNotDetectedWhenSearchFieldIsOpen() {
        // فیلد جستجوی باز یعنی صفحه جستجو است، نه ریلز.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(
                    hasEditableTextField = true,
                    editableHint = "Search",
                    contentDescriptions = listOf("Reels"),
                ),
            ),
        )
    }

    @Test
    fun instagramReelsNotDetectedBySearchIconAlone() {
        // فقط آیکون جستجو، بدون هیچ مارکر ریلز — هرگز بلاک نمی‌شود.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(contentDescriptions = listOf("Search")),
            ),
        )
    }

    @Test
    fun instagramReelsNotDetectedWithNoSignals() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(),
            ),
        )
    }

    @Test
    fun instagramReelsNotDetectedInOtherApps() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(contentDescriptions = listOf("Reels")),
            ),
        )
    }

    // ---------- شورتس یوتیوب ----------

    @Test
    fun youtubeShortsDetectedByWindowTitle() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_SHORTS,
                SectionSignals(windowTitle = "Shorts"),
            ),
        )
    }

    @Test
    fun youtubeShortsDetectedByPersianText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_SHORTS,
                SectionSignals(texts = listOf("شورتس")),
            ),
        )
    }

    @Test
    fun youtubeShortsNotDetectedWhenSearchFieldIsOpen() {
        // فیلد جستجوی باز یعنی صفحه جستجو است، نه شورتس.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_SHORTS,
                SectionSignals(
                    hasEditableTextField = true,
                    editableHint = "Search YouTube",
                    texts = listOf("Shorts"),
                ),
            ),
        )
    }

    @Test
    fun youtubeShortsNotDetectedBySearchIconAlone() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_SHORTS,
                SectionSignals(contentDescriptions = listOf("Search")),
            ),
        )
    }

    @Test
    fun youtubeShortsNotDetectedWithNoSignals() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_SHORTS,
                SectionSignals(),
            ),
        )
    }

    @Test
    fun youtubeShortsNotDetectedInOtherApps() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_YOUTUBE_SHORTS,
                SectionSignals(texts = listOf("Shorts")),
            ),
        )
    }
}
