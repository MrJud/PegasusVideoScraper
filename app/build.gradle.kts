import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pegasus.videoplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pegasus.videoplayer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        ndk { abiFilters += listOf("arm64-v8a") }  // ARM64 only — riduce APK ~20MB (FFmpegKit)
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val media3 = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-exoplayer-dash:$media3")  // Fase 3: DASH manifest
    implementation("androidx.media3:media3-datasource-okhttp:$media3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    // Lifecycle (coroutines + ViewModel)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    // Download + JSON callback
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // Fase 3: YouTube stream resolution (GPLv3)
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.1")
    // Fase 3.5: DASH -> MP4 mux (LGPLv3) — aggiungere repo corretto prima di decommentare
    // implementation("com.arthenica:ffmpeg-kit-min:6.0-2.LTS")
}
