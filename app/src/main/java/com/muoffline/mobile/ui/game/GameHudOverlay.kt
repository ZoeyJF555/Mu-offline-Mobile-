package com.muoffline.mobile.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muoffline.mobile.R
import com.muoffline.mobile.model.*
import com.muoffline.mobile.ui.theme.*
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun GameHudOverlay(
    hero: HeroSave,
    mapCatalog: Map<MapId, MapMeta>,
    npcsOnMap: List<MapNpcDef>,
    monsters: List<LiveMonster>,
    lanPlayers: List<LanPlayer>,
    hudSettings: MobileHudSettings,
    autoHunt: Boolean,
    muted: Boolean,
    isLandscapeLocked: Boolean,
    activeModsCount: Int,
    skillCooldowns: Map<String, Long>,
    lanConnectedRoom: String?,
    lanPlayersCount: Int,
    activeEvent: LanEventState?,
    incomingTrade: IncomingTradeRequest?,
    chatMessages: List<LanChatMessage>,
    nearbyNpc: MapNpcDef?,
    onOpenCharacter: () -> Unit,
    onChangeMap: (MapId) -> Unit,
    onOpenInventory: () -> Unit,
    onOpenMods: () -> Unit,
    onOpenLan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit,
    onToggleLandscape: () -> Unit,
    onToggleMute: () -> Unit,
    onExitToTitle: () -> Unit,
    onToggleAutoHunt: () -> Unit,
    onJoystickMove: (Float, Float) -> Unit,
    onUsePotion: (PotionType) -> Unit,
    onCastSkill: (String?) -> Unit,
    onTalkNpc: (MapNpcDef) -> Unit,
    onAcceptTrade: () -> Unit,
    onDeclineTrade: () -> Unit,
    onSendChat: (String) -> Unit
) {
    var mapMenuExpanded by remember { mutableStateOf(false) }
    var chatOpen by remember { mutableStateOf(false) }
    var chatDraft by remember { mutableStateOf("") }
    var clockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(skillCooldowns) {
        while (skillCooldowns.values.any { it > System.currentTimeMillis() }) {
            clockMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(100L)
        }
        clockMs = System.currentTimeMillis()
    }

    val hpQty = remember(hero.inventory) {
        hero.inventory.filter { it.potionType == PotionType.HP }.sumOf { it.quantity }
    }
    val mpQty = remember(hero.inventory) {
        hero.inventory.filter { it.potionType == PotionType.MP }.sumOf { it.quantity }
    }
    val skills = remember(hero.classId) {
        MuCatalog.SKILLS_BY_CLASS[hero.classId].orEmpty()
    }
    val currentMapMeta = mapCatalog[hero.mapId] ?: MuCatalog.DEFAULT_MAP_CATALOG.getValue(hero.mapId)
    val scale = hudSettings.hudScale.coerceIn(0.75f, 1.35f)

    Box(modifier = Modifier.fillMaxSize()) {
        // Screen brightness adjustment overlay
        val dimAlpha = (1f - hudSettings.brightness).coerceIn(0f, 0.65f)
        if (dimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            // TOP HUD BAR & BANNERS
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = MuObsidian.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MuGold.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = MuStoneCard,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(MuCatalog.CLASS_CATALOG.getValue(hero.classId).accentColor).copy(alpha = 0.65f)),
                                    modifier = Modifier
                                        .clickable(onClick = onOpenCharacter)
                                        .testTag("hud_character_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = "${hero.name} Nv.${hero.level}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MuTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (hero.statPoints > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(MuGold)
                                            )
                                        }
                                    }
                                }

                                Box {
                                    Surface(
                                        color = MuStoneCard,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(currentMapMeta.accentColor).copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .clickable { mapMenuExpanded = true }
                                            .testTag("hud_map_warp")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = currentMapMeta.name,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(currentMapMeta.accentColor),
                                                maxLines = 1
                                            )
                                            Icon(
                                                Icons.Rounded.ArrowDropDown,
                                                contentDescription = "Cambiar mapa",
                                                tint = MuTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = mapMenuExpanded,
                                        onDismissRequest = { mapMenuExpanded = false }
                                    ) {
                                        MapId.entries.forEach { mid ->
                                            val m = mapCatalog[mid] ?: MuCatalog.DEFAULT_MAP_CATALOG.getValue(mid)
                                            DropdownMenuItem(
                                                text = {
                                                    Text("${m.name} (Nv.${m.minLevel}+ · Warp ${m.warpLevel})")
                                                },
                                                onClick = {
                                                    mapMenuExpanded = false
                                                    onChangeMap(mid)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "${hero.zen} Zen",
                                style = MaterialTheme.typography.labelMedium,
                                color = MuAmberBright,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }

                        // Compact always-visible HP / MP / EXP strip
                        val hpRatioTop = (hero.hp.toFloat() / hero.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val mpRatioTop = (hero.mp.toFloat() / hero.maxMp.coerceAtLeast(1)).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { hpRatioTop },
                                color = MuCrimson,
                                trackColor = MuStoneCard,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Text("HP ${hero.hp}/${hero.maxHp}", style = MaterialTheme.typography.labelSmall, color = MuTextPrimary, fontSize = 10.sp)
                            LinearProgressIndicator(
                                progress = { mpRatioTop },
                                color = MuSky,
                                trackColor = MuStoneCard,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                            Text("MP ${hero.mp}/${hero.maxMp}", style = MaterialTheme.typography.labelSmall, color = MuSky, fontSize = 10.sp)
                        }

                        // Scrollable labeled action pills (never crushed on narrow screens)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HudChipAction(Icons.Rounded.Person, "PJ", Color(MuCatalog.CLASS_CATALOG.getValue(hero.classId).accentColor), "hud_pj_chip", onOpenCharacter)
                            HudChipAction(Icons.Rounded.Backpack, "Mochila", MuAmberBright, "hud_inventory_button", onOpenInventory)
                            HudChipAction(Icons.Rounded.Extension, "Mods($activeModsCount)", MuGold, "hud_mods_button", onOpenMods)
                            HudChipAction(
                                Icons.Rounded.Wifi,
                                lanConnectedRoom ?: "LAN/BT",
                                if (lanConnectedRoom != null) MuEmerald else MuSky,
                                "hud_lan_button",
                                onOpenLan
                            )
                            if (lanConnectedRoom != null) {
                                HudChipAction(Icons.AutoMirrored.Rounded.Chat, "Chat($lanPlayersCount)", MuSky, "hud_chat_toggle") {
                                    chatOpen = !chatOpen
                                }
                            }
                            HudChipAction(Icons.Rounded.Tune, "Ajustes", MuTextPrimary, "hud_settings_button", onOpenSettings)
                            HudChipAction(Icons.Rounded.AdminPanelSettings, "Admin", MuPurple, "hud_admin_button", onOpenAdmin)
                            HudChipAction(
                                Icons.Rounded.ScreenRotation,
                                if (isLandscapeLocked) "Vertical" else "Horiz.",
                                if (isLandscapeLocked) MuAmberBright else MuTextSecondary,
                                "hud_landscape_button",
                                onToggleLandscape
                            )
                            HudChipAction(
                                if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                                if (muted) "Silencio" else "Audio",
                                if (muted) MuCrimson else MuTextPrimary,
                                "hud_mute_button",
                                onToggleMute
                            )
                            HudChipAction(
                                Icons.AutoMirrored.Rounded.ExitToApp,
                                "Menú",
                                MuCrimson,
                                "hud_exit_button",
                                onExitToTitle
                            )
                        }
                    }
                }

                // Top-Right Minimap & Exact (X, Y) Coordinates + Left Event/Trade Banners
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (activeEvent != null && activeEvent.active) {
                            Surface(
                                color = Color(0xFF2D1906).copy(alpha = 0.92f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MuAmberBright),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Evento: ${activeEvent.name}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MuAmberBright
                                        )
                                        Text(
                                            text = "+${activeEvent.rewardZen} Zen · ${(mapCatalog[activeEvent.mapId] ?: MuCatalog.DEFAULT_MAP_CATALOG.getValue(activeEvent.mapId)).name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MuTextPrimary
                                        )
                                    }
                                    if (hero.mapId != activeEvent.mapId) {
                                        Button(
                                            onClick = { onChangeMap(activeEvent.mapId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MuGold, contentColor = Color(0xFF1B1202)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("Ir", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (incomingTrade != null) {
                            Surface(
                                color = Color(0xFF0A2438).copy(alpha = 0.94f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MuSky),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${incomingTrade.fromPlayerName} pide intercambio",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MuTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = onAcceptTrade,
                                            colors = ButtonDefaults.buttonColors(containerColor = MuEmerald, contentColor = Color(0xFF022016)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("accept_trade_banner")
                                        ) { Text("Sí", fontSize = 12.sp) }
                                        OutlinedButton(
                                            onClick = onDeclineTrade,
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("decline_trade_banner")
                                        ) { Text("No", fontSize = 12.sp, color = MuCrimson) }
                                    }
                                }
                            }
                        }

                        if (chatOpen && lanConnectedRoom != null) {
                            Surface(
                                color = MuObsidian.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MuSky.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .widthIn(max = 320.dp)
                                    .heightIn(max = 160.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(chatMessages, key = { it.id }) { m ->
                                            Text(
                                                text = "${m.senderName}: ${m.text}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(MuCatalog.CLASS_CATALOG.getValue(m.classId).accentColor)
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = chatDraft,
                                            onValueChange = { if (it.length <= 160) chatDraft = it },
                                            placeholder = { Text("Chat LAN...", fontSize = 12.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                if (chatDraft.isNotBlank()) {
                                                    onSendChat(chatDraft.trim())
                                                    chatDraft = ""
                                                }
                                            }
                                        ) {
                                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar", tint = MuSky)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Top-Right Minimap & Exact (X, Y) Coordinates
                    MinimapWidget(
                        hero = hero,
                        mapMeta = currentMapMeta,
                        npcs = npcsOnMap,
                        monsters = monsters,
                        lanPlayers = lanPlayers
                    )
                }
            }

            // Nearby NPC Prompt
            if (nearbyNpc != null) {
                Button(
                    onClick = { onTalkNpc(nearbyNpc) },
                    colors = ButtonDefaults.buttonColors(containerColor = MuAmberBright, contentColor = Color(0xFF1B1202)),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = (110f + hudSettings.hudBottomPaddingDp).dp)
                        .heightIn(min = 48.dp)
                        .testTag("talk_npc_button")
                ) {
                    Icon(Icons.Rounded.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hablar con ${nearbyNpc.name}")
                }
            }

            // BOTTOM CONTROLS
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = hudSettings.hudBottomPaddingDp.dp)
            ) {
                val compactWidth = maxWidth < 520.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = if (autoHunt) MuEmerald else MuStoneCard.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, if (autoHunt) Color(0xFFA7F3D0) else MuBorderSlate),
                            modifier = Modifier
                                .clickable(onClick = onToggleAutoHunt)
                                .testTag("mu_helper_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (autoHunt) Color(0xFF022016) else MuAmberBright,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(id = if (autoHunt) R.string.mu_helper_on else R.string.mu_helper),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (autoHunt) Color(0xFF022016) else MuTextPrimary
                                )
                            }
                        }

                        VirtualJoystick(sizeDp = (114f * scale).roundToInt(), onMove = onJoystickMove)
                    }

                    if (!compactWidth) {
                        ClassicMuCenterBar(
                            hero = hero,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 10.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PotionCircleButton("HP", hpQty, MuCrimson, "potion_hp_button") { onUsePotion(PotionType.HP) }
                            PotionCircleButton("MP", mpQty, MuSky, "potion_mp_button") { onUsePotion(PotionType.MP) }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            skills.forEachIndexed { idx, sk ->
                                val cdUntil = skillCooldowns[sk.id] ?: 0L
                                val remainingSec = ((cdUntil - clockMs) / 1000.0).coerceAtLeast(0.0)
                                val onCd = remainingSec > 0.05
                                val skillColor = Color(sk.colorHex)
                                val btnSize = (48f * scale).roundToInt().dp
                                Surface(
                                    color = if (onCd) MuObsidian.copy(alpha = 0.85f) else skillColor.copy(alpha = 0.25f),
                                    shape = CircleShape,
                                    border = BorderStroke(1.5.dp, if (onCd) MuBorderSlate else skillColor),
                                    modifier = Modifier
                                        .size(btnSize)
                                        .clickable(enabled = !onCd) { onCastSkill(sk.id) }
                                        .testTag("skill_button_$idx")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (onCd) {
                                            Text(String.format("%.1f", remainingSec), style = MaterialTheme.typography.labelMedium, color = MuTextSecondary)
                                        } else {
                                            Text(
                                                text = sk.name.split(" ").take(2).joinToString("\n") { it.take(4) },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MuTextPrimary,
                                                lineHeight = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                color = MuGold,
                                shape = CircleShape,
                                border = BorderStroke(2.dp, Color(0xFFFDE68A)),
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .size((64f * scale).roundToInt().dp)
                                    .clickable { onCastSkill(null) }
                                    .testTag("attack_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Gavel,
                                        contentDescription = stringResource(id = R.string.cd_attack),
                                        tint = Color(0xFF1B1202),
                                        modifier = Modifier.size((28f * scale).roundToInt().dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimapWidget(
    hero: HeroSave,
    mapMeta: MapMeta,
    npcs: List<MapNpcDef>,
    monsters: List<LiveMonster>,
    lanPlayers: List<LanPlayer>
) {
    Surface(
        color = MuObsidian.copy(alpha = 0.84f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(mapMeta.accentColor).copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Canvas(modifier = Modifier.size(84.dp)) {
                val scale = size.width / MuWorldConstants.WORLD_SIZE
                drawRect(color = Color(mapMeta.groundPrimary))
                drawCircle(
                    color = Color(mapMeta.accentColor).copy(alpha = 0.45f),
                    radius = MuWorldConstants.SAFE_RADIUS * scale,
                    center = Offset(MuWorldConstants.SAFE_CENTER * scale, MuWorldConstants.SAFE_CENTER * scale),
                    style = Stroke(width = 1.5f)
                )
                for (npc in npcs) {
                    drawCircle(color = Color(0xFF34D399), radius = 2.5f, center = Offset(npc.wx * scale, npc.wy * scale))
                }
                for (m in monsters) {
                    if (m.hp > 0) {
                        drawCircle(
                            color = if (m.isBoss) Color(0xFFFBBF24) else Color(0xFFEF4444),
                            radius = if (m.isBoss) 3.5f else 2f,
                            center = Offset(m.x * scale, m.y * scale)
                        )
                    }
                }
                for (lp in lanPlayers) {
                    if (lp.id != hero.id && lp.mapId == hero.mapId) {
                        drawCircle(color = Color(0xFF38BDF8), radius = 3f, center = Offset(lp.x * scale, lp.y * scale))
                    }
                }
                drawCircle(color = Color.White, radius = 3.5f, center = Offset(hero.x * scale, hero.y * scale))
            }
            Text(
                text = "X:${hero.x.roundToInt()} Y:${hero.y.roundToInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = MuAmberBright,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun HudChipAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        color = MuStoneCard,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.4f)),
        modifier = Modifier
            .heightIn(min = 34.dp)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(15.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MuTextPrimary, maxLines = 1)
        }
    }
}

@Composable
private fun VirtualJoystick(sizeDp: Int, onMove: (Float, Float) -> Unit) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 92f

    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(MuObsidian.copy(alpha = 0.52f))
            .border(1.5.dp, MuGold.copy(alpha = 0.42f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    val candidate = knobOffset + dragAmount
                    val dist = hypot(candidate.x.toDouble(), candidate.y.toDouble()).toFloat()
                    val clamped = if (dist > maxRadiusPx) {
                        Offset(candidate.x / dist * maxRadiusPx, candidate.y / dist * maxRadiusPx)
                    } else {
                        candidate
                    }
                    knobOffset = clamped
                    onMove(clamped.x / maxRadiusPx, clamped.y / maxRadiusPx)
                }
            }
            .testTag("virtual_joystick"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(knobOffset.x.roundToInt(), knobOffset.y.roundToInt()) }
                .size((sizeDp * 0.40f).roundToInt().dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(MuAmberBright, MuGoldMuted)))
                .border(1.5.dp, Color(0xFFFDE68A), CircleShape)
        )
    }
}

@Composable
private fun PotionCircleButton(
    label: String,
    count: Int,
    color: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        color = MuObsidian.copy(alpha = 0.85f),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, if (count > 0) color else MuBorderSlate),
        modifier = Modifier
            .size(46.dp)
            .clickable(enabled = count > 0, onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            Text("x$count", style = MaterialTheme.typography.labelSmall, color = MuTextPrimary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ClassicMuCenterBar(hero: HeroSave, modifier: Modifier = Modifier) {
    val hpRatio = (hero.hp.toFloat() / hero.maxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
    val mpRatio = (hero.mp.toFloat() / hero.maxMp.coerceAtLeast(1)).coerceIn(0f, 1f)
    val expRatio = (hero.exp.toFloat() / hero.expToNext.coerceAtLeast(1)).coerceIn(0f, 1f)
    val filledSegments = (expRatio * 10f).toInt().coerceIn(0, 10)

    Surface(
        color = MuObsidian.copy(alpha = 0.86f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MuBorderSlate),
        modifier = modifier.widthIn(max = 420.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ResourceGlobe(ratio = hpRatio, color = MuCrimson, label = "${hero.hp}", modifier = Modifier.size(42.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Nv.${hero.level} · RR ${hero.resets}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MuTextPrimary
                    )
                    Text(
                        text = "${hero.zen} Zen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MuAmberBright
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (seg in 0 until 10) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (seg < filledSegments) MuGold else MuStoneCard)
                        )
                    }
                }
            }
            ResourceGlobe(ratio = mpRatio, color = MuSky, label = "${hero.mp}", modifier = Modifier.size(42.dp))
        }
    }
}

@Composable
private fun ResourceGlobe(ratio: Float, color: Color, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MuStoneCard)
            .border(1.5.dp, color.copy(alpha = 0.75f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(ratio)
                .background(color.copy(alpha = 0.72f))
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 10.sp)
    }
}
