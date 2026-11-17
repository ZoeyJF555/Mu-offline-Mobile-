package com.muoffline.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muoffline.mobile.R
import com.muoffline.mobile.model.*
import com.muoffline.mobile.ui.theme.*

@Composable
fun TitleScreen(
    accountId: String,
    heroes: List<HeroSave>,
    activeModsCount: Int,
    lanConnectedRoom: String?,
    onSelectHero: (HeroSave) -> Unit,
    onCreateHero: (String, ClassId) -> Unit,
    onDeleteHero: (String) -> Unit,
    onOpenMods: () -> Unit,
    onOpenLan: () -> Unit,
    onOpenCredits: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedClass by remember { mutableStateOf(ClassId.DK) }
    var heroName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<HeroSave?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0D14), Color(0xFF131928), Color(0xFF0B0D13))
                )
            )
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 980.dp)
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar
            Surface(
                color = MuAbyssSlate,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MuBorderSlate)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_mu_crest),
                                contentDescription = stringResource(id = R.string.cd_crest),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                            )
                            Column {
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MuAmberBright
                                )
                                Text(
                                    text = "Cuenta: $accountId · Guardado SQLite Local",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MuTextSecondary
                                )
                            }
                        }
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = "Cerrar sesión",
                                tint = MuCrimson
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenMods,
                            border = BorderStroke(1.dp, MuGold.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("title_mods_button")
                        ) {
                            Icon(Icons.Rounded.Extension, contentDescription = null, modifier = Modifier.size(18.dp), tint = MuGold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Mods ($activeModsCount)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        OutlinedButton(
                            onClick = onOpenLan,
                            border = BorderStroke(1.dp, (if (lanConnectedRoom != null) MuEmerald else MuSky).copy(alpha = 0.55f)),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("title_lan_button")
                        ) {
                            Icon(
                                Icons.Rounded.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (lanConnectedRoom != null) MuEmerald else MuSky
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = lanConnectedRoom ?: "LAN Wi-Fi",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenCredits,
                            border = BorderStroke(1.dp, MuBorderSlate),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("title_credits_button")
                        ) {
                            Icon(Icons.Rounded.Info, contentDescription = "Créditos", tint = MuTextSecondary)
                        }
                    }
                }
            }

            // Saved Heroes Section
            Text(
                text = stringResource(id = R.string.title_choose_hero),
                style = MaterialTheme.typography.headlineSmall,
                color = MuTextPrimary
            )

            if (heroes.isEmpty()) {
                Surface(
                    color = MuAbyssSlate,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MuBorderSlate),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Aún no tienes héroes en esta cuenta. Elige una clase abajo para comenzar en Lorencia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MuTextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    heroes.forEach { hero ->
                        val classMeta = MuCatalog.CLASS_CATALOG.getValue(hero.classId)
                        val mapMeta = MuCatalog.MAP_CATALOG.getValue(hero.mapId)
                        Surface(
                            color = MuStoneCard,
                            shape = MaterialTheme.shapes.large,
                            border = BorderStroke(1.dp, Color(classMeta.accentColor).copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = classMeta.portraitRes),
                                    contentDescription = hero.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = hero.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MuTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${classMeta.name} · Nv ${hero.level} · RR ${hero.resets}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(classMeta.accentColor)
                                    )
                                    Text(
                                        text = "${mapMeta.name} · ${hero.zen} Zen · ${hero.kills} bajas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MuTextSecondary
                                    )
                                }
                                IconButton(
                                    onClick = { pendingDelete = hero },
                                    modifier = Modifier.testTag("delete_hero_${hero.id}")
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar héroe", tint = MuCrimson)
                                }
                                Button(
                                    onClick = { onSelectHero(hero) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MuGold,
                                        contentColor = Color(0xFF1B1202)
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("enter_hero_${hero.id}")
                                ) {
                                    Text(stringResource(id = R.string.action_enter_world))
                                }
                            }
                        }
                    }
                }
            }

            // Create New Hero Section
            Surface(
                color = MuAbyssSlate,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MuBorderSlate),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Forjar Nuevo Héroe",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MuAmberBright
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isWide = maxWidth >= 620.dp
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ClassId.entries.forEach { cid ->
                                    ClassCard(
                                        meta = MuCatalog.CLASS_CATALOG.getValue(cid),
                                        selected = selectedClass == cid,
                                        onClick = { selectedClass = cid },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                ClassId.entries.forEach { cid ->
                                    ClassCard(
                                        meta = MuCatalog.CLASS_CATALOG.getValue(cid),
                                        selected = selectedClass == cid,
                                        onClick = { selectedClass = cid },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = heroName,
                            onValueChange = { if (it.length <= 16) heroName = it },
                            label = { Text("Nombre del Héroe") },
                            placeholder = { Text(MuCatalog.CLASS_CATALOG.getValue(selectedClass).name) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_name_input")
                        )
                        Button(
                            onClick = {
                                val chosen = heroName.trim().ifEmpty {
                                    MuCatalog.CLASS_CATALOG.getValue(selectedClass).name
                                }
                                onCreateHero(chosen, selectedClass)
                                heroName = ""
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MuGold,
                                contentColor = Color(0xFF1B1202)
                            ),
                            modifier = Modifier
                                .heightIn(min = 52.dp)
                                .testTag("create_hero_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(id = R.string.action_start_adventure))
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { heroToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar a ${heroToDelete.name}") },
            text = { Text("Esta acción borrará permanentemente su equipo, joyas y resets locales.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteHero(heroToDelete.id)
                        pendingDelete = null
                    }
                ) {
                    Text("Eliminar", color = MuCrimson)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ClassCard(
    meta: ClassMeta,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(meta.accentColor)
    Surface(
        color = if (selected) accent.copy(alpha = 0.16f) else MuStoneCard,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else MuBorderSlate),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("class_card_${meta.id.wire}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = meta.portraitRes),
                contentDescription = meta.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(meta.name, style = MaterialTheme.typography.titleMedium, color = MuTextPrimary)
                Text(meta.title, style = MaterialTheme.typography.labelMedium, color = accent)
                Text(meta.role, style = MaterialTheme.typography.bodySmall, color = MuTextSecondary)
                Text(
                    text = "STR ${meta.baseStats.str} · AGI ${meta.baseStats.agi} · VIT ${meta.baseStats.vit} · ENE ${meta.baseStats.ene}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MuTextSecondary
                )
            }
        }
    }
}
