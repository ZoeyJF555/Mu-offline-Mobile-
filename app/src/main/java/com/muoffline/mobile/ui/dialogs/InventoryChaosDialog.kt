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
    onEquipItem: (Item) -> Unit,
    onUnequipSlot: (ItemSlot) -> Unit,
    onUpgradeWithJewel: (String, JewelType) -> String,
    onChaosUpgrade: (String) -> Pair<Boolean, String>,
    onCraftWings: () -> Pair<Boolean, String>,
    onSellItem: (String) -> String,
    onDismiss: () -> Unit
) {
    var tabChaos by remember { mutableStateOf(initialTabChaos) }
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val allEquipmentSlots = listOf(
        ItemSlot.WEAPON, ItemSlot.OFFHAND, ItemSlot.ARMOR,
        ItemSlot.HELM, ItemSlot.WINGS, ItemSlot.PENDANT
    )
    val allItems = hero.equipment.allEquipped() + hero.inventory
    val selectedItem = allItems.firstOrNull { it.id == selectedItemId }
    val selectedIsEquipped = selectedItem != null && hero.equipment.allEquipped().any { it.id == selectedItem.id }

    val blessCount = hero.inventory.filter { it.jewelType == JewelType.BLESS }.sumOf { it.quantity }
    val soulCount = hero.inventory.filter { it.jewelType == JewelType.SOUL }.sumOf { it.quantity }
    val chaosCount = hero.inventory.filter { it.jewelType == JewelType.CHAOS }.sumOf { it.quantity }

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
                .widthIn(max = 760.dp)
                .fillMaxHeight(0.88f)
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
                    Column {
                        Text("Mochila y Chaos Machine", style = MaterialTheme.typography.headlineSmall, color = MuAmberBright)
                        Text(
                            text = "${hero.zen} Zen · Bless: $blessCount · Soul: $soulCount · Chaos: $chaosCount",
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
                        Text("Equipo y Mochila (${hero.inventory.size}/24)")
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
                            Text("Equipo Activo", style = MaterialTheme.typography.titleMedium, color = MuAmberBright)
                        }
                        items(allEquipmentSlots) { slot ->
                            val eq = hero.equipment.getSlot(slot)
                            Surface(
                                color = if (eq != null && selectedItemId == eq.id) MuGold.copy(alpha = 0.14f) else MuStoneCard,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, if (eq != null) Color(eq.colorHex).copy(alpha = 0.55f) else MuBorderSlate),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = eq != null) { selectedItemId = eq?.id }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(slot.labelEs, style = MaterialTheme.typography.labelMedium, color = MuTextSecondary, modifier = Modifier.width(86.dp))
                                    if (eq != null) {
                                        Text(
                                            text = "${eq.name} +${eq.plus}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(eq.colorHex),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        TextButton(onClick = { onUnequipSlot(slot) }) {
                                            Text("Quitar")
                                        }
                                    } else {
                                        Text("Vacío", style = MaterialTheme.typography.bodySmall, color = MuTextSecondary, modifier = Modifier.weight(1f))
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
                                        Text(
                                            text = "${item.slot.labelEs} · ${item.rarity.name} · Valor ${item.valueZen} Zen",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MuTextSecondary
                                        )
                                    }
                                    if (item.slot != ItemSlot.JEWEL && item.slot != ItemSlot.POTION) {
                                        OutlinedButton(
                                            onClick = { onEquipItem(item) },
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
                                    text = "${selectedItem.name} +${selectedItem.plus} (ATK ${selectedItem.attack} · MAG ${selectedItem.magic} · DEF ${selectedItem.defense} · HP +${selectedItem.hpBonus})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(selectedItem.colorHex)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (selectedItem.slot != ItemSlot.JEWEL && selectedItem.slot != ItemSlot.POTION) {
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
                        it.slot != ItemSlot.JEWEL && it.slot != ItemSlot.POTION && it.plus in 9..12
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Surface(
                                color = MuStoneCard,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MuGold.copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Forjar Alas Nivel 2 (Wings)", style = MaterialTheme.typography.titleMedium, color = MuAmberBright)
                                    Text(
                                        text = "Costo: 1 Jewel of Chaos + 1 Jewel of Bless + 5,000 Zen · Tasa éxito: ${(70 + chaosBonus).coerceAtMost(95)}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MuTextSecondary
                                    )
                                    Button(
                                        onClick = {
                                            val (_, msg) = onCraftWings()
                                            statusMessage = msg
                                        },
                                        enabled = chaosCount > 0 && blessCount > 0 && hero.zen >= 5000,
                                        colors = ButtonDefaults.buttonColors(containerColor = MuGold, contentColor = Color(0xFF1B1202)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("craft_wings_button")
                                    ) {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Combinar Alas en Chaos Goblin")
                                    }
                                }
                            }
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
