# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run all unit tests
./gradlew test

# Run single unit test class
./gradlew testDebugUnitTest --tests "com.dpart.tradeflow.ExampleUnitTest"

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Check for dependency updates
./gradlew dependencyUpdates
```

## Architecture

**Single Activity + Jetpack Compose** architecture with Material Design 3.

### Current Structure
```
app/src/main/java/com/dpart/tradeflow/
├── MainActivity.kt          # Single activity hosting Compose UI
└── ui/theme/                 # Material3 theming (Color, Theme, Type)
```

### Intended Architecture (Clean Architecture)
When adding features, follow this package structure:
```
com.dpart.tradeflow/
├── di/                       # Hilt modules
├── data/
│   ├── remote/               # API services, DTOs
│   ├── local/                # Room database, DAOs
│   └── repository/           # Repository implementations
├── domain/
│   ├── model/                # Domain models
│   ├── repository/           # Repository interfaces
│   └── usecase/              # Use cases
├── presentation/
│   ├── navigation/           # Navigation graph
│   └── feature/              # Feature screens (composables + viewmodels)
└── ui/theme/                 # Theming
```

## Key Configuration

| Setting | Value |
|---------|-------|
| Package | `com.dpart.tradeflow` |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 (Android 15) |
| JVM Target | 11 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.09.00 |

## Dependency Management

All dependencies are managed via **Version Catalog** at `gradle/libs.versions.toml`.

To add a new dependency:
1. Add version in `[versions]` section
2. Add library in `[libraries]` section
3. Reference in build.gradle.kts as `libs.library.name`

## Testing

- **Unit tests**: `app/src/test/` - JUnit 4
- **Instrumented tests**: `app/src/androidTest/` - AndroidJUnit4 + Espresso + Compose UI Test

## Conventions

- Kotlin DSL for all Gradle files
- No comments in generated code
- Compose previews use `@Preview` annotation
- Material3 theming applied at app level via `TradeFlowTheme`
