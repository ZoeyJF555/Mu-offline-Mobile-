package com.muoffline.mobile

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muoffline.mobile.model.MuCatalog
import com.muoffline.mobile.model.PotionType
import com.muoffline.mobile.ui.dialogs.*
import com.muoffline.mobile.ui.screens.AuthScreen
import com.muoffline.mobile.ui.screens.GameScreen
import com.muoffline.mobile.ui.screens.TitleScreen
import com.muoffline.mobile.ui.theme.MuOfflineTheme
import com.muoffline.mobile.viewmodel.GameViewModel
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuOfflineTheme {
                MuOfflineRoot(viewModel = gameViewModel)
            }
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK &&
            event.action == MotionEvent.ACTION_MOVE &&
            gameViewModel.activeHero.value != null
        ) {
            val rawX = event.getAxisValue(MotionEvent.AXIS_X)
            val rawY = event.getAxisValue(MotionEvent.AXIS_Y)
            val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)
            val jx = if (abs(rawX) > 0.14f) rawX else if (abs(hatX) > 0.14f) hatX else 0f
            val jy = if (abs(rawY) > 0.14f) rawY else if (abs(hatY) > 0.14f) hatY else 0f
            gameViewModel.joystickX = jx.coerceIn(-1f, 1f)
            gameViewModel.joystickY = jy.coerceIn(-1f, 1f)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val hero = gameViewModel.activeHero.value
        if (hero != null) {
            val skills = MuCatalog.SKILLS_BY_CLASS[hero.classId].orEmpty()
            when (keyCode) {
                KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_SPACE -> {
                    gameViewModel.executeCombat()
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_1 -> {
                    skills.getOrNull(0)?.let { gameViewModel.executeCombat(it.id) }
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_2 -> {
                    skills.getOrNull(1)?.let { gameViewModel.executeCombat(it.id) }
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_3 -> {
                    skills.getOrNull(2)?.let { gameViewModel.executeCombat(it.id) }
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_Q -> {
                    gameViewModel.usePotion(PotionType.HP)
                    return true
                }
                KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_E -> {
                    gameViewModel.usePotion(PotionType.MP)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
private fun MuOfflineRoot(viewModel: GameViewModel) {
    val accountId by viewModel.accountId.collectAsStateWithLifecycle()
    val authBusy by viewModel.authBusy.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val heroes by viewModel.heroes.collectAsStateWithLifecycle()
    val activeHero by viewModel.activeHero.collectAsStateWithLifecycle()

    val mods by viewModel.mods.collectAsStateWithLifecycle()
    val activeModRates by viewModel.activeModRates.collectAsStateWithLifecycle()
    val mapCatalog by viewModel.mapCatalog.collectAsStateWithLifecycle()
    val hudSettings by viewModel.hudSettings.collectAsStateWithLifecycle()
    val manifestInfo by viewModel.manifestInfo.collectAsStateWithLifecycle()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()

    val lanStatus by viewModel.lanStatus.collectAsStateWithLifecycle()
    val connectedRoom by viewModel.connectedRoom.collectAsStateWithLifecycle()
    val hostEndpoint by viewModel.hostEndpoint.collectAsStateWithLifecycle()
    val lanError by viewModel.lanError.collectAsStateWithLifecycle()
    val discoveredRooms by viewModel.discoveredRooms.collectAsStateWithLifecycle()
    val lanPlayers by viewModel.lanPlayers.collectAsStateWithLifecycle()

    var showModsDialog by remember { mutableStateOf(false) }
    var showLanDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showCreditsDialog by remember { mutableStateOf(false) }

    when {
        accountId == null -> {
            AuthScreen(
                busy = authBusy,
                errorText = authError,
                onLogin = { id, pw -> viewModel.login(id, pw) },
                onRegister = { id, pw, conf -> viewModel.register(id, pw, conf) },
                onQuickPlay = { viewModel.quickGuestLogin() },
                onClearError = { viewModel.clearAuthError() }
            )
        }
        activeHero == null -> {
            TitleScreen(
                accountId = accountId.orEmpty(),
                heroes = heroes,
                activeModsCount = mods.count { it.enabled },
                lanConnectedRoom = connectedRoom,
                maxHeroLevel = heroes.maxOfOrNull { it.level } ?: 1,
                manifestVersion = manifestInfo.version,
                onSelectHero = { viewModel.selectHero(it) },
                onCreateHero = { name, cid -> viewModel.createHero(name, cid) },
                onDeleteHero = { viewModel.deleteHero(it) },
                onOpenMods = { showModsDialog = true },
                onOpenLan = { showLanDialog = true },
                onOpenAdmin = { showAdminDialog = true },
                onOpenCredits = { showCreditsDialog = true },
                onLogout = { viewModel.logoutAccount() }
            )
        }
        else -> {
            GameScreen(
                hero = activeHero!!,
                viewModel = viewModel,
                onOpenMods = { showModsDialog = true },
                onOpenLan = { showLanDialog = true },
                onOpenSettings = { showSettingsDialog = true },
                onOpenAdmin = { showAdminDialog = true }
            )
        }
    }

    if (showModsDialog) {
        ModsDialog(
            mods = mods,
            activeRates = activeModRates,
            onToggleMod = { viewModel.toggleMod(it) },
            onAddMod = { viewModel.addMod(it) },
            onDeleteMod = { viewModel.deleteMod(it) },
            onExportModJson = { viewModel.exportModJson(it) },
            onImportModJson = { viewModel.importModJson(it) },
            onDismiss = { showModsDialog = false }
        )
    }

    if (showLanDialog) {
        LanServerDialog(
            localHeroId = activeHero?.id ?: heroes.firstOrNull()?.id,
            status = lanStatus,
            connectedRoom = connectedRoom,
            hostEndpoint = hostEndpoint,
            lanError = lanError,
            localIps = remember(showLanDialog) { viewModel.lanManager.getLocalIpv4Addresses() },
            discoveredRooms = discoveredRooms,
            players = lanPlayers,
            onStartDiscovery = { viewModel.lanManager.startDiscovery() },
            onStopDiscovery = { viewModel.lanManager.stopDiscovery() },
            onHostRoom = { code -> viewModel.hostLanRoom(code) },
            onJoinRoom = { ipPort, code -> viewModel.joinLanRoom(ipPort, code) },
            onLeaveRoom = { viewModel.leaveLanRoom() },
            onRequestTrade = { pid -> viewModel.lanManager.sendTradeRequest(pid) },
            onStartEvent = { evType -> viewModel.lanManager.sendStartEvent(evType) },
            onDismiss = { showLanDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = hudSettings,
            onUpdateSettings = { transform -> viewModel.updateHudSettings(transform) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showAdminDialog) {
        AdminDialog(
            mapCatalog = mapCatalog,
            manifestInfo = manifestInfo,
            initialManifestUrl = hudSettings.manifestUrl,
            syncStatusMessage = syncStatusMessage,
            onUpdateMapRates = { mid, exp, zen, drop -> viewModel.updateMapRates(mid, exp, zen, drop) },
            onLoadMapsRawJson = { viewModel.loadMapsRawJson() },
            onSaveMapsRawJson = { raw, cb -> viewModel.saveMapsRawJson(raw, cb) },
            onLoadNpcsRawJson = { viewModel.loadNpcsRawJson() },
            onSaveNpcsRawJson = { raw, cb -> viewModel.saveNpcsRawJson(raw, cb) },
            onSyncManifestUrl = { url -> viewModel.syncManifestFromUrl(url) },
            onDismiss = { showAdminDialog = false }
        )
    }

    if (showCreditsDialog) {
        CreditsDialog(onDismiss = { showCreditsDialog = false })
    }
}
