pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack — برای کتابخانه Poolakey کافه‌بازار (پرداخت درون‌برنامه‌ای سد پرو).
        maven { url = uri("https://jitpack.io") }
        // مخزن Maven خود مایکت — کتابخانه پرداخت مایکت (ir.myket:billing) فقط اینجاست.
        maven("https://maven.myket.ir")
    }
}
rootProject.name = "Blocker"
include(":app")
