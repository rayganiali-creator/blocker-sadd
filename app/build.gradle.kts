plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.sadd.blocker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sadd.blocker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "3.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // کتابخانه پرداخت مایکت (ir.myket:billing) در مانیفست خودش این
        // placeholderها را می‌خواهد؛ مقادیر ثابت مایکت‌اند (مستندات رسمی
        // مایکت و پلاگین رسمی myket_iap). Poolakey placeholder نمی‌خواهد.
        manifestPlaceholders.putAll(
            mapOf(
                "marketApplicationId" to "ir.mservices.market",
                "marketBindAddress" to "ir.mservices.market.InAppBillingService.BIND",
                "marketPermission" to "ir.mservices.market.BILLING",
            )
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // کتابخانه پرداخت مایکت (ir.myket:billing) با Kotlin 2.2 کامپایل شده
        // ولی پروژه روی Kotlin 2.0.21 است؛ بدون این پرچم کامپایلر متادیتای
        // 2.2.0 را رد می‌کند. API مایکت کلاس‌های جاواست و این پرچم فقط چک
        // نسخه متادیتا را رد می‌کند. با ارتقای Kotlin پروژه به 2.2+ حذف شود.
        freeCompilerArgs += "-Xskip-metadata-version-check"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended")
    // Material Components (XML) — provides Theme.Material3.DayNight.NoActionBar,
    // the parent of Theme.Blocker in values/themes.xml (Compose Material3 alone
    // does not define XML themes; without this, AAPT linking fails).
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    ksp("com.google.dagger:hilt-compiler:2.51.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // پرداخت درون‌برنامه‌ای سد پرو — کافه‌بازار (Poolakey از JitPack) و مایکت.
    // مختصات مایکت از پلاگین رسمی myket_iap (نسخه 2.0.1، فایل
    // android/build.gradle.kts در گیت‌هاب myketstore/billing-sample-flutter)
    // راستی‌آزمایی شد: ir.myket:billing:2.2 روی https://maven.myket.ir
    implementation("com.github.cafebazaar.Poolakey:poolakey:2.2.0")
    implementation("ir.myket:billing:2.2")

    // Tests (unit)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

// حذف کلاس‌های تکراری پرداخت از AAR مایکت (تداخل با Poolakey، ران ۲۰۵):
// ثبت transform در پلاگین buildSrc انجام می‌شود تا اسکریپت DSL درگیر
// wiring نشود (ران‌های ۲۰۶ و ۲۰۷ روی همین wiring خطای کامپایل دادند).
// جزئیات: buildSrc/src/main/kotlin/StripVendingBillingPlugin.kt
apply<StripVendingBillingPlugin>()
