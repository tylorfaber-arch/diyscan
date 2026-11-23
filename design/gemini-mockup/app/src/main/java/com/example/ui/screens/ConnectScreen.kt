package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AdapterDevice
import com.example.model.AdapterType
import com.example.model.ConnectionState
import com.example.ui.components.HardwarePill
import com.example.ui.components.InstrumentCard
import com.example.ui.components.PrimaryInstrumentButton
import com.example.ui.components.SectionMicroHeader
import com.example.ui.theme.BlueprintBorder
import com.example.ui.theme.BlueprintSlate
import com.example.ui.theme.HexCodeStyle
import com.example.ui.theme.LocalInstrumentColors
import com.example.ui.theme.MonoFontFamily
import com.example.viewmodel.ScanUiState

@Composable
fun ConnectScreen(
    state: ScanUiState,
    onConnect: (AdapterDevice) -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalInstrumentColors.current
    val simulators = state.adapters.filter { it.type == AdapterType.SIMULATOR }
    val hardwareAdapters = state.adapters.filter { it.type != AdapterType.SIMULATOR }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(BlueprintSlate)
                                .border(1.dp, BlueprintBorder, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_diyscan_icon_1795411137123),
                                contentDescription = stringResource(R.string.app_name),
                                modifier = Modifier.size(32.dp),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "DIY SCAN",
                                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 1.4.sp),
                                color = c.ink,
                            )
                            Text(
                                text = "OBD-II · CAN · CHRYSLER PCI WORKBENCH",
                                style = MaterialTheme.typography.labelSmall,
                                color = c.inkMuted,
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(c.plate)
                            .border(1.dp, c.bezel, CircleShape)
                            .testTag("connect_theme_toggle"),
                    ) {
                        Icon(
                            imageVector = if (state.darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = stringResource(R.string.cd_theme_toggle),
                            tint = c.ink,
                        )
                    }
                }
            }

            item {
                BlueprintConnectHero(
                    onQuickDemo = { simulators.firstOrNull()?.let(onConnect) },
                    isBusy = state.connection is ConnectionState.Handshaking,
                )
            }

            if (state.connection is ConnectionState.Handshaking) {
                item {
                    HandshakeTerminalCard(state.connection)
                }
            }

            item {
                SectionMicroHeader(
                    title = "Demo Vehicle Simulators",
                    badge = "NO ADAPTER NEEDED",
                )
            }

            items(simulators, key = { it.id }) { adapter ->
                AdapterRowCard(
                    device = adapter,
                    enabled = state.connection !is ConnectionState.Handshaking,
                    onClick = { onConnect(adapter) },
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                SectionMicroHeader(
                    title = "ELM327 Hardware Interfaces",
                    badge = "BT SPP · USB-OTG",
                )
            }

            items(hardwareAdapters, key = { it.id }) { adapter ->
                AdapterRowCard(
                    device = adapter,
                    enabled = state.connection !is ConnectionState.Handshaking,
                    onClick = { onConnect(adapter) },
                )
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BlueprintConnectHero(
    onQuickDemo: () -> Unit,
    isBusy: Boolean,
) {
    val c = LocalInstrumentColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BlueprintSlate,
        border = BorderStroke(1.dp, BlueprintBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_vehicle_telemetry_hero_1795411135025),
                    contentDescription = stringResource(R.string.cd_vehicle_blueprint),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    BlueprintSlate.copy(alpha = 0.88f),
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HardwarePill(
                        text = "ELM327 v2.3",
                        fg = Color(0xFFFF7A52),
                        bg = Color(0xCC1E1410),
                    )
                    HardwarePill(
                        text = "J1850 / CAN",
                        fg = Color(0xFF7DD3FC),
                        bg = Color(0xCC0D1C26),
                        showDot = false,
                    )
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = stringResource(R.string.connect_headline),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFFF4F6F9),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Precision diagnostics, live gauges, ATMA bus monitor & DRB-III modules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFAAB4C3),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                PrimaryInstrumentButton(
                    text = if (isBusy) "Initializing Bus…" else stringResource(R.string.btn_connect_simulator),
                    onClick = onQuickDemo,
                    enabled = !isBusy,
                    containerColor = c.accent,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "launch_demo_vehicle_button",
                )
            }
        }
    }
}

@Composable
private fun HandshakeTerminalCard(handshake: ConnectionState.Handshaking) {
    val c = LocalInstrumentColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BlueprintSlate,
        border = BorderStroke(1.dp, c.accent.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = handshake.device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
                Text(
                    text = "${(handshake.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.accent,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = handshake.currentStep,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB4BFCE),
            )
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { handshake.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = c.accent,
                trackColor = Color(0xFF26303D),
            )
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0A0D12))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                handshake.logLines.forEach { line ->
                    Text(
                        text = line,
                        style = HexCodeStyle.copy(fontSize = 11.sp),
                        color = Color(0xFF7DD3FC),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdapterRowCard(
    device: AdapterDevice,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalInstrumentColors.current
    val icon = when (device.type) {
        AdapterType.SIMULATOR -> Icons.Filled.DirectionsCar
        AdapterType.BLUETOOTH_SPP -> Icons.Filled.Bluetooth
        AdapterType.USB_OTG -> Icons.Filled.Usb
    }
    val badgeFg = when (device.type) {
        AdapterType.SIMULATOR -> c.accent
        AdapterType.BLUETOOTH_SPP -> c.bus
        AdapterType.USB_OTG -> c.good
    }
    val badgeBg = when (device.type) {
        AdapterType.SIMULATOR -> c.accentBg
        AdapterType.BLUETOOTH_SPP -> c.busBg
        AdapterType.USB_OTG -> c.goodBg
    }

    InstrumentCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adapter_${device.id}"),
        onClick = if (enabled) onClick else null,
        padding = 14.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = device.type.badge,
                    tint = badgeFg,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = c.ink,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = device.type.badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = badgeFg,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = device.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkMuted,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Connect ${device.name}",
                tint = c.inkMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
