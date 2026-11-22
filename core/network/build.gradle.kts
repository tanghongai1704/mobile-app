plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.onespace.core.network"
    compileSdk { version = release(37) }
    defaultConfig {
        minSdk = 26
        // Empty = in-process fake backend (default, no server needed). To use the real server:
        //   ./gradlew installDebug -Ponespace.apiBaseUrl=http://10.0.2.2:8080/v1/
        val apiBaseUrl = (project.findProperty("onespace.apiBaseUrl") as String?).orEmpty().trim()
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("boolean", "USE_FAKE_BACKEND", (apiBaseUrl.isEmpty()).toString())
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(libs.retrofit)
    api(libs.okhttp)
    api(libs.retrofit.kotlinx.serialization)
    api(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
