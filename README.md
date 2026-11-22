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
- **Bus monitor**: listen to all bus traffic (ATMA), grouped by message ID, with changed bytes highlighted, plus a saved capture. On pre-CAN Chryslers this is the PCI bus, the starting point for decoding manufacturer-specific messages
- **Chrysler DRB-III database browser**: import your own `database.mem` (or a .zip containing it) to browse every module and command the dealer DRB-III tool knew, including request bytes and how replies are decoded. The database isn't included; it stays on your device. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
- **USB adapters** over OTG (FTDI, CH340, Prolific, CP210x chips) with baud auto-detect, as well as Bluetooth
- **Simulator mode**: a virtual 2005 Ram 1500 (J1850 VPW) with real-world faults, for testing without a vehicle

## Layout

```
app/src/main/java/dev/diyscan/
  transport/   Bluetooth Classic SPP, USB-serial (OTG), simulator
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

## Tooling

- `tools/obd_catalog.py` — turn a raw bus capture (app Bus monitor, SavvyCAN, or candump) into a
  request/reply catalog with the reply bytes that move. See [docs/reverse-engineering.md](docs/reverse-engineering.md).
  Tests: `python3 -m unittest discover -s tools -p "test_*.py"`

## Roadmap

- Manufacturer-specific modules (ABS, airbag, TCM) via enhanced PIDs
- BLE adapters (needed for iOS-style adapters)
- Mode 06 test results
- OBD1 (GM ALDL first)
