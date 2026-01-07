# 🔋 [SUPERSEDED] Battery Optimization & Doze Mode

Effort level: Medium
Priority: Medium
Status: Done
Blocked by: Replaced by: 🔋 SERVICE: Battery Optimization & Doze

## Objective

Ensure service survives Android battery optimization.

## The Problem

Android aggressively kills background services to save battery. Vendors like Xiaomi, Huawei, and Samsung are even more aggressive.

## Solution Layers

### Layer 1: Foreground Service

```kotlin
startForeground(NOTIFICATION_ID, notification)
```

- Must have visible notification
- Use `foregroundServiceType="dataSync"`

### Layer 2: Wake Lock

```kotlin
val wakeLock = powerManager.newWakeLock(
    PowerManager.PARTIAL_WAKE_LOCK,
    "Engine::TradingService"
).apply { acquire() }
```

- Keeps CPU running
- Release in onDestroy()

### Layer 3: Battery Optimization Exemption

```kotlin
fun requestBatteryExemption(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
    [intent.data](http://intent.data) = Uri.parse("package:${context.packageName}")
    context.startActivity(intent)
}
```

- **Must prompt user on first launch**
- Check with `isIgnoringBatteryOptimizations()`

### Layer 4: WorkManager Dead-Man-Switch

```kotlin
class ServiceWatchdogWorker : CoroutineWorker() {
    override suspend fun doWork(): Result {
        if (!isServiceRunning()) {
            startTradingService()
        }
        return Result.success()
    }
}

// Schedule every 15 minutes
val request = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
    15, TimeUnit.MINUTES
).build()
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "service_watchdog",
    ExistingPeriodicWorkPolicy.KEEP,
    request
)
```

## Vendor-Specific Issues

### Xiaomi

- Disable MIUI battery saver for app
- Enable "Autostart" permission
- Lock app in recent apps

### Huawei

- Disable PowerGenie
- Add to "Protected apps"
- Enable "Ignore battery optimization"

### Samsung

- Disable "Adaptive battery"
- Add to "Never sleeping apps"

## Testing

1. Start service
2. Lock screen for 1 hour
3. Verify service still running
4. Check notification still visible
5. Verify WebSocket reconnected if dropped

## Acceptance Criteria

- Service survives 8+ hours of screen-off
- WebSocket auto-reconnects after Doze
- WorkManager restarts killed service
- User prompted for battery exemption