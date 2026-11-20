package com.sadd.blocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorialPagesTest {

    @Test
    fun tutorialHasSevenPages() {
        assertEquals(7, TUTORIAL_PAGES.size)
    }

    @Test
    fun tutorialTitlesMatchRealSectionNamesInOrder() {
        // نام‌ها دقیقاً نام واقعی بخش‌ها در اپ‌اند: شش تب نوار پایین
        // و صفحه «حالت بلاک» که از داشبورد باز می‌شود.
        assertEquals(
            listOf("داشبورد", "اپ‌ها", "وب", "کلمات", "سشن‌ها", "بازی‌ها", "حالت بلاک"),
            TUTORIAL_PAGES.map { it.title },
        )
    }

    @Test
    fun tutorialDescriptionsAreNonBlank() {
        for (page in TUTORIAL_PAGES) {
            assertTrue("توضیح «${page.title}» خالی است", page.description.isNotBlank())
        }
    }

    @Test
    fun tutorialTitlesAreUnique() {
        val titles = TUTORIAL_PAGES.map { it.title }
        assertEquals(titles.size, titles.toSet().size)
    }
}
