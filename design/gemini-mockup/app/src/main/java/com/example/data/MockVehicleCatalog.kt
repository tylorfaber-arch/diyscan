package com.example.data

import com.example.model.AdapterDevice
import com.example.model.AdapterType
import com.example.model.BusFrame
import com.example.model.DrbCommand
import com.example.model.DrbModule
import com.example.model.DtcCategory
import com.example.model.DtcEntry
import com.example.model.DtcSeverity
import com.example.model.FreezeFrame
import com.example.model.MonitorState
import com.example.model.PidProfileItem
import com.example.model.PidSpec
import com.example.model.ReadinessMonitor
import com.example.model.ReplayRequest

object MockVehicleCatalog {

    val adapters: List<AdapterDevice> = listOf(
        AdapterDevice(
            id = "sim_can",
            name = "2018 Sport Sedan · Demo",
            subtitle = "Built-in simulator · ISO 15765-4 CAN · 3 active faults",
            type = AdapterType.SIMULATOR,
            protocol = "ISO 15765-4 CAN (11-bit, 500 kbps)",
            firmware = "ELM327 v2.3 (Sim)",
            vin = "1FA6P8CF5J5148920",
            vehicleLabel = "2018 Sport Sedan 2.0T",
            ecuId = "7E0 / 7E8 (PCM)",
            isChryslerPci = false,
        ),
        AdapterDevice(
            id = "sim_pci",
            name = "2004 Dodge Ram 1500 · Demo",
            subtitle = "Built-in simulator · Chrysler PCI / J1850 VPW",
            type = AdapterType.SIMULATOR,
            protocol = "SAE J1850 VPW (10.4 kbps · PCI Bus)",
            firmware = "ELM327 v2.3 (Sim)",
            vin = "1D7HA18D74S629411",
            vehicleLabel = "2004 Ram 1500 5.7 HEMI",
            ecuId = "10 / F1 (JTEC PCM)",
            isChryslerPci = true,
        ),
        AdapterDevice(
            id = "bt_obdlink",
            name = "OBDLink MX+",
            subtitle = "Bluetooth Classic SPP · 00:04:3E:8B:12:F4 · Paired",
            type = AdapterType.BLUETOOTH_SPP,
            protocol = "ISO 15765-4 CAN (11-bit, 500 kbps)",
            firmware = "STN2255 v5.6.19",
            vin = "2HGFC2F75JH541209",
            vehicleLabel = "OBDLink MX+ Vehicle",
            ecuId = "7E0 / 7E8 (ECU #1)",
            isChryslerPci = false,
        ),
        AdapterDevice(
            id = "usb_ftdi",
            name = "FTDI FT232R USB-OBD",
            subtitle = "USB-OTG Serial · Auto-baud 115200 bps",
            type = AdapterType.USB_OTG,
            protocol = "ISO 15765-4 CAN (29-bit, 500 kbps)",
            firmware = "ELM327 v1.4b (FTDI)",
            vin = "3GCUKREC2JG284711",
            vehicleLabel = "USB-OTG Connected Truck",
            ecuId = "7E0 / 7E8 (ECM)",
            isChryslerPci = false,
        ),
    )

    private val cToF: (Double) -> Double = { it * 9.0 / 5.0 + 32.0 }
    private val kphToMph: (Double) -> Double = { it * 0.621371 }
    private val kpaToPsi: (Double) -> Double = { it * 0.145038 }
    private val gsToLbMin: (Double) -> Double = { it * 0.132277 }

    val standardPids: List<PidSpec> = listOf(
        PidSpec("rpm", "01 0C", "Engine RPM", "RPM", "rpm", "rpm", 0.0, 7000.0, 0.0, 7000.0, redlineMetric = 5800.0, decimals = 0),
        PidSpec("speed", "01 0D", "Vehicle Speed", "Speed", "km/h", "mph", 0.0, 200.0, 0.0, 125.0, toImperial = kphToMph, decimals = 0),
        PidSpec("coolant", "01 05", "Coolant Temperature", "Coolant", "°C", "°F", 40.0, 125.0, 104.0, 257.0, redlineMetric = 108.0, toImperial = cToF, decimals = 0),
        PidSpec("load", "01 04", "Calculated Engine Load", "Load", "%", "%", 0.0, 100.0, 0.0, 100.0, redlineMetric = 88.0, decimals = 1),
        PidSpec("stft", "01 06", "Short Term Fuel Trim B1", "STFT B1", "%", "%", -25.0, 25.0, -25.0, 25.0, decimals = 1),
        PidSpec("ltft", "01 07", "Long Term Fuel Trim B1", "LTFT B1", "%", "%", -25.0, 25.0, -25.0, 25.0, decimals = 1),
        PidSpec("throttle", "01 11", "Throttle Position", "Throttle", "%", "%", 0.0, 100.0, 0.0, 100.0, decimals = 1),
        PidSpec("maf", "01 10", "Mass Air Flow", "MAF", "g/s", "lb/min", 0.0, 160.0, 0.0, 21.0, toImperial = gsToLbMin, decimals = 1),
        PidSpec("timing", "01 0E", "Ignition Timing Advance", "Timing", "°", "°", -10.0, 48.0, -10.0, 48.0, decimals = 1),
        PidSpec("iat", "01 0F", "Intake Air Temp", "IAT", "°C", "°F", -10.0, 75.0, 14.0, 167.0, toImperial = cToF, decimals = 0),
        PidSpec("o2b1s1", "01 14", "O2 Sensor Voltage B1S1", "O2 B1S1", "V", "V", 0.0, 1.0, 0.0, 1.0, decimals = 2),
        PidSpec("batt", "AT RV", "Control Module Voltage", "Battery", "V", "V", 11.0, 15.2, 11.0, 15.2, decimals = 2),
    )

    val customProfilePids: List<PidSpec> = listOf(
        PidSpec(
            id = "trans_temp",
            codeHex = "22 19 40",
            name = "Transmission Fluid Temp",
            shortName = "Trans Temp",
            metricUnit = "°C",
            imperialUnit = "°F",
            metricMin = 20.0,
            metricMax = 130.0,
            imperialMin = 68.0,
            imperialMax = 266.0,
            redlineMetric = 110.0,
            isCustomProfile = true,
            profileId = "prof_chrysler",
            toImperial = cToF,
            decimals = 0,
        ),
        PidSpec(
            id = "oil_psi",
            codeHex = "22 02 15",
            name = "Engine Oil Pressure",
            shortName = "Oil Press",
            metricUnit = "kPa",
            imperialUnit = "psi",
            metricMin = 0.0,
            metricMax = 550.0,
            imperialMin = 0.0,
            imperialMax = 80.0,
            isCustomProfile = true,
            profileId = "prof_chrysler",
            toImperial = kpaToPsi,
            decimals = 1,
        ),
        PidSpec(
            id = "boost",
            codeHex = "22 F4 0B",
            name = "Turbo Boost Pressure",
            shortName = "Boost",
            metricUnit = "kPa",
            imperialUnit = "psi",
            metricMin = -70.0,
            metricMax = 160.0,
            imperialMin = -10.0,
            imperialMax = 23.0,
            isCustomProfile = true,
            profileId = "prof_gm",
            toImperial = kpaToPsi,
            decimals = 1,
        ),
        PidSpec(
            id = "knock_ret",
            codeHex = "22 11 A6",
            name = "Knock Spark Retard",
            shortName = "Knock Ret",
            metricUnit = "°",
            imperialUnit = "°",
            metricMin = 0.0,
            metricMax = 12.0,
            imperialMin = 0.0,
            imperialMax = 12.0,
            isCustomProfile = true,
            profileId = "prof_gm",
            decimals = 1,
        ),
    )

    val initialProfiles: List<PidProfileItem> = listOf(
        PidProfileItem(
            id = "prof_chrysler",
            fileName = "Chrysler_PCI_Enhanced.csv",
            makeLabel = "Chrysler / Dodge / Jeep",
            sensorCount = 2,
            enabled = true,
            protocolHint = "Mode 22 · TCM & PCM",
        ),
        PidProfileItem(
            id = "prof_gm",
            fileName = "GM_Turbo_Enhanced.csv",
            makeLabel = "Chevrolet / GMC",
            sensorCount = 2,
            enabled = false,
            protocolHint = "Mode 22 · 7E0 ECM",
        ),
    )

    val initialDtcs: List<DtcEntry> = listOf(
        DtcEntry(
            code = "P0171",
            title = "System Too Lean (Bank 1)",
            subsystem = "Fuel & Air Metering",
            severity = DtcSeverity.MEDIUM,
            category = DtcCategory.STORED,
            tip = "Inspect intake boot and PCV vacuum lines for unmetered air leaks, verify fuel pressure, and check MAF readings at idle.",
            causes = listOf(
                "Vacuum leak downstream of MAF sensor",
                "Contaminated MAF hot-wire element",
                "Weak fuel pump or clogged fuel filter",
                "Exhaust leak upstream of B1S1 O2 sensor",
            ),
        ),
        DtcEntry(
            code = "P0300",
            title = "Random / Multiple Cylinder Misfire Detected",
            subsystem = "Ignition System",
            severity = DtcSeverity.HIGH,
            category = DtcCategory.STORED,
            tip = "Avoid heavy throttle loads; raw unburned fuel from persistent misfires can rapidly overheat the catalytic converter.",
            causes = listOf(
                "Worn spark plugs or weak ignition coils",
                "Lean air/fuel mixture triggered by P0171",
                "Low fuel rail pressure under load",
            ),
        ),
        DtcEntry(
            code = "P0420",
            title = "Catalyst System Efficiency Below Threshold (Bank 1)",
            subsystem = "Emission Control",
            severity = DtcSeverity.LOW,
            category = DtcCategory.PENDING,
            tip = "Resolve active lean/misfire codes first before condemning the catalytic converter; compare B1S1 vs B1S2 O2 waveforms.",
            causes = listOf(
                "Degraded three-way catalytic converter",
                "Exhaust flange leak near downstream O2 sensor",
                "Secondary effect from recent cylinder misfires",
            ),
        ),
        DtcEntry(
            code = "P0128",
            title = "Coolant Thermostat Below Regulating Temp",
            subsystem = "Cooling System",
            severity = DtcSeverity.LOW,
            category = DtcCategory.PERMANENT,
            tip = "Commonly a thermostat stuck slightly open. Permanent codes clear automatically after 3 consecutive warm-up cycles pass.",
            causes = listOf(
                "Thermostat stuck partially open",
                "Drifting ECT sensor resistance curve",
            ),
        ),
    )

    val initialFreezeFrame = FreezeFrame(
        triggerCode = "P0171",
        rpm = 2140.0,
        speedKph = 72.0,
        coolantC = 89.0,
        loadPct = 46.5,
        stftPct = 14.8,
        ltftPct = 18.2,
        mafGs = 19.4,
        fuelSystem = "Closed Loop · Fault Bank 1",
    )

    val initialReadiness: List<ReadinessMonitor> = listOf(
        ReadinessMonitor("Misfire", "MIS", MonitorState.READY, continuous = true),
        ReadinessMonitor("Fuel System", "FUE", MonitorState.READY, continuous = true),
        ReadinessMonitor("Components", "CCM", MonitorState.READY, continuous = true),
        ReadinessMonitor("Catalyst", "CAT", MonitorState.INCOMPLETE),
        ReadinessMonitor("O2 Sensor", "O2S", MonitorState.READY),
        ReadinessMonitor("O2 Heater", "HTR", MonitorState.READY),
        ReadinessMonitor("EVAP System", "EVA", MonitorState.INCOMPLETE),
        ReadinessMonitor("EGR / VVT", "EGR", MonitorState.READY),
        ReadinessMonitor("Sec. Air", "AIR", MonitorState.UNSUPPORTED),
    )

    fun initialBusFrames(isPci: Boolean): List<BusFrame> = if (isPci) {
        listOf(
            BusFrame("0x10", "PCM · RPM / MAP", listOf(0x0C, 0x38, 0x41, 0x7A, 0x00, 0x1E), listOf(0x0C, 0x38, 0x41, 0x7A, 0x00, 0x1E), 412, 20.0, List(6) { false }),
            BusFrame("0x2A", "PCM · Coolant & Oil", listOf(0x81, 0x58, 0x2F, 0x44, 0x09), listOf(0x81, 0x58, 0x2F, 0x44, 0x09), 184, 8.2, List(5) { false }),
            BusFrame("0x3A", "TCM · Gear & Line PSI", listOf(0x04, 0x11, 0x62, 0x00, 0x8C, 0x10), listOf(0x04, 0x11, 0x62, 0x00, 0x8C, 0x10), 290, 14.5, List(6) { false }),
            BusFrame("0x68", "BCM · Lighting & Doors", listOf(0x00, 0x12, 0x04, 0xFF, 0x01), listOf(0x00, 0x12, 0x04, 0xFF, 0x01), 98, 4.0, List(5) { false }),
            BusFrame("0xB0", "MIC · Odometer & Fuel", listOf(0x21, 0xA4, 0x55, 0x3C, 0x77, 0x02), listOf(0x21, 0xA4, 0x55, 0x3C, 0x77, 0x02), 64, 2.5, List(6) { false }),
            BusFrame("0xE2", "ABS · Wheel Speeds", listOf(0x00, 0x24, 0x00, 0x25, 0x00, 0x24), listOf(0x00, 0x24, 0x00, 0x25, 0x00, 0x24), 520, 25.0, List(6) { false }),
        )
    } else {
        listOf(
            BusFrame("0x0C8", "ECM · Torque & RPM", listOf(0x0B, 0x90, 0x24, 0x00, 0x7F, 0x12, 0x44, 0x09), listOf(0x0B, 0x90, 0x24, 0x00, 0x7F, 0x12, 0x44, 0x09), 842, 50.0, List(8) { false }),
            BusFrame("0x114", "EPS · Steering Angle", listOf(0x00, 0x04, 0xFF, 0xFE, 0x10, 0x00, 0x82, 0xC1), listOf(0x00, 0x04, 0xFF, 0xFE, 0x10, 0x00, 0x82, 0xC1), 618, 35.0, List(8) { false }),
            BusFrame("0x210", "TCM · Gear & Clutch", listOf(0x04, 0x04, 0x1A, 0x5C, 0x00, 0x00, 0x3E, 0x01), listOf(0x04, 0x04, 0x1A, 0x5C, 0x00, 0x00, 0x3E, 0x01), 405, 22.5, List(8) { false }),
            BusFrame("0x329", "ECM · Coolant & Fuel", listOf(0x80, 0x7B, 0x3E, 0x14, 0x00, 0x52, 0x19, 0x00), listOf(0x80, 0x7B, 0x3E, 0x14, 0x00, 0x52, 0x19, 0x00), 219, 10.0, List(8) { false }),
            BusFrame("0x4B0", "ABS · 4-Wheel Speed", listOf(0x1C, 0x22, 0x1C, 0x20, 0x1C, 0x24, 0x1C, 0x21), listOf(0x1C, 0x22, 0x1C, 0x20, 0x1C, 0x24, 0x1C, 0x21), 510, 28.0, List(8) { false }),
            BusFrame("0x545", "IPC · Cluster & MIL", listOf(0x02, 0x48, 0x00, 0x88, 0x10, 0x00, 0x00, 0x04), listOf(0x02, 0x48, 0x00, 0x88, 0x10, 0x00, 0x00, 0x04), 130, 5.0, List(8) { false }),
            BusFrame("0x7E8", "OBD · ECU Diagnostic", listOf(0x04, 0x41, 0x0C, 0x1A, 0xF8, 0x00, 0x00, 0x00), listOf(0x04, 0x41, 0x0C, 0x1A, 0xF8, 0x00, 0x00, 0x00), 94, 4.8, List(8) { false }),
        )
    }

    val initialDrbModules: List<DrbModule> = listOf(
        DrbModule(
            id = 1,
            name = "Powertrain Control Module (JTEC / NGC)",
            shortCode = "PCM",
            busType = "PCI · J1850 VPW",
            category = "Engine",
            isPci = true,
            isFavorite = true,
            commands = listOf(
                DrbCommand("pcm_1", "Injector Pulse Width Cyl #1", "22 01 1A", "((A * 256) + B) * 0.004", "ms", "Fuel"),
                DrbCommand("pcm_2", "Target Idle RPM", "22 01 0C", "(A * 256 + B) / 4", "rpm", "Idle"),
                DrbCommand("pcm_3", "Adaptive Fuel Memory Bank 1", "22 01 44", "(A - 128) * 0.78125", "%", "Fuel"),
                DrbCommand("pcm_4", "Knock Sensor Total Retard", "22 01 62", "A * 0.5", "deg", "Ignition"),
                DrbCommand("pcm_5", "Battery Charging Target Voltage", "22 01 90", "A * 0.0625 + 6.0", "V", "Charging"),
            ),
        ),
        DrbModule(
            id = 2,
            name = "Transmission Control Module (545RFE)",
            shortCode = "TCM",
            busType = "PCI · J1850 VPW",
            category = "Drivetrain",
            isPci = true,
            isFavorite = true,
            commands = listOf(
                DrbCommand("tcm_1", "Transmission Sump Temperature", "22 19 40", "A - 40", "°C", "Thermal"),
                DrbCommand("tcm_2", "Desired Line Pressure", "22 19 12", "A * 0.784", "psi", "Hydraulics"),
                DrbCommand("tcm_3", "Torque Converter Slip RPM", "22 19 28", "((A * 256 + B) - 32768) / 4", "rpm", "Clutch"),
                DrbCommand("tcm_4", "2C Clutch Volume Index (CVI)", "22 19 52", "A", "cnt", "Adaptive"),
                DrbCommand("tcm_5", "OD Clutch Volume Index (CVI)", "22 19 54", "A", "cnt", "Adaptive"),
            ),
        ),
        DrbModule(
            id = 3,
            name = "Body Control Module (FCM / BCM)",
            shortCode = "BCM",
            busType = "PCI · J1850 VPW",
            category = "Body",
            isPci = true,
            isFavorite = false,
            commands = listOf(
                DrbCommand("bcm_1", "Door Ajar Switch Bitmask", "22 40 01", "BITMASK(A, 0..3)", "bits", "Inputs"),
                DrbCommand("bcm_2", "Interior Dimmer PWM Duty", "22 40 18", "A * 100 / 255", "%", "Lighting"),
                DrbCommand("bcm_3", "Key-In-Ignition Sense State", "22 40 05", "A & 0x01", "bool", "Security"),
            ),
        ),
        DrbModule(
            id = 4,
            name = "Mechanical Instrument Cluster",
            shortCode = "MIC",
            busType = "PCI · J1850 VPW",
            category = "Cluster",
            isPci = true,
            isFavorite = false,
            commands = listOf(
                DrbCommand("mic_1", "Fuel Level Sender Resistance", "22 60 0A", "A * 1.25", "Ω", "Fuel"),
                DrbCommand("mic_2", "Stored Odometer Value", "22 60 20", "(A*65536 + B*256 + C) * 0.1", "mi", "Cluster"),
            ),
        ),
        DrbModule(
            id = 5,
            name = "Anti-Lock Brake Controller (CAB)",
            shortCode = "ABS",
            busType = "ISO 9141 / PCI",
            category = "Chassis",
            isPci = true,
            isFavorite = false,
            commands = listOf(
                DrbCommand("abs_1", "Left Front Wheel Speed", "22 30 01", "A * 0.5", "mph", "Sensors"),
                DrbCommand("abs_2", "Right Front Wheel Speed", "22 30 02", "A * 0.5", "mph", "Sensors"),
                DrbCommand("abs_3", "Brake Booster Vacuum Sensor", "22 30 14", "A * 0.45", "kPa", "Hydraulics"),
            ),
        ),
        DrbModule(
            id = 6,
            name = "Sentry Key Immobilizer Module",
            shortCode = "SKIM",
            busType = "CCD / PCI",
            category = "Security",
            isPci = false,
            isFavorite = false,
            commands = listOf(
                DrbCommand("skim_1", "Programmed Transponder Count", "22 50 02", "A", "keys", "Keys"),
                DrbCommand("skim_2", "Valid Key Challenge Status", "22 50 08", "ENUM(A)", "state", "Auth"),
            ),
        ),
    )

    val demoReplayPlan: List<ReplayRequest> = listOf(
        ReplayRequest("7E0", "22 F1 90", "Read VIN (UDS DID F190)", readOnly = true),
        ReplayRequest("7E0", "22 F4 0C", "Enhanced Engine RPM", readOnly = true),
        ReplayRequest("7E0", "22 11 A6", "Knock Spark Retard", readOnly = true),
        ReplayRequest("7E1", "22 19 40", "TCM Fluid Temperature", readOnly = true),
        ReplayRequest("7E1", "22 19 12", "TCM Line Pressure", readOnly = true),
        ReplayRequest("7E0", "14 FF FF FF", "Clear Diagnostic Info (Write)", readOnly = false),
        ReplayRequest("7E0", "27 01", "Security Seed Request (Seed/Key)", readOnly = false),
        ReplayRequest("7E1", "22 19 52", "TCM 2C Clutch Volume Index", readOnly = true),
    )

    val mode22SweepPlan: List<ReplayRequest> = listOf(
        ReplayRequest("7E0", "22 F4 05", "Mode 22 Sweep · Coolant", readOnly = true),
        ReplayRequest("7E0", "22 F4 0B", "Mode 22 Sweep · MAP / Boost", readOnly = true),
        ReplayRequest("7E0", "22 F4 0C", "Mode 22 Sweep · Engine RPM", readOnly = true),
        ReplayRequest("7E0", "22 F4 0D", "Mode 22 Sweep · Vehicle Speed", readOnly = true),
        ReplayRequest("7E0", "22 F4 10", "Mode 22 Sweep · Mass Air Flow", readOnly = true),
        ReplayRequest("7E0", "22 F4 11", "Mode 22 Sweep · Throttle Pos", readOnly = true),
        ReplayRequest("7E0", "22 F4 1F", "Mode 22 Sweep · Engine Runtime", readOnly = true),
        ReplayRequest("7E1", "22 19 10", "Mode 22 Sweep · TCM Input Shaft", readOnly = true),
        ReplayRequest("7E1", "22 19 11", "Mode 22 Sweep · TCM Output Shaft", readOnly = true),
        ReplayRequest("7E1", "22 19 40", "Mode 22 Sweep · TCM Fluid Temp", readOnly = true),
    )
}
