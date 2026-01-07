# 🏗️ Project Setup - Gradle, Manifest & Dependencies

Effort level: Small
Priority: High
Status: Ongoing

## Objective

Set up the Android project foundation with all required dependencies.

## Deliverables

- [ ]  Create new Android project (minSdk 26, targetSdk 34)
- [ ]  Configure Kotlin 1.9+ with serialization plugin
- [ ]  Add KSP for Room compiler
- [ ]  Add all dependencies from blueprint

## Dependencies (build.gradle.kts)

```kotlin
// Core
implementation("androidx.core:core-ktx:1.12.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

// Network (Ktor with OkHttp engine)
implementation("io.ktor:ktor-client-core:3.3.3")
implementation("io.ktor:ktor-client-okhttp:3.3.3")
implementation("io.ktor:ktor-client-websockets:3.3.3")

// JWT
implementation("com.nimbusds:nimbus-jose-jwt:9.37")

// Room
implementation("
```

## Manifest Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="[android.permission.POST](http://android.permission.POST)_NOTIFICATIONS" />
```

## Acceptance Criteria

- Project builds successfully
- All dependencies resolve without conflicts
- Can run on emulator API 26+