import java.util.Properties

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.0"
    application
}

application {
    mainClass.set("com.tradeflow.standalone.MainKt")
}

dependencies {
    // REUSE existing modules (domain logic + Coinbase integration)
    implementation(project(":core:domain"))
    implementation(project(":exchange:coinbase"))

    // Ktor (needed for HTTP client in Main.kt)
    implementation("io.ktor:ktor-client-core:3.3.3")
    implementation("io.ktor:ktor-client-okhttp:3.3.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3")
    implementation("io.ktor:ktor-client-logging:3.3.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3")

    // Coroutines (needed for runBlocking in Main.kt)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")
}

// Task to run the standalone application
tasks.named<JavaExec>("run") {
    // Load credentials from local.properties or environment
    val localProperties = File(rootProject.projectDir, "local.properties")
    val props = Properties()
    if (localProperties.exists()) {
        props.load(localProperties.inputStream())
    }

    val apiKey = System.getenv("COINBASE_API_KEY")
        ?: props.getProperty("coinbase.api.key", "")
    val apiSecret = System.getenv("COINBASE_API_SECRET")
        ?: props.getProperty("coinbase.api.secret", "")

    environment("COINBASE_API_KEY", apiKey)
    environment("COINBASE_API_SECRET", apiSecret)

    standardInput = System.`in`
}
