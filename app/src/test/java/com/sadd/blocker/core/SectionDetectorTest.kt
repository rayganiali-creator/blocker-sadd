package com.sadd.blocker.core

import com.sadd.blocker.core.SectionDetector.SectionSignals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionDetectorTest {

    /**
     * فیلد جستجوی فعال: کاربر رویش زده (فوکوس) یا در آن تایپ کرده است.
     * فیلدِ صرفاً دیده‌شده (فوکوس false و بدون متن) با سیگنال جداگانه
     * ساخته می‌شود — آن حالت هرگز جستجو نیست.
     */
    private fun searchField(hint: String, focused: Boolean = true) = SectionSignals(
        hasEditableTextField = true,
        editableHint = hint,
        searchFieldFocused = focused,
    )

    private fun typedSearchField(hint: String) = SectionSignals(
        hasEditableTextField = true,
        editableHint = hint,
        searchFieldHasText = true,
    )

    /** فیلد جستجویِ صرفاً دیده‌شده: هست، ولی نه فوکوس دارد نه متن. */
    private fun untouchedSearchField(hint: String) = SectionSignals(
        hasEditableTextField = true,
        editableHint = hint,
        searchFieldFocused = false,
        searchFieldHasText = false,
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
    fun instagramExploreNotDetectedByTitleAlone() {
        // عنوان پنجره دیگر شاهد فید نیست (گزارش سید ۲۰۲۶-۱۱-۲۰): فقط
        // تب واقعاً انتخاب‌شده بلاک می‌کند.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_EXPLORE,
                SectionSignals(windowTitle = "Explore"),
            ),
        )
    }

    @Test
    fun instagramExploreDetectedBySelectedTabDescription() {
        // تب اکسپلور واقعاً انتخاب شده است (گره selected).
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_EXPLORE,
                SectionSignals(selectedContentDescriptions = listOf("Search and explore")),
            ),
        )
    }

    @Test
    fun instagramExploreLabelPresentButNotSelectedIsNotExplore() {
        // برچسب اکسپلور در درخت هست ولی تب انتخاب نشده — صفحه اصلی است، نه اکسپلور.
        assertFalse(
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
        assertTrue(keys.contains(SectionDetector.SECTION_CHROME_APP))
        assertTrue(keys.contains(SectionDetector.SECTION_CHROME_INCOGNITO))
        // ۹ قدیمی + ۳ واتساپ + ۳ تیک‌تاک در هر دو پکیج + ۳ ایکس + ۳ فیسبوک + ۳ اسنپ‌چت + ۲ کروم.
        assertTrue(SectionDetector.PRESETS.size == 29)
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
    fun instagramReelsDetectedBySelectedTabDescription() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(selectedContentDescriptions = listOf("Reels")),
            ),
        )
    }

    @Test
    fun instagramReelsDetectedBySelectedPersianText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(selectedTexts = listOf("ریلز")),
            ),
        )
    }

    @Test
    fun instagramReelsLabelPresentButNotSelectedIsNotReels() {
        // برچسب «Reels» در ناوبری پایین روی همه صفحه‌ها هست؛ بدون
        // انتخاب‌شدن تب، صفحه اصلی بلاک نمی‌شود.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(
                    texts = listOf("Reels"),
                    contentDescriptions = listOf("Reels"),
                ),
            ),
        )
    }

    @Test
    fun instagramReelsNotDetectedByWindowTitleAlone() {
        // عنوان/کلاس پنجره شاهد فید نیست؛ فقط تب انتخاب‌شده.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(windowTitle = "Reels"),
            ),
        )
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(windowClassName = "com.instagram.reels.ReelsActivity"),
            ),
        )
    }

    @Test
    fun instagramReelsNotDetectedWhenSearchFieldIsOpen() {
        // فیلد جستجوی فعال یعنی صفحه جستجو است، نه ریلز — حتی اگر تب ریلز انتخاب شده باشد.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_REELS,
                SectionSignals(
                    hasEditableTextField = true,
                    editableHint = "Search",
                    searchFieldFocused = true,
                    selectedContentDescriptions = listOf("Reels"),
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
    fun youtubeShortsNotDetectedByWindowTitleAlone() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_SHORTS,
                SectionSignals(windowTitle = "Shorts"),
            ),
        )
    }

    @Test
    fun youtubeShortsDetectedBySelectedPersianText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_SHORTS,
                SectionSignals(selectedTexts = listOf("شورتس")),
            ),
        )
    }

    @Test
    fun youtubeShortsLabelPresentButNotSelectedIsNotShorts() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_SHORTS,
                SectionSignals(texts = listOf("Shorts")),
            ),
        )
    }

    @Test
    fun youtubeShortsNotDetectedWhenSearchFieldIsOpen() {
        // فیلد جستجوی فعال یعنی صفحه جستجو است، نه شورتس.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_SHORTS,
                SectionSignals(
                    hasEditableTextField = true,
                    editableHint = "Search YouTube",
                    searchFieldFocused = true,
                    selectedTexts = listOf("Shorts"),
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
    fun whatsappStatusDetectedBySelectedText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_STATUS,
                SectionSignals(selectedTexts = listOf("Status")),
            ),
        )
    }

    @Test
    fun whatsappStatusDetectedBySelectedPersianText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_STATUS,
                SectionSignals(selectedTexts = listOf("وضعیت")),
            ),
        )
    }

    @Test
    fun whatsappStatusLabelPresentButNotSelectedIsNotStatus() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_STATUS,
                SectionSignals(texts = listOf("Status")),
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
                    searchFieldFocused = true,
                    selectedTexts = listOf("Status"),
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
    fun tiktokForYouDetectedBySelectedText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.zhiliaoapp.musically",
                SectionDetector.SECTION_TIKTOK_FORYOU,
                SectionSignals(selectedTexts = listOf("For You")),
            ),
        )
    }

    @Test
    fun tiktokForYouDetectedBySelectedPersianTextInAltPackage() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.ss.android.ugc.trill",
                SectionDetector.SECTION_TIKTOK_FORYOU,
                SectionSignals(selectedTexts = listOf("برای شما")),
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
                    searchFieldFocused = true,
                    selectedTexts = listOf("For You"),
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
    fun xHomeDetectedBySelectedContentDescription() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.twitter.android",
                SectionDetector.SECTION_X_HOME,
                SectionSignals(selectedContentDescriptions = listOf("Home")),
            ),
        )
    }

    @Test
    fun xHomeDetectedBySelectedPersianText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.twitter.android",
                SectionDetector.SECTION_X_HOME,
                SectionSignals(selectedTexts = listOf("خانه")),
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
                    searchFieldFocused = true,
                    selectedContentDescriptions = listOf("Home"),
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
    fun facebookReelsDetectedBySelectedText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.facebook.katana",
                SectionDetector.SECTION_FACEBOOK_REELS,
                SectionSignals(selectedTexts = listOf("Reels")),
            ),
        )
    }

    @Test
    fun facebookWatchDetectedBySelectedContentDescription() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.facebook.katana",
                SectionDetector.SECTION_FACEBOOK_REELS,
                SectionSignals(selectedContentDescriptions = listOf("Watch")),
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
                    searchFieldFocused = true,
                    selectedTexts = listOf("Reels"),
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
    fun snapchatSpotlightDetectedBySelectedText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.snapchat.android",
                SectionDetector.SECTION_SNAPCHAT_SPOTLIGHT,
                SectionSignals(selectedTexts = listOf("Spotlight")),
            ),
        )
    }

    @Test
    fun snapchatSpotlightDetectedBySelectedPersianText() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.snapchat.android",
                SectionDetector.SECTION_SNAPCHAT_SPOTLIGHT,
                SectionSignals(selectedTexts = listOf("اسپات‌لایت")),
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
                    searchFieldFocused = true,
                    selectedTexts = listOf("Spotlight"),
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

    // ---------- دقت بخش: فقط ورود واقعی، نه صرفِ دیده شدن (گزارش سید) ----------

    @Test
    fun telegramSearchBarPresentButUntouchedDoesNotBlock() {
        // گزارش سید: باز کردن تلگرام فقط چون نوار جستجو بالای صفحه است،
        // هرگز نباید بلاک شود — فیلد هست ولی نه فوکوس دارد نه متن.
        assertFalse(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                SectionDetector.SECTION_TELEGRAM_SEARCH,
                untouchedSearchField("Search"),
            ),
        )
    }

    @Test
    fun telegramSearchBlocksWhenFieldFocused() {
        // کاربر روی فیلد جستجو زده است (فوکوس) — ورود واقعی به جستجو.
        assertTrue(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                SectionDetector.SECTION_TELEGRAM_SEARCH,
                searchField("Search", focused = true),
            ),
        )
    }

    @Test
    fun telegramSearchBlocksWhenTextTypedIntoField() {
        // کاربر در فیلد جستجو تایپ کرده است — حتی بدون فوکوس ثبت‌شده.
        assertTrue(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                SectionDetector.SECTION_TELEGRAM_SEARCH,
                typedSearchField("Search"),
            ),
        )
    }

    @Test
    fun telegramSearchUntouchedInPersianUiDoesNotBlock() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                SectionDetector.SECTION_TELEGRAM_SEARCH,
                untouchedSearchField("جستجو"),
            ),
        )
    }

    @Test
    fun instagramSearchUntouchedFieldDoesNotBlock() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_SEARCH,
                untouchedSearchField("Search"),
            ),
        )
    }

    @Test
    fun youtubeSearchUntouchedFieldDoesNotBlock() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.google.android.youtube",
                SectionDetector.SECTION_YOUTUBE_SEARCH,
                untouchedSearchField("Search YouTube"),
            ),
        )
    }

    @Test
    fun searchTitleAloneWithoutActiveFieldDoesNotBlock() {
        // عنوان «Search» به‌تنهایی، بدون فیلد فعال، ورود به جستجو نیست.
        assertFalse(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                SectionDetector.SECTION_TELEGRAM_SEARCH,
                SectionSignals(windowTitle = "Search"),
            ),
        )
    }

    @Test
    fun isSearchHintOnlyForSearchHints() {
        assertTrue(SectionDetector.isSearchHint("Search"))
        assertTrue(SectionDetector.isSearchHint("جستجو"))
        assertTrue(SectionDetector.isSearchHint("Search YouTube"))
        assertFalse(SectionDetector.isSearchHint("Message"))
        assertFalse(SectionDetector.isSearchHint(null))
        assertFalse(SectionDetector.isSearchHint(""))
    }

    @Test
    fun isSearchSectionOnlyForSearchKeys() {
        assertTrue(SectionDetector.isSearchSection(SectionDetector.SECTION_TELEGRAM_SEARCH))
        assertTrue(SectionDetector.isSearchSection(SectionDetector.SECTION_INSTAGRAM_SEARCH))
        assertTrue(SectionDetector.isSearchSection(SectionDetector.SECTION_TIKTOK_SEARCH))
        assertFalse(SectionDetector.isSearchSection(SectionDetector.SECTION_TELEGRAM_APP))
        assertFalse(SectionDetector.isSearchSection(SectionDetector.SECTION_INSTAGRAM_REELS))
        assertFalse(SectionDetector.isSearchSection(SectionDetector.SECTION_CHROME_INCOGNITO))
        assertFalse(SectionDetector.isSearchSection("custom:for you"))
        assertFalse(SectionDetector.isSearchSection("no_such_section"))
    }

    // ---------- کروم ----------

    @Test
    fun chromeWholeAppMatchesWithNoSignalsAtAll() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_APP,
                SectionSignals(),
            ),
        )
        assertTrue(SectionDetector.isWholeAppSection(SectionDetector.SECTION_CHROME_APP))
        assertTrue(SectionDetector.hasPresets("com.android.chrome"))
    }

    @Test
    fun chromeWholeAppNeverMatchesWrongPackage() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_CHROME_APP,
                SectionSignals(),
            ),
        )
        assertFalse(
            SectionDetector.isSectionShowing(
                "org.mozilla.firefox",
                SectionDetector.SECTION_CHROME_APP,
                SectionSignals(),
            ),
        )
    }

    @Test
    fun chromePresetsAreWholeAppFirstThenIncognito() {
        val chrome = SectionDetector.PRESETS.filter {
            it.packageName == SectionDetector.CHROME_PACKAGE
        }
        assertTrue(chrome.size == 2)
        assertTrue(chrome[0].sectionKey == SectionDetector.SECTION_CHROME_APP)
        assertTrue(chrome[1].sectionKey == SectionDetector.SECTION_CHROME_INCOGNITO)
        assertTrue(chrome[0].labelFa == "خود کروم")
        assertTrue(chrome[1].labelFa.contains("بهترین تلاش"))
    }

    @Test
    fun chromeIncognitoWordPresentInMenuDoesNotBlock() {
        // گزارش سید ۲۰۲۶-۱۱-۲۰: کلمه «incognito» که فقط در منو/فهرست
        // دیده شود (بدون کلیک، بدون صفحه تب خصوصی) هرگز بلاک نمی‌کند.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(texts = listOf("Incognito")),
            ),
        )
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(contentDescriptions = listOf("Incognito")),
            ),
        )
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(texts = listOf("مرور خصوصی")),
            ),
        )
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(texts = listOf("ناشناس")),
            ),
        )
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(windowTitle = "Incognito"),
            ),
        )
        // حتی متن خود آیتم منو، بدون کلیک، شاهد نیست.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(texts = listOf("New Incognito tab")),
            ),
        )
    }

    @Test
    fun chromeIncognitoDetectedByMenuItemClick() {
        // کلیک واقعی روی آیتم منوی مرور خصوصی.
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(clickedTexts = listOf("New Incognito tab")),
            ),
        )
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(clickedContentDescriptions = listOf("New Incognito tab")),
            ),
        )
    }

    @Test
    fun chromeIncognitoDetectedByPersianMenuItemClick() {
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(clickedTexts = listOf("زبانه ناشناس جدید")),
            ),
        )
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(clickedTexts = listOf("برگه ناشناس جدید")),
            ),
        )
    }

    @Test
    fun chromeIncognitoDetectedByIncognitoTabPage() {
        // خودِ صفحه تب خصوصی باز است (متن خالی تب تازه خصوصی).
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(texts = listOf("You've gone incognito")),
            ),
        )
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(texts = listOf("ناشناس شده‌اید")),
            ),
        )
    }

    @Test
    fun searchBlocksOnSearchFieldClickWithoutFocusOrText() {
        // کلیک روی خودِ فیلد جستجو (قبل از فوکوس/تایپ) شاهد ورود به
        // جستجو است — سرویس این پرچم را فقط برای فیلد ویرایشی می‌گذارد.
        assertTrue(
            SectionDetector.isSectionShowing(
                "org.telegram.messenger",
                SectionDetector.SECTION_TELEGRAM_SEARCH,
                SectionSignals(searchFieldClicked = true),
            ),
        )
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_SEARCH,
                SectionSignals(searchFieldClicked = true),
            ),
        )
    }

    @Test
    fun isSearchFieldClickOnlyForEditableSearchFields() {
        // خودِ فیلد ویرایشی جستجو: کلیک شاهد است.
        assertTrue(SectionDetector.isSearchFieldClick(isEditable = true, hint = "Search"))
        assertTrue(SectionDetector.isSearchFieldClick(isEditable = true, hint = "جستجو"))
        assertTrue(SectionDetector.isSearchFieldClick(isEditable = true, hint = "Search YouTube"))
        // تب «Search and explore» اینستاگرام ویرایشی نیست — کلیک رویش
        // ورود به اکسپلور است، نه جستجو (گزارش سید ۲۰۲۶-۱۱-۲۱).
        assertFalse(SectionDetector.isSearchFieldClick(isEditable = false, hint = null))
        assertFalse(SectionDetector.isSearchFieldClick(isEditable = false, hint = "Search"))
        // فیلد ویرایشی غیرجستجو (چت/کامنت) شاهد جستجو نیست.
        assertFalse(SectionDetector.isSearchFieldClick(isEditable = true, hint = "Message"))
        assertFalse(SectionDetector.isSearchFieldClick(isEditable = true, hint = null))
    }

    @Test
    fun instagramExploreTabTapDoesNotTriggerSearch() {
        // رگرسیون گزارش سید ۲۰۲۶-۱۱-۲۱: قانون جستجو روشن، قانون
        // اکسپلور خاموش؛ زدن روی تب اکسپلور («Search and explore»)
        // نباید بخش جستجو را بلاک کند. سیگنال‌ها همان چیزی است که
        // سرویس بعد از کلیک روی تب می‌سازد: تب انتخاب شده، متن
        // کلیک‌شده «Search and explore»، ولی searchFieldClicked=false
        // (تب ویرایشی نیست) و فیلد جستجو فوکوس/متن ندارد.
        val signals = SectionSignals(
            hasEditableTextField = true,
            editableHint = "Search",
            selectedContentDescriptions = listOf("Search and explore"),
            clickedContentDescriptions = listOf("Search and explore"),
        )
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_SEARCH,
                signals,
            ),
        )
        // و اکسپلور با همان سیگنال‌ها (اگر قانونش روشن باشد) بلاک می‌شود.
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_EXPLORE,
                signals,
            ),
        )
    }

    @Test
    fun instagramSearchBlocksWhenTypingIntoSearchField() {
        // تایپ در فیلد جستجوی اینستاگرام = ورود به جستجو.
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.instagram.android",
                SectionDetector.SECTION_INSTAGRAM_SEARCH,
                typedSearchField("Search"),
            ),
        )
    }

    @Test
    fun whatsappStatusDetectedByUpdatesTab() {
        // واتساپ جدید تب را «Updates» نامیده است؛ وضعیت داخل همان تب
        // است و قانون وضعیت باید روی آن بلاک کند.
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_STATUS,
                SectionSignals(selectedTexts = listOf("Updates")),
            ),
        )
        assertTrue(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_STATUS,
                SectionSignals(selectedTexts = listOf("به‌روزرسانی‌ها")),
            ),
        )
        // برچسب Updates در درخت هست ولی تب انتخاب نشده — بلاک نمی‌شود.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.whatsapp",
                SectionDetector.SECTION_WHATSAPP_STATUS,
                SectionSignals(texts = listOf("Updates")),
            ),
        )
    }

    @Test
    fun chromeIncognitoNotDetectedInNormalWindow() {
        // پنجره عادی کروم: نه مارکری، نه سیگنالی — بلاک نمی‌شود.
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(),
            ),
        )
        assertFalse(
            SectionDetector.isSectionShowing(
                "com.android.chrome",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(texts = listOf("Google", "New tab")),
            ),
        )
    }

    @Test
    fun chromeIncognitoNotDetectedInOtherBrowsers() {
        assertFalse(
            SectionDetector.isSectionShowing(
                "org.mozilla.firefox",
                SectionDetector.SECTION_CHROME_INCOGNITO,
                SectionSignals(texts = listOf("Incognito")),
            ),
        )
    }

    @Test
    fun chromeIncognitoIsNotWholeAppSection() {
        assertFalse(SectionDetector.isWholeAppSection(SectionDetector.SECTION_CHROME_INCOGNITO))
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
