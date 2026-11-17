package com.muoffline.mobile.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.muoffline.mobile.R
import com.muoffline.mobile.model.*
import com.muoffline.mobile.ui.theme.*

@Composable
fun TradeDialog(
    hero: HeroSave,
    session: TradeSession,
    onUpdateOffer: (List<Item>, Int) -> Unit,
    onConfirmTrade: () -> Unit,
    onCancelTrade: () -> Unit
) {
    var zenInput by remember(session.tradeId) { mutableStateOf(session.myZen.toString()) }
    val selectedIds = remember(session.myItems) { session.myItems.map { it.id }.toSet() }

    Dialog(
        onDismissRequest = onCancelTrade,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MuAbyssSlate,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MuSky.copy(alpha = 0.65f)),
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
                        Text("Intercambio P2P con ${session.partnerName}", style = MaterialTheme.typography.headlineSmall, color = MuSky)
                        Text("Tu Zen disponible: ${hero.zen}", style = MaterialTheme.typography.bodySmall, color = MuTextSecondary)
                    }
                    IconButton(onClick = onCancelTrade, modifier = Modifier.testTag("close_trade_dialog")) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(id = R.string.cd_close))
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left column: My offer + selectable inventory
                    Surface(
                        color = MuStoneCard,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, if (session.myConfirmed) MuEmerald else MuBorderSlate),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Tu Oferta", style = MaterialTheme.typography.titleMedium, color = MuAmberBright)
                                if (session.myConfirmed) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Confirmado", tint = MuEmerald)
                                }
                            }
                            OutlinedTextField(
                                value = zenInput,
                                onValueChange = { raw ->
                                    val digits = raw.filter { it.isDigit() }
                                    zenInput = digits
                                    val clamped = (digits.toIntOrNull() ?: 0).coerceIn(0, hero.zen)
                                    onUpdateOffer(session.myItems, clamped)
                                },
                                label = { Text("Zen a ofrecer") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Toca ítems de tu mochila:", style = MaterialTheme.typography.labelSmall, color = MuTextSecondary)
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(hero.inventory, key = { it.id }) { item ->
                                    val offered = item.id in selectedIds
                                    Surface(
                                        color = if (offered) MuGold.copy(alpha = 0.22f) else MuObsidian,
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, if (offered) MuGold else MuBorderSlate),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val nextItems = if (offered) {
                                                    session.myItems.filterNot { it.id == item.id }
                                                } else {
                                                    session.myItems + item
                                                }
                                                val zenVal = (zenInput.toIntOrNull() ?: 0).coerceIn(0, hero.zen)
                                                onUpdateOffer(nextItems, zenVal)
                                            }
                                    ) {
                                        Text(
                                            text = "${if (offered) "✓ " else ""}${item.name}${if (item.plus > 0) " +${item.plus}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(item.colorHex),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Right column: Partner offer
                    Surface(
                        color = MuStoneCard,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, if (session.partnerConfirmed) MuEmerald else MuBorderSlate),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Oferta de ${session.partnerName}", style = MaterialTheme.typography.titleMedium, color = MuSky)
                                if (session.partnerConfirmed) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Confirmado", tint = MuEmerald)
                                }
                            }
                            Text("Zen ofrecido: ${session.partnerZen}", style = MaterialTheme.typography.labelMedium, color = MuAmberBright)
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(session.partnerItems, key = { it.id }) { item ->
                                    Surface(
                                        color = MuObsidian,
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, Color(item.colorHex).copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "${item.name}${if (item.plus > 0) " +${item.plus}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(item.colorHex),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onCancelTrade,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("cancel_trade_button")
                    ) {
                        Text("Cancelar", color = MuCrimson)
                    }
                    Button(
                        onClick = onConfirmTrade,
                        enabled = !session.myConfirmed,
                        colors = ButtonDefaults.buttonColors(containerColor = MuEmerald, contentColor = Color(0xFF022016)),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("confirm_trade_button")
                    ) {
                        Text(if (session.myConfirmed) "Esperando al otro jugador..." else "Confirmar Intercambio")
                    }
                }
            }
        }
    }
}
