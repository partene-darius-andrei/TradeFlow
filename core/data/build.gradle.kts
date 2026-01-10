plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Modules
    implementation(project(":core:domain"))

    // Dependency Injection (annotations only, no Android deps)
    implementation(libs.javax.inject)

    // Room is Android-specific - removing for now
    // TODO: Replace with SQLite or H2 database for JVM

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
