package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BusFrame
import com.example.model.ConnectionState
import com.example.model.ReplayResult
import com.example.model.ReplayStatus
import com.example.ui.components.HardwarePill
import com.example.ui.components.InstrumentCard
import com.example.ui.components.OutlineInstrumentButton
import com.example.ui.components.PrimaryInstrumentButton
import com.example.ui.components.SectionMicroHeader
import com.example.ui.theme.HexCodeStyle
import com.example.ui.theme.LocalInstrumentColors
import com.example.ui.theme.MonoFontFamily
import com.example.viewmodel.ScanUiState

@Composable
fun BusMonitorScreen(
    state: ScanUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSaveCapture: () -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalInstrumentColors.current
    val conn = state.connection as? ConnectionState.Connected
    val busLabel = if (conn?.device?.isChryslerPci == true) "ATMA · CHRYSLER PCI BUS" else "ATMA · CAN 500 KBPS"
    val filteredFrames = state.busFrames.filter {
        state.busSearchQuery.isBlank() ||
            it.arbId.contains(state.busSearchQuery, ignoreCase = true) ||
            it.moduleHint.contains(state.busSearchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Bus Monitor",
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.ink,
                    )
                    Text(
                        text = "$busLabel · ${state.busTotalFrames} frames",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkMuted,
                    )
                }
                HardwarePill(
                    text = if (state.busRunning) "SNIFFING" else "PAUSED",
                    fg = if (state.busRunning) c.bus else c.inkMuted,
                    bg = if (state.busRunning) c.busBg else c.well,
                )
            }
        }

        // Control strip
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryInstrumentButton(
                    text = if (state.busRunning) "Pause Stream" else "Listen (ATMA)",
                    onClick = { if (state.busRunning) onStop() else onStart() },
                    containerColor = if (state.busRunning) c.ink else c.accent,
                    modifier = Modifier.weight(1f),
                    testTag = "bus_stream_toggle",
                )
                OutlineInstrumentButton(
                    text = "Save Log",
                    onClick = onSaveCapture,
                    modifier = Modifier.weight(0.75f),
                    testTag = "bus_save_capture_button",
                )
            }
        }

        // Request Replay & Mode 22 Sweep Launcher Card
        item {
            InstrumentCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_replay_workbench_card"),
                accentBorder = c.accent.copy(alpha = 0.4f),
                onClick = onOpenReplay,
                padding = 14.dp,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(c.accentBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Terminal,
                            contentDescription = null,
                            tint = c.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Request Replay & Mode 22 Sweep",
                            style = MaterialTheme.typography.titleMedium,
                            color = c.ink,
                        )
                        Text(
                            text = state.replayPlanName ?: "Re-run scan tool catalogs or sweep 7E0/7E1 read-only",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.inkMuted,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open request replay",
                        tint = c.inkMuted,
                    )
                }
            }
        }

        // Filter search bar
        item {
            OutlinedTextField(
                value = state.busSearchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Filter by Arb ID (0x0C8) or module…", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = c.inkMuted) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.accent,
                    unfocusedBorderColor = c.bezel,
                    focusedContainerColor = c.plate,
                    unfocusedContainerColor = c.plate,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bus_filter_input"),
            )
        }

        item {
            SectionMicroHeader(
                title = "Grouped Arbitration IDs (${filteredFrames.size})",
                badge = "CHANGED BYTES HIGHLIGHTED",
            )
        }

        items(filteredFrames, key = { it.arbId }) { frame ->
            BusFrameCard(frame)
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BusFrameCard(frame: BusFrame) {
    val c = LocalInstrumentColors.current
    val hasChange = frame.changedMask.any { it }

    InstrumentCard(
        modifier = Modifier.fillMaxWidth(),
        accentBorder = if (hasChange) c.accent.copy(alpha = 0.45f) else null,
        padding = 12.dp,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(c.busBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = frame.arbId,
                        style = HexCodeStyle.copy(fontWeight = FontWeight.Bold),
                        color = c.bus,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = frame.moduleHint,
                    style = MaterialTheme.typography.titleSmall,
                    color = c.ink,
                )
            }
            Text(
                text = "%.1f Hz · #%d".format(frame.rateHz, frame.count),
                style = MaterialTheme.typography.labelSmall,
                color = c.inkFaint,
            )
        }

        Spacer(Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            frame.bytes.forEachIndexed { idx, value ->
                val changed = frame.changedMask.getOrElse(idx) { false }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (changed) c.accentBg else c.well)
                        .border(
                            width = 1.dp,
                            color = if (changed) c.accent else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = value.toString(16).uppercase().padStart(2, '0'),
                        style = HexCodeStyle.copy(
                            fontWeight = if (changed) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                        ),
                        color = if (changed) c.accent else c.ink,
                    )
                }
            }
        }
    }
}

@Composable
fun ReplayScreen(
    state: ScanUiState,
    onClose: () -> Unit,
    onLoadDemoPlan: () -> Unit,
    onLoadSweepPlan: () -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onSaveCsv: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)
    val c = LocalInstrumentColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .statusBarsPadding(),
    ) {
        // Top header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("replay_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = c.ink,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Request Replay & Sweep",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink,
                )
                Text(
                    text = state.replayStatus ?: "Read-only UDS & Mode 22 safety engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkMuted,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Read-only safety badge card
            item {
                InstrumentCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = c.good.copy(alpha = 0.45f),
                    padding = 14.dp,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = c.good,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Read-only firewall active: Clears (0x14), routines (0x31), writes (0x2E), and seed/key (0x27) are automatically refused.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.ink,
                        )
                    }
                }
            }

            // Plan loader / executor
            item {
                InstrumentCard(Modifier.fillMaxWidth()) {
                    SectionMicroHeader(
                        title = "Replay Plan",
                        badge = if (state.replayPlan.isEmpty()) "NONE LOADED" else "${state.replayPlan.size} REQUESTS",
                    )
                    Spacer(Modifier.height(10.dp))

                    if (state.replayPlan.isEmpty()) {
                        Text(
                            text = "Load a catalog replay plan (tools/obd_catalog.py) or launch a CAN Mode 22 discovery sweep on 7E0 (Engine) & 7E1 (TCM).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.inkMuted,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryInstrumentButton(
                                text = "Demo Plan",
                                onClick = onLoadDemoPlan,
                                modifier = Modifier.weight(1f),
                                testTag = "load_demo_replay_button",
                            )
                            OutlineInstrumentButton(
                                text = "Mode 22 Sweep",
                                onClick = onLoadSweepPlan,
                                modifier = Modifier.weight(1f),
                                testTag = "load_mode22_sweep_button",
                            )
                        }
                    } else {
                        Text(
                            text = state.replayPlanName ?: "Loaded Plan",
                            style = MaterialTheme.typography.titleMedium,
                            color = c.ink,
                        )
                        val safeCount = state.replayPlan.count { it.readOnly }
                        val blockedCount = state.replayPlan.size - safeCount
                        Text(
                            text = "$safeCount read-only · $blockedCount write/seed commands will be blocked",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.inkMuted,
                        )
                        Spacer(Modifier.height(10.dp))
                        if (state.replayRunning) {
                            val progress = state.replayDone.toFloat() / state.replayPlan.size.coerceAtLeast(1)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = c.accent,
                                trackColor = c.well,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Sending request ${state.replayDone} of ${state.replayPlan.size}…",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.accent,
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (state.replayRunning) {
                                OutlineInstrumentButton(
                                    text = "Stop",
                                    onClick = onStop,
                                    accentColor = c.fault,
                                    modifier = Modifier.weight(1f),
                                    testTag = "stop_replay_button",
                                )
                            } else {
                                PrimaryInstrumentButton(
                                    text = "Run Plan (${state.replayPlan.size})",
                                    onClick = onRun,
                                    modifier = Modifier.weight(1f),
                                    testTag = "run_replay_button",
                                )
                            }
                            OutlineInstrumentButton(
                                text = if (state.replayPlanName?.contains("Sweep") == true) "Demo Plan" else "Sweep 22",
                                onClick = {
                                    if (state.replayPlanName?.contains("Sweep") == true) onLoadDemoPlan()
                                    else onLoadSweepPlan()
                                },
                                modifier = Modifier.weight(0.85f),
                                testTag = "switch_replay_plan_button",
                            )
                        }
                    }
                }
            }

            if (state.replayResults.isNotEmpty()) {
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SectionMicroHeader(
                            title = "ECU Replies (${state.replayResults.size})",
                            modifier = Modifier.weight(1f),
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onSaveCsv)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("save_replay_csv_button"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Export CSV", style = MaterialTheme.typography.labelLarge, color = c.accent)
                        }
                    }
                }

                items(state.replayResults) { res ->
                    ReplayResultRow(res)
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun ReplayResultRow(res: ReplayResult) {
    val c = LocalInstrumentColors.current
    val (statusLabel, fg, bg) = when (res.status) {
        ReplayStatus.ANSWERED -> Triple("OK", c.good, c.goodBg)
        ReplayStatus.NO_ANSWER -> Triple("NO ANSWER", c.warn, c.warnBg)
        ReplayStatus.REFUSED -> Triple("REFUSED", c.fault, c.faultBg)
    }

    InstrumentCard(
        modifier = Modifier.fillMaxWidth(),
        accentBorder = if (res.status == ReplayStatus.REFUSED) c.fault.copy(alpha = 0.45f) else null,
        padding = 12.dp,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${res.module}  ${res.requestHex}",
                style = HexCodeStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = c.accent,
            )
            HardwarePill(text = statusLabel, fg = fg, bg = bg, showDot = false)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = res.label,
            style = MaterialTheme.typography.bodySmall,
            color = c.inkMuted,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(c.well)
                .padding(horizontal = 10.dp, vertical = 7.dp),
        ) {
            Text(
                text = res.payloadHex ?: res.note ?: "—",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = if (res.payloadHex != null) MonoFontFamily else MaterialTheme.typography.bodySmall.fontFamily,
                    fontWeight = if (res.payloadHex != null) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (res.payloadHex != null) c.ink else fg,
            )
        }
    }
}
