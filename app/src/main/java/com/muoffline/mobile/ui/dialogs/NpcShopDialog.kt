package com.muoffline.mobile.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Whatshot
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
    npc: LorenciaNpcDef,
    hero: HeroSave,
    onBuyItem: (Item, Int) -> String,
    onOpenChaosMachine: () -> Unit,
    onDismiss: () -> Unit
) {
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val shopPool = remember(npc.id) {
        when (npc.id) {
            "barmaid" -> MuCatalog.SHOP_TEMPLATES.filter { it.slot == ItemSlot.POTION }
            "wizard" -> MuCatalog.SHOP_TEMPLATES.filter { it.classReq == "dw" || it.slot == ItemSlot.POTION }
            "smith" -> MuCatalog.SHOP_TEMPLATES.filter { it.classReq != "dw" || it.slot == ItemSlot.OFFHAND }
            else -> MuCatalog.SHOP_TEMPLATES
        }
    }

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

                if (npc.id == "chaos") {
                    Surface(
                        color = MuStoneCard,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MuPurple)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("¡Hola viajero! Tengo acceso directo a la Chaos Machine.", style = MaterialTheme.typography.bodyMedium, color = MuTextPrimary)
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
