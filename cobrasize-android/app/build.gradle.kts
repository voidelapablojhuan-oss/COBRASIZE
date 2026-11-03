plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.cobrasize.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cobrasize.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "1.0.6"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = false }
}
