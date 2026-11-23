package dev.diyscan.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.diyscan.ProfileInfo
import dev.diyscan.UiState
import dev.diyscan.ui.theme.Palette

/**
 * Everything the user brings to the app: PID profiles for any make, the Chrysler DRB-III
 * database, and request replay plans. Nothing here ships with the app; it all stays on the device.
 */
@Composable
fun ImportsScreen(
    state: UiState,
    onClose: () -> Unit,
    onAddProfile: () -> Unit,
    onToggleProfile: (String, Boolean) -> Unit,
    onRemoveProfile: (String) -> Unit,
    onOpenDrb: () -> Unit,
    onImportReplay: () -> Unit,
    onOpenReplay: () -> Unit,
) {
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ArrowBack, "Back", tint = Palette.Text,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onClose).padding(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Imports", style = MaterialTheme.typography.titleLarge)
                Text("Vehicle data you bring. Stored only on this device.", color = Palette.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // ---------------------------------------------------- PID profiles
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    SectionLabel("PID profiles", Modifier.weight(1f))
                    Text("${state.customPids.size} sensors active", color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Extra sensors for any make: GM, Ford, Toyota, Chrysler and more. Use a Torque-style .csv " +
                        "(Name, ShortName, ModeAndPID, Equation, Min, Max, Units, Header). Turn profiles on and off to match the vehicle you're on.",
                    color = Palette.TextMuted, style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.profiles.isEmpty()) {
                item {
                    Panel(Modifier.fillMaxWidth()) {
                        Text("No profiles yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Find PID lists on vehicle forums, or convert one from the open OBDb project with tools/obdb_to_profile.py in the repo.",
                            color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            items(state.profiles, key = { it.id }) { p -> ProfileRow(p, onToggleProfile, onRemoveProfile) }
            item { AmberButton("Add CSV profile", icon = Icons.Filled.Add, onClick = onAddProfile) }

            // ---------------------------------------------------- Make-specific databases
            item {
                Spacer(Modifier.height(14.dp))
                SectionLabel("Manufacturer databases")
            }
            item {
                ImportRow(
                    badge = "DRB", title = "Chrysler DRB-III database",
                    subtitle = state.drbStatus ?: if (state.drbModules.isEmpty()) "Import your own database.mem (Chrysler, Dodge, Jeep 1996–2007)"
                    else "%,d modules · %,d commands".format(state.drbModules.size, state.drbCommandTotal),
                    onClick = onOpenDrb,
                )
            }

            // ---------------------------------------------------- Replay plans
            item {
                Spacer(Modifier.height(14.dp))
                SectionLabel("Request replay plans")
                Spacer(Modifier.height(4.dp))
                Text(
                    "A list of requests captured from another scan tool. Running one asks the vehicle the same questions and saves the answers. Read-only requests only.",
                    color = Palette.TextMuted, style = MaterialTheme.typography.bodySmall,
                )
            }
            item {
                ImportRow(
                    badge = "RE", title = state.replayName ?: "No plan loaded",
                    subtitle = if (state.replayPlan.isEmpty()) "Import a plan, or open replay to try the demo"
                    else "${state.replayPlan.size} requests",
                    onClick = onOpenReplay,
                )
            }
            item { GhostButton("Import plan", icon = Icons.Filled.Add, onClick = onImportReplay) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ProfileRow(p: ProfileInfo, onToggle: (String, Boolean) -> Unit, onRemove: (String) -> Unit) {
    var showSkipped by remember(p.id) { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth(), padding = 14.dp, accent = if (p.enabled) Palette.Amber else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, color = if (p.enabled) Palette.Text else Palette.TextMuted)
                Text(
                    "${p.pids.size} sensors" + if (p.skipped.isNotEmpty()) " · ${p.skipped.size} skipped" else "",
                    color = Palette.TextMuted, style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = p.enabled, onCheckedChange = { onToggle(p.id, it) },
                colors = SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Palette.OnAccent),
            )
            Icon(Icons.Filled.Delete, "Remove", tint = Palette.TextMuted,
                modifier = Modifier.padding(start = 6.dp).clip(CircleShape).clickable { onRemove(p.id) }.padding(8.dp))
        }
        if (p.skipped.isNotEmpty()) {
            Text(
                if (showSkipped) "Hide skipped" else "Why were some skipped?",
                color = Palette.Amber, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 6.dp).clickable { showSkipped = !showSkipped },
            )
            if (showSkipped) {
                Spacer(Modifier.height(4.dp))
                p.skipped.take(30).forEach { Text(it, color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall) }
                if (p.skipped.size > 30) Text("…and ${p.skipped.size - 30} more", color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ImportRow(badge: String, title: String, subtitle: String, onClick: () -> Unit) {
    Panel(Modifier.fillMaxWidth(), onClick = onClick, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Palette.AmberGlow),
                contentAlignment = Alignment.Center,
            ) { Text(badge, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Palette.Amber) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = Palette.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = Palette.TextMuted)
        }
    }
}

/** Summary card that opens the Imports screen. Used on the connect screen and in Settings. */
@Composable
fun ImportsEntry(state: UiState, onOpen: () -> Unit) {
    val parts = buildList {
        val on = state.profiles.count { it.enabled }
        if (state.profiles.isNotEmpty()) add("$on of ${state.profiles.size} profiles on")
        if (state.drbModules.isNotEmpty()) add("DRB-III database")
        if (state.replayPlan.isNotEmpty()) add("replay plan")
    }
    ImportRow(
        badge = "IMP", title = "Imports",
        subtitle = if (parts.isEmpty()) "Add PID profiles and manufacturer data" else parts.joinToString(" · "),
        onClick = onOpen,
    )
}
