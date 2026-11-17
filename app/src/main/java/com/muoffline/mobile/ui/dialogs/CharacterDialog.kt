package com.muoffline.mobile.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun CharacterDialog(
    hero: HeroSave,
    resetLevel: Int,
    onAddStat: (StatKind, Int) -> Unit,
    onMasterReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val derived = MuCatalog.computeDerivedHeroStats(hero)
    val classMeta = MuCatalog.CLASS_CATALOG.getValue(hero.classId)
    val canReset = hero.level >= resetLevel

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
                .widthIn(max = 560.dp)
                .heightIn(max = 620.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${hero.name} · ${classMeta.name}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MuAmberBright
                        )
                        Text(
                            text = "Nivel ${hero.level} · Resets ${hero.resets} · ${hero.kills} bajas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_character_dialog")) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(id = R.string.cd_close))
                    }
                }

                // Derived stats grid
                Surface(
                    color = MuStoneCard,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, MuBorderSlate)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatPill("Poder Ataque", "${derived.attackPower}", MuAmberBright)
                            StatPill("Defensa", "${derived.defense}", MuSky)
                            StatPill("Crítico", "${(derived.critChance * 100).roundToInt()}%", MuEmerald)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatPill("HP Máx", "${derived.maxHp}", MuCrimson)
                            StatPill("MP Máx", "${derived.maxMp}", MuSky)
                            StatPill("Velocidad", "${derived.moveSpeed.roundToInt()}", MuTextPrimary)
                        }
                    }
                }

                Surface(
                    color = MuObsidian,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, if (hero.statPoints > 0) MuGold else MuBorderSlate)
                ) {
                    Text(
                        text = "Puntos disponibles para asignar: ${hero.statPoints}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hero.statPoints > 0) MuAmberBright else MuTextSecondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    )
                }

                StatRow("Fuerza (STR)", "Daño físico y espadas", hero.stats.str, hero.statPoints, StatKind.STR, onAddStat)
                StatRow("Agilidad (AGI)", "Defensa, velocidad y crítico", hero.stats.agi, hero.statPoints, StatKind.AGI, onAddStat)
                StatRow("Vitalidad (VIT)", "HP máximo y resistencia", hero.stats.vit, hero.statPoints, StatKind.VIT, onAddStat)
                StatRow("Energía (ENE)", "Daño mágico y MP máximo", hero.stats.ene, hero.statPoints, StatKind.ENE, onAddStat)

                HorizontalDivider(color = MuBorderSlate)

                Button(
                    onClick = onMasterReset,
                    enabled = canReset,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MuPurple,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("master_reset_button")
                ) {
                    Icon(Icons.Rounded.AutoFixHigh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (canReset) "Hacer Master Reset (+150 pts base por reset)"
                        else "Master Reset disponible en Nivel $resetLevel"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MuTextSecondary)
        Text(value, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun StatRow(
    title: String,
    subtitle: String,
    value: Int,
    availablePoints: Int,
    kind: StatKind,
    onAddStat: (StatKind, Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("$title: $value", style = MaterialTheme.typography.titleMedium, color = MuTextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MuTextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { onAddStat(kind, 1) },
                enabled = availablePoints >= 1,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .testTag("add_stat_${kind.name.lowercase()}_1")
            ) {
                Text("+1")
            }
            Button(
                onClick = { onAddStat(kind, 10) },
                enabled = availablePoints >= 10,
                colors = ButtonDefaults.buttonColors(containerColor = MuGold, contentColor = Color(0xFF1B1202)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .testTag("add_stat_${kind.name.lowercase()}_10")
            ) {
                Text("+10")
            }
        }
    }
}
