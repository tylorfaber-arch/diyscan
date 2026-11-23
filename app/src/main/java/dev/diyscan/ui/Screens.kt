package dev.diyscan.ui

import dev.diyscan.ui.theme.BlueprintBorder
import dev.diyscan.ui.theme.BlueprintSlate
import dev.diyscan.R
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.diyscan.Conn
import dev.diyscan.DeviceItem
import dev.diyscan.UiState
import dev.diyscan.display
import dev.diyscan.format
import dev.diyscan.obd.DtcDatabase
import dev.diyscan.obd.Pid
import dev.diyscan.obd.Pids
import dev.diyscan.obd.Severity
import dev.diyscan.rangeFor
import dev.diyscan.ui.theme.Numeric
import dev.diyscan.ui.theme.Palette
import dev.diyscan.unitFor

private val ScreenPad = PaddingValues(horizontal = 20.dp, vertical = 20.dp)

@Composable
private fun ScreenTitle(title: String, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted)
        }
        trailing?.invoke()
    }
}

// =================================================================== CONNECT

@Composable
fun ConnectScreen(
    state: UiState,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onConnect: (DeviceItem) -> Unit,
    onSimulator: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onOpenImports: () -> Unit = {},
    onConnectUsb: (dev.diyscan.UsbItem) -> Unit = {},
    darkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize().background(Palette.Bg)) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Logo(Modifier.weight(1f))
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(Palette.Panel)
                            .border(1.dp, Palette.Line, CircleShape).clickable(onClick = onToggleTheme),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            if (darkTheme) "Switch to Daylight theme" else "Switch to Night theme", tint = Palette.Text)
                    }
                }
                Spacer(Modifier.height(18.dp))
                BlueprintHero(busy = state.conn is Conn.Connecting, onDemo = onSimulator)
                Spacer(Modifier.height(20.dp))

                when (val c = state.conn) {
                    is Conn.Connecting -> ConnectingCard(c.step)
                    is Conn.Failed -> {
                        Panel(Modifier.fillMaxWidth(), accent = Palette.Fault) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, null, tint = Palette.Fault)
                                Spacer(Modifier.width(10.dp))
                                Text("Couldn't connect", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(c.message, color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(18.dp))
                    }
                    else -> {}
                }

                if (state.conn !is Conn.Connecting) {
                    if (state.usbDevices.isNotEmpty()) {
                        SectionLabel("USB adapters")
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.usbDevices.forEach { u ->
                                Panel(Modifier.fillMaxWidth(), onClick = { onConnectUsb(u) }, padding = 16.dp) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Palette.InfoBg),
                                            contentAlignment = Alignment.Center,
                                        ) { Text("USB", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Palette.Info) }
                                        Spacer(Modifier.width(14.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(u.name, style = MaterialTheme.typography.titleMedium)
                                            Text(u.detail, style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint)
                                        }
                                        Text("Connect", color = Palette.Amber, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Paired adapters", Modifier.weight(1f))
                        if (hasPermission) {
                            Icon(Icons.Filled.Refresh, "Refresh", tint = Palette.TextMuted,
                                modifier = Modifier.clip(CircleShape).clickable(onClick = onRefresh).padding(6.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))

                    if (!hasPermission) {
                        Panel(Modifier.fillMaxWidth()) {
                            Text("Bluetooth permission needed", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Android asks before an app can talk to paired devices.", color = Palette.TextMuted)
                            Spacer(Modifier.height(14.dp))
                            AmberButton("Allow Bluetooth", onClick = onRequestPermission)
                        }
                    } else if (state.devices.isEmpty()) {
                        Panel(Modifier.fillMaxWidth()) {
                            Text("No paired devices", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Plug the adapter into the port under the dash, turn the key on, then pair it in Android Bluetooth settings. The PIN is usually 1234 or 0000. " +
                                    "Using a USB adapter? Plug it in with an OTG cable and it shows up above.",
                                color = Palette.TextMuted,
                            )
                            Spacer(Modifier.height(14.dp))
                            GhostButton("Open Bluetooth settings", icon = Icons.Filled.Settings, onClick = onOpenBluetoothSettings)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.devices.forEach { d -> DeviceRow(d) { onConnect(d) } }
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    SectionLabel("Imports")
                    Spacer(Modifier.height(10.dp))
                    ImportsEntry(state, onOpenImports)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun Logo(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(BlueprintSlate)
                .border(1.dp, BlueprintBorder, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Image(painterResource(R.drawable.ic_launcher), "DIY Scan", Modifier.size(36.dp)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("DIY SCAN", style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 1.4.sp))
            Text("OBD-II · CAN · J1850 · IMPORTS", style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted)
        }
    }
}

/** Dark "blueprint" hero: vehicle telemetry art, bus pills, headline and the demo-vehicle button. */
@Composable
private fun BlueprintHero(busy: Boolean, onDemo: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BlueprintSlate)
            .border(1.dp, BlueprintBorder, RoundedCornerShape(20.dp)),
    ) {
        Box(Modifier.fillMaxWidth().height(176.dp)) {
            Image(
                painterResource(R.drawable.hero_telemetry), "Vehicle telemetry illustration",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, BlueprintSlate.copy(alpha = 0.9f)))))
            Row(Modifier.align(Alignment.TopStart).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("ELM327", Color(0xFFFF7A52), Color(0xCC1E1410))
                Pill("J1850 · ISO · CAN", Color(0xFF7DD3FC), Color(0xCC0D1C26), dot = false)
            }
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("Plug in. Key on.", style = MaterialTheme.typography.headlineMedium, color = Color(0xFFF4F6F9))
                Spacer(Modifier.height(2.dp))
                Text(
                    "Codes, live gauges, readiness and bus traffic on any 1996+ vehicle.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFFAAB4C3),
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            AmberButton(
                if (busy) "Connecting…" else "Try the demo vehicle",
                icon = Icons.Filled.DirectionsCar, enabled = !busy, onClick = onDemo,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ConnectingCard(step: String) {
    Panel(Modifier.fillMaxWidth(), accent = Palette.Amber) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(26.dp), color = Palette.Amber, strokeWidth = 3.dp, trackColor = Palette.Track)
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Connecting", style = MaterialTheme.typography.titleMedium)
                Text(step, color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Older vehicles (J1850 / K-line) can take 5–10 seconds to answer the first time.",
            color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DeviceRow(d: DeviceItem, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), onClick = onClick, padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Palette.AmberGlow),
                contentAlignment = Alignment.Center,
            ) { Text("OBD", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Palette.Amber) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.titleMedium)
                Text(d.address, style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint)
            }
            Text("Connect", color = Palette.Amber, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// =================================================================== OVERVIEW

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewScreen(state: UiState, onRefresh: () -> Unit, onGoCodes: () -> Unit, onGoLive: () -> Unit) {
    val c = state.conn as? Conn.Connected ?: return
    val r = state.readiness
    val d = state.dtcs
    LazyVerticalGrid(
        columns = GridCells.Adaptive(320.dp),
        contentPadding = ScreenPad,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenTitle(c.name, if (c.simulated) "Simulator" else "Connected") {
                Icon(Icons.Filled.Refresh, "Refresh", tint = Palette.TextMuted,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onRefresh).padding(8.dp))
            }
        }

        // Check engine light hero
        item {
            val mil = r?.milOn == true
            val count = d?.total ?: r?.dtcCount ?: 0
            val (fg, bg) = when {
                mil -> Palette.Fault to Palette.FaultBg
                count > 0 -> Palette.Warn to Palette.WarnBg
                else -> Palette.Good to Palette.GoodBg
            }
            Panel(Modifier.fillMaxWidth().heightIn(min = 190.dp), accent = fg, onClick = onGoCodes) {
                SectionLabel("Check engine")
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(58.dp).clip(CircleShape).background(bg).border(2.dp, fg.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (count == 0) Icons.Filled.CheckCircle else Icons.Filled.Warning, null, tint = fg, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            when { r == null && d == null -> "Checking…"; mil -> "Light is ON"; count > 0 -> "Light is off"; else -> "All clear" },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            when (count) { 0 -> "No trouble codes stored"; 1 -> "1 trouble code"; else -> "$count trouble codes" },
                            color = Palette.TextMuted,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (d != null && d.total > 0) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        (d.stored + d.pending).distinct().forEach { code ->
                            val sev = DtcDatabase.lookup(code).severity
                            Pill(code, severityColor(sev), severityBg(sev), dot = false)
                        }
                    }
                }
            }
        }

        // Readiness
        item {
            Panel(Modifier.fillMaxWidth().heightIn(min = 190.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Smog readiness", Modifier.weight(1f))
                    if (r != null) {
                        if (r.incomplete == 0) Pill("Ready", Palette.Good, Palette.GoodBg)
                        else Pill("${r.incomplete} not ready", Palette.Warn, Palette.WarnBg)
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (r == null) {
                    Text("—", color = Palette.TextMuted)
                } else {
                    r.monitors.forEach { m ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusDot(if (m.complete) Palette.Good else Palette.Warn)
                            Spacer(Modifier.width(10.dp))
                            Text(m.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(if (m.complete) "Complete" else "Incomplete",
                                color = if (m.complete) Palette.TextMuted else Palette.Warn, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Quick live glance
        item {
            Panel(Modifier.fillMaxWidth(), onClick = onGoLive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Live data", Modifier.weight(1f))
                    Text("Open gauges", color = Palette.Amber, style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Filled.KeyboardArrowRight, null, tint = Palette.Amber)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "${state.availablePids.size} sensors available on this vehicle",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("RPM, coolant, fuel trims, O2 sensors, timing and more.", color = Palette.TextMuted)
            }
        }

        // Vehicle info
        item {
            Panel(Modifier.fillMaxWidth()) {
                SectionLabel("Vehicle")
                Spacer(Modifier.height(12.dp))
                InfoRow("VIN", state.vin ?: "Not reported", mono = true)
                InfoRow("Protocol", c.protocol.label)
                InfoRow("Bus type", if (c.protocol.isCan) "CAN (fast)" else "Legacy serial (pre-CAN)")
                InfoRow("Adapter", c.adapter.ifBlank { "Unknown" })
            }
        }

        if (state.busy != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { BusyRow(state.busy) }
        }
    }
}

@Composable
private fun InfoRow(k: String, v: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(k, color = Palette.TextMuted, modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium)
        Text(v, style = if (mono) MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            else MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun BusyRow(text: String) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator(Modifier.size(16.dp), color = Palette.Amber, strokeWidth = 2.dp, trackColor = Palette.Track)
        Spacer(Modifier.width(10.dp))
        Text(text, color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable fun severityColor(s: Severity) = when (s) { Severity.HIGH -> Palette.Fault; Severity.MEDIUM -> Palette.Warn; Severity.LOW -> Palette.Info }
@Composable fun severityBg(s: Severity) = when (s) { Severity.HIGH -> Palette.FaultBg; Severity.MEDIUM -> Palette.WarnBg; Severity.LOW -> Palette.InfoBg }

// =================================================================== LIVE

private val gaugePids = setOf(Pids.RPM.code, Pids.SPEED.code, Pids.COOLANT.code)

@Composable
fun LiveScreen(
    state: UiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onToggleLog: () -> Unit,
    onTogglePid: (Pid) -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    val imp = state.imperial
    val pids = state.visibleDashboard
    val gauges = pids.filter { it.code in gaugePids }
    val tiles = pids.filter { it.code !in gaugePids }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(170.dp),
        contentPadding = ScreenPad,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                ScreenTitle(
                    "Live data",
                    if (state.liveRunning) "%.1f updates/sec · ${if (state.conn.let { it is Conn.Connected && it.protocol.isCan }) "CAN" else "legacy bus — fewer sensors = faster"}".format(state.refreshHz)
                    else "Paused",
                ) {
                    Icon(Icons.Filled.Edit, "Choose sensors", tint = Palette.TextMuted,
                        modifier = Modifier.clip(CircleShape).clickable { picking = true }.padding(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.liveRunning) GhostButton("Pause", icon = Icons.Filled.Close, onClick = onStop)
                    else AmberButton("Start", icon = Icons.Filled.PlayArrow, onClick = onStart)
                    RecordButton(recording = state.logFile != null, onClick = onToggleLog)
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        if (gauges.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    gauges.forEach { p ->
                        val v = state.live[p.code]
                        val range = p.rangeFor(imp)
                        val frac = v?.let { ((p.display(it, imp) - range.start) / (range.endInclusive - range.start)).toFloat() } ?: 0f
                        Panel(Modifier.weight(1f), padding = 12.dp) {
                            ArcGauge(
                                code = pidLabel(p),
                                fraction = frac,
                                valueText = p.format(v, imp),
                                unit = p.unitFor(imp),
                                label = p.shortName,
                                redFrom = when (p.code) {
                                    Pids.RPM.code -> 5200f / 6000f
                                    Pids.COOLANT.code -> if (imp) ((235 - range.start) / (range.endInclusive - range.start)).toFloat()
                                    else ((113 - range.start) / (range.endInclusive - range.start)).toFloat()
                                    else -> null
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        items(tiles, key = { it.code }) { p ->
            val r = p.rangeFor(imp)
            SensorTile(
                label = p.shortName,
                valueText = p.format(state.live[p.code], imp),
                unit = p.unitFor(imp),
                history = state.history[p.code].orEmpty().map { p.display(it.toDouble(), imp).toFloat() },
                min = r.start.toFloat(),
                max = r.endInclusive.toFloat(),
                code = pidLabel(p),
                custom = p.isCustom,
            )
        }

        if (state.logFile != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Recording to ${state.logFile}", color = Palette.TextFaint, style = MaterialTheme.typography.labelSmall)
            }
        }
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Done", color = Palette.Amber) } },
            title = { Text("Sensors to watch") },
            text = {
                Column {
                    Text("Fewer sensors = faster updates on older vehicles.", color = Palette.TextMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                        state.availablePids.forEach { p ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onTogglePid(p) }.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = p in state.dashboard, onCheckedChange = { onTogglePid(p) },
                                    colors = CheckboxDefaults.colors(checkedColor = Palette.Amber, checkmarkColor = Palette.OnAccent),
                                )
                                Column {
                                    Text(p.name, style = MaterialTheme.typography.bodyMedium)
                                    if (p.isCustom) Text(
                                        "${p.source ?: "Profile"} · ${p.request}" + (p.header?.let { " @ $it" } ?: ""),
                                        color = Palette.TextFaint, style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = Palette.PanelHigh,
        )
    }
}

@Composable
private fun RecordButton(recording: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp))
            .border(1.dp, if (recording) Palette.Fault else Palette.Line, RoundedCornerShape(14.dp))
            .background(if (recording) Palette.FaultBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(Palette.Fault, size = 12.dp)
        Spacer(Modifier.width(8.dp))
        Text(if (recording) "Stop recording" else "Record CSV", style = MaterialTheme.typography.labelLarge,
            color = if (recording) Palette.Fault else Palette.Text)
    }
}

// =================================================================== CODES

private enum class CodeTab(val label: String) { STORED("Stored"), PENDING("Pending"), PERMANENT("Permanent") }

@Composable
fun CodesScreen(state: UiState, onRead: () -> Unit, onClear: () -> Unit) {
    var tab by remember { mutableStateOf(CodeTab.STORED) }
    var confirm by remember { mutableStateOf(false) }
    val d = state.dtcs
    val list = when (tab) {
        CodeTab.STORED -> d?.stored; CodeTab.PENDING -> d?.pending; CodeTab.PERMANENT -> d?.permanent
    }.orEmpty()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPad, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenTitle("Trouble codes", when (val n = d?.total) { null -> "Not read yet"; 0 -> "Nothing stored"; else -> "$n found" })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AmberButton("Read codes", icon = Icons.Filled.Refresh, enabled = state.busy == null, onClick = onRead)
                GhostButton("Clear codes", icon = Icons.Filled.Delete, tint = Palette.Fault,
                    enabled = state.busy == null && d != null, onClick = { confirm = true })
            }
            Spacer(Modifier.height(16.dp))
            Segmented(CodeTab.entries.map { t ->
                val n = when (t) { CodeTab.STORED -> d?.stored?.size; CodeTab.PENDING -> d?.pending?.size; CodeTab.PERMANENT -> d?.permanent?.size }
                t.label + (n?.let { " · $it" } ?: "")
            }, tab.ordinal) { tab = CodeTab.entries[it] }
            Spacer(Modifier.height(4.dp))
            Text(
                when (tab) {
                    CodeTab.STORED -> "Confirmed faults. These usually turn on the check engine light."
                    CodeTab.PENDING -> "Seen once, not confirmed yet. Early warning — may become stored."
                    CodeTab.PERMANENT -> "2010+ only. Can't be cleared with a scanner; they clear themselves once fixed."
                },
                color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall,
            )
        }

        if (state.busy != null) item { BusyRow(state.busy) }

        if (d != null && list.isEmpty()) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Palette.Good)
                        Spacer(Modifier.width(10.dp))
                        Text("No ${tab.label.lowercase()} codes", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        items(list, key = { tab.name + it }) { code -> DtcCard(code) }

        val ff = state.freezeFrame
        if (tab == CodeTab.STORED && ff != null) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Freeze frame", Modifier.weight(1f))
                        ff.dtc?.let { Pill("Set by $it", Palette.Info, Palette.InfoBg, dot = false) }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Engine conditions at the moment the code was set.", color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    ff.values.forEach { (p, v) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            Text(p.name, Modifier.weight(1f), color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium)
                            Text("${p.format(v, state.imperial)} ${p.unitFor(state.imperial)}", style = Numeric)
                        }
                    }
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            icon = { Icon(Icons.Filled.Warning, null, tint = Palette.Warn) },
            title = { Text("Clear all codes?") },
            text = {
                Text(
                    "This erases stored and pending codes and freeze-frame data, and turns off the check engine light.\n\n" +
                        "It also resets all smog readiness monitors — the vehicle won't pass an emissions test until it's been driven through its monitors again (often 50–100 miles).\n\n" +
                        "If the problem isn't fixed, the code will come back.\n\nKey ON, engine OFF is best.",
                    color = Palette.TextMuted,
                )
            },
            confirmButton = { TextButton(onClick = { confirm = false; onClear() }) { Text("Clear codes", color = Palette.Fault) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel", color = Palette.Text) } },
            containerColor = Palette.PanelHigh,
        )
    }
}

@Composable
private fun DtcCard(code: String) {
    val info = DtcDatabase.lookup(code)
    val fg = severityColor(info.severity)
    Panel(Modifier.fillMaxWidth(), padding = 0.dp) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(fg))
            Column(Modifier.padding(18.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(code, style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Monospace), color = fg)
                    Spacer(Modifier.weight(1f))
                    Pill(
                        when (info.severity) { Severity.HIGH -> "Fix soon"; Severity.MEDIUM -> "Check it"; Severity.LOW -> "Minor" },
                        fg, severityBg(info.severity),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(info.description, style = MaterialTheme.typography.titleMedium)
                info.tip?.let {
                    Spacer(Modifier.height(10.dp))
                    Row {
                        Icon(Icons.Filled.Info, null, tint = Palette.TextMuted, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(Palette.Panel).border(1.dp, Palette.Line, RoundedCornerShape(12.dp)).padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.clip(RoundedCornerShape(9.dp))
                    .background(if (on) Palette.PanelHigh else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Text(label, color = if (on) Palette.Text else Palette.TextMuted, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// =================================================================== SETTINGS

@Composable
fun SettingsScreen(
    state: UiState,
    onThemeMode: (dev.diyscan.ui.theme.ThemeMode) -> Unit = {},
    onImperial: (Boolean) -> Unit,
    onDisconnect: () -> Unit,
    onOpenImports: () -> Unit = {},
    onAutoUpdate: (Boolean) -> Unit = {},
    onCheckUpdate: () -> Unit = {},
) {
    val c = state.conn as? Conn.Connected
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPad)) {
        ScreenTitle("Tools & settings", "Theme, units, imports and updates")
        Panel(Modifier.fillMaxWidth()) {
            SectionLabel("Instrument theme")
            Spacer(Modifier.height(4.dp))
            Text(
                when (state.themeMode) {
                    dev.diyscan.ui.theme.ThemeMode.SYSTEM -> "Follows your phone's light/dark setting."
                    dev.diyscan.ui.theme.ThemeMode.LIGHT -> "Workshop Daylight: high contrast under shop lights."
                    dev.diyscan.ui.theme.ThemeMode.DARK -> "Night Cockpit: low glare in a dark cab."
                },
                color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            val modes = dev.diyscan.ui.theme.ThemeMode.entries
            SegmentedControl(modes.map { it.label }, modes.indexOf(state.themeMode), { onThemeMode(modes[it]) })
        }
        Spacer(Modifier.height(12.dp))
        Panel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("US units", style = MaterialTheme.typography.titleMedium)
                    Text("°F, mph, psi. Off for °C, km/h, kPa.", color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = state.imperial, onCheckedChange = onImperial,
                    colors = SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Palette.OnAccent),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        ImportsEntry(state, onOpenImports)
        Spacer(Modifier.height(12.dp))
        Panel(Modifier.fillMaxWidth()) {
            SectionLabel("Updates")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Check on startup", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Asks GitHub if a newer build exists. Nothing else is sent." +
                            if (state.installedBuild > 1) " You have build ${state.installedBuild}." else " This is a local build.",
                        color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(
                    checked = state.autoUpdateCheck, onCheckedChange = onAutoUpdate,
                    colors = SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Palette.OnAccent),
                )
            }
            Spacer(Modifier.height(12.dp))
            GhostButton(if (state.updateChecking) "Checking…" else "Check now", icon = Icons.Filled.Refresh,
                enabled = !state.updateChecking, onClick = onCheckUpdate)
        }
        Spacer(Modifier.height(12.dp))
        if (c != null) {
            Panel(Modifier.fillMaxWidth()) {
                SectionLabel("Connection")
                Spacer(Modifier.height(10.dp))
                InfoRow("Device", c.name)
                InfoRow("Adapter", c.adapter.ifBlank { "Unknown" })
                InfoRow("Protocol", c.protocol.label)
                Spacer(Modifier.height(12.dp))
                GhostButton("Disconnect", icon = Icons.Filled.Close, tint = Palette.Fault, onClick = onDisconnect)
            }
            Spacer(Modifier.height(12.dp))
        }
        Panel(Modifier.fillMaxWidth()) {
            SectionLabel("About")
            Spacer(Modifier.height(10.dp))
            Text("DIY Scan " + if (state.installedBuild > 1) "build ${state.installedBuild}" else "(local build)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Generic OBD-II (SAE J1979) for 1996+ vehicles. Reads codes, freeze frame, readiness, VIN and live sensors. " +
                    "Manufacturer-specific modules (ABS, airbag, transmission) aren't supported yet.",
                color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text("Don't use the app while driving.", color = Palette.Warn, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start)
        }
    }
}

// =================================================================== BUS MONITOR

@Composable
fun MonitorScreen(
    state: UiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit,
    onOpenReplay: () -> Unit,
) {
    var view by remember { mutableStateOf(0) }
    val c = state.conn as? Conn.Connected
    val now = System.currentTimeMillis()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = ScreenPad, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            ScreenTitle(
                "Bus monitor",
                when {
                    state.monitorRunning -> "%.0f msgs/sec · %d total · %d IDs".format(state.busRate, state.busTotal, state.busIds.size)
                    state.busTotal > 0 -> "Stopped · ${state.busTotal} captured"
                    else -> c?.protocol?.label ?: "Idle"
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.monitorRunning) GhostButton("Stop", icon = Icons.Filled.Close, onClick = onStop)
                else AmberButton("Start listening", icon = Icons.Filled.PlayArrow, onClick = onStart)
                GhostButton("Save capture", icon = Icons.Filled.Done, enabled = state.busTotal > 0, onClick = onSave)
            }
            Spacer(Modifier.height(14.dp))
            Segmented(listOf("By ID · ${state.busIds.size}", "Raw stream"), view) { view = it }
            Spacer(Modifier.height(6.dp))
        }

        item { ReplayEntry(state, onOpenReplay) }

        if (!state.monitorRunning && state.busTotal == 0) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionLabel("How to find a message")
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        "Tap Start listening and let the ID list settle for a few seconds.",
                        "Do one thing: open a door, switch the headlights on, press the brake.",
                        "Watch which bytes flash amber — that ID and byte carry that signal.",
                        "Save capture to keep a timestamped record you can study later.",
                    ).forEachIndexed { i, t ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("${i + 1}", color = Palette.Amber, style = Numeric, modifier = Modifier.width(22.dp))
                            Text(t, color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (c?.simulated == true) "Simulator traffic is made-up — it's here to try the screen, not real Chrysler messages."
                        else "On pre-CAN Chryslers this is the PCI bus: every module, not just the engine. Generic OBD requests pause while listening.",
                        color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        if (view == 0) {
            items(state.busIds, key = { it.id }) { b -> BusIdRow(b, now) }
        } else {
            items(state.busRaw) { line ->
                Text(
                    line.chunked(2).joinToString(" "),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = if (line.all { it.isLetterOrDigit() }) Palette.Text else Palette.Warn,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BusIdRow(b: dev.diyscan.BusId, now: Long) {
    Panel(Modifier.fillMaxWidth(), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                b.id.chunked(2).joinToString(" "),
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                color = Palette.Amber, modifier = Modifier.width(96.dp),
            )
            FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                b.data.forEachIndexed { i, byte ->
                    val hot = now - (b.changedAt.getOrNull(i) ?: 0L) < 1500
                    Text(
                        byte,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                        color = if (hot) Palette.OnAccent else Palette.Text,
                        modifier = Modifier.clip(RoundedCornerShape(5.dp))
                            .background(if (hot) Palette.Amber else Palette.Track)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Text("×${b.count}", color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/** Entry card for the request replay / mode 22 sweep screen. */
@Composable
private fun ReplayEntry(state: UiState, onOpen: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Palette.AmberGlow),
                contentAlignment = Alignment.Center,
            ) { Text("RE", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Palette.Amber) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Request replay", style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        state.replayPlan.isNotEmpty() -> "${state.replayPlan.size} requests · ${state.replayName ?: "plan"}"
                        else -> "Ask the vehicle what a scan tool asked it, and keep the answers"
                    },
                    color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = Palette.TextMuted)
        }
    }
}



/** Shown on startup (or after Check now) when GitHub has a newer build. */
@Composable
fun UpdateDialog(info: dev.diyscan.UpdateInfo, installed: Int, onDownload: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        icon = { Icon(Icons.Filled.Refresh, null, tint = Palette.Amber) },
        title = { Text("Update available") },
        text = {
            Column {
                Text(
                    "Build ${info.build} is out" + if (installed > 1) " (you have $installed)." else ".",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (info.notes.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(info.notes.take(600), color = Palette.TextMuted, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Download opens the APK in your browser. Open it when it finishes and tap Update. Your imports and settings are kept.",
                    color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDownload) { Text("Download", color = Palette.Amber) } },
        dismissButton = { TextButton(onClick = onLater) { Text("Later", color = Palette.Text) } },
        containerColor = Palette.PanelHigh,
    )
}

/** Short PID tag shown on gauges and tiles: "0x0C" for standard PIDs, the request (e.g. "221940") for imported ones. */
fun pidLabel(p: Pid): String = p.request ?: "0x${p.hex}"
