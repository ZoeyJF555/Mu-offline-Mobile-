package com.muoffline.mobile.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    nearbyNpc: LorenciaNpcDef?,
    onOpenCharacter: () -> Unit,
    onChangeMap: (MapId) -> Unit,
    onOpenInventory: () -> Unit,
    onOpenMods: () -> Unit,
    onOpenLan: () -> Unit,
    onToggleLandscape: () -> Unit,
    onToggleMute: () -> Unit,
    onExitToTitle: () -> Unit,
    onToggleAutoHunt: () -> Unit,
    onJoystickMove: (Float, Float) -> Unit,
    onUsePotion: (PotionType) -> Unit,
    onCastSkill: (String?) -> Unit,
    onTalkNpc: (LorenciaNpcDef) -> Unit,
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
    val currentMapMeta = MuCatalog.MAP_CATALOG.getValue(hero.mapId)

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
                color = MuObsidian.copy(alpha = 0.86f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MuGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Hero + Map Warp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = MuStoneCard,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(MuCatalog.CLASS_CATALOG.getValue(hero.classId).accentColor).copy(alpha = 0.6f)),
                            modifier = Modifier
                                .clickable(onClick = onOpenCharacter)
                                .testTag("hud_character_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                border = BorderStroke(1.dp, Color(currentMapMeta.accentColor).copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .clickable { mapMenuExpanded = true }
                                    .testTag("hud_map_warp")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                                    val m = MuCatalog.MAP_CATALOG.getValue(mid)
                                    DropdownMenuItem(
                                        text = {
                                            Text("${m.name} (Nv.${m.minLevel}+)")
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

                    // Right: Quick actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        HudIconAction(
                            icon = Icons.Rounded.Backpack,
                            contentDescription = "Mochila y Chaos",
                            tint = MuAmberBright,
                            tag = "hud_inventory_button",
                            onClick = onOpenInventory
                        )
                        HudIconAction(
                            icon = Icons.Rounded.Extension,
                            contentDescription = "Mods ($activeModsCount)",
                            tint = MuGold,
                            tag = "hud_mods_button",
                            onClick = onOpenMods
                        )
                        HudIconAction(
                            icon = Icons.Rounded.Wifi,
                            contentDescription = "Sala LAN",
                            tint = if (lanConnectedRoom != null) MuEmerald else MuSky,
                            tag = "hud_lan_button",
                            onClick = onOpenLan
                        )
                        if (lanConnectedRoom != null) {
                            HudIconAction(
                                icon = Icons.AutoMirrored.Rounded.Chat,
                                contentDescription = "Chat LAN ($lanPlayersCount)",
                                tint = MuSky,
                                tag = "hud_chat_toggle",
                                onClick = { chatOpen = !chatOpen }
                            )
                        }
                        HudIconAction(
                            icon = Icons.Rounded.ScreenRotation,
                            contentDescription = stringResource(id = R.string.cd_landscape),
                            tint = if (isLandscapeLocked) MuAmberBright else MuTextSecondary,
                            tag = "hud_landscape_button",
                            onClick = onToggleLandscape
                        )
                        HudIconAction(
                            icon = if (muted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = stringResource(id = R.string.cd_mute),
                            tint = if (muted) MuCrimson else MuTextPrimary,
                            tag = "hud_mute_button",
                            onClick = onToggleMute
                        )
                        HudIconAction(
                            icon = Icons.AutoMirrored.Rounded.ExitToApp,
                            contentDescription = stringResource(id = R.string.cd_exit_title),
                            tint = MuCrimson,
                            tag = "hud_exit_button",
                            onClick = onExitToTitle
                        )
                    }
                }
            }

            // Cooperative Event Banner
            if (activeEvent != null && activeEvent.active) {
                Surface(
                    color = Color(0xFF2D1906).copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MuAmberBright),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Evento Cooperativo: ${activeEvent.name}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MuAmberBright
                            )
                            Text(
                                text = "Recompensa: +${activeEvent.rewardZen} Zen · ${MuCatalog.MAP_CATALOG.getValue(activeEvent.mapId).name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MuTextPrimary
                            )
                        }
                        if (hero.mapId != activeEvent.mapId) {
                            Button(
                                onClick = { onChangeMap(activeEvent.mapId) },
                                colors = ButtonDefaults.buttonColors(containerColor = MuGold, contentColor = Color(0xFF1B1202)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Ir al Evento", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Incoming P2P Trade Banner
            if (incomingTrade != null) {
                Surface(
                    color = Color(0xFF0A2438).copy(alpha = 0.94f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MuSky),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${incomingTrade.fromPlayerName} quiere intercambiar contigo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuTextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = onAcceptTrade,
                                colors = ButtonDefaults.buttonColors(containerColor = MuEmerald, contentColor = Color(0xFF022016)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("accept_trade_banner")
                            ) {
                                Text("Aceptar", fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = onDeclineTrade,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("decline_trade_banner")
                            ) {
                                Text("Rechazar", fontSize = 12.sp, color = MuCrimson)
                            }
                        }
                    }
                }
            }

            // Collapsible LAN Chat Drawer
            if (chatOpen && lanConnectedRoom != null) {
                Surface(
                    color = MuObsidian.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MuSky.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .heightIn(max = 175.dp)
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = chatDraft,
                                onValueChange = { if (it.length <= 160) chatDraft = it },
                                placeholder = { Text("Mensaje LAN...", fontSize = 12.sp) },
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

        // Nearby NPC Floating Prompt
        if (nearbyNpc != null) {
            Button(
                onClick = { onTalkNpc(nearbyNpc) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MuAmberBright,
                    contentColor = Color(0xFF1B1202)
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 110.dp)
                    .heightIn(min = 48.dp)
                    .testTag("talk_npc_button")
            ) {
                Icon(Icons.Rounded.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hablar con ${nearbyNpc.name}")
            }
        }

        // BOTTOM CONTROLS (Virtual Joystick + Classic MU Bar + Skills/Attack)
        BoxWithConstraints(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            val compactWidth = maxWidth < 520.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT THUMB: MU Helper + Virtual Analog Joystick
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

                    VirtualJoystick(onMove = onJoystickMove)
                }

                // CENTER CLASSIC MU BAR (HP Globe, EXP Bar, MP Globe)
                if (!compactWidth) {
                    ClassicMuCenterBar(
                        hero = hero,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                    )
                }

                // RIGHT THUMB: Potions + Skills + Primary Attack
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PotionCircleButton(
                            label = "HP",
                            count = hpQty,
                            color = MuCrimson,
                            tag = "potion_hp_button",
                            onClick = { onUsePotion(PotionType.HP) }
                        )
                        PotionCircleButton(
                            label = "MP",
                            count = mpQty,
                            color = MuSky,
                            tag = "potion_mp_button",
                            onClick = { onUsePotion(PotionType.MP) }
                        )
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
                            Surface(
                                color = if (onCd) MuObsidian.copy(alpha = 0.85f) else skillColor.copy(alpha = 0.25f),
                                shape = CircleShape,
                                border = BorderStroke(1.5.dp, if (onCd) MuBorderSlate else skillColor),
                                modifier = Modifier
                                    .size(50.dp)
                                    .clickable(enabled = !onCd) { onCastSkill(sk.id) }
                                    .testTag("skill_button_$idx")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (onCd) {
                                        Text(
                                            text = String.format("%.1f", remainingSec),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MuTextSecondary
                                        )
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

                        // Primary Circular Attack Button
                        Surface(
                            color = MuGold,
                            shape = CircleShape,
                            border = BorderStroke(2.dp, Color(0xFFFDE68A)),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(66.dp)
                                .clickable { onCastSkill(null) }
                                .testTag("attack_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Gavel,
                                    contentDescription = stringResource(id = R.string.cd_attack),
                                    tint = Color(0xFF1B1202),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HudIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .testTag(tag)
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun VirtualJoystick(onMove: (Float, Float) -> Unit) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = 92f

    Box(
        modifier = Modifier
            .size(116.dp)
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
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(MuAmberBright, MuGoldMuted))
                )
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
            .size(48.dp)
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
            // Red HP Globe
            ResourceGlobe(
                ratio = hpRatio,
                color = MuCrimson,
                label = "${hero.hp}",
                modifier = Modifier.size(44.dp)
            )

            // Center EXP 10-segment bar + Zen
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

            // Blue MP Globe
            ResourceGlobe(
                ratio = mpRatio,
                color = MuSky,
                label = "${hero.mp}",
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
private fun ResourceGlobe(
    ratio: Float,
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
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
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontSize = 10.sp
        )
    }
}
