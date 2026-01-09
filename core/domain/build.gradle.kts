plugins {
    kotlin("jvm")  // Pure Kotlin, NO Android
}

dependencies {
    // Core Kotlin only
    implementation(libs.kotlinx.coroutines.core)

    // Technical Analysis (for DecisionEngine)
    implementation(libs.ta4j.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test)
}
