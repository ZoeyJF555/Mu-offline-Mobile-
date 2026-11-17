package com.muoffline.mobile.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
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
fun ModsDialog(
    mods: List<ModPackage>,
    activeRates: ActiveModRates,
    onToggleMod: (String) -> Unit,
    onAddMod: (ModPackage) -> Unit,
    onDeleteMod: (String) -> Unit,
    onExportModJson: (ModPackage) -> String,
    onImportModJson: (String) -> Result<ModPackage>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showCreateForm by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    // Create custom mod form fields
    var modName by remember { mutableStateOf("Mi Mod MU") }
    var expText by remember { mutableStateOf("4.0") }
    var zenText by remember { mutableStateOf("4.0") }
    var dropText by remember { mutableStateOf("1.5") }
    var chaosBonusText by remember { mutableStateOf("10") }
    var bossName by remember { mutableStateOf("Kundun Supremo") }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Archivo vacío")
            }.onSuccess { raw ->
                val res = onImportModJson(raw)
                feedback = res.fold(
                    onSuccess = { "Mod importado correctamente: ${it.name}" },
                    onFailure = { "Error al importar JSON: ${it.message}" }
                )
            }.onFailure { err ->
                feedback = "No se pudo leer el archivo: ${err.message}"
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val json = pendingExportJson
        pendingExportJson = null
        if (uri != null && json != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
            }.onSuccess {
                feedback = "Mod exportado correctamente en formato .mu-mod.json."
            }.onFailure { err ->
                feedback = "No se pudo guardar el archivo: ${err.message}"
            }
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
                .fillMaxWidth(0.94f)
                .widthIn(max = 720.dp)
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
                        Text("Gestor de Mods Offline", style = MaterialTheme.typography.headlineSmall, color = MuAmberBright)
                        Text(
                            text = "Rates Activos: EXP x${(activeRates.expMul * 10).roundToInt() / 10.0} · Zen x${(activeRates.zenMul * 10).roundToInt() / 10.0} · Chaos +${activeRates.chaosBonus}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MuEmerald
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_mods_dialog")) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(id = R.string.cd_close))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = MuSky, contentColor = Color(0xFF041C2C)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_mod_button")
                    ) {
                        Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Importar .json")
                    }
                    Button(
                        onClick = { showCreateForm = !showCreateForm },
                        colors = ButtonDefaults.buttonColors(containerColor = MuGold, contentColor = Color(0xFF1B1202)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("toggle_create_mod_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (showCreateForm) "Ver Lista" else "Crear Mod")
                    }
                }

                feedback?.let { msg ->
                    Surface(
                        color = MuStoneCard,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MuSky.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MuSky,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }

                if (showCreateForm) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = modName,
                                onValueChange = { modName = it },
                                label = { Text("Nombre del Mod") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = expText,
                                    onValueChange = { expText = it },
                                    label = { Text("EXP x") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = zenText,
                                    onValueChange = { zenText = it },
                                    label = { Text("Zen x") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = dropText,
                                    onValueChange = { dropText = it },
                                    label = { Text("Drop x") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = chaosBonusText,
                                    onValueChange = { chaosBonusText = it },
                                    label = { Text("Chaos Bonus %") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = bossName,
                                    onValueChange = { bossName = it },
                                    label = { Text("Boss en Arena Mod") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.6f)
                                )
                            }
                        }
                        item {
                            Button(
                                onClick = {
                                    val newMod = ModPackage(
                                        id = MuCatalog.newUid("mod"),
                                        name = modName.trim().ifEmpty { "Mod Personalizado" },
                                        author = "Local Creator",
                                        version = "1.0.0",
                                        description = "Mod creado en el dispositivo con jefe personalizado en Arena Mod.",
                                        enabled = true,
                                        builtIn = false,
                                        expMultiplier = (expText.toDoubleOrNull() ?: 2.0).coerceIn(0.1, 100.0),
                                        zenMultiplier = (zenText.toDoubleOrNull() ?: 2.0).coerceIn(0.1, 100.0),
                                        dropRateMultiplier = (dropText.toDoubleOrNull() ?: 1.2).coerceIn(0.2, 10.0),
                                        chaosSuccessBonus = (chaosBonusText.toIntOrNull() ?: 5).coerceIn(-20, 40),
                                        resetLevel = 200,
                                        customMonsters = listOf(
                                            ModMonsterDef(
                                                id = MuCatalog.newUid("mboss"),
                                                name = bossName.trim().ifEmpty { "Boss Mod" },
                                                mapId = MapId.CUSTOM,
                                                level = 55,
                                                maxHp = 3400,
                                                attack = 95,
                                                defense = 32,
                                                expReward = 2200,
                                                zenReward = 9500,
                                                colorHex = 0xFFF43F5E,
                                                isBoss = true
                                            )
                                        )
                                    )
                                    onAddMod(newMod)
                                    showCreateForm = false
                                    feedback = "Mod '${newMod.name}' creado y activado."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MuEmerald, contentColor = Color(0xFF022016)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("save_custom_mod_button")
                            ) {
                                Text("Guardar y Activar Mod")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mods, key = { it.id }) { mod ->
                            Surface(
                                color = MuStoneCard,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, if (mod.enabled) MuEmerald.copy(alpha = 0.6f) else MuBorderSlate)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(mod.name, style = MaterialTheme.typography.titleMedium, color = MuTextPrimary)
                                            Text("v${mod.version} · ${mod.author}", style = MaterialTheme.typography.labelSmall, color = MuTextSecondary)
                                        }
                                        Switch(
                                            checked = mod.enabled,
                                            onCheckedChange = { onToggleMod(mod.id) }
                                        )
                                    }
                                    Text(mod.description, style = MaterialTheme.typography.bodySmall, color = MuTextSecondary)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "EXP x${mod.expMultiplier} · Zen x${mod.zenMultiplier} · Drop x${mod.dropRateMultiplier}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MuAmberBright
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    pendingExportJson = onExportModJson(mod)
                                                    val safeName = mod.name.lowercase().replace(Regex("[^a-z0-9]+"), "-")
                                                    exportLauncher.launch("$safeName.mu-mod.json")
                                                }
                                            ) {
                                                Icon(Icons.Rounded.FileDownload, contentDescription = "Exportar Mod JSON", tint = MuSky)
                                            }
                                            if (!mod.builtIn) {
                                                IconButton(onClick = { onDeleteMod(mod.id) }) {
                                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar Mod", tint = MuCrimson)
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
    }
}
