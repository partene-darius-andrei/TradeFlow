plugins {
    kotlin("jvm")  // Pure Kotlin, NO Android
}

kotlin {
    jvmToolchain(21)
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

    // HTTP client for historical data fetching in tests
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.okhttp)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
}
