package com.muoffline.mobile.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.muoffline.mobile.R
import com.muoffline.mobile.model.*
import com.muoffline.mobile.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun NpcShopDialog(
    npc: MapNpcDef,
    hero: HeroSave,
    onBuyItem: (Item, Int) -> String,
    onOpenChaosMachine: () -> Unit,
    onOpenVault: () -> Unit,
    onAdvanceEvolution: () -> Pair<Boolean, String>,
    onRedeemArcherWings: () -> Pair<Boolean, String>,
    onDismiss: () -> Unit
) {
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val shopPool = remember(npc.id) {
        when (npc.id) {
            "barmaid" -> MuCatalog.SHOP_TEMPLATES.filter { it.slot == ItemSlot.POTION }
            "wizard" -> MuCatalog.SHOP_TEMPLATES.filter { it.classReq == "dw" || it.slot == ItemSlot.POTION }
            "smith" -> MuCatalog.SHOP_TEMPLATES.filter { it.classReq in setOf("dk", "mg", "dl", "all") && it.slot != ItemSlot.ARMOR }
            "armor-lorencia" -> MuCatalog.SHOP_TEMPLATES.filter { it.slot == ItemSlot.ARMOR || it.slot == ItemSlot.OFFHAND }
            "elf-merchant" -> MuCatalog.SHOP_TEMPLATES.filter { it.classReq == "elf" || it.slot == ItemSlot.POTION }
            "devias-potion" -> MuCatalog.SHOP_TEMPLATES
            "chaos", "vault-lorencia", "marlon", "golden-archer" -> emptyList()
            else -> MuCatalog.SHOP_TEMPLATES
        }
    }

    val talismanQty = hero.inventory.filter { it.slot == ItemSlot.TALISMAN }.sumOf { it.quantity }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MuAbyssSlate,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MuGold.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 580.dp)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(npc.name, style = MaterialTheme.typography.headlineSmall, color = MuAmberBright)
                        Text("${npc.subtitle} · Tu Zen: ${hero.zen}", style = MaterialTheme.typography.labelMedium, color = MuTextSecondary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_npc_dialog")) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(id = R.string.cd_close))
                    }
                }

                statusMessage?.let { msg ->
                    Surface(
                        color = MuStoneCard,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MuEmerald.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MuEmerald,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }

                when (npc.role) {
                    "chaos" -> {
                        Surface(
                            color = MuStoneCard,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MuPurple)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("¡Hola viajero! Aquí en Noria puedes forjar Alas S1, S2, S3 y subir equipo a +13.", style = MaterialTheme.typography.bodyMedium, color = MuTextPrimary)
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenChaosMachine()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MuPurple, contentColor = Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("npc_open_chaos")
                                ) {
                                    Icon(Icons.Rounded.Whatshot, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Abrir Chaos Machine")
                                }
                            }
                        }
                    }

                    "vault" -> {
                        Surface(
                            color = MuStoneCard,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MuSky)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Guarda joyas, alas y equipo en tu Baúl compartido entre tus 5 personajes.", style = MaterialTheme.typography.bodyMedium, color = MuTextPrimary)
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenVault()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MuGold, contentColor = Color(0xFF1B1202)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("npc_open_vault")
                                ) {
                                    Icon(Icons.Rounded.Inventory2, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Abrir Baúl de Almacén")
                                }
                            }
                        }
                    }

                    "quest" -> {
                        val meta = MuCatalog.CLASS_CATALOG.getValue(hero.classId)
                        Surface(
                            color = MuStoneCard,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MuPurple)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Marlon supervisa las evoluciones de Nivel 150 (${meta.evo150Title}) y Nivel 400 (${meta.evo400Title}).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MuTextPrimary
                                )
                                Button(
                                    onClick = {
                                        val (_, msg) = onAdvanceEvolution()
                                        statusMessage = msg
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MuPurple, contentColor = Color.White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("npc_advance_evolution")
                                ) {
                                    Icon(Icons.Rounded.MilitaryTech, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Completar Quest de Evolución")
                                }
                            }
                        }
                    }

                    "archer" -> {
                        Surface(
                            color = MuStoneCard,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MuGold)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Entrega 3 Talisman / Rena + 3,000 Zen para recibir Alas S2 exclusivas de evento. Tienes: $talismanQty Talismanes.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MuTextPrimary
                                )
                                Button(
                                    onClick = {
                                        val (_, msg) = onRedeemArcherWings()
                                        statusMessage = msg
                                    },
                                    enabled = talismanQty >= 3 && hero.zen >= 3000,
                                    colors = ButtonDefaults.buttonColors(containerColor = MuGold, contentColor = Color(0xFF1B1202)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("npc_redeem_archer")
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Canjear 3 Talismanes por Alas S2")
                                }
                            }
                        }
                    }
                }

                if (shopPool.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(shopPool, key = { it.templateId }) { tpl ->
                            val costZen = if (tpl.slot == ItemSlot.POTION) tpl.valueZen * 5 else (tpl.valueZen * 1.4).roundToInt()
                            Surface(
                                color = MuStoneCard,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, Color(tpl.colorHex).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (tpl.slot == ItemSlot.POTION) "${tpl.name} (x5)" else "${tpl.name} +${tpl.plus}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(tpl.colorHex)
                                        )
                                        Text(
                                            text = "${tpl.slot.labelEs} · Precio: $costZen Zen",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MuTextSecondary
                                        )
                                    }
                                    Button(
                                        onClick = { statusMessage = onBuyItem(tpl, costZen) },
                                        enabled = hero.zen >= costZen,
                                        colors = ButtonDefaults.buttonColors(containerColor = MuGold, contentColor = Color(0xFF1B1202))
                                    ) {
                                        Text("Comprar")
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
