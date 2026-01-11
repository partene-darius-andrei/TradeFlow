plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Modules - ONLY depends on :core:domain
    implementation(project(":core:domain"))

    // Ktor (HTTP/WebSocket client)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // JWT ES256 (Coinbase authentication)
    implementation(libs.nimbus.jose.jwt)

    // BouncyCastle (required by Nimbus for PEM parsing)
    implementation(libs.bcprov.jdk18on)
    implementation(libs.bcpkix.jdk18on)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
}
