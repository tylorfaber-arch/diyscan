package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BusMonitorScreen
import com.example.ui.screens.CodesScreen
import com.example.ui.screens.ConnectScreen
import com.example.ui.screens.DrbBrowserScreen
import com.example.ui.screens.ImportsScreen
import com.example.ui.screens.LiveScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.ReplayScreen
import com.example.ui.screens.ToolsSettingsScreen
import com.example.ui.theme.DiyScanMockupTheme
import com.example.ui.theme.LocalInstrumentColors
import com.example.viewmodel.MainTab
import com.example.viewmodel.ScanMockViewModel
import com.example.viewmodel.ScanUiState
import com.example.viewmodel.SubScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: ScanMockViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()

            LaunchedEffect(state.darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (state.darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                    navigationBarStyle = if (state.darkTheme) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT,
                        )
                    },
                )
            }

            DiyScanMockupTheme(darkTheme = state.darkTheme) {
                DiyScanApp(state = state, vm = vm)
            }
        }
    }
}

@Composable
private fun DiyScanApp(
    state: ScanUiState,
    vm: ScanMockViewModel,
) {
    val c = LocalInstrumentColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        vm.consumeSnackbar()
    }

    // Back press from any non-Overview main tab returns to Overview
    BackHandler(enabled = state.isConnected && state.subScreen == SubScreen.NONE && state.activeTab != MainTab.OVERVIEW) {
        vm.selectTab(MainTab.OVERVIEW)
    }

    Scaffold(
        containerColor = c.canvas,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { rootPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(rootPadding),
        ) {
            when {
                !state.isConnected -> {
                    ConnectScreen(
                        state = state,
                        onConnect = vm::connect,
                        onToggleTheme = vm::toggleDarkTheme,
                    )
                }

                state.subScreen == SubScreen.REPLAY -> {
                    ReplayScreen(
                        state = state,
                        onClose = vm::closeSubScreen,
                        onLoadDemoPlan = vm::loadDemoReplayPlan,
                        onLoadSweepPlan = vm::loadMode22SweepPlan,
                        onRun = vm::runReplayPlan,
                        onStop = vm::stopReplayPlan,
                        onSaveCsv = vm::saveReplayCsv,
                    )
                }

                state.subScreen == SubScreen.IMPORTS -> {
                    ImportsScreen(
                        state = state,
                        onClose = vm::closeSubScreen,
                        onToggleProfile = vm::togglePidProfile,
                        onAddSampleProfile = vm::addSampleProfile,
                        onRemoveProfile = vm::removePidProfile,
                        onOpenDrb = { vm.openSubScreen(SubScreen.DRB_BROWSER) },
                        onOpenReplay = { vm.openSubScreen(SubScreen.REPLAY) },
                    )
                }

                state.subScreen == SubScreen.DRB_BROWSER -> {
                    DrbBrowserScreen(
                        state = state,
                        onClose = vm::closeSubScreen,
                        onToggleFavorite = vm::toggleDrbFavorite,
                        onToggleLoaded = vm::toggleDrbDatabaseLoaded,
                    )
                }

                else -> {
                    ConnectedWorkspace(state = state, vm = vm)
                }
            }
        }
    }
}

@Composable
private fun ConnectedWorkspace(
    state: ScanUiState,
    vm: ScanMockViewModel,
) {
    val c = LocalInstrumentColors.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        if (useRail) {
            Row(
                Modifier
                    .fillMaxSize()
                    .background(c.canvas),
            ) {
                NavigationRail(
                    containerColor = c.plate,
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding(),
                ) {
                    MainTab.entries.forEach { tab ->
                        val selected = state.activeTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { vm.selectTab(tab) },
                            icon = { TabNavIcon(tab, selected, state.activeFaultCount) },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = c.accent,
                                selectedTextColor = c.accent,
                                indicatorColor = c.accentBg,
                                unselectedIconColor = c.inkMuted,
                                unselectedTextColor = c.inkMuted,
                            ),
                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .statusBarsPadding(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(Modifier.widthIn(max = 860.dp).fillMaxSize()) {
                        ActiveTabContent(state = state, vm = vm)
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = c.canvas,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    androidx.compose.foundation.layout.Column {
                        HorizontalDivider(color = c.bezel)
                        NavigationBar(
                            containerColor = c.plate,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars,
                        ) {
                            MainTab.entries.forEach { tab ->
                                val selected = state.activeTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { vm.selectTab(tab) },
                                    icon = { TabNavIcon(tab, selected, state.activeFaultCount) },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = c.accent,
                                        selectedTextColor = c.accent,
                                        indicatorColor = c.accentBg,
                                        unselectedIconColor = c.inkMuted,
                                        unselectedTextColor = c.inkMuted,
                                    ),
                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .statusBarsPadding(),
                ) {
                    ActiveTabContent(state = state, vm = vm)
                }
            }
        }
    }
}

@Composable
private fun ActiveTabContent(
    state: ScanUiState,
    vm: ScanMockViewModel,
) {
    when (state.activeTab) {
        MainTab.OVERVIEW -> OverviewScreen(
            state = state,
            onRefresh = vm::refreshOverview,
            onGoCodes = { vm.selectTab(MainTab.CODES) },
            onGoLive = { vm.selectTab(MainTab.LIVE) },
            onToggleTheme = vm::toggleDarkTheme,
        )

        MainTab.LIVE -> LiveScreen(
            state = state,
            onToggleLive = vm::toggleLivePolling,
            onToggleLogging = vm::toggleLogging,
            onSelectDriveMode = vm::setDriveMode,
            onTogglePid = vm::togglePidSelected,
        )

        MainTab.CODES -> CodesScreen(
            state = state,
            onReadCodes = vm::readCodes,
            onClearCodes = vm::clearCodes,
            onRestoreDemoFaults = vm::restoreDemoFaults,
        )

        MainTab.BUS -> BusMonitorScreen(
            state = state,
            onStart = vm::startBusMonitor,
            onStop = vm::stopBusMonitor,
            onSaveCapture = vm::saveBusCapture,
            onSearchChange = vm::setBusSearch,
            onOpenReplay = { vm.openSubScreen(SubScreen.REPLAY) },
        )

        MainTab.TOOLS -> ToolsSettingsScreen(
            state = state,
            onSetDarkTheme = vm::setDarkTheme,
            onSetImperial = vm::setImperialUnits,
            onOpenImports = { vm.openSubScreen(SubScreen.IMPORTS) },
            onOpenDrb = { vm.openSubScreen(SubScreen.DRB_BROWSER) },
            onOpenReplay = { vm.openSubScreen(SubScreen.REPLAY) },
            onDisconnect = vm::disconnect,
        )
    }
}

@Composable
private fun TabNavIcon(tab: MainTab, selected: Boolean, faultCount: Int) {
    val c = LocalInstrumentColors.current
    val icon: ImageVector = when (tab) {
        MainTab.OVERVIEW -> if (selected) Icons.Filled.Dashboard else Icons.Outlined.Dashboard
        MainTab.LIVE -> if (selected) Icons.Filled.Speed else Icons.Outlined.Speed
        MainTab.CODES -> if (selected) Icons.Filled.Warning else Icons.Outlined.WarningAmber
        MainTab.BUS -> if (selected) Icons.Filled.Sensors else Icons.Outlined.Sensors
        MainTab.TOOLS -> if (selected) Icons.Filled.Build else Icons.Outlined.Build
    }

    if (tab == MainTab.CODES && faultCount > 0) {
        BadgedBox(
            badge = {
                Badge(containerColor = c.fault, contentColor = Color.White) {
                    Text(faultCount.toString())
                }
            },
        ) {
            Icon(imageVector = icon, contentDescription = tab.label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = tab.label)
    }
}
