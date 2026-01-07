plugins {
    kotlin("jvm")  // Pure Kotlin, NO Android
}

dependencies {
    // Core Kotlin only
    implementation(libs.kotlinx.coroutines.core)

    // Technical Analysis (for DecisionEngine) - TODO: Add back when implementing DecisionEngine
    // ta4j 0.22.0 uses Java Records (requires Java 17+), need to handle desugaring or downgrade
    // implementation(libs.ta4j.core)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
