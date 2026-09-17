plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.kivo"
    compileSdk = 37

    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    defaultConfig {
        applicationId = "com.example.kivo"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.3.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "KIVO_BASE_URL",
            "\"${providers.gradleProperty("KIVO_BASE_URL").orElse("http://192.168.0.24:3000/").get()}\""
        )
        buildConfigField(
            "String",
            "KIVO_BASE_URL_ALT",
            "\"${providers.gradleProperty("KIVO_BASE_URL_ALT").orElse("").get()}\""
        )
        buildConfigField(
            "String",
            "ONESIGNAL_APP_ID",
            "\"${providers.gradleProperty("ONESIGNAL_APP_ID").orElse("").get()}\""
        )
        buildConfigField(
            "String",
            "CLERK_PUBLISHABLE_KEY",
            "\"${providers.gradleProperty("CLERK_PUBLISHABLE_KEY").orElse("").get()}\""
        )
        buildConfigField(
            "String",
            "AGORA_APP_ID",
            "\"${providers.gradleProperty("AGORA_APP_ID").orElse("").get()}\""
        )
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.coil.compose)

    // Backend propio (Docker)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.socket.io.client)

    // Notificaciones push
    implementation(libs.onesignal)

    // Autenticación Clerk
    implementation(libs.clerk.api)
    implementation(libs.clerk.ui)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Audio recording
    implementation("androidx.media:media:1.7.0")

    // Agora SDK para llamadas/videollamadas
    implementation("io.agora.rtc:lite-sdk:4.4.1")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
