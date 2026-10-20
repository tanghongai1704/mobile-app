plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.onespace.data.music"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
