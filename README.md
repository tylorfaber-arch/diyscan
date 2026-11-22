# DIY Scan

An Android OBD-II scanner app for ELM327-compatible Bluetooth adapters. It works on any 1996+ vehicle, including older pre-CAN trucks on J1850 VPW/PWM, ISO 9141 and KWP2000, as well as CAN.

**[Download the latest APK](https://github.com/tylorfaber-arch/diyscan/releases/latest/download/diyscan.apk)** · [Project page](https://tylorfaber-arch.github.io/diyscan/)

## Features

- Read stored, pending and permanent trouble codes, with descriptions, severity and tips
- Clear codes (with a warning about readiness monitors)
- Freeze frame data
- Live gauges and sensor tiles with history graphs; pick which sensors to poll
- Smog readiness monitors and check engine light status
- VIN, protocol and adapter info
- CSV drive logging
- **Simulator mode**: a virtual 2005 Ram 1500 (J1850 VPW) with real-world faults, for testing without a vehicle

## Layout

```
app/src/main/java/dev/diyscan/
  transport/   Bluetooth Classic SPP + simulator (add BLE / Wi-Fi / USB here)
  elm/         ELM327 driver: AT commands, prompt handling, protocol detect
  obd/         PIDs, response parsing (CAN + legacy), DTC database, readiness
  ui/          Compose theme, components (gauges, sparklines), screens
  ScanViewModel.kt   app state
```

The ELM327 layer hides the vehicle bus, so the same code runs on a 2005 Ram (J1850 VPW) and a 2015 F-150 (CAN).

## Building

GitHub Actions builds a signed APK on every push to `main` and publishes it as the latest release. Locally, open the folder in Android Studio and run on a device.

Parser unit tests: `gradle :app:testDebugUnitTest`

## Adapter notes

Many cheap ELM327 clones only work on CAN. For pre-2008 vehicles, use an OBDLink MX+/LX or a Vgate iCar Pro.

## Roadmap

- Manufacturer-specific modules (ABS, airbag, TCM) via enhanced PIDs
- BLE adapters (needed for iOS-style adapters)
- Mode 06 test results
- OBD1 (GM ALDL first)
