// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Force Espresso and androidx.test to versions that support API 37.
subprojects {
    configurations.all {
        resolutionStrategy {
            force("androidx.test.espresso:espresso-core:3.7.0")
            force("androidx.test.espresso:espresso-idling-resource:3.7.0")
            force("androidx.test:runner:1.6.1")
            force("androidx.test:rules:1.6.1")
            force("androidx.test:monitor:1.7.1")
            force("androidx.test.ext:junit:1.2.1")
        }
    }
}