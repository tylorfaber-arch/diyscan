package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DtcCategory
import com.example.model.DtcEntry
import com.example.model.DtcSeverity
import com.example.model.FreezeFrame
import com.example.ui.components.HardwarePill
import com.example.ui.components.InstrumentCard
import com.example.ui.components.OutlineInstrumentButton
import com.example.ui.components.PrimaryInstrumentButton
import com.example.ui.components.SectionMicroHeader
import com.example.ui.components.SegmentedControl
import com.example.ui.theme.LocalInstrumentColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.TelemetryReadoutStyle
import com.example.viewmodel.ScanUiState

@Composable
fun CodesScreen(
    state: ScanUiState,
    onReadCodes: () -> Unit,
    onClearCodes: () -> Unit,
    onRestoreDemoFaults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalInstrumentColors.current
    var selectedCategoryIdx by rememberSaveable { mutableStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }

    val selectedCategory = DtcCategory.entries[selectedCategoryIdx]
    val filteredCodes = state.dtcs.filter { it.category == selectedCategory }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Trouble Codes",
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.ink,
                    )
                    Text(
                        text = "${state.dtcs.size} total across Modes 03 / 07 / 0A",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkMuted,
                    )
                }
                HardwarePill(
                    text = if (state.activeFaultCount > 0) "${state.activeFaultCount} ACTIVE" else "CLEAR",
                    fg = if (state.activeFaultCount > 0) c.fault else c.good,
                    bg = if (state.activeFaultCount > 0) c.faultBg else c.goodBg,
                )
            }
        }

        if (state.readingCodes) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = c.accent,
                    trackColor = c.well,
                )
            }
        }

        // Actions row
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryInstrumentButton(
                    text = "Scan ECU",
                    onClick = onReadCodes,
                    enabled = !state.readingCodes,
                    modifier = Modifier.weight(1f),
                    testTag = "scan_codes_button",
                )
                OutlineInstrumentButton(
                    text = "Clear Codes",
                    onClick = { confirmClear = true },
                    enabled = !state.readingCodes && state.activeFaultCount > 0,
                    accentColor = if (state.activeFaultCount > 0) c.fault else c.bezel,
                    modifier = Modifier.weight(1f),
                    testTag = "clear_codes_button",
                )
            }
        }

        // Segmented Category Tabs
        item {
            val labels = DtcCategory.entries.map { cat ->
                val count = state.dtcs.count { it.category == cat }
                "${cat.label} ($count)"
            }
            SegmentedControl(
                options = labels,
                selectedIndex = selectedCategoryIdx,
                onSelect = { selectedCategoryIdx = it },
                testTagPrefix = "code_category",
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = selectedCategory.helper,
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
            )
        }

        if (filteredCodes.isEmpty()) {
            item {
                InstrumentCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = c.good.copy(alpha = 0.45f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "No trouble codes",
                            tint = c.good,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "No ${selectedCategory.label.lowercase()} codes",
                                style = MaterialTheme.typography.titleMedium,
                                color = c.ink,
                            )
                            Text(
                                text = "ECU reported zero DTCs in this category.",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.inkMuted,
                            )
                        }
                    }
                    if (state.activeFaultCount == 0) {
                        Spacer(Modifier.height(12.dp))
                        OutlineInstrumentButton(
                            text = "Restore Demo Faults",
                            onClick = onRestoreDemoFaults,
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "restore_demo_faults_button",
                        )
                    }
                }
            }
        } else {
            items(filteredCodes, key = { "${it.category}_${it.code}" }) { dtc ->
                DtcDiagnosticCard(dtc)
            }
        }

        // Freeze Frame Snapshot (shown under Stored codes)
        if (selectedCategory == DtcCategory.STORED && state.freezeFrame != null) {
            item {
                FreezeFrameCard(state.freezeFrame, imperial = state.imperialUnits)
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = c.fault)
            },
            title = {
                Text("Clear Stored & Pending Codes?", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Text(
                    "Sending Mode 04 clears stored DTCs, freeze frame data, and turns off the check engine light. " +
                        "It also resets emissions readiness monitors to Incomplete until you complete a full drive cycle. " +
                        "Permanent codes (Mode 0A) cannot be erased by any scanner.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearCodes()
                    },
                    modifier = Modifier.testTag("confirm_clear_codes_button"),
                ) {
                    Text("Send Mode 04 Clear", color = c.fault, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun DtcDiagnosticCard(dtc: DtcEntry) {
    val c = LocalInstrumentColors.current
    var expanded by rememberSaveable(dtc.code) { mutableStateOf(dtc.severity == DtcSeverity.HIGH) }

    val (sevFg, sevBg) = when (dtc.severity) {
        DtcSeverity.HIGH -> c.fault to c.faultBg
        DtcSeverity.MEDIUM -> c.warn to c.warnBg
        DtcSeverity.LOW -> c.bus to c.busBg
    }

    InstrumentCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dtc_card_${dtc.code}"),
        accentBorder = if (dtc.severity == DtcSeverity.HIGH) c.fault.copy(alpha = 0.5f) else null,
        onClick = { expanded = !expanded },
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(sevBg)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = dtc.code,
                        style = TelemetryReadoutStyle.copy(fontSize = 16.sp, letterSpacing = 0.8.sp),
                        color = sevFg,
                    )
                }
                Spacer(Modifier.width(10.dp))
                HardwarePill(
                    text = dtc.severity.label.uppercase(),
                    fg = sevFg,
                    bg = sevBg,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse details" else "Expand details",
                tint = c.inkMuted,
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = dtc.title,
            style = MaterialTheme.typography.titleMedium,
            color = c.ink,
        )
        Text(
            text = dtc.subsystem,
            style = MaterialTheme.typography.labelSmall,
            color = c.inkMuted,
        )

        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(top = 12.dp)) {
                HorizontalDivider(color = c.bezel.copy(alpha = 0.7f))
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(c.well)
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = null,
                        tint = c.accent,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = dtc.tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.ink,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "COMMON CAUSES",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkFaint,
                )
                Spacer(Modifier.height(4.dp))
                dtc.causes.forEach { cause ->
                    Text(
                        text = "•  $cause",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkMuted,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FreezeFrameCard(ff: FreezeFrame, imperial: Boolean) {
    val c = LocalInstrumentColors.current
    val speedDisp = if (imperial) "%.0f mph".format(ff.speedKph * 0.621371) else "%.0f km/h".format(ff.speedKph)
    val tempDisp = if (imperial) "%.0f °F".format(ff.coolantC * 9.0 / 5.0 + 32.0) else "%.0f °C".format(ff.coolantC)

    InstrumentCard(Modifier.fillMaxWidth()) {
        SectionMicroHeader(
            title = "Mode 02 · Freeze Frame Snapshot",
            badge = "TRIGGER ${ff.triggerCode}",
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = ff.fuelSystem,
            style = MaterialTheme.typography.bodySmall,
            color = c.inkMuted,
        )
        Spacer(Modifier.height(12.dp))
        val metrics = listOf(
            "RPM" to "%.0f rpm".format(ff.rpm),
            "Speed" to speedDisp,
            "Coolant" to tempDisp,
            "Engine Load" to "%.1f%%".format(ff.loadPct),
            "STFT Bank 1" to "+%.1f%%".format(ff.stftPct),
            "LTFT Bank 1" to "+%.1f%%".format(ff.ltftPct),
            "MAF Flow" to "%.1f g/s".format(ff.mafGs),
        )
        metrics.chunked(2).forEach { pair ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pair.forEach { (k, v) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(c.well)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(k, style = MaterialTheme.typography.labelSmall, color = c.inkMuted)
                        Text(
                            text = v,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = MonoFontFamily,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = c.ink,
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
