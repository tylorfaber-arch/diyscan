plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.diyscan"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.diyscan"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
    // CI signs with a fixed key (from GitHub secrets) so new versions install over old ones.
    val ks = System.getenv("SIGNING_KEYSTORE")
    signingConfigs {
        if (ks != null) create("ci") {
            storeFile = file(ks)
            storeType = "pkcs12"
            storePassword = System.getenv("SIGNING_PASSWORD")
            keyAlias = "diyscan"
            keyPassword = System.getenv("SIGNING_PASSWORD")
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = if (ks != null) signingConfigs.getByName("ci") else signingConfigs.getByName("debug")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.github.mik3y:usb-serial-for-android:3.10.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
