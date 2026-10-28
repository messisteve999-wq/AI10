plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ai10.messi"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ai10.messi"
        minSdk = 23
        targetSdk = 35
        versionCode = 7
        versionName = "7.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
