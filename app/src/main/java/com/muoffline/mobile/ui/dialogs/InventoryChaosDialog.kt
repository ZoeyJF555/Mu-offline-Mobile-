package com.muoffline.mobile.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.muoffline.mobile.R
import com.muoffline.mobile.model.*
import com.muoffline.mobile.ui.theme.*

@Composable
fun InventoryChaosDialog(
    hero: HeroSave,
    chaosBonus: Int,
    initialTabChaos: Boolean = false,
    onEquipItem: (Item) -> String?,
    onUnequipSlotKey: (EquipSlotKey) -> Unit,
    onUpgradeWithJewel: (String, JewelType) -> String,
    onChaosUpgrade: (String) -> Pair<Boolean, String>,
    onCraftWingsTier: (Int) -> Pair<Boolean, String>,
    onSellItem: (String) -> String,
    onDismiss: () -> Unit
) {
    var tabChaos by remember { mutableStateOf(initialTabChaos) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val classMeta = MuCatalog.CLASS_CATALOG.getValue(hero.classId)
    val classGuide = remember(hero.classId) {
        when (hero.classId) {
            ClassId.DK -> "Dark Knight / Blade Master: Espadas, Hachas, Escudos, Set completo (Casco, Pechera, Guantes, Botas), Alas S1-S3, Collar, 2 Anillos y Mascotas universales (Ángel, Imp, Uniria)."
            ClassId.DW -> "Dark Wizard / Grand Master: Bastones mágicos, Escudos ligeros, Set completo (Casco, Pechera, Guantes, Botas), Alas S1-S3, Collar, 2 Anillos y Mascotas universales."
            ClassId.ELF -> "Fairy Elf / High Elf: Arcos y Ballestas élficas, Set completo (Casco, Pechera, Guantes, Botas), Alas S1-S3, Collar, 2 Anillos y Mascotas universales."
            ClassId.MG -> "Magic Gladiator / Magic Knight: Espadas híbridas y Bastones, Escudos, Pechera, Guantes, Botas, Alas, Collar, 2 Anillos y Mascotas. NO usa Casco por regla clásica MU."
            ClassId.DL -> "Dark Lord / Empire Lord: Cetros imperiales, Escudos, Set completo (Casco/Máscara, Pechera, Guantes, Botas), Capas/Alas, Collar, 2 Anillos y Mascotas exclusivas (Cuervo Oscuro y Caballo Oscuro) + universales."
        }
    }

    val allItems = hero.equipment.allEquipped() + hero.inventory
    val selectedItem = allItems.firstOrNull { it.id == selectedItemId }
    val selectedIsEquipped = selectedItem != null && hero.equipment.allEquipped().any { it.id == selectedItem.id }

    val blessCount = hero.inventory.filter { it.jewelType == JewelType.BLESS }.sumOf { it.quantity }
    val soulCount = hero.inventory.filter { it.jewelType == JewelType.SOUL }.sumOf { it.quantity }
    val chaosCount = hero.inventory.filter { it.jewelType == JewelType.CHAOS }.sumOf { it.quantity }
    val lifeCount = hero.inventory.filter { it.jewelType == JewelType.LIFE }.sumOf { it.quantity }
    val creationCount = hero.inventory.filter { it.jewelType == JewelType.CREATION }.sumOf { it.quantity }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MuAbyssSlate,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MuGold.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 860.dp)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Inventario y Equipo · ${classMeta.titleForTier(hero.evolutionTier)}", style = MaterialTheme.typography.headlineSmall, color = MuAmberBright)
                        Text(
                            text = "${hero.zen} Zen · B:$blessCount S:$soulCount C:$chaosCount L:$lifeCount Cr:$creationCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = MuTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_inventory_dialog")) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(id = R.string.cd_close))
                    }
                }

                // Tab bar
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { tabChaos = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!tabChaos) MuGold else MuStoneCard,
                            contentColor = if (!tabChaos) Color(0xFF1B1202) else MuTextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_bag")
                    ) {
                        Text("Equipo por Raza (11 Slots) y Mochila (${hero.inventory.size}/24)")
                    }
                    Button(
                        onClick = { tabChaos = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (tabChaos) MuPurple else MuStoneCard,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_chaos")
                    ) {
                        Icon(Icons.Rounded.Whatshot, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chaos Machine")
                    }
                }

                statusMessage?.let { msg ->
                    Surface(
                        color = MuStoneCard,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MuAmberBright.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MuAmberBright,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                if (!tabChaos) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Surface(
                                color = MuStoneCard,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, Color(classMeta.accentColor).copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = classGuide,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MuTextPrimary,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        item {
                            Text("Equipamiento Completo (11 Casillas)", style = MaterialTheme.typography.titleMedium, color = MuAmberBright)
                            Spacer(modifier = Modifier.height(6.dp))
                            val rows = listOf(
                                listOf(EquipSlotKey.HELM, EquipSlotKey.PENDANT, EquipSlotKey.WINGS),
                                listOf(EquipSlotKey.WEAPON, EquipSlotKey.ARMOR, EquipSlotKey.OFFHAND),
                                listOf(EquipSlotKey.GLOVES, EquipSlotKey.BOOTS, EquipSlotKey.PET),
                                listOf(EquipSlotKey.RING1, EquipSlotKey.RING2)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                rows.forEach { rowKeys ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowKeys.forEach { key ->
                                            val eq = hero.equipment.getSlotKey(key)
                                            val blockedHelm = key == EquipSlotKey.HELM && !classMeta.canEquipHelm
                                            PaperdollSlotCard(
                                                slotKey = key,
                                                item = eq,
                                                blockedHelm = blockedHelm,
                                                selected = eq != null && selectedItemId == eq.id,
                                                onSelect = { if (eq != null) selectedItemId = eq.id },
                                                onUnequip = { onUnequipSlotKey(key) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Mochila (${hero.inventory.size}/24)", style = MaterialTheme.typography.titleMedium, color = MuSky)
                        }
                        items(hero.inventory, key = { it.id }) { item ->
                            val isSel = selectedItemId == item.id
                            val canEquip = item.slot != ItemSlot.JEWEL && item.slot != ItemSlot.POTION && item.slot != ItemSlot.TALISMAN
                            Surface(
                                color = if (isSel) MuGold.copy(alpha = 0.16f) else MuStoneCard,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(if (isSel) 2.dp else 1.dp, Color(item.colorHex).copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedItemId = item.id }
                                    .testTag("inv_item_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = buildString {
                                                append(item.name)
                                                if (item.plus > 0) append(" +${item.plus}")
                                                if (item.quantity > 1) append(" x${item.quantity}")
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(item.colorHex)
                                        )
                                        val compatOk = (item.classReq == "all" || item.classReq == hero.classId.wire) && !(item.slot == ItemSlot.HELM && !classMeta.canEquipHelm)
                                        val reqLabel = when {
                                            item.slot == ItemSlot.HELM && !classMeta.canEquipHelm -> "MG sin casco"
                                            item.classReq == "all" -> "Todas las razas"
                                            else -> "Solo ${item.classReq.uppercase()}"
                                        }
                                        Text(
                                            text = "${item.slot.labelEs} · $reqLabel · ${item.rarity.name} · ${item.valueZen} Zen",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (compatOk || !canEquip) MuTextSecondary else MuCrimson
                                        )
                                    }
                                    if (canEquip) {
                                        OutlinedButton(
                                            onClick = {
                                                val err = onEquipItem(item)
                                                if (err != null) statusMessage = err
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("Equipar")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Inspector Footer
                    if (selectedItem != null) {
                        val isUpgradeable = selectedItem.slot != ItemSlot.JEWEL &&
                            selectedItem.slot != ItemSlot.POTION &&
                            selectedItem.slot != ItemSlot.TALISMAN
                        Surface(
                            color = MuObsidian,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, Color(selectedItem.colorHex))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${selectedItem.name} +${selectedItem.plus} (ATK ${selectedItem.attack} · MAG ${selectedItem.magic} · DEF ${selectedItem.defense} · HP +${selectedItem.hpBonus} · Dur ${selectedItem.durability}%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(selectedItem.colorHex)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isUpgradeable) {
                                        if (selectedItem.plus < 6) {
                                            Button(
                                                onClick = { statusMessage = onUpgradeWithJewel(selectedItem.id, JewelType.BLESS) },
                                                enabled = blessCount > 0,
                                                colors = ButtonDefaults.buttonColors(containerColor = MuPurple),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("+1 Bless")
                                            }
                                        }
                                        if (selectedItem.plus < 9) {
                                            Button(
                                                onClick = { statusMessage = onUpgradeWithJewel(selectedItem.id, JewelType.SOUL) },
                                                enabled = soulCount > 0,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("+1 Soul")
                                            }
                                        }
                                        Button(
                                            onClick = { statusMessage = onUpgradeWithJewel(selectedItem.id, JewelType.LIFE) },
                                            enabled = lifeCount > 0,
                                            colors = ButtonDefaults.buttonColors(containerColor = MuEmerald, contentColor = Color(0xFF022016)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("+Life")
                                        }
                                    }
                                    if (!selectedIsEquipped) {
                                        OutlinedButton(
                                            onClick = {
                                                statusMessage = onSellItem(selectedItem.id)
                                                selectedItemId = null
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Vender (${selectedItem.valueZen * selectedItem.quantity})", color = MuAmberBright)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // CHAOS MACHINE TAB
                    val chaosCandidates = allItems.filter {
                        it.slot != ItemSlot.JEWEL &&
                            it.slot != ItemSlot.POTION &&
                            it.slot != ItemSlot.TALISMAN &&
                            it.plus in 9..12
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            WingsCraftCard(
                                title = "Forjar Alas Nivel 1 (Satan / Elf / Heaven)",
                                costText = "1 Chaos + 2,500 Zen · Éxito ${(82 + chaosBonus).coerceAtMost(95)}%",
                                enabled = chaosCount > 0 && hero.zen >= 2500,
                                tag = "craft_wings_s1_button",
                                onClick = {
                                    val (_, msg) = onCraftWingsTier(1)
                                    statusMessage = msg
                                }
                            )
                        }
                        item {
                            WingsCraftCard(
                                title = "Forjar Alas Nivel 2 (Dragon / Soul / Cape of Lord)",
                                costText = "1 Chaos + 1 Bless + 5,000 Zen · Éxito ${(70 + chaosBonus).coerceAtMost(95)}%",
                                enabled = chaosCount > 0 && blessCount > 0 && hero.zen >= 5000,
                                tag = "craft_wings_button",
                                onClick = {
                                    val (_, msg) = onCraftWingsTier(2)
                                    statusMessage = msg
                                }
                            )
                        }
                        item {
                            WingsCraftCard(
                                title = "Forjar Alas Nivel 3 (Wings of Storm · Mítica)",
                                costText = "1 Chaos + 1 Bless + 1 Creation + 12,000 Zen · Éxito ${(55 + chaosBonus).coerceAtMost(95)}%",
                                enabled = chaosCount > 0 && blessCount > 0 && creationCount > 0 && hero.zen >= 12000,
                                tag = "craft_wings_s3_button",
                                onClick = {
                                    val (_, msg) = onCraftWingsTier(3)
                                    statusMessage = msg
                                }
                            )
                        }

                        item {
                            Text("Combinación +10 a +13 (Requiere +9 o superior)", style = MaterialTheme.typography.titleMedium, color = MuSky)
                            Text(
                                text = "Costo por intento: 1 Chaos + 1 Soul + 3,000 Zen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MuTextSecondary
                            )
                        }

                        if (chaosCandidates.isEmpty()) {
                            item {
                                Surface(
                                    color = MuStoneCard,
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, MuBorderSlate)
                                ) {
                                    Text(
                                        text = "Sube primero un arma, armadura o alas a +9 usando Jewel of Bless y Soul para desbloquear la forja +10 a +13.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MuTextSecondary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        } else {
                            items(chaosCandidates, key = { it.id }) { cand ->
                                val rate = (75 - (cand.plus - 9) * 10 + chaosBonus).coerceIn(35, 95)
                                Surface(
                                    color = MuStoneCard,
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, MuPurple.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${cand.name} +${cand.plus} → +${cand.plus + 1}", style = MaterialTheme.typography.bodyMedium, color = MuTextPrimary)
                                            Text("Probabilidad de éxito: $rate%", style = MaterialTheme.typography.bodySmall, color = MuSky)
                                        }
                                        Button(
                                            onClick = {
                                                val (_, msg) = onChaosUpgrade(cand.id)
                                                statusMessage = msg
                                            },
                                            enabled = chaosCount > 0 && soulCount > 0 && hero.zen >= 3000,
                                            colors = ButtonDefaults.buttonColors(containerColor = MuPurple)
                                        ) {
                                            Text("Forjar +${cand.plus + 1}")
                                        }
                                    }
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
private fun WingsCraftCard(
    title: String,
    costText: String,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        color = MuStoneCard,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MuGold.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MuAmberBright)
            Text(costText, style = MaterialTheme.typography.bodySmall, color = MuTextSecondary)
            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = MuGold, contentColor = Color(0xFF1B1202)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(tag)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Combinar en Chaos Goblin")
            }
        }
    }
}

@Composable
private fun PaperdollSlotCard(
    slotKey: EquipSlotKey,
    item: Item?,
    blockedHelm: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onUnequip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = when {
        blockedHelm -> MuCrimson.copy(alpha = 0.55f)
        selected && item != null -> MuAmberBright
        item != null -> Color(item.colorHex).copy(alpha = 0.7f)
        else -> MuBorderSlate
    }
    Surface(
        color = when {
            selected && item != null -> MuGold.copy(alpha = 0.16f)
            blockedHelm -> MuCrimson.copy(alpha = 0.08f)
            else -> MuStoneCard
        },
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .heightIn(min = 68.dp)
            .clickable(enabled = item != null, onClick = onSelect)
            .testTag("paperdoll_slot_${slotKey.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = slotKey.labelEs.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (blockedHelm) MuCrimson else MuTextSecondary
                )
                if (item != null) {
                    Text(
                        text = "Quitar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MuSky,
                        modifier = Modifier.clickable(onClick = onUnequip)
                    )
                }
            }
            if (item != null) {
                Text(
                    text = "${item.name}${if (item.plus > 0) " +${item.plus}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(item.colorHex),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ATK ${item.attack} · DEF ${item.defense} · Dur ${item.durability}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.durability <= 15) MuCrimson else MuTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = if (blockedHelm) "Bloqueado (MG)" else "Vacío",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (blockedHelm) MuCrimson else MuTextSecondary
                )
            }
        }
    }
}
