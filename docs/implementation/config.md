# Project Configuration

**Parent:** [../reference.md](../reference.md)

Gradle dependencies, Android manifest, and testing checklist.

---

## Gradle Dependencies

### app/build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.dpart.tradeflow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dpart.tradeflow"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Ktor (HTTP + WebSocket)
    implementation("io.ktor:ktor-client-core:3.3.3")
    implementation("io.ktor:ktor-client-okhttp:3.3.3")  // Using OkHttp engine
    implementation("io.ktor:ktor-client-websockets:3.3.3")

    // JWT
    implementation("com.nimbusds:nimbus-jose-jwt:9.37")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Technical Analysis
    implementation("org.ta4j:ta4j-core:0.15")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
}
```

---

## Android Manifest

### AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".EngineApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="Engine"
        android:theme="@style/Theme.Engine">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.TradingService"
            android:foregroundServiceType="dataSync"
            android:exported="false" />

    </application>
</manifest>
```

---

## Testing Checklist

Since Coinbase sandbox is static-only, use these approaches:

### Unit Tests

- [ ] Decision engine produces correct modes for test data
- [ ] Hysteresis requires 3 confirmations
- [ ] Defense mode activates instantly (no hysteresis)
- [ ] Grid spacing respects 1.5% minimum
- [ ] ATR/SMA/ADX calculations match reference values

### Integration Tests (Small Real Trades)

- [ ] JWT generation produces valid tokens
- [ ] REST API authentication succeeds
- [ ] Can create and cancel orders
- [ ] WebSocket connects and receives ticker
- [ ] Order updates flow through correctly

### System Tests

- [ ] Service survives device sleep (Doze)
- [ ] Service restarts after force stop (START_STICKY)
- [ ] Drawdown calculation triggers at 15%
- [ ] Emergency liquidation sells all BTC

---

## Development Environment

### Required Tools

| Tool | Version | Purpose |
|------|---------|---------|
| Android Studio | Latest | IDE |
| JDK | 17 | Compilation |
| Android SDK | 26+ | Min supported version |
| Gradle | 8.13.2 | Build system |

### Coinbase API Setup

1. Create Coinbase account
2. Enable Advanced Trade
3. Generate API key with permissions:
   - ✅ View accounts
   - ✅ Trade
   - ❌ Transfer funds
   - ❌ Withdraw

4. Save API key ID and private key PEM
5. Enter in app Settings screen

### Device Setup

**Battery Optimization:**
1. Settings → Apps → TradeFlow
2. Battery → Unrestricted
3. Required for 24/7 operation

**Xiaomi/Huawei/Samsung:**
- Additional steps required for aggressive power management
- See vendor-specific doze exemption guides

---

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Run unit tests
./gradlew testDebugUnitTest

# Install on device
./gradlew installDebug
```

---

## CI/CD

**See:** [../ci.md](../ci.md) for complete CI/CD documentation

**GitHub Actions workflow:**
1. Triggered on push to `claude/*` branches
2. Builds debug APK
3. Uploads to Firebase App Distribution
4. Commits build status back to branch

---

## Navigation

- **[Back to Technical Reference](../reference.md)** - Parent document
- **[Previous: Storage & Service](storage.md)** - Database and service
- **[See Also: GitHub Actions](../ci.md)** - CI/CD workflows
