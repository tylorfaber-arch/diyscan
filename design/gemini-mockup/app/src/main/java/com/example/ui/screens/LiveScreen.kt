package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MockVehicleCatalog
import com.example.model.DriveMode
import com.example.ui.components.HardwarePill
import com.example.ui.components.InstrumentCard
import com.example.ui.components.PrecisionArcGauge
import com.example.ui.components.SectionMicroHeader
import com.example.ui.components.SegmentedControl
import com.example.ui.components.TelemetrySparklineCard
import com.example.ui.theme.LocalInstrumentColors
import com.example.ui.theme.MonoFontFamily
import com.example.viewmodel.ScanUiState

@Composable
fun LiveScreen(
    state: ScanUiState,
    onToggleLive: () -> Unit,
    onToggleLogging: () -> Unit,
    onSelectDriveMode: (DriveMode) -> Unit,
    onTogglePid: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalInstrumentColors.current
    var showPicker by remember { mutableStateOf(false) }

    val rpmSpec = MockVehicleCatalog.standardPids.first { it.id == "rpm" }
    val speedSpec = MockVehicleCatalog.standardPids.first { it.id == "speed" }
    val rpmVal = state.liveValues["rpm"] ?: 0.0
    val speedVal = state.liveValues["speed"] ?: 0.0

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val tileColumns = if (maxWidth >= 700.dp) 3 else 2
        val tiles = state.dashboardTiles
        val rows = remember(tiles, tileColumns) { tiles.chunked(tileColumns) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header + Live controls
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Live Telemetry",
                            style = MaterialTheme.typography.headlineSmall,
                            color = c.ink,
                        )
                        Text(
                            text = if (state.liveRunning) "Polling Mode 01 & 22 · 4.2 Hz" else "Polling paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.inkMuted,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Play / Pause pill
                        Row(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (state.liveRunning) c.plate else c.accent)
                                .border(1.dp, if (state.liveRunning) c.bezel else c.accent, RoundedCornerShape(11.dp))
                                .clickable(onClick = onToggleLive)
                                .padding(horizontal = 12.dp)
                                .testTag("live_poll_toggle"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (state.liveRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (state.liveRunning) "Pause polling" else "Resume polling",
                                tint = if (state.liveRunning) c.ink else Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (state.liveRunning) "Pause" else "Live",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                color = if (state.liveRunning) c.ink else Color.White,
                            )
                        }

                        // CSV Record button
                        Row(
                            modifier = Modifier
                                .height(42.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (state.loggingActive) c.fault else c.plate)
                                .border(1.dp, if (state.loggingActive) c.fault else c.bezel, RoundedCornerShape(11.dp))
                                .clickable(onClick = onToggleLogging)
                                .padding(horizontal = 12.dp)
                                .testTag("live_csv_record_button"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (state.loggingActive) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                                contentDescription = if (state.loggingActive) "Stop CSV log" else "Record CSV log",
                                tint = if (state.loggingActive) Color.White else c.fault,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = if (state.loggingActive) "Stop" else "CSV",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                color = if (state.loggingActive) Color.White else c.ink,
                            )
                        }
                    }
                }
            }

            // Active CSV Recording Banner
            item {
                AnimatedVisibility(visible = state.loggingActive) {
                    InstrumentCard(
                        modifier = Modifier.fillMaxWidth(),
                        accentBorder = c.fault.copy(alpha = 0.65f),
                        padding = 12.dp,
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HardwarePill(
                                text = "REC · DRIVE LOGGING",
                                fg = c.fault,
                                bg = c.faultBg,
                            )
                            Text(
                                text = "%02d:%02d · %d rows".format(
                                    state.logSeconds / 60,
                                    state.logSeconds % 60,
                                    state.logSamples,
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                ),
                                color = c.ink,
                            )
                        }
                    }
                }
            }

            // Interactive Demo Throttle / Drive Cycle Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionMicroHeader(
                        title = "Simulator Load Profile",
                        badge = "TAP TO REV MOCK ECU",
                    )
                    SegmentedControl(
                        options = DriveMode.entries.map { it.label },
                        selectedIndex = state.driveMode.ordinal,
                        onSelect = { onSelectDriveMode(DriveMode.entries[it]) },
                        testTagPrefix = "drive_mode",
                    )
                }
            }

            // Dual Precision Radial Arc Gauges (RPM + Vehicle Speed)
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PrecisionArcGauge(
                        label = rpmSpec.shortName,
                        pidCode = rpmSpec.codeHex,
                        value = rpmVal,
                        formattedValue = rpmSpec.formatValue(rpmVal, state.imperialUnits),
                        unit = rpmSpec.displayUnit(state.imperialUnits),
                        min = rpmSpec.displayMin(state.imperialUnits),
                        max = rpmSpec.displayMax(state.imperialUnits),
                        redlineStart = rpmSpec.redlineMetric,
                        accentColor = c.accent,
                        modifier = Modifier.weight(1f),
                    )
                    PrecisionArcGauge(
                        label = speedSpec.shortName,
                        pidCode = speedSpec.codeHex,
                        value = speedSpec.displayValue(speedVal, state.imperialUnits),
                        formattedValue = speedSpec.formatValue(speedVal, state.imperialUnits),
                        unit = speedSpec.displayUnit(state.imperialUnits),
                        min = speedSpec.displayMin(state.imperialUnits),
                        max = speedSpec.displayMax(state.imperialUnits),
                        accentColor = c.bus,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Sensor Grid Header + Picker button
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SectionMicroHeader(
                        title = "Sensor Sparklines",
                        modifier = Modifier.weight(1f),
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(c.plate)
                            .border(1.dp, c.bezel, RoundedCornerShape(10.dp))
                            .clickable { showPicker = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("pick_sensors_button"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Pick sensors",
                            tint = c.accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Pick (${tiles.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                            color = c.ink,
                        )
                    }
                }
            }

            // Sensor Sparkline Grid
            items(rows) { rowPids ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowPids.forEach { pid ->
                        val raw = state.liveValues[pid.id]
                        val historyMetric = state.liveHistory[pid.id].orEmpty()
                        val displayHist = historyMetric.map { pid.displayValue(it, state.imperialUnits) }
                        TelemetrySparklineCard(
                            shortName = pid.shortName,
                            pidCode = pid.codeHex,
                            formattedValue = pid.formatValue(raw, state.imperialUnits),
                            unit = pid.displayUnit(state.imperialUnits),
                            history = displayHist,
                            minBound = pid.displayMin(state.imperialUnits),
                            maxBound = pid.displayMax(state.imperialUnits),
                            isCustom = pid.isCustomProfile,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(tileColumns - rowPids.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = {
                Text("Select Polled Sensors", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val selectable = state.availablePids.filter { it.id != "rpm" && it.id != "speed" }
                    items(selectable, key = { it.id }) { pid ->
                        val checked = pid.id in state.selectedPidIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onTogglePid(pid.id) }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onTogglePid(pid.id) },
                                colors = CheckboxDefaults.colors(checkedColor = c.accent),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = pid.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = c.ink,
                                )
                                Text(
                                    text = "${pid.codeHex} · ${pid.displayUnit(state.imperialUnits)}" +
                                        if (pid.isCustomProfile) " · Custom CSV" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (pid.isCustomProfile) c.bus else c.inkMuted,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Done", color = c.accent)
                }
            },
        )
    }
}
