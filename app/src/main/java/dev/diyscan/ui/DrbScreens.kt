package dev.diyscan.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.diyscan.UiState
import dev.diyscan.drb.DrbCommand
import dev.diyscan.drb.DrbDecode
import dev.diyscan.drb.DrbModule
import dev.diyscan.drb.DrbProtocol
import dev.diyscan.ui.theme.Palette

private enum class ModFilter(val label: String) { FAV("★ Favorites"), PCI("PCI bus"), ALL("All") }

@Composable
fun DrbBrowser(
    state: UiState,
    commandsFor: (Int) -> List<DrbCommand>,
    onImport: () -> Unit,
    onRemove: () -> Unit,
    onClose: () -> Unit,
    onToggleFavorite: (Int) -> Unit = {},
) {
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selected = state.drbModules.firstOrNull { it.id == selectedId }
    BackHandler { if (selectedId != null) selectedId = null else onClose() }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.ArrowBack, "Back", tint = Palette.Text,
                modifier = Modifier.clip(CircleShape).clickable { if (selectedId != null) selectedId = null else onClose() }.padding(12.dp))
            Column(Modifier.weight(1f)) {
                Text(selected?.name ?: "Chrysler database", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (selected != null) "${selected.category} · ${selected.commandCount} commands"
                    else if (state.drbModules.isEmpty()) "Not imported"
                    else "%,d modules · %,d commands".format(state.drbModules.size, state.drbCommandTotal),
                    color = Palette.TextMuted, style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        when {
            state.drbModules.isEmpty() -> DrbEmpty(state.drbStatus, onImport)
            selected != null -> ModuleDetail(selected, commandsFor)
            else -> ModuleList(state.drbModules, state.drbFavorites, onSelect = { selectedId = it.id }, onToggleFavorite = onToggleFavorite,
                onImport = onImport, onRemove = onRemove)
        }
    }
}

@Composable
private fun DrbEmpty(status: String?, onImport: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Panel(Modifier.fillMaxWidth()) {
            Text("Import a DRB-III database", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Pick your database.mem, or a .zip that contains one (the newest one inside is used). " +
                    "It's parsed on this device and never uploaded. The database belongs to Chrysler and isn't included with the app.",
                color = Palette.TextMuted, style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            if (status != null) Text(status, color = Palette.Amber, style = MaterialTheme.typography.bodyMedium)
            else AmberButton("Choose file", onClick = onImport)
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, hint: String) {
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true,
        placeholder = { Text(hint, color = Palette.TextFaint) },
        leadingIcon = { Icon(Icons.Filled.Search, null, tint = Palette.TextMuted) },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Palette.Amber, unfocusedBorderColor = Palette.Line, cursorColor = Palette.Amber,
            focusedContainerColor = Palette.Panel, unfocusedContainerColor = Palette.Panel,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Chip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (on) Palette.Amber else Palette.Panel)
            .border(1.dp, if (on) Palette.Amber else Palette.Line, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) { Text(label, color = if (on) Color(0xFF1C1300) else Palette.Text, style = MaterialTheme.typography.labelLarge) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModuleList(
    modules: List<DrbModule>,
    favorites: Set<Int>,
    onSelect: (DrbModule) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onImport: () -> Unit,
    onRemove: () -> Unit,
) {
    var q by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(if (favorites.isEmpty()) ModFilter.ALL else ModFilter.FAV) }
    val shown = remember(modules, q, filter, favorites) {
        modules.filter { m ->
            (q.isBlank() || m.name.contains(q, true) || m.category.contains(q, true)) &&
                when (filter) {
                    ModFilter.FAV -> m.id in favorites
                    ModFilter.PCI -> DrbProtocol.J1850 in m.protocols || DrbProtocol.J2190 in m.protocols
                    ModFilter.ALL -> true
                }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SearchField(q, { q = it }, "Search modules (PCM, ABS, BCM…)")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModFilter.entries.forEach { f -> Chip(f.label, filter == f) { filter = f } }
            }
            if (filter == ModFilter.FAV && favorites.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap the ★ on a module to keep it here, for example the engine, transmission and body modules on your vehicle.",
                    color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        if (shown.isEmpty() && !(filter == ModFilter.FAV && favorites.isEmpty())) item { Text("No modules match.", color = Palette.TextMuted, modifier = Modifier.padding(8.dp)) }
        items(shown, key = { it.id }) { m ->
            Panel(Modifier.fillMaxWidth(), onClick = { onSelect(m) }, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "${m.category} · ${m.commandCount} commands · " + m.protocols.filter { it != DrbProtocol.UNKNOWN }.joinToString { it.label },
                            color = Palette.TextMuted, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Icon(Icons.Filled.Star, if (m.id in favorites) "Unfavorite" else "Favorite",
                        tint = if (m.id in favorites) Palette.Amber else Palette.TextFaint,
                        modifier = Modifier.clip(CircleShape).clickable { onToggleFavorite(m.id) }.padding(8.dp))
                    Icon(Icons.Filled.KeyboardArrowRight, null, tint = Palette.TextMuted)
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Re-import", onClick = onImport)
                GhostButton("Remove from device", icon = Icons.Filled.Delete, tint = Palette.Fault, onClick = onRemove)
            }
        }
    }
}

@Composable
private fun ModuleDetail(m: DrbModule, commandsFor: (Int) -> List<DrbCommand>) {
    val all = remember(m.id) { commandsFor(m.id) }
    var q by rememberSaveable(m.id) { mutableStateOf("") }
    var expanded by remember(m.id) { mutableStateOf<Int?>(null) }
    val shown = remember(all, q) { if (q.isBlank()) all else all.filter { it.name.contains(q, true) || it.requestHex.contains(q, true) } }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            SearchField(q, { q = it }, "Search ${all.size} commands (O2, RPM, MISFIRE…)")
            Spacer(Modifier.height(8.dp))
            Text(
                "Reference only — nothing here is sent to the vehicle yet. Some entries are actuator tests or module routines, not just reads.",
                color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(6.dp))
        }
        items(shown, key = { it.id }) { c ->
            val open = expanded == c.id
            Panel(Modifier.fillMaxWidth(), onClick = { expanded = if (open) null else c.id }, padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(decodeKind(c.decode), color = Palette.TextFaint, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.requestHex, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = Palette.Amber,
                        modifier = Modifier.weight(1f))
                    Text(c.protocol.label, color = Palette.TextMuted, style = MaterialTheme.typography.bodySmall)
                }
                if (open) {
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Line))
                    Spacer(Modifier.height(10.dp))
                    DecodeDetail(c)
                }
            }
        }
    }
}

private fun decodeKind(d: DrbDecode) = when (d) {
    is DrbDecode.Numeric -> d.unit?.takeIf { it.isNotBlank() } ?: "value"
    is DrbDecode.States -> "${d.states.size} states"
    is DrbDecode.Binary -> "on/off"
    is DrbDecode.Unknown -> ""
}

@Composable
private fun DecodeDetail(c: DrbCommand) {
    @Composable fun kv(k: String, v: String) {
        Row(Modifier.padding(vertical = 2.dp)) {
            Text(k, color = Palette.TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(110.dp))
            Text(v, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
        }
    }
    kv("Category", c.category.ifBlank { "—" })
    kv("Request len", "${c.requestLen}")
    kv("Response len", "${c.responseLen}")
    when (val d = c.decode) {
        is DrbDecode.Numeric -> {
            kv("Type", "Numeric")
            d.slope?.let { kv("Value", "raw × $it + ${d.offset ?: 0f}") }
            kv("Unit", d.unit?.ifBlank { null } ?: "—")
            if (d.metricUnit != null && d.metricUnit != d.unit) kv("Metric", "× ${d.metricSlope} + ${d.metricOffset} ${d.metricUnit}")
        }
        is DrbDecode.Binary -> {
            kv("Type", "On / off")
            kv("True", d.whenTrue); kv("False", d.whenFalse)
            d.mask?.let { kv("Mask", "0x%X ${d.operator ?: ""}".format(it)) }
        }
        is DrbDecode.States -> {
            kv("Type", "States")
            d.mask?.let { kv("Mask", "0x%X".format(it)) }
            d.default?.let { kv("Default", it) }
            d.states.take(40).forEach { (v, s) -> kv("0x%02X".format(v), s) }
            if (d.states.size > 40) kv("…", "${d.states.size - 40} more")
        }
        is DrbDecode.Unknown -> kv("Type", "Unknown (0x%02X)".format(d.type))
    }
}
