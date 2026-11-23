package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ConnectionState
import com.example.model.DrbCommand
import com.example.model.DrbModule
import com.example.ui.components.HardwarePill
import com.example.ui.components.InstrumentCard
import com.example.ui.components.OutlineInstrumentButton
import com.example.ui.components.PrimaryInstrumentButton
import com.example.ui.components.SectionMicroHeader
import com.example.ui.components.SegmentedControl
import com.example.ui.theme.HexCodeStyle
import com.example.ui.theme.LocalInstrumentColors
import com.example.ui.theme.MonoFontFamily
import com.example.viewmodel.ScanUiState

@Composable
fun ToolsSettingsScreen(
    state: ScanUiState,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetImperial: (Boolean) -> Unit,
    onOpenImports: () -> Unit,
    onOpenDrb: () -> Unit,
    onOpenReplay: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalInstrumentColors.current
    val conn = state.connection as? ConnectionState.Connected
    val enabledProfiles = state.pidProfiles.count { it.enabled }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column {
                Text(
                    text = "Tools & Workbench",
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.ink,
                )
                Text(
                    text = "Local imports, Chrysler DRB-III database, display & units",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkMuted,
                )
            }
        }

        // Theme Visual Picker (Workshop Daylight vs Night Cockpit)
        item {
            InstrumentCard(Modifier.fillMaxWidth()) {
                SectionMicroHeader(
                    title = "Instrument Surface Theme",
                    badge = if (state.darkTheme) "NIGHT COCKPIT" else "DAYLIGHT SHOP",
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ThemeOptionTile(
                        title = "Workshop Daylight",
                        subtitle = "High-sun garage contrast",
                        icon = Icons.Filled.LightMode,
                        selected = !state.darkTheme,
                        swatchBg = Color(0xFFF3F1EA),
                        swatchAccent = Color(0xFFE8481C),
                        onClick = { onSetDarkTheme(false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_option_daylight"),
                    )
                    ThemeOptionTile(
                        title = "Night Cockpit",
                        subtitle = "Low-glare dark bezel",
                        icon = Icons.Filled.DarkMode,
                        selected = state.darkTheme,
                        swatchBg = Color(0xFF0C0F14),
                        swatchAccent = Color(0xFFFF6436),
                        onClick = { onSetDarkTheme(true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_option_night"),
                    )
                }
            }
        }

        // Units Toggle
        item {
            InstrumentCard(Modifier.fillMaxWidth()) {
                SectionMicroHeader(title = "Measurement Units")
                Spacer(Modifier.height(10.dp))
                SegmentedControl(
                    options = listOf("Imperial (°F · mph · psi)", "Metric (°C · km/h · kPa)"),
                    selectedIndex = if (state.imperialUnits) 0 else 1,
                    onSelect = { onSetImperial(it == 0) },
                    testTagPrefix = "unit_system",
                )
            }
        }

        // Power-user tools section
        item {
            SectionMicroHeader(
                title = "Manufacturer Data & Imports",
                badge = "ON-DEVICE ONLY",
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolLinkCard(
                    title = "Imports Hub",
                    subtitle = "$enabledProfiles active PID profiles · DRB-III · Replay plans",
                    icon = Icons.Filled.FolderOpen,
                    tint = c.accent,
                    bg = c.accentBg,
                    onClick = onOpenImports,
                    testTag = "tools_open_imports",
                )
                ToolLinkCard(
                    title = "Chrysler DRB-III Database",
                    subtitle = if (state.drbImported) "${state.drbModules.size} modules · PCI & CCD command browser" else "Not imported — tap to load demo database.mem",
                    icon = Icons.Filled.Memory,
                    tint = c.bus,
                    bg = c.busBg,
                    onClick = onOpenDrb,
                    testTag = "tools_open_drb",
                )
                ToolLinkCard(
                    title = "Request Replay & Mode 22 Sweep",
                    subtitle = state.replayPlanName ?: "Re-run read-only scan tool catalogs on 7E0 / 7E1",
                    icon = Icons.Filled.Terminal,
                    tint = c.good,
                    bg = c.goodBg,
                    onClick = onOpenReplay,
                    testTag = "tools_open_replay",
                )
            }
        }

        // Active Session & Disconnect
        if (conn != null) {
            item {
                InstrumentCard(Modifier.fillMaxWidth()) {
                    SectionMicroHeader(
                        title = "Active Session",
                        badge = conn.device.type.badge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = conn.device.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = c.ink,
                    )
                    Text(
                        text = "${conn.device.protocol} · ${conn.device.vin}",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.inkMuted,
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlineInstrumentButton(
                        text = stringResource(R.string.btn_disconnect),
                        onClick = onDisconnect,
                        accentColor = c.fault,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "disconnect_adapter_button",
                    )
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ThemeOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    swatchBg: Color,
    swatchAccent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalInstrumentColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.accentBg.copy(alpha = 0.5f) else c.well)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) c.accent else c.bezel,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(swatchBg)
                    .border(2.dp, swatchAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(swatchAccent)
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) c.accent else c.inkMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = c.ink,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = c.inkMuted,
        )
    }
}

@Composable
private fun ToolLinkCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    val c = LocalInstrumentColors.current
    InstrumentCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        onClick = onClick,
        padding = 14.dp,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = title, tint = c.inkFaint)
        }
    }
}

@Composable
fun ImportsScreen(
    state: ScanUiState,
    onClose: () -> Unit,
    onToggleProfile: (String, Boolean) -> Unit,
    onAddSampleProfile: () -> Unit,
    onRemoveProfile: (String) -> Unit,
    onOpenDrb: () -> Unit,
    onOpenReplay: () -> Unit,
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
                    .testTag("imports_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = c.ink,
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Imports Hub", style = MaterialTheme.typography.titleLarge, color = c.ink)
                Text(
                    "Vehicle data you bring. Stored strictly on this device.",
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
            item {
                SectionMicroHeader(
                    title = "Torque-Style PID Profiles (.csv)",
                    badge = "${state.pidProfiles.count { it.enabled }} ACTIVE",
                )
            }

            items(state.pidProfiles, key = { it.id }) { profile ->
                InstrumentCard(
                    modifier = Modifier.fillMaxWidth(),
                    accentBorder = if (profile.enabled) c.bus.copy(alpha = 0.5f) else null,
                    padding = 14.dp,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = profile.fileName,
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = MonoFontFamily),
                                color = c.ink,
                            )
                            Text(
                                text = "${profile.makeLabel} · ${profile.sensorCount} sensors · ${profile.protocolHint}",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.inkMuted,
                            )
                        }
                        Switch(
                            checked = profile.enabled,
                            onCheckedChange = { onToggleProfile(profile.id, it) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = c.accent,
                                checkedThumbColor = Color.White,
                            ),
                            modifier = Modifier.testTag("profile_switch_${profile.id}"),
                        )
                        IconButton(onClick = { onRemoveProfile(profile.id) }) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Remove ${profile.fileName}",
                                tint = c.inkFaint,
                            )
                        }
                    }
                }
            }

            item {
                OutlineInstrumentButton(
                    text = "Import Sample Toyota HV CSV",
                    onClick = onAddSampleProfile,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "import_sample_profile_button",
                )
            }

            item {
                Spacer(Modifier.height(6.dp))
                SectionMicroHeader(title = "Manufacturer Databases & Replay")
            }

            item {
                ToolLinkCard(
                    title = "Chrysler DRB-III database.mem",
                    subtitle = if (state.drbImported) {
                        "${state.drbModules.size} modules loaded · Tap to browse commands"
                    } else {
                        "Not imported · Tap to import local archive"
                    },
                    icon = Icons.Filled.Memory,
                    tint = c.bus,
                    bg = c.busBg,
                    onClick = onOpenDrb,
                    testTag = "imports_open_drb",
                )
            }

            item {
                ToolLinkCard(
                    title = "Request Replay Catalogs",
                    subtitle = state.replayPlanName ?: "Import replay.txt from tools/obd_catalog.py",
                    icon = Icons.Filled.Terminal,
                    tint = c.accent,
                    bg = c.accentBg,
                    onClick = onOpenReplay,
                    testTag = "imports_open_replay",
                )
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun DrbBrowserScreen(
    state: ScanUiState,
    onClose: () -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onToggleLoaded: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalInstrumentColors.current
    var selectedModuleId by rememberSaveable { mutableStateOf<Int?>(null) }
    var filterIdx by rememberSaveable { mutableStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }

    val selectedModule = state.drbModules.firstOrNull { it.id == selectedModuleId }

    BackHandler {
        if (selectedModuleId != null) selectedModuleId = null else onClose()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { if (selectedModuleId != null) selectedModuleId = null else onClose() },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("drb_back_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = c.ink,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = selectedModule?.shortCode ?: "Chrysler DRB-III Browser",
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink,
                )
                Text(
                    text = selectedModule?.name ?: if (state.drbImported) {
                        "${state.drbModules.size} modules · ${state.drbModules.sumOf { it.commands.size }} commands"
                    } else "No database.mem loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkMuted,
                )
            }
            if (selectedModule == null && state.drbImported) {
                IconButton(onClick = { onToggleLoaded(false) }) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Unload DRB database",
                        tint = c.inkMuted,
                    )
                }
            }
        }

        if (!state.drbImported) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                InstrumentCard(Modifier.fillMaxWidth()) {
                    SectionMicroHeader(title = "Bring Your Own Database")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Import database.mem (or a .zip containing it) to browse dealer DRB-III modules, PCI request frames, and reply decode equations locally.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.inkMuted,
                    )
                    Spacer(Modifier.height(14.dp))
                    PrimaryInstrumentButton(
                        text = "Load Demo DRB-III Modules",
                        onClick = { onToggleLoaded(true) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "load_demo_drb_button",
                    )
                }
            }
            return
        }

        if (selectedModule != null) {
            DrbModuleCommandsList(module = selectedModule)
        } else {
            val filtered = state.drbModules.filter { mod ->
                val matchesTab = when (filterIdx) {
                    0 -> mod.isFavorite
                    1 -> mod.isPci
                    else -> true
                }
                val matchesQuery = search.isBlank() ||
                    mod.name.contains(search, ignoreCase = true) ||
                    mod.shortCode.contains(search, ignoreCase = true) ||
                    mod.category.contains(search, ignoreCase = true)
                matchesTab && matchesQuery
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    SegmentedControl(
                        options = listOf("★ Favorites", "PCI Bus", "All Modules"),
                        selectedIndex = filterIdx,
                        onSelect = { filterIdx = it },
                        testTagPrefix = "drb_filter",
                    )
                }

                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search PCM, TCM, ABS, BCM…", style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = c.inkMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = c.accent,
                            unfocusedBorderColor = c.bezel,
                            focusedContainerColor = c.plate,
                            unfocusedContainerColor = c.plate,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                items(filtered, key = { it.id }) { mod ->
                    DrbModuleRow(
                        module = mod,
                        onClick = { selectedModuleId = mod.id },
                        onToggleFavorite = { onToggleFavorite(mod.id) },
                    )
                }

                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun DrbModuleRow(
    module: DrbModule,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val c = LocalInstrumentColors.current
    InstrumentCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("drb_module_${module.shortCode}"),
        onClick = onClick,
        padding = 14.dp,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.busBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = module.shortCode,
                    style = HexCodeStyle.copy(fontWeight = FontWeight.Bold),
                    color = c.bus,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(module.name, style = MaterialTheme.typography.titleSmall, color = c.ink)
                Text(
                    "${module.busType} · ${module.commands.size} commands",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkMuted,
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (module.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Favorite ${module.shortCode}",
                    tint = if (module.isFavorite) c.accent else c.inkFaint,
                )
            }
        }
    }
}

@Composable
private fun DrbModuleCommandsList(module: DrbModule) {
    val c = LocalInstrumentColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            SectionMicroHeader(
                title = "${module.category} · ${module.busType}",
                badge = "${module.commands.size} COMMANDS",
            )
        }
        items(module.commands, key = { it.id }) { cmd ->
            DrbCommandCard(cmd)
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun DrbCommandCard(cmd: DrbCommand) {
    val c = LocalInstrumentColors.current
    InstrumentCard(Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = cmd.name,
                style = MaterialTheme.typography.titleSmall,
                color = c.ink,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            HardwarePill(
                text = cmd.category.uppercase(),
                fg = c.bus,
                bg = c.busBg,
                showDot = false,
            )
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = c.bezel.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("TX REQUEST BYTES", style = MaterialTheme.typography.labelSmall, color = c.inkFaint)
                Text(
                    text = cmd.requestHex,
                    style = HexCodeStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = c.accent,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("DECODE (${cmd.unit})", style = MaterialTheme.typography.labelSmall, color = c.inkFaint)
                Text(
                    text = cmd.decodeFormula,
                    style = HexCodeStyle.copy(fontSize = 12.sp),
                    color = c.ink,
                )
            }
        }
    }
}
