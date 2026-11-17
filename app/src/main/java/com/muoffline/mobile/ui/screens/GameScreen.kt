package com.muoffline.mobile.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muoffline.mobile.model.*
import com.muoffline.mobile.ui.dialogs.*
import com.muoffline.mobile.ui.game.GameHudOverlay
import com.muoffline.mobile.ui.game.GameWorldCanvas
import com.muoffline.mobile.viewmodel.GameViewModel
import kotlin.math.hypot

@Composable
fun GameScreen(
    hero: HeroSave,
    viewModel: GameViewModel,
    onOpenMods: () -> Unit,
    onOpenLan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val monsters by viewModel.monsters.collectAsStateWithLifecycle()
    val drops by viewModel.drops.collectAsStateWithLifecycle()
    val floatingTexts by viewModel.floatingTexts.collectAsStateWithLifecycle()
    val vfxList by viewModel.vfxList.collectAsStateWithLifecycle()
    val skillCooldowns by viewModel.skillCooldowns.collectAsStateWithLifecycle()
    val autoHunt by viewModel.autoHunt.collectAsStateWithLifecycle()
    val muted by viewModel.muted.collectAsStateWithLifecycle()
    val heroFacingAngle by viewModel.heroFacingAngle.collectAsStateWithLifecycle()
    val activeModRates by viewModel.activeModRates.collectAsStateWithLifecycle()
    val mods by viewModel.mods.collectAsStateWithLifecycle()
    val mapCatalog by viewModel.mapCatalog.collectAsStateWithLifecycle()
    val allNpcs by viewModel.npcs.collectAsStateWithLifecycle()
    val hudSettings by viewModel.hudSettings.collectAsStateWithLifecycle()
    val vaultItems by viewModel.vaultItems.collectAsStateWithLifecycle()

    val connectedRoom by viewModel.connectedRoom.collectAsStateWithLifecycle()
    val lanPlayers by viewModel.lanPlayers.collectAsStateWithLifecycle()
    val lanBosses by viewModel.lanBosses.collectAsStateWithLifecycle()
    val activeEvent by viewModel.activeEvent.collectAsStateWithLifecycle()
    val incomingTrade by viewModel.incomingTrade.collectAsStateWithLifecycle()
    val tradeSession by viewModel.tradeSession.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isEditingHudLayout by viewModel.isEditingHudLayout.collectAsStateWithLifecycle()

    var showCharacterDialog by remember { mutableStateOf(false) }
    var showInventoryDialog by remember { mutableStateOf(false) }
    var showVaultDialog by remember { mutableStateOf(false) }
    var inventoryInitialChaos by remember { mutableStateOf(false) }
    var activeNpc by remember { mutableStateOf<MapNpcDef?>(null) }
    var isLandscapeLocked by remember { mutableStateOf(true) }

    val currentMapMeta = mapCatalog[hero.mapId] ?: MuCatalog.DEFAULT_MAP_CATALOG.getValue(hero.mapId)
    val npcsOnMap = remember(hero.mapId, allNpcs) {
        allNpcs.filter { it.mapId == hero.mapId }
    }
    val nearbyNpc = remember(hero.x, hero.y, npcsOnMap) {
        npcsOnMap.minByOrNull {
            hypot((it.wx - hero.x).toDouble(), (it.wy - hero.y).toDouble())
        }?.takeIf {
            hypot((it.wx - hero.x).toDouble(), (it.wy - hero.y).toDouble()) <= 95.0
        }
    }

    BackHandler {
        when {
            isEditingHudLayout -> viewModel.setEditingHudLayout(false)
            tradeSession != null -> viewModel.cancelTrade()
            activeNpc != null -> activeNpc = null
            showVaultDialog -> showVaultDialog = false
            showInventoryDialog -> showInventoryDialog = false
            showCharacterDialog -> showCharacterDialog = false
            else -> viewModel.exitToTitle()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GameWorldCanvas(
            hero = hero,
            heroFacingAngle = heroFacingAngle,
            mapMeta = currentMapMeta,
            npcsOnMap = npcsOnMap,
            monsters = monsters,
            drops = drops,
            floatingTexts = floatingTexts,
            vfxList = vfxList,
            lanPlayers = lanPlayers,
            lanBosses = lanBosses,
            hdGraphics = hudSettings.hdGraphics,
            animationsEnabled = hudSettings.animationsEnabled,
            onTapNpc = { npc ->
                when (npc.role) {
                    "chaos" -> {
                        inventoryInitialChaos = true
                        showInventoryDialog = true
                    }
                    "vault" -> showVaultDialog = true
                    else -> activeNpc = npc
                }
            },
            onTapWorld = { wx, wy -> viewModel.handleTapWorld(wx, wy) }
        )

        GameHudOverlay(
            hero = hero,
            mapCatalog = mapCatalog,
            npcsOnMap = npcsOnMap,
            monsters = monsters,
            lanPlayers = lanPlayers,
            hudSettings = hudSettings,
            autoHunt = autoHunt,
            muted = muted,
            isLandscapeLocked = isLandscapeLocked,
            isEditingHudLayout = isEditingHudLayout,
            activeModsCount = mods.count { it.enabled },
            skillCooldowns = skillCooldowns,
            lanConnectedRoom = connectedRoom,
            lanPlayersCount = lanPlayers.size,
            activeEvent = activeEvent,
            incomingTrade = incomingTrade,
            chatMessages = chatMessages,
            nearbyNpc = nearbyNpc,
            onOpenCharacter = { showCharacterDialog = true },
            onChangeMap = { viewModel.changeMap(it) },
            onOpenInventory = {
                inventoryInitialChaos = false
                showInventoryDialog = true
            },
            onOpenTrade = { viewModel.openTradeMenu() },
            onOpenMods = onOpenMods,
            onOpenLan = onOpenLan,
            onOpenSettings = onOpenSettings,
            onOpenAdmin = onOpenAdmin,
            onToggleLandscape = {
                val next = !isLandscapeLocked
                isLandscapeLocked = next
                activity?.requestedOrientation = if (next) {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            },
            onToggleMute = { viewModel.toggleMute() },
            onExitToTitle = { viewModel.exitToTitle() },
            onToggleAutoHunt = { viewModel.toggleAutoHunt() },
            onJoystickMove = { jx, jy ->
                viewModel.joystickX = jx
                viewModel.joystickY = jy
            },
            onUsePotion = { viewModel.usePotion(it) },
            onCastSkill = { viewModel.executeCombat(it) },
            onTalkNpc = { npc ->
                when (npc.role) {
                    "chaos" -> {
                        inventoryInitialChaos = true
                        showInventoryDialog = true
                    }
                    "vault" -> showVaultDialog = true
                    else -> activeNpc = npc
                }
            },
            onAcceptTrade = { viewModel.acceptIncomingTrade() },
            onDeclineTrade = { viewModel.declineIncomingTrade() },
            onSendChat = { msg -> viewModel.lanManager.sendChat(msg) },
            onUpdateButtonOffset = { id, dxDp, dyDp -> viewModel.updateHudButtonOffset(id, dxDp, dyDp) },
            onSaveHudLayout = { viewModel.setEditingHudLayout(false) },
            onResetHudLayout = { viewModel.resetHudButtonOffsets() }
        )
    }

    if (showCharacterDialog) {
        CharacterDialog(
            hero = hero,
            resetLevel = activeModRates.resetLevel,
            onAddStat = { kind, amt -> viewModel.addStat(kind, amt) },
            onAdvanceEvolution = { viewModel.advanceEvolutionQuest() },
            onMasterReset = { viewModel.resetHero() },
            onDismiss = { showCharacterDialog = false }
        )
    }

    if (showInventoryDialog) {
        InventoryChaosDialog(
            hero = hero,
            chaosBonus = activeModRates.chaosBonus,
            initialTabChaos = inventoryInitialChaos,
            onEquipItem = { viewModel.equipItem(it) },
            onUnequipSlotKey = { viewModel.unequipSlotKey(it) },
            onUpgradeWithJewel = { id, jewel -> viewModel.upgradeWithJewel(id, jewel) },
            onChaosUpgrade = { id -> viewModel.chaosUpgrade(id) },
            onCraftWingsTier = { tier -> viewModel.craftWingsTier(tier) },
            onSellItem = { id -> viewModel.sellItem(id) },
            onDismiss = { showInventoryDialog = false }
        )
    }

    if (showVaultDialog) {
        VaultDialog(
            hero = hero,
            vaultItems = vaultItems,
            onDeposit = { item -> viewModel.depositToVault(item) },
            onWithdraw = { item -> viewModel.withdrawFromVault(item) },
            onDismiss = { showVaultDialog = false }
        )
    }

    activeNpc?.let { npc ->
        NpcShopDialog(
            npc = npc,
            hero = hero,
            repairCostForItem = { item -> viewModel.repairCostForItem(item) },
            onBuyItem = { tpl, cost -> viewModel.buyShopItem(tpl, cost) },
            onSellItem = { itemId -> viewModel.sellItem(itemId) },
            onRepairItem = { itemId -> viewModel.repairItem(itemId) },
            onRepairAll = { viewModel.repairAllEquipment() },
            onClaimLeoSupply = { viewModel.claimLeoHelperSupply() },
            onOpenChaosMachine = {
                inventoryInitialChaos = true
                showInventoryDialog = true
            },
            onOpenVault = { showVaultDialog = true },
            onAdvanceEvolution = { viewModel.advanceEvolutionQuest() },
            onRedeemArcherWings = { viewModel.redeemGoldenArcherWings() },
            onDismiss = { activeNpc = null }
        )
    }

    tradeSession?.let { session ->
        TradeDialog(
            hero = hero,
            session = session,
            onUpdateOffer = { items, zen -> viewModel.updateTradeOffer(items, zen) },
            onConfirmTrade = { viewModel.confirmTrade() },
            onCancelTrade = { viewModel.cancelTrade() }
        )
    }
}
