# 🔋 SERVICE: Battery Optimization & Doze

Effort level: Small
Priority: Medium
Status: Not started
Blocked by: SERVICE: Trading Foreground Service
Module: :service:trading

## Objective

Handle Android battery optimization for reliable background operation.

## Module

`:service:trading`

## Battery Optimization Request

```kotlin
fun requestBatteryOptimizationExemption(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }
}
```

## Doze Mode Handling

```kotlin
class DozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isDeviceIdleMode) {
                // Device entered Doze - log but continue
                Log.d(TAG, "Device entered Doze mode")
            } else {
                // Exited Doze - maybe trigger immediate sync
                Log.d(TAG, "Device exited Doze mode")
            }
        }
    }
}
```

## Manifest

```xml
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>

<receiver android:name=".DozeReceiver">
    <intent-filter>
        <action android:name="android.os.action.DEVICE_IDLE_MODE_CHANGED"/>
    </intent-filter>
</receiver>
```

## UI Prompt

Show in Settings screen if battery optimization is not disabled:

```kotlin
@Composable
fun BatteryOptimizationCard(
    isOptimized: Boolean,
    onRequestExemption: () -> Unit
) {
    if (isOptimized) {
        Card {
            Text("⚠️ Battery optimization may stop trading")
            Button(onClick = onRequestExemption) {
                Text("Disable Optimization")
            }
        }
    }
}
```

## Acceptance Criteria

- [ ]  Request exemption on first launch
- [ ]  Service continues in Doze mode
- [ ]  UI warns if optimization enabled
- [ ]  Wake lock acquired correctly