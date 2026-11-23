# DIY Scan

A generic Android OBD-II reader for ELM327-compatible Bluetooth and USB adapters. It works on any 1996+ vehicle (J1850 VPW/PWM, ISO 9141, KWP2000 and CAN) and extends to manufacturer data through imports: PID profiles for any make, Chrysler's DRB-III database, and request replay plans.

**[Download the latest APK](https://github.com/tylorfaber-arch/diyscan/releases/latest/download/diyscan.apk)** · [Project page](https://tylorfaber-arch.github.io/diyscan/)

## Features

- **Two instrument themes** from the Gemini design pass ([design/gemini-mockup](design/gemini-mockup/)): Workshop Daylight and Night Cockpit, or follow the system setting
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
- **Request replay & sweep**: import the request list from a capture catalog (`tools/obd_catalog.py`) and ask the vehicle the same questions the scan tool asked, or sweep mode 22 across the engine and transmission for PIDs it never showed. Read services only: clears, routines, writes and seed/key are refused
- **Imports**: one screen for everything you bring. Any number of Torque-style PID profiles (switch each on or off per vehicle), the Chrysler DRB-III database with favorite modules, and replay plans. All stored only on the device
- **Demo vehicle**: a built-in simulator with a few common faults, for trying the app without an adapter

## Layout

```
app/src/main/java/dev/diyscan/
  transport/   Bluetooth Classic SPP, USB-serial (OTG), simulator
  elm/         ELM327 driver: AT commands, prompt handling, protocol detect
  obd/         PIDs, response parsing (CAN + legacy), DTC database, readiness
  ui/          Compose theme, components (gauges, sparklines), screens
  ScanViewModel.kt   app state
```

The ELM327 layer hides the vehicle bus, so the same code runs on a 2002 pickup (J1850 VPW) and a 2018 sedan (CAN).

## Building

GitHub Actions builds a signed APK on every push to `main` and publishes it as the latest release. Locally, open the folder in Android Studio and run on a device.

Parser unit tests: `gradle :app:testDebugUnitTest`

## Adapter notes

Many cheap ELM327 clones only work on CAN. For pre-2008 vehicles, use an OBDLink MX+/LX or a Vgate iCar Pro.

## Tooling

- `tools/obd_catalog.py` — turn a raw bus capture (app Bus monitor, SavvyCAN, or candump) into a
  request/reply catalog with the reply bytes that move. See [docs/reverse-engineering.md](docs/reverse-engineering.md).
- `tools/obdb_to_profile.py` — convert an [OBDb](https://github.com/OBDb) vehicle file into the
  Torque-style CSV the app imports. `python3 tools/obdb_to_profile.py --from-github Chevrolet-Cruze -o cruze.csv`
- `tools/elm_sniff.py` — log an ELM327's bus traffic to CSV from a PC (standard library only)
- `tools/test_obd_catalog.py`, `tools/test_elm_sniff.py`, `tools/test_obdb_to_profile.py` — run with
  `python3 -m unittest discover -s tools -p "test_*.py"`

## Roadmap

- Manufacturer-specific modules (ABS, airbag, TCM) via enhanced PIDs
- BLE adapters (needed for iOS-style adapters)
- Mode 06 test results
- OBD1 (GM ALDL first)
