package com.sadd.blocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * تست منطق خالص پرو: دوره آزمایشی سه‌روزه، انقضای اشتراک، مادام‌العمر،
 * انباشته‌شدن خریدها، کد توسعه‌دهنده، ثبت یک‌باره زمان نصب و آشتی‌دادن
 * با خریدهای فروشگاه (idempotent بودن).
 */
class ProEntitlementTest {

    private val day = 24L * 60L * 60L * 1000L
    private val install = 1_700_000_000_000L

    private fun stateAtInstall() = ProState(firstInstallAtMs = install)

    // ---------- دوره آزمایشی ----------

    @Test
    fun trialDay0IsPro() {
        val status = ProEntitlement.evaluate(stateAtInstall(), install)
        assertTrue(status is ProStatus.Trial)
        assertTrue(ProEntitlement.isPro(stateAtInstall(), install))
        assertEquals(3, ProEntitlement.trialDaysLeft(stateAtInstall(), install))
    }

    @Test
    fun trialDay2IsPro() {
        val now = install + 2 * day
        assertTrue(ProEntitlement.evaluate(stateAtInstall(), now) is ProStatus.Trial)
        assertEquals(1, ProEntitlement.trialDaysLeft(stateAtInstall(), now))
    }

    @Test
    fun trialJustBeforeDay3IsPro() {
        val now = install + 3 * day - 1L
        assertTrue(ProEntitlement.isPro(stateAtInstall(), now))
        assertEquals(1, ProEntitlement.trialDaysLeft(stateAtInstall(), now))
    }

    @Test
    fun trialExactlyDay3IsFree() {
        val now = install + 3 * day
        assertEquals(ProStatus.Free, ProEntitlement.evaluate(stateAtInstall(), now))
        assertFalse(ProEntitlement.isPro(stateAtInstall(), now))
        assertEquals(0, ProEntitlement.trialDaysLeft(stateAtInstall(), now))
    }

    @Test
    fun trialDay4IsFree() {
        val now = install + 4 * day
        assertEquals(ProStatus.Free, ProEntitlement.evaluate(stateAtInstall(), now))
    }

    @Test
    fun noInstallTimeMeansNoTrial() {
        val state = ProState()
        assertEquals(ProStatus.Free, ProEntitlement.evaluate(state, install))
        assertEquals(0, ProEntitlement.trialDaysLeft(state, install))
    }

    // ---------- اشتراک زمانی ----------

    @Test
    fun activeSubscriptionIsPro() {
        val state = ProState(firstInstallAtMs = install, proUntilMs = install + 40 * day)
        val status = ProEntitlement.evaluate(state, install + 10 * day)
        assertEquals(ProStatus.Active(install + 40 * day), status)
    }

    @Test
    fun expiredSubscriptionIsFreeAfterTrial() {
        val state = ProState(firstInstallAtMs = install, proUntilMs = install + 10 * day)
        val now = install + 11 * day
        assertEquals(ProStatus.Free, ProEntitlement.evaluate(state, now))
    }

    @Test
    fun expiredSubscriptionFallsBackToTrialWhenTrialStillActive() {
        // اشتراک تمام شده ولی هنوز در سه روز آزمایشی هستیم.
        val state = ProState(firstInstallAtMs = install, proUntilMs = install + day)
        val status = ProEntitlement.evaluate(state, install + 2 * day)
        assertTrue(status is ProStatus.Trial)
    }

    // ---------- مادام‌العمر و کد توسعه‌دهنده ----------

    @Test
    fun lifetimeIsAlwaysPro() {
        val state = ProState(firstInstallAtMs = install, lifetimeOwned = true)
        assertEquals(ProStatus.Lifetime, ProEntitlement.evaluate(state, install + 365 * day))
    }

    @Test
    fun devUnlockIsAlwaysPro() {
        val state = ProState(firstInstallAtMs = install, devUnlock = true)
        assertEquals(ProStatus.Lifetime, ProEntitlement.evaluate(state, install + 365 * day))
        assertTrue(ProEntitlement.isPro(state, install + 365 * day))
    }

    // ---------- انباشته‌شدن ----------

    @Test
    fun buyOneMonthWhileTenDaysRemainStacksFromExpiry() {
        val now = install + 100 * day
        val currentUntil = now + 10 * day
        val newUntil = ProEntitlement.extendProUntil(currentUntil, ProPlan.MONTH_1, now)
        assertEquals(currentUntil + 30 * day, newUntil)
    }

    @Test
    fun buyWhenExpiredStartsFromNow() {
        val now = install + 100 * day
        val expiredUntil = now - 5 * day
        val newUntil = ProEntitlement.extendProUntil(expiredUntil, ProPlan.MONTH_3, now)
        assertEquals(now + 90 * day, newUntil)
    }

    @Test
    fun buyWithNoPreviousStartsFromNow() {
        val now = install + 100 * day
        val newUntil = ProEntitlement.extendProUntil(0L, ProPlan.MONTH_6, now)
        assertEquals(now + 180 * day, newUntil)
    }

    @Test
    fun applyPurchaseStacksAndRecordsToken() {
        val now = install + 100 * day
        val state = ProState(firstInstallAtMs = install, proUntilMs = now + 10 * day)
        val purchase = OwnedPurchaseInfo("pro_1m", now, "token-1")
        val updated = ProEntitlement.applyPurchase(state, purchase, now)
        assertEquals(now + 40 * day, updated.proUntilMs)
        assertTrue(updated.processedPurchaseTokens.contains("token-1"))
    }

    @Test
    fun applySamePurchaseTwiceDoesNotDoubleGrant() {
        val now = install + 100 * day
        val state = ProState(firstInstallAtMs = install)
        val purchase = OwnedPurchaseInfo("pro_1m", now, "token-1")
        val once = ProEntitlement.applyPurchase(state, purchase, now)
        val twice = ProEntitlement.applyPurchase(once, purchase, now + day)
        assertEquals(once.proUntilMs, twice.proUntilMs)
    }

    @Test
    fun applyLifetimePurchaseSetsFlag() {
        val now = install + 100 * day
        val state = ProState(firstInstallAtMs = install)
        val purchase = OwnedPurchaseInfo("pro_lifetime", now, "token-life")
        val updated = ProEntitlement.applyPurchase(state, purchase, now)
        assertTrue(updated.lifetimeOwned)
        assertEquals(ProStatus.Lifetime, ProEntitlement.evaluate(updated, now + 1000 * day))
    }

    @Test
    fun unknownProductIsIgnored() {
        val now = install + 100 * day
        val state = ProState(firstInstallAtMs = install)
        val purchase = OwnedPurchaseInfo("some_other_product", now, "token-x")
        assertEquals(state, ProEntitlement.applyPurchase(state, purchase, now))
    }

    // ---------- آشتی‌دادن با فروشگاه ----------

    @Test
    fun reconcileGrantsFromPurchaseTimeAndMarksConsume() {
        val purchaseTime = install + 50 * day
        val now = purchaseTime + 5 * day
        val state = ProState(firstInstallAtMs = install)
        val owned = listOf(OwnedPurchaseInfo("pro_1m", purchaseTime, "token-1"))
        val (updated, toConsume) = ProEntitlement.applyOwnedPurchases(state, owned, now)
        // از زمان خرید شمرده می‌شود: ۲۵ روز از الان باقی است.
        assertEquals(purchaseTime + 30 * day, updated.proUntilMs)
        assertEquals(listOf("token-1"), toConsume)
        assertTrue(ProEntitlement.isPro(updated, now))
    }

    @Test
    fun reconcileIsIdempotent() {
        val purchaseTime = install + 50 * day
        val now = purchaseTime + 5 * day
        val state = ProState(firstInstallAtMs = install)
        val owned = listOf(OwnedPurchaseInfo("pro_1m", purchaseTime, "token-1"))
        val (once, _) = ProEntitlement.applyOwnedPurchases(state, owned, now)
        val (twice, toConsumeTwice) = ProEntitlement.applyOwnedPurchases(once, owned, now + day)
        assertEquals(once.proUntilMs, twice.proUntilMs)
        assertTrue(toConsumeTwice.isEmpty())
    }

    @Test
    fun reconcileStacksMultiplePurchasesInTimeOrder() {
        val t1 = install + 50 * day
        val t2 = t1 + 10 * day
        val now = t2 + day
        val state = ProState(firstInstallAtMs = install)
        val owned = listOf(
            OwnedPurchaseInfo("pro_1m", t2, "token-2"),
            OwnedPurchaseInfo("pro_1m", t1, "token-1"),
        )
        val (updated, toConsume) = ProEntitlement.applyOwnedPurchases(state, owned, now)
        // اولی: t1+۳۰؛ دومی از همان پایان: +۳۰ دیگر.
        assertEquals(t1 + 60 * day, updated.proUntilMs)
        assertEquals(listOf("token-1", "token-2"), toConsume)
    }

    @Test
    fun reconcileLifetimeSetsFlagAndNeverConsumes() {
        val now = install + 50 * day
        val state = ProState(firstInstallAtMs = install)
        val owned = listOf(OwnedPurchaseInfo("pro_lifetime", now, "token-life"))
        val (updated, toConsume) = ProEntitlement.applyOwnedPurchases(state, owned, now)
        assertTrue(updated.lifetimeOwned)
        assertTrue(toConsume.isEmpty())
    }

    @Test
    fun reconcileExpiredPurchaseDoesNotRevivePro() {
        val purchaseTime = install + 10 * day
        val now = purchaseTime + 40 * day
        val state = ProState(firstInstallAtMs = install)
        val owned = listOf(OwnedPurchaseInfo("pro_1m", purchaseTime, "token-old"))
        val (updated, _) = ProEntitlement.applyOwnedPurchases(state, owned, now)
        assertEquals(purchaseTime + 30 * day, updated.proUntilMs)
        assertFalse(ProEntitlement.isPro(updated, now))
    }

    // ---------- ثبت یک‌باره زمان نصب ----------

    @Test
    fun firstInstallStoredValueWins() {
        val resolved = ProEntitlement.resolveFirstInstallAt(
            storedMs = install,
            packageFirstInstallMs = install + 999L,
            nowMs = install + 5000L,
        )
        assertEquals(install, resolved)
    }

    @Test
    fun firstInstallUsesPackageTimeWhenNotStored() {
        val resolved = ProEntitlement.resolveFirstInstallAt(
            storedMs = 0L,
            packageFirstInstallMs = install,
            nowMs = install + 5000L,
        )
        assertEquals(install, resolved)
    }

    @Test
    fun firstInstallFallsBackToNow() {
        val now = install + 5000L
        val resolved = ProEntitlement.resolveFirstInstallAt(
            storedMs = 0L,
            packageFirstInstallMs = 0L,
            nowMs = now,
        )
        assertEquals(now, resolved)
    }

    // ---------- پلن‌ها و قیمت ----------

    @Test
    fun planProductIdsMatchSpec() {
        assertEquals(
            listOf("pro_1m", "pro_2m", "pro_3m", "pro_6m", "pro_lifetime"),
            ProPlan.ALL_PRODUCT_IDS,
        )
        assertEquals(50_000L, ProPlan.MONTH_1.fallbackPriceToman)
        assertEquals(99_000L, ProPlan.MONTH_2.fallbackPriceToman)
        assertEquals(140_000L, ProPlan.MONTH_3.fallbackPriceToman)
        assertEquals(280_000L, ProPlan.MONTH_6.fallbackPriceToman)
        assertEquals(1_000_000L, ProPlan.LIFETIME.fallbackPriceToman)
    }

    @Test
    fun formatTomanUsesPersianDigitsAndGrouping() {
        assertEquals("۵۰٬۰۰۰ تومان", ProEntitlement.formatTomanFa(50_000L))
        assertEquals("۹۹٬۰۰۰ تومان", ProEntitlement.formatTomanFa(99_000L))
        assertEquals("۱٬۰۰۰٬۰۰۰ تومان", ProEntitlement.formatTomanFa(1_000_000L))
        assertEquals("۱۴۰٬۰۰۰ تومان", ProEntitlement.formatTomanFa(140_000L))
    }

    // ---------- سقف نسخه رایگان ----------

    private fun keywords(count: Int): List<CappableRule> =
        (1..count).map { CappableRule(id = it.toString(), enabled = true, createdOrder = it.toLong()) }

    @Test
    fun freeWithFiveKeywordsEnforcesFirstThree() {
        val now = install + 10 * day // بعد از آزمایشی
        val enforced = ProEntitlement.enforcedRuleIds(keywords(5), stateAtInstall(), now)
        assertEquals(setOf("1", "2", "3"), enforced)
    }

    @Test
    fun freeWithExactlyThreeEnforcesAll() {
        val now = install + 10 * day
        val enforced = ProEntitlement.enforcedRuleIds(keywords(3), stateAtInstall(), now)
        assertEquals(setOf("1", "2", "3"), enforced)
    }

    @Test
    fun freeWithTwoEnforcesBoth() {
        val now = install + 10 * day
        val enforced = ProEntitlement.enforcedRuleIds(keywords(2), stateAtInstall(), now)
        assertEquals(setOf("1", "2"), enforced)
    }

    @Test
    fun trialEnforcesAll() {
        val now = install + day // وسط آزمایشی
        val enforced = ProEntitlement.enforcedRuleIds(keywords(5), stateAtInstall(), now)
        assertEquals(setOf("1", "2", "3", "4", "5"), enforced)
    }

    @Test
    fun purchasedProEnforcesAll() {
        val now = install + 10 * day
        val state = ProState(firstInstallAtMs = install, proUntilMs = now + 20 * day)
        val enforced = ProEntitlement.enforcedRuleIds(keywords(5), state, now)
        assertEquals(setOf("1", "2", "3", "4", "5"), enforced)
    }

    @Test
    fun lifetimeAndAdminEnforceAll() {
        val now = install + 10 * day
        val lifetime = ProState(firstInstallAtMs = install, lifetimeOwned = true)
        val admin = ProState(firstInstallAtMs = install, devUnlock = true)
        assertEquals(5, ProEntitlement.enforcedRuleIds(keywords(5), lifetime, now).size)
        assertEquals(5, ProEntitlement.enforcedRuleIds(keywords(5), admin, now).size)
    }

    @Test
    fun disabledRulesNeverEnforcedAndDoNotCountTowardCap() {
        val now = install + 10 * day
        val rules = listOf(
            CappableRule("1", enabled = true, createdOrder = 1),
            CappableRule("2", enabled = false, createdOrder = 2),
            CappableRule("3", enabled = true, createdOrder = 3),
            CappableRule("4", enabled = true, createdOrder = 4),
            CappableRule("5", enabled = true, createdOrder = 5),
        )
        val enforced = ProEntitlement.enforcedRuleIds(rules, stateAtInstall(), now)
        // قانون ۲ غیرفعال است؛ سه فعال اول (۱، ۳، ۴) اعمال می‌شوند و ۵ متوقف است.
        assertEquals(setOf("1", "3", "4"), enforced)
    }

    @Test
    fun disablingAnEnforcedRulePromotesTheNextPausedOne() {
        val now = install + 10 * day
        val rules = keywords(5).map { if (it.id == "1") it.copy(enabled = false) else it }
        val enforced = ProEntitlement.enforcedRuleIds(rules, stateAtInstall(), now)
        assertEquals(setOf("2", "3", "4"), enforced)
    }

    @Test
    fun canEnableMoreRespectsCapOnlyWhenFree() {
        val now = install + 10 * day
        val free = stateAtInstall()
        assertTrue(ProEntitlement.canEnableMore(0, free, now))
        assertTrue(ProEntitlement.canEnableMore(2, free, now))
        assertFalse(ProEntitlement.canEnableMore(3, free, now))
        assertFalse(ProEntitlement.canEnableMore(5, free, now))
        // آزمایشی و پرو سقف ندارند.
        assertTrue(ProEntitlement.canEnableMore(5, free, install + day))
        val pro = ProState(firstInstallAtMs = install, lifetimeOwned = true)
        assertTrue(ProEntitlement.canEnableMore(50, pro, now))
    }
}
