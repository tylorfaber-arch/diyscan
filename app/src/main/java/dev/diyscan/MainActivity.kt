package dev.diyscan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.diyscan.ui.CodesScreen
import dev.diyscan.ui.ConnectScreen
import dev.diyscan.ui.DrbBrowser
import dev.diyscan.ui.LiveScreen
import dev.diyscan.ui.MonitorScreen
import dev.diyscan.ui.OverviewScreen
import dev.diyscan.ui.SettingsScreen
import dev.diyscan.ui.theme.DiyScanTheme
import dev.diyscan.ui.theme.Palette

class MainActivity : ComponentActivity() {

    private val vm: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DiyScanTheme { App(vm) } }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Filled.Home),
    LIVE("Live", Icons.Filled.PlayArrow),
    CODES("Codes", Icons.Filled.Warning),
    BUS("Bus", Icons.Filled.List),
    SETTINGS("Settings", Icons.Filled.Settings),
}

private val btPermission =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null

@Composable
private fun App(vm: ScanViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    var hasPermission by remember {
        mutableStateOf(btPermission == null ||
            ContextCompat.checkSelfPermission(ctx, btPermission) == PackageManager.PERMISSION_GRANTED)
    }
    val drbPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importDrb(uri)
    }
    val pickDrb = { drbPicker.launch(arrayOf("*/*")) }
    val profilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importProfile(uri)
    }
    val pickProfile = { profilePicker.launch(arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        hasPermission = ok
        if (ok) vm.refreshDevices()
    }
    LaunchedEffect(hasPermission) { if (hasPermission) vm.refreshDevices() }
    // USB adapters need no runtime permission to list; refresh whenever the app comes back to the front
    // (plugging one in launches/resumes the app via the USB_DEVICE_ATTACHED filter).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshUsb() }

    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    var tab by rememberSaveable { mutableStateOf(Tab.OVERVIEW) }
    // Leaving the live tab pauses polling so other requests aren't starved on slow buses.
    LaunchedEffect(tab, state.connected) {
        if (tab != Tab.LIVE && state.logFile == null) vm.stopLive()
        if (tab == Tab.LIVE && state.connected) vm.startLive()
        if (tab != Tab.BUS) vm.stopMonitor()
    }
    BackHandler(enabled = state.connected && tab != Tab.OVERVIEW) { tab = Tab.OVERVIEW }

    Scaffold(
        containerColor = Palette.Bg,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { pad ->
        Box(Modifier.fillMaxSize().background(Palette.Bg).padding(pad)) {
            if (state.drbOpen) {
                DrbBrowser(
                    state = state,
                    commandsFor = vm::drbCommands,
                    onImport = pickDrb,
                    onRemove = vm::removeDrb,
                    onClose = { vm.openDrb(false) },
                )
            } else if (!state.connected) {
                ConnectScreen(
                    state = state,
                    hasPermission = hasPermission,
                    onRequestPermission = { btPermission?.let { launcher.launch(it) } },
                    onRefresh = { vm.refreshDevices(); vm.refreshUsb() },
                    onConnect = vm::connect,
                    onSimulator = { tab = Tab.OVERVIEW; vm.connectSimulator() },
                    onOpenBluetoothSettings = { ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                    onOpenDrb = { vm.openDrb(true) },
                    onConnectUsb = vm::connectUsb,
                )
            } else {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 720.dp
                    val codeCount = state.dtcs?.total ?: 0
                    if (wide) {
                        Row(Modifier.fillMaxSize()) {
                            NavigationRail(containerColor = Palette.Panel, windowInsets = WindowInsets(0)) {
                                Tab.entries.forEach { t ->
                                    NavigationRailItem(
                                        selected = tab == t, onClick = { tab = t },
                                        icon = { TabIcon(t, codeCount) }, label = { Text(t.label) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = Color(0xFF1C1300), indicatorColor = Palette.Amber,
                                            selectedTextColor = Palette.Amber, unselectedIconColor = Palette.TextMuted,
                                            unselectedTextColor = Palette.TextMuted,
                                        ),
                                    )
                                }
                            }
                            Box(Modifier.weight(1f)) { TabContent(tab, state, vm, pickProfile) { tab = it } }
                        }
                    } else {
                        Scaffold(
                            containerColor = Palette.Bg,
                            contentWindowInsets = WindowInsets(0),
                            bottomBar = {
                                NavigationBar(containerColor = Palette.Panel, windowInsets = WindowInsets(0)) {
                                    Tab.entries.forEach { t ->
                                        NavigationBarItem(
                                            selected = tab == t, onClick = { tab = t },
                                            icon = { TabIcon(t, codeCount) }, label = { Text(t.label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color(0xFF1C1300), indicatorColor = Palette.Amber,
                                                selectedTextColor = Palette.Amber, unselectedIconColor = Palette.TextMuted,
                                                unselectedTextColor = Palette.TextMuted,
                                            ),
                                        )
                                    }
                                }
                            },
                        ) { inner ->
                            Box(Modifier.padding(inner)) { TabContent(tab, state, vm, pickProfile) { tab = it } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabIcon(t: Tab, codes: Int) {
    if (t == Tab.CODES && codes > 0) {
        BadgedBox(badge = { Badge(containerColor = Palette.Fault) { Text("$codes") } }) { Icon(t.icon, t.label) }
    } else Icon(t.icon, t.label)
}

@Composable
private fun TabContent(tab: Tab, state: UiState, vm: ScanViewModel, pickProfile: () -> Unit, go: (Tab) -> Unit) {
    when (tab) {
        Tab.OVERVIEW -> OverviewScreen(state, onRefresh = vm::refreshOverview, onGoCodes = { go(Tab.CODES) }, onGoLive = { go(Tab.LIVE) })
        Tab.LIVE -> LiveScreen(state, onStart = vm::startLive, onStop = vm::stopLive, onToggleLog = vm::toggleLogging, onTogglePid = vm::toggleDashboardPid)
        Tab.CODES -> CodesScreen(state, onRead = vm::readCodes, onClear = vm::clearCodes)
        Tab.BUS -> MonitorScreen(state, onStart = vm::startMonitor, onStop = vm::stopMonitor, onSave = vm::saveCapture)
        Tab.SETTINGS -> SettingsScreen(state, onImperial = vm::setImperial, onDisconnect = vm::disconnect, onOpenDrb = { vm.openDrb(true) },
            onImportProfile = pickProfile, onRemoveProfile = vm::removeProfile)
    }
}
