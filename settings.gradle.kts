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
    }
}
rootProject.name = "Blocker"
include(":app")
