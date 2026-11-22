// buildSrc — فقط برای Artifact Transform حذف کلاس‌های تکراری پرداخت
// (StripVendingBillingClasses). هیچ منطق اپی اینجا نیست.
plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}
