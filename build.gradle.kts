// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.ksp) apply false // Ensure this uses the alias from libs.versions.toml
    alias(libs.plugins.hilt.android.gradlePlugin) apply false // Added Hilt plugin
}
