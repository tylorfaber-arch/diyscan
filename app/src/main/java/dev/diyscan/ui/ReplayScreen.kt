package dev.diyscan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.diyscan.UiState
import dev.diyscan.obd.Replay
import dev.diyscan.obd.ReplayResult
import dev.diyscan.ui.theme.Palette

/**
 * Re-run the requests a scan tool made, or sweep a module for mode 22 PIDs.
 *
 * The point is keeping access after a borrowed tool goes back: a module answers the same
 * bytes to the same request no matter which device asked.
 */
@Composable
fun ReplayScreen(
    state: UiState,
    onClose: () -> Unit,
    onImport: () -> Unit,
    onDemo: () -> Unit,
    onSweep: () -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onSave: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Request replay", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (state.replayRunning) "${state.replayDone} / ${state.replayTotal}"
                        else state.replayStatus ?: "Read-only requests",
                        style = MaterialTheme.typography.bodyMedium, color = Palette.TextMuted,
                    )
                }
                TextButton(onClick = onClose) { Text("Close", color = Palette.Amber) }
            }
        }

        if (state.replayPlan.isEmpty()) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    SectionLabel("Nothing loaded")
                    Spacer(Modifier.height(8.dp))
                    Text("Import the replay.txt written by tools/obd_catalog.py", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "It holds every request your scan tool made, one per line: the module, then the bytes. " +
                            "Running it asks the vehicle the same questions, so the readings outlive the tool.",
                        color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Only read services are sent. Code clears, routines, writes and seed/key exchanges are refused.",
                        color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(14.dp))
                    AmberButton("Import replay.txt", onClick = onImport)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GhostButton("Demo plan", onClick = onDemo)
                        GhostButton("Sweep mode 22", onClick = onSweep)
                    }
                }
            }
            item { SweepNote() }
        } else {
            item { PlanPanel(state, onImport, onSweep, onRun, onStop) }

            if (state.replayResults.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Replies · ${state.replayResults.count { r -> r.answered }} answered")
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onSave) { Text("Save CSV", color = Palette.Amber, style = MaterialTheme.typography.labelLarge) }
                    }
                }
                items(state.replayResults) { r -> ReplyRow(r) }
            }
        }
    }
}

@Composable
private fun PlanPanel(
    state: UiState,
    onImport: () -> Unit,
    onSweep: () -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
) {
    Panel(Modifier.fillMaxWidth()) {
        SectionLabel("Plan")
        Spacer(Modifier.height(8.dp))
        Text(state.replayName ?: "Plan", style = MaterialTheme.typography.titleMedium)
        val reads = state.replayPlan.count { Replay.isReadOnly(it.request) }
        Text(
            "${state.replayPlan.size} requests · $reads read-only" +
                if (reads < state.replayPlan.size) " · ${state.replayPlan.size - reads} will be refused" else "",
            color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(10.dp))
        state.replayPlan.take(6).forEach { r ->
            Text(
                "${r.module ?: "default"}  ${r.request}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = Palette.Text,
            )
        }
        if (state.replayPlan.size > 6) {
            Text("+${state.replayPlan.size - 6} more", color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall)
        }
        if (state.replayRunning) {
            Spacer(Modifier.height(8.dp))
            Text("Asking… ${state.replayDone} of ${state.replayTotal}", color = Palette.Amber, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.replayRunning) GhostButton("Stop", onClick = onStop)
            else AmberButton("Run ${state.replayPlan.size}", onClick = onRun)
            GhostButton("Import", onClick = onImport)
            GhostButton("Sweep", onClick = onSweep)
        }
    }
}

@Composable
private fun ReplyRow(r: ReplayResult) {
    Panel(Modifier.fillMaxWidth(), padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${r.module ?: "default"}  ${r.request}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                color = Palette.Amber,
            )
            Spacer(Modifier.weight(1f))
            Text(
                when {
                    r.skipped != null -> "refused"
                    r.answered -> "ok"
                    else -> "no answer"
                },
                color = if (r.answered) Palette.Good else Palette.Warn,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            when {
                r.answered -> r.payload
                r.skipped != null -> r.skipped
                else -> r.error ?: "—"
            },
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = if (r.answered) Palette.Text else Palette.TextMuted,
        )
    }
}

@Composable
private fun SweepNote() {
    Panel(Modifier.fillMaxWidth()) {
        SectionLabel("About the sweep")
        Spacer(Modifier.height(8.dp))
        Text(
            "A sweep asks a module for every mode 22 PID in a range, 256 requests, and keeps whatever answers. " +
                "It runs on CAN only, on the engine (7E0) and transmission (7E1): other modules stay asleep until a " +
                "diagnostic session is opened, and opening one is a write.",
            color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Expect it to take a couple of minutes. Most PIDs answer nothing; the ones that do are the map.",
            color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall,
        )
    }
}
