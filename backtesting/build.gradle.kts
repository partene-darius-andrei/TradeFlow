plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.tradeflow.BacktestEngineKt")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Engine module (contains all domain logic)
    implementation(project(":engine"))

    // Core Kotlin
    implementation(libs.kotlinx.coroutines.core)

    // Dependency Injection
    implementation(libs.koin.core)

    // HTTP client for historical data fetching
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")
}
