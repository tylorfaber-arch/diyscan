package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.AdapterType
import com.example.model.ConnectionState
import com.example.model.MonitorState
import com.example.ui.components.HardwarePill
import com.example.ui.components.InstrumentCard
import com.example.ui.components.SectionMicroHeader
import com.example.ui.theme.BlueprintBorder
import com.example.ui.theme.BlueprintSlate
import com.example.ui.theme.HexCodeStyle
import com.example.ui.theme.LocalInstrumentColors
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.TelemetryReadoutStyle
import com.example.viewmodel.ScanUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OverviewScreen(
    state: ScanUiState,
    onRefresh: () -> Unit,
    onGoCodes: () -> Unit,
    onGoLive: () -> Unit,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalInstrumentColors.current
    val conn = state.connection as? ConnectionState.Connected ?: return
    val device = conn.device
    val readyCount = state.readiness.count { it.state == MonitorState.READY }
    val supportedCount = state.readiness.count { it.state != MonitorState.UNSUPPORTED }
    val faultCount = state.activeFaultCount

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = device.vehicleLabel,
                            style = MaterialTheme.typography.headlineSmall,
                            color = c.ink,
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = if (device.type == AdapterType.SIMULATOR) {
                            "Demo simulator · ${conn.lastRefreshed}"
                        } else {
                            "${device.name} · ${conn.lastRefreshed}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkMuted,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(c.plate)
                            .border(1.dp, c.bezel, CircleShape)
                            .testTag("overview_theme_toggle"),
                    ) {
                        Icon(
                            imageVector = if (state.darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = stringResource(R.string.cd_theme_toggle),
                            tint = c.ink,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(c.plate)
                            .border(1.dp, c.bezel, CircleShape)
                            .testTag("overview_refresh_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.btn_refresh),
                            tint = c.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // Blueprint Vehicle Health Banner
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = BlueprintSlate,
                border = BorderStroke(
                    1.dp,
                    if (conn.milOn) c.accent.copy(alpha = 0.7f) else BlueprintBorder,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(142.dp),
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
                                            BlueprintSlate.copy(alpha = 0.25f),
                                            BlueprintSlate.copy(alpha = 0.92f),
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            HardwarePill(
                                text = if (conn.milOn) "MIL · CHECK ENGINE ON" else "MIL · OFF",
                                fg = if (conn.milOn) Color(0xFFFF7A52) else Color(0xFF34D399),
                                bg = if (conn.milOn) Color(0xE02E130B) else Color(0xE00A291F),
                            )
                            HardwarePill(
                                text = "%.2f V".format(conn.batteryVolts),
                                fg = Color(0xFFE6ECF5),
                                bg = Color(0xCC161E29),
                                showDot = false,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = "VIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8C9BB0),
                            )
                            Text(
                                text = device.vin,
                                style = HexCodeStyle.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp,
                                ),
                                color = Color.White,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0E1319))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = device.protocol,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7DD3FC),
                        )
                        Text(
                            text = device.ecuId,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9BA8B9),
                        )
                    }
                }
            }
        }

        // Quick Action Split Cards: Trouble Codes + Live Cockpit
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InstrumentCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("overview_codes_shortcut"),
                    accentBorder = if (faultCount > 0) c.fault.copy(alpha = 0.5f) else c.good.copy(alpha = 0.5f),
                    onClick = onGoCodes,
                    padding = 14.dp,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (faultCount > 0) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (faultCount > 0) c.fault else c.good,
                            modifier = Modifier.size(20.dp),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open trouble codes",
                            tint = c.inkFaint,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "$faultCount",
                        style = TelemetryReadoutStyle.copy(fontSize = 28.sp),
                        color = if (faultCount > 0) c.fault else c.good,
                    )
                    Text(
                        text = if (faultCount == 1) "Active Fault" else "Active Faults",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink,
                    )
                    Text(
                        text = if (faultCount > 0) "Inspect DTCs & freeze" else "All clear",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkMuted,
                    )
                }

                InstrumentCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("overview_live_shortcut"),
                    onClick = onGoLive,
                    padding = 14.dp,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = null,
                            tint = c.accent,
                            modifier = Modifier.size(20.dp),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open live telemetry",
                            tint = c.inkFaint,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    val rpm = state.liveValues["rpm"] ?: 0.0
                    Text(
                        text = "%.0f".format(rpm),
                        style = TelemetryReadoutStyle.copy(fontSize = 28.sp),
                        color = c.ink,
                    )
                    Text(
                        text = "Live RPM",
                        style = MaterialTheme.typography.titleSmall,
                        color = c.ink,
                    )
                    Text(
                        text = "${state.dashboardTiles.size + 2} sensors active",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.inkMuted,
                    )
                }
            }
        }

        // Smog Readiness Monitor Matrix
        item {
            InstrumentCard(Modifier.fillMaxWidth()) {
                SectionMicroHeader(
                    title = "Smog Readiness Monitors",
                    badge = "$readyCount / $supportedCount READY",
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.readiness.forEach { monitor ->
                        val (fg, bg, icon) = when (monitor.state) {
                            MonitorState.READY -> Triple(c.good, c.goodBg, Icons.Filled.CheckCircle)
                            MonitorState.INCOMPLETE -> Triple(c.warn, c.warnBg, Icons.Filled.Schedule)
                            MonitorState.UNSUPPORTED -> Triple(c.inkFaint, c.well, null)
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(bg)
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (icon != null) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = monitor.state.label,
                                    tint = fg,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                text = monitor.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                                color = fg,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (readyCount == supportedCount && !conn.milOn) {
                        "All supported emissions monitors complete and MIL is off."
                    } else {
                        "Incomplete monitors require a full OBD-II drive cycle before emissions inspection."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.inkMuted,
                )
            }
        }

        // Adapter & Protocol Spec Sheet
        item {
            InstrumentCard(Modifier.fillMaxWidth()) {
                SectionMicroHeader(
                    title = "Interface & Protocol",
                    badge = device.type.badge,
                )
                Spacer(Modifier.height(12.dp))
                SpecRow("Adapter", device.name)
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = c.bezel.copy(alpha = 0.6f))
                SpecRow("Firmware", device.firmware, mono = true)
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = c.bezel.copy(alpha = 0.6f))
                SpecRow("Bus Protocol", device.protocol, mono = true)
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = c.bezel.copy(alpha = 0.6f))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.BatteryChargingFull,
                            contentDescription = null,
                            tint = c.good,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Battery Voltage", style = MaterialTheme.typography.bodyMedium, color = c.inkMuted)
                    }
                    Text(
                        text = "%.2f V".format(conn.batteryVolts),
                        style = HexCodeStyle.copy(fontWeight = FontWeight.Bold),
                        color = c.good,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun SpecRow(label: String, value: String, mono: Boolean = false) {
    val c = LocalInstrumentColors.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = c.inkMuted,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = if (mono) {
                MaterialTheme.typography.bodySmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            },
            color = c.ink,
        )
    }
}
