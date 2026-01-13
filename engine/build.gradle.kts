plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Core Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Dependency Injection
    implementation(libs.koin.core)

    // Technical Analysis
    implementation(libs.ta4j.core)
}
