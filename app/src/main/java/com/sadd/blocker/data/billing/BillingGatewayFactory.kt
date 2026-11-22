package com.sadd.blocker.data.billing

import android.content.Context
import com.sadd.blocker.data.system.InstallSource
import com.sadd.blocker.domain.BillingStore
import com.sadd.blocker.domain.BillingStoreSelector

/**
 * ساخت درگاه پرداخت بر اساس نصب‌کننده اپ: نصب از بازار → درگاه بازار،
 * نصب از مایکت → درگاه مایکت، هر چیز دیگر (نصب دستی/دیباگ) → درگاه
 * «در دسترس نیست» تا صفحه پرو بدون خرید رندر شود.
 */
object BillingGatewayFactory {

    fun create(context: Context): BillingGateway {
        val installer = InstallSource.installerPackage(context)
        return when (BillingStoreSelector.storeForInstaller(installer)) {
            BillingStore.BAZAAR -> BazaarBillingGateway(context)
            BillingStore.MYKET -> MyketBillingGateway(context)
            BillingStore.UNAVAILABLE -> UnavailableBillingGateway()
        }
    }
}
