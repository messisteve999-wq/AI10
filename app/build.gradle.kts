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
        targetSdk = 28
        versionCode = 2
        versionName = "2.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
