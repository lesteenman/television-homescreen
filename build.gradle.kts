plugins {
    alias(libs.plugins.android.application) apply false
    // Puts the matching Kotlin Gradle plugin on the classpath for AGP's built-in Kotlin.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
