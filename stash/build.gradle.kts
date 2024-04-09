plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlinx.serialization)
    id("maven-publish")
}

android {
    namespace = "com.jhight.stash"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                group = "com.jhight"
                version = run("git", "tag", "--list").split("\n").lastOrNull() ?: "0.0.0"
                description = "An encrypted Android DataStore for sensitive data"
                groupId = "com.jhight"
                artifactId = "stash"
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.crypto)
    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

fun run(vararg command: String): String {
    val process = ProcessBuilder(*command)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
    process.waitFor()
    return process.inputStream.bufferedReader().use { it.readText() }.trim()
}