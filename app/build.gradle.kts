plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// GitHub numbers each automatic build; that number becomes the app version
// so every new build installs over the last one.
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "uk.lloyd.tvhome"
    compileSdk = 34

    defaultConfig {
        applicationId = "uk.lloyd.tvhome"
        minSdk = 26
        targetSdk = 34
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
    }

    // A fixed signing key, so updates install over the previous version.
    // This app is only ever sideloaded onto your own TV, so the key lives in the repo.
    signingConfigs {
        create("tvhome") {
            storeFile = file("tvhome.keystore")
            storePassword = "tvhome-sideload"
            keyAlias = "tvhome"
            keyPassword = "tvhome-sideload"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("tvhome")
        }
        debug {
            signingConfig = signingConfigs.getByName("tvhome")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    // Play Store checks don't apply to a sideloaded app; don't let them block the build.
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // QR codes on the Setup page (scan with the Pixel camera).
    implementation("com.google.zxing:core:3.5.3")
}
