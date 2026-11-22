package com.sadd.blocker.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/** تست انتخاب فروشگاه پرداخت بر اساس نصب‌کننده. */
class BillingStoreTest {

    @Test
    fun bazaarInstallerSelectsBazaar() {
        assertEquals(
            BillingStore.BAZAAR,
            BillingStoreSelector.storeForInstaller("com.farsitel.bazaar"),
        )
    }

    @Test
    fun myketInstallerSelectsMyket() {
        assertEquals(
            BillingStore.MYKET,
            BillingStoreSelector.storeForInstaller("ir.mservices.market"),
        )
    }

    @Test
    fun otherInstallersAreUnavailable() {
        assertEquals(
            BillingStore.UNAVAILABLE,
            BillingStoreSelector.storeForInstaller("com.android.vending"),
        )
        assertEquals(
            BillingStore.UNAVAILABLE,
            BillingStoreSelector.storeForInstaller("com.android.shell"),
        )
        assertEquals(
            BillingStore.UNAVAILABLE,
            BillingStoreSelector.storeForInstaller("com.google.android.packageinstaller"),
        )
        assertEquals(
            BillingStore.UNAVAILABLE,
            BillingStoreSelector.storeForInstaller(""),
        )
        assertEquals(
            BillingStore.UNAVAILABLE,
            BillingStoreSelector.storeForInstaller(null),
        )
    }
}
