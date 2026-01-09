plugins {
    kotlin("jvm")  // Pure Kotlin, NO Android
}

dependencies {
    // Core Kotlin only
    implementation(libs.kotlinx.coroutines.core)

    // Dependency Injection (annotations only, no Android deps)
    implementation(libs.javax.inject)

    // Technical Analysis (for DecisionEngine)
    implementation(libs.ta4j.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test)
}
