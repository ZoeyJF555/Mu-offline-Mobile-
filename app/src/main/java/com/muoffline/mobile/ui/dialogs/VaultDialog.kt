package com.muoffline.mobile.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
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
import com.muoffline.mobile.model.HeroSave
import com.muoffline.mobile.model.Item
import com.muoffline.mobile.ui.theme.*

@Composable
fun VaultDialog(
    hero: HeroSave,
    vaultItems: List<Item>,
    onDeposit: (Item) -> String,
    onWithdraw: (Item) -> String,
    onDismiss: () -> Unit
) {
    var statusMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MuAbyssSlate,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MuGold.copy(alpha = 0.6f)),
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = MuAmberBright)
                        Column {
                            Text("Baúl de Almacén Compartido", style = MaterialTheme.typography.headlineSmall, color = MuAmberBright)
                            Text(
                                text = "Compartido entre tus 5 slots de personaje (${vaultItems.size}/40)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MuTextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_vault_dialog")) {
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

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("En el Baúl (${vaultItems.size}/40)", style = MaterialTheme.typography.titleMedium, color = MuSky)
                    }
                    if (vaultItems.isEmpty()) {
                        item {
                            Text("El baúl está vacío. Deposita ítems o joyas de tu mochila abajo.", style = MaterialTheme.typography.bodySmall, color = MuTextSecondary)
                        }
                    } else {
                        items(vaultItems, key = { "vault_${it.id}" }) { item ->
                            VaultItemRow(
                                item = item,
                                actionLabel = "Retirar",
                                buttonColor = MuSky,
                                onAction = { statusMessage = onWithdraw(item) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tu Mochila (${hero.inventory.size}/24)", style = MaterialTheme.typography.titleMedium, color = MuAmberBright)
                    }
                    if (hero.inventory.isEmpty()) {
                        item {
                            Text("Tu mochila no tiene ítems para depositar.", style = MaterialTheme.typography.bodySmall, color = MuTextSecondary)
                        }
                    } else {
                        items(hero.inventory, key = { "bag_${it.id}" }) { item ->
                            VaultItemRow(
                                item = item,
                                actionLabel = "Guardar",
                                buttonColor = MuGold,
                                onAction = { statusMessage = onDeposit(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultItemRow(
    item: Item,
    actionLabel: String,
    buttonColor: Color,
    onAction: () -> Unit
) {
    Surface(
        color = MuStoneCard,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, Color(item.colorHex).copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
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
                    text = "${item.slot.labelEs} · ${item.rarity.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MuTextSecondary
                )
            }
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = Color(0xFF0A141D))
            ) {
                Text(actionLabel)
            }
        }
    }
}
