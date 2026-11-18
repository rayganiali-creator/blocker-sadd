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
    fun presetsCoverAllSections() {
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
        assertTrue(keys.contains(SectionDetector.SECTION_WHATSAPP_APP))
        assertTrue(keys.contains(SectionDetector.SECTION_WHATSAPP_SEARCH))
        assertTrue(keys.contains(SectionDetector.SECTION_WHATSAPP_STATUS))
        assertTrue(keys.contains(SectionDetector.SECTION_TIKTOK_APP))
        assertTrue(keys.contains(SectionDetector.SECTION_TIKTOK_SEARCH))
        assertTrue(keys.contains(SectionDetector.SECTION_TIKTOK_FORYOU))
        assertTrue(keys.contains(SectionDetector.SECTION_X_APP))
        assertTrue(keys.contains(SectionDetector.SECTION_X_SEARCH))
        assertTrue(keys.contains(SectionDetector.SECTION_X_HOME))
        assertTrue(keys.contains(SectionDetector.SECTION_FACEBOOK_APP))
        assertTrue(keys.contains(SectionDetector.SECTION_FACEBOOK_SEARCH))
        assertTrue(keys.contains(SectionDetector.SECTION_FACEBOOK_REELS))
        assertTrue(keys.contains(SectionDetector.SECTION_SNAPCHAT_APP))
        assertTrue(keys.contains(SectionDetector.SECTION_SNAPCHAT_SEARCH))
        assertTrue(keys.contains(SectionDetector.SECTION_SNAPCHAT_SPOTLIGHT))
        // ۹ قدیمی + ۳ واتساپ + ۳ تیک‌تاک در هر دو پکیج + ۳ ایکس + ۳ فیسبوک + ۳ اسنپ‌چت.
        assertTrue(SectionDetector.PRESETS.size == 27)
        // تیک‌تاک در هر دو پکیج پیش‌فرض دارد.
        assertTrue(
            SectionDetector.PRESETS.any {
                it.packageName == SectionDetector.TIKTOK_PACKAGE &&
                    it.sectionKey == SectionDetector.SECTION_TIKTOK_FORYOU
            },
        )
        assertTrue(
            SectionDetector.PRESETS.any {
                it.packageName == SectionDetector.TIKTOK_PACKAGE_ALT &&
                    it.sectionKey == SectionDetector.SECTION_TIKTOK_FORYOU
            },
        )
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

    // ---------- واتساپ ----------

    @Test
    fun whatsappSearchDetectedWithEditableSearchField() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_SEARCH,
                searchField("Search"),
            ),
        )
    }

    @Test
    fun whatsappSearchRequiresEditableField() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_SEARCH,
                SectionSignals(contentDescriptions = listOf("Search")),
            ),
        )
    }

    @Test
    fun whatsappStatusDetectedByText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_STATUS,
                SectionSignals(texts = listOf("Status")),
            ),
        )
    }

    @Test
    fun whatsappStatusDetectedByPersianText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_STATUS,
                SectionSignals(texts = listOf("وضعیت")),
            ),
        )
    }

    @Test
    fun whatsappStatusNotDetectedWhenSearchFieldIsOpen() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_STATUS,
                SectionSignals(
                    hasEditableTextField = true,
                    editableHint = "Search",
                    texts = listOf("Status"),
                ),
            ),
        )
    }

    @Test
    fun whatsappStatusNotDetectedWithNoSignals() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_STATUS,
                SectionSignals(),
            ),
        )
    }

    @Test
    fun whatsappWholeAppMatchesWithNoSignals() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_APP,
                SectionSignals(),
            ),
        )
        assertTrue(SectionDetector.isWholeAppSection(SectionDetector.SECTION_WHATSAPP_APP))
    }

    // ---------- تیک‌تاک (هر دو پکیج) ----------

    @Test
    fun tiktokSearchDetectedInBothPackages() {
        for (pkg in listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")) {
            assertTrue(
                SectionDetector.isSectionShowing(
                    pkg,
                    SectionDetector.SECTION_TIKTOK_SEARCH,
                    searchField("Search"),
                ),
            )
        }
    }

    @Test
    fun tiktokForYouDetectedByText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.zhiliaoapp.musically",
                SectionDetector.SECTION_TIKTOK_FORYOU,
                SectionSignals(texts = listOf("For You")),
            ),
        )
    }

    @Test
    fun tiktokForYouDetectedByPersianTextInAltPackage() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.ss.android.ugc.trill",
                SectionDetector.SECTION_TIKTOK_FORYOU,
                SectionSignals(texts = listOf("برای شما")),
            ),
        )
    }

    @Test
    fun tiktokForYouNotDetectedWhenSearchFieldIsOpen() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.zhiliaoapp.musically",
                SectionDetector.SECTION_TIKTOK_FORYOU,
                SectionSignals(
                    hasEditableTextField = true,
                    editableHint = "Search",
                    texts = listOf("For You"),
                ),
            ),
        )
    }

    @Test
    fun tiktokForYouNotDetectedWithNoSignals() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.zhiliaoapp.musically",
                SectionDetector.SECTION_TIKTOK_FORYOU,
                SectionSignals(),
            ),
        )
    }

    @Test
    fun tiktokWholeAppMatchesBothPackages() {
        for (pkg in listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")) {
            assertTrue(
                SectionDetector.isSectionShowing(
                    pkg,
                    SectionDetector.SECTION_TIKTOK_APP,
                    SectionSignals(),
                ),
            )
        }
        assertTrue(SectionDetector.isWholeAppSection(SectionDetector.SECTION_TIKTOK_APP))
    }

    // ---------- ایکس ----------

    @Test
    fun xSearchDetectedWithEditableSearchField() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.twitter.android",
                SectionDetector.SECTION_X_SEARCH,
                searchField("Search X"),
            ),
        )
    }

    @Test
    fun xHomeDetectedByContentDescription() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.twitter.android",
                SectionDetector.SECTION_X_HOME,
                SectionSignals(contentDescriptions = listOf("Home")),
            ),
        )
    }

    @Test
    fun xHomeDetectedByPersianText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.twitter.android",
                SectionDetector.SECTION_X_HOME,
                SectionSignals(texts = listOf("خانه")),
            ),
        )
    }

    @Test
    fun xHomeNotDetectedWhenSearchFieldIsOpen() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.twitter.android",
                SectionDetector.SECTION_X_HOME,
                SectionSignals(
                    hasEditableTextField = true,
                    editableHint = "Search X",
                    contentDescriptions = listOf("Home"),
                ),
            ),
        )
    }

    @Test
    fun xHomeNotDetectedWithNoSignals() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.twitter.android",
                SectionDetector.SECTION_X_HOME,
                SectionSignals(),
            ),
        )
    }

    // ---------- فیسبوک ----------

    @Test
    fun facebookSearchDetectedWithEditableSearchField() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.facebook.katana",
                SectionDetector.SECTION_FACEBOOK_SEARCH,
                searchField("Search Facebook"),
            ),
        )
    }

    @Test
    fun facebookReelsDetectedByText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.facebook.katana",
                SectionDetector.SECTION_FACEBOOK_REELS,
                SectionSignals(texts = listOf("Reels")),
            ),
        )
    }

    @Test
    fun facebookWatchDetectedByContentDescription() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.facebook.katana",
                SectionDetector.SECTION_FACEBOOK_REELS,
                SectionSignals(contentDescriptions = listOf("Watch")),
            ),
        )
    }

    @Test
    fun facebookReelsNotDetectedWhenSearchFieldIsOpen() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.facebook.katana",
                SectionDetector.SECTION_FACEBOOK_REELS,
                SectionSignals(
                    hasEditableTextField = true,
                    editableHint = "Search Facebook",
                    texts = listOf("Reels"),
                ),
            ),
        )
    }

    // ---------- اسنپ‌چت ----------

    @Test
    fun snapchatSearchDetectedWithEditableSearchField() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.snapchat.android",
                SectionDetector.SECTION_SNAPCHAT_SEARCH,
                searchField("Search"),
            ),
        )
    }

    @Test
    fun snapchatSpotlightDetectedByText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.snapchat.android",
                SectionDetector.SECTION_SNAPCHAT_SPOTLIGHT,
                SectionSignals(texts = listOf("Spotlight")),
            ),
        )
    }

    @Test
    fun snapchatSpotlightDetectedByPersianText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.snapchat.android",
                SectionDetector.SECTION_SNAPCHAT_SPOTLIGHT,
                SectionSignals(texts = listOf("اسپات‌لایت")),
            ),
        )
    }

    @Test
    fun snapchatSpotlightNotDetectedWhenSearchFieldIsOpen() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.snapchat.android",
                SectionDetector.SECTION_SNAPCHAT_SPOTLIGHT,
                SectionSignals(
                    hasEditableTextField = true,
                    editableHint = "Search",
                    texts = listOf("Spotlight"),
                ),
            ),
        )
    }

    @Test
    fun snapchatSpotlightNotDetectedWithNoSignals() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.snapchat.android",
                SectionDetector.SECTION_SNAPCHAT_SPOTLIGHT,
                SectionSignals(),
            ),
        )
    }

    // ---------- بخش سفارشی کاربر ----------

    @Test
    fun customSectionKeyRoundTrip() {
        val key = SectionDetector.customSectionKey("For You")
        assertTrue(key == "custom:for you")
        assertTrue(SectionDetector.isCustomSectionKey(key!!))
        assertTrue(SectionDetector.customSignal(key) == "for you")
    }

    @Test
    fun customSectionKeyNormalizesArabicYeh() {
        // ي عربی (U+064A) به ی فارسی تبدیل می‌شود تا کلید یکتا بماند.
        val key = SectionDetector.customSectionKey("استور\u064A")
        assertTrue(key == "custom:استوری")
        assertTrue(SectionDetector.customSignal(key!!) == "استوری")
    }

    @Test
    fun customSectionKeyRejectsTooShortSignal() {
        assertTrue(SectionDetector.customSectionKey("") == null)
        assertTrue(SectionDetector.customSectionKey("x") == null)
        assertTrue(SectionDetector.customSectionKey("  ") == null)
    }

    @Test
    fun customSignalReturnsNullForPresetKeys() {
        assertTrue(SectionDetector.customSignal(SectionDetector.SECTION_TELEGRAM_SEARCH) == null)
        assertFalse(SectionDetector.isCustomSectionKey(SectionDetector.SECTION_TELEGRAM_SEARCH))
    }

    @Test
    fun customSectionDetectedByText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.example.social",
                "custom:for you",
                SectionSignals(texts = listOf("For You")),
            ),
        )
    }

    @Test
    fun customSectionDetectedByContentDescription() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.example.social",
                "custom:spotlight",
                SectionSignals(contentDescriptions = listOf("Spotlight")),
            ),
        )
    }

    @Test
    fun customSectionDetectedByEditableHint() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.example.social",
                "custom:search",
                SectionSignals(hasEditableTextField = true, editableHint = "Search"),
            ),
        )
    }

    @Test
    fun customSectionNotDetectedWithNoSignals() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.example.social",
                "custom:for you",
                SectionSignals(),
            ),
        )
    }

    @Test
    fun customSectionNotDetectedForDifferentSignal() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.example.social",
                "custom:for you",
                SectionSignals(texts = listOf("Following")),
            ),
        )
    }

    @Test
    fun customAppIsWholeAppSection() {
        assertTrue(SectionDetector.isWholeAppSection(SectionDetector.SECTION_CUSTOM_APP))
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.example.social",
                SectionDetector.SECTION_CUSTOM_APP,
                SectionSignals(),
            ),
        )
    }

    // ---------- مرورگرها ----------

    @Test
    fun isBrowserPackageCoversAllSupportedBrowsers() {
        assertTrue(SectionDetector.isBrowserPackage("com.android.chrome"))
        assertTrue(SectionDetector.isBrowserPackage("org.mozilla.firefox"))
        assertTrue(SectionDetector.isBrowserPackage("com.sec.android.app.sbrowser"))
        assertTrue(SectionDetector.isBrowserPackage("com.microsoft.emmx"))
        assertTrue(SectionDetector.isBrowserPackage("com.brave.browser"))
    }

    @Test
    fun isBrowserPackageFalseForSocialApps() {
        assertFalse(SectionDetector.isBrowserPackage("com.instagram.android"))
        assertFalse(SectionDetector.isBrowserPackage("org.telegram.messenger"))
        assertFalse(SectionDetector.isBrowserPackage("com.whatsapp"))
    }
}
