# Air Mouse Diagnostics — Phase 1

## Verified device profile (real hardware, 2026-09-11)

**Redmi Note 11T 5G** (model 21091116AI, `evergo`), Android 13 / MIUI — screenshot-verified:

| Capability        | Result | Detail |
|-------------------|--------|--------|
| Gyroscope         | ✅ PASS | live x=0.001, y=0.000, z=-0.001 rad/s |
| Accelerometer     | ✅ PASS | live x=0.101, y=0.130, z=9.928 m/s² |
| Gravity           | ✅ PASS | tilt compensation reference |
| Magnetometer      | ✅ PASS | absolute-orientation (yaw) stabilisation |
| Bluetooth adapter | ⚠️ WARN | present but disabled — enable in system settings |
| IR transmitter    | ✅ PASS | ConsumerIrManager reports an emitter → IR remote mode possible |
| HID readiness     | ✅      | "Device meets API requirements for Bluetooth HID mouse mode (Phase 2 candidate)" |

**Conclusion: this phone is fully capable of the air mouse project** (gyro+accel motion, Bluetooth HID
transport, plus a bonus IR emitter for the IR-remote feature). Bluetooth was simply off at scan time.

---

Standalone Android app that verifies whether a phone (e.g. a Redmi) has the hardware needed for the
planned **air mouse** project: gyroscope + accelerometer + Bluetooth HID, with an optional IR emitter.

This phase **only diagnoses**. It does not move the PC cursor, pair devices, or transmit IR.

## What it checks

| Capability     | Why it matters                          | Missing behaviour shown |
|----------------|------------------------------------------|-------------------------|
| Gyroscope      | Primary motion sensor for the air mouse  | FAIL — air-mouse impossible |
| Accelerometer  | Stabilises motion estimation             | FAIL — limited fusion    |
| Gravity        | Tilt compensation reference              | WARN — fallback path     |
| Magnetometer   | Yaw stabilisation                        | WARN — yaw drift uncorrected |
| Bluetooth      | HID transport                            | FAIL/WARN + enable hint  |
| IR transmitter | Optional IR remote mode (ConsumerIrManager) | WARN/UNKNOWN          |

It also reports whether the device meets the **API-28+ requirement** for `BluetoothHidDevice`
(device-profile support still varies by firmware — Phase 2 must be validated on the real phone),
plus live gyroscope/accelerometer readings with axis values.

## Build & run

Requires JDK 17 and an Android SDK with platform 35 + build-tools 35.0.0
(a project-local SDK lives in `.tools/` after the initial setup; `local.properties` points at it).

```powershell
# Using the wrapper
.\gradlew.bat assembleDebug          # APK at app\build\outputs\apk\debug\app-debug.apk
.\gradlew.bat testDebugUnitTest      # unit tests (CapabilityChecksTest)
.\gradlew.bat lintDebug              # lint (abortOnError = true)
```

Install on the phone: enable *Developer options → USB debugging*, then
`.\.tools\android-sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk`
(or open the project in Android Studio and press Run).

## Project layout

```
app/src/main/java/com/airmouse/diagnostics/
├── MainActivity.kt                    # Compose host
├── diagnostics/                       # capability scanning (pure, tested logic)
│   ├── CapabilityChecks.kt            # decision logic — unit tested
│   ├── SensorScanner.kt               # Android lookups → CapabilityReport
│   └── Types.kt
├── live/LiveSensorCollector.kt        # callbackFlow-based live sensor stream
└── ui/                                # DiagnosticsScreen + ViewModel
```

## Design notes

- Decision logic is pure Kotlin (`CapabilityChecks`) so it is unit-testable without a device.
- Sensor streams are cold Flows: registration happens on collection, unregistering on stop —
  no leaked listeners, correct lifecycle behaviour.
- Manifest marks every used hardware feature `required="false"`; the app installs and runs on
  devices missing any of them and simply reports the gap.
- No runtime permissions needed in Phase 1: nothing is scanned, paired, transmitted, or read
  from the radio state via protected APIs.

## Next phases (from the project roadmap)

2. Touchpad → Bluetooth: first fixed HID reports (clicks), then movement — validated on the phone.
3. Motion engine: calibration, dead zone, EMA filtering, sensitivity, acceleration.
4. Bluetooth HID mouse: `BluetoothHidDevice` app registration + report descriptors.
5. IR remote module (only if an emitter is present), independent of the mouse engine.
