package dev.diyscan.ui

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
) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Palette.AmberGlow, Color.Transparent), radius = 900f)
        )
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                Spacer(Modifier.height(28.dp))
                Logo()
                Spacer(Modifier.height(22.dp))
                Text("Plug in. Key on.\nSee what your truck is saying.", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Works with ELM327-compatible Bluetooth adapters on any 1996+ vehicle — J1850, ISO 9141, KWP2000 and CAN.",
                    style = MaterialTheme.typography.bodyLarge, color = Palette.TextMuted,
                )
                Spacer(Modifier.height(28.dp))

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
                                "Plug the adapter into the port under the dash, turn the key on, then pair it in Android Bluetooth settings. The PIN is usually 1234 or 0000.",
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

                    Spacer(Modifier.height(26.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).height(1.dp).background(Palette.Line))
                        Text("  no adapter handy?  ", color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall)
                        Box(Modifier.weight(1f).height(1.dp).background(Palette.Line))
                    }
                    Spacer(Modifier.height(18.dp))
                    Panel(Modifier.fillMaxWidth(), onClick = onSimulator, accent = Palette.Info) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Palette.InfoBg),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.PlayArrow, null, tint = Palette.Info) }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Try the simulator", style = MaterialTheme.typography.titleMedium)
                                Text("A virtual 2005 Ram 1500 with a few real-world faults.", color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                            Icon(Icons.Filled.KeyboardArrowRight, null, tint = Palette.TextMuted)
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun Logo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFFC24D), Palette.Amber))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Build, null, tint = Color(0xFF1C1300), modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(12.dp))
        Text("DIY", style = MaterialTheme.typography.titleLarge, color = Palette.Amber, fontWeight = FontWeight.Black)
        Text(" Scan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
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
            "Older trucks (J1850 / K-line) can take 5–10 seconds to answer the first time.",
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

fun severityColor(s: Severity) = when (s) { Severity.HIGH -> Palette.Fault; Severity.MEDIUM -> Palette.Warn; Severity.LOW -> Palette.Info }
fun severityBg(s: Severity) = when (s) { Severity.HIGH -> Palette.FaultBg; Severity.MEDIUM -> Palette.WarnBg; Severity.LOW -> Palette.InfoBg }

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
                        Panel(Modifier.weight(1f), padding = 10.dp) {
                            ArcGauge(
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
                    Text("Fewer sensors = faster updates on older trucks.", color = Palette.TextMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                        state.availablePids.forEach { p ->
                            Row(
                                Modifier.fillMaxWidth().clickable { onTogglePid(p) }.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = p in state.dashboard, onCheckedChange = { onTogglePid(p) },
                                    colors = CheckboxDefaults.colors(checkedColor = Palette.Amber, checkmarkColor = Color(0xFF1C1300)),
                                )
                                Text(p.name, style = MaterialTheme.typography.bodyMedium)
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
                        "It also resets all smog readiness monitors — the truck won't pass an emissions test until it's been driven through its monitors again (often 50–100 miles).\n\n" +
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
fun SettingsScreen(state: UiState, onImperial: (Boolean) -> Unit, onDisconnect: () -> Unit) {
    val c = state.conn as? Conn.Connected
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(ScreenPad)) {
        ScreenTitle("Settings")
        Panel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("US units", style = MaterialTheme.typography.titleMedium)
                    Text("°F, mph, psi. Off for °C, km/h, kPa.", color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = state.imperial, onCheckedChange = onImperial,
                    colors = SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Color(0xFF1C1300)),
                )
            }
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
            Text("DIY Scan 0.1", style = MaterialTheme.typography.titleMedium)
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
