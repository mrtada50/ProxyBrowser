plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.proxybrowser.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.proxybrowser.app"
        minSdk = 29
        targetSdk = 34
        // رقم الإصدار يزيد تلقائياً مع كل بناء على GitHub Actions، عشان التحديث يتثبّت فوق القديم
        val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionCode = runNumber
        versionName = "1.$runNumber"
    }

    // مفتاح توقيع ثابت: كل نسخة تنبني تتوقّع بنفس المفتاح، فيتثبّت التحديث فوق القديم
    // بدون حذف التطبيق وإعادة تثبيته
    signingConfigs {
        create("shared") {
            storeFile = file("proxybrowser.jks")
            storeType = "pkcs12"
            storePassword = "proxybrowser123"
            keyAlias = "proxybrowser"
            keyPassword = "proxybrowser123"
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            signingConfig = signingConfigs.getByName("shared")
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // دعم توجيه WebView عبر بروكسي (PROXY_OVERRIDE)
    implementation("androidx.webkit:webkit:1.11.0")

    // شبكات + فحص البروكسيات
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // مهام دورية بالخلفية (تنظيف الكوكيز المنتهية + تحديث قائمة حظر الإعلانات)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // سحب للتحديث
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // مسح QR Code
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
