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

@Composable
fun AdminDialog(
    mapCatalog: Map<MapId, MapMeta>,
    manifestInfo: ManifestInfo,
    initialManifestUrl: String,
    syncStatusMessage: String?,
    onUpdateMapRates: (MapId, Double, Double, Double) -> Unit,
    onLoadMapsRawJson: suspend () -> String,
    onSaveMapsRawJson: (String, (Result<String>) -> Unit) -> Unit,
    onLoadNpcsRawJson: suspend () -> String,
    onSaveNpcsRawJson: (String, (Result<String>) -> Unit) -> Unit,
    onSyncManifestUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }

    var editingMapId by remember { mutableStateOf(MapId.LORENCIA) }
    val currentMeta = mapCatalog[editingMapId] ?: MuCatalog.DEFAULT_MAP_CATALOG.getValue(editingMapId)
    var expText by remember(editingMapId, currentMeta) { mutableStateOf(currentMeta.expRate.toString()) }
    var zenText by remember(editingMapId, currentMeta) { mutableStateOf(currentMeta.dropZenRate.toString()) }
    var dropText by remember(editingMapId, currentMeta) { mutableStateOf(currentMeta.dropItemRate.toString()) }

    var mapsRaw by remember { mutableStateOf("") }
    var npcsRaw by remember { mutableStateOf("") }
    var rawTargetIsNpcs by remember { mutableStateOf(false) }
    var pendingExportText by remember { mutableStateOf<String?>(null) }

    var manifestUrl by remember(initialManifestUrl) { mutableStateOf(initialManifestUrl) }

    LaunchedEffect(selectedTab, rawTargetIsNpcs) {
        if (selectedTab == 1) {
            if (rawTargetIsNpcs && npcsRaw.isBlank()) {
                npcsRaw = onLoadNpcsRawJson()
            } else if (!rawTargetIsNpcs && mapsRaw.isBlank()) {
                mapsRaw = onLoadMapsRawJson()
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val payload = pendingExportText
        pendingExportText = null
        if (uri != null && payload != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
            }.onSuccess {
                feedback = "Archivo JSON exportado correctamente."
            }.onFailure { err ->
                feedback = "Error al exportar JSON: ${err.message}"
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
            border = BorderStroke(1.dp, MuPurple.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 760.dp)
                .fillMaxHeight(0.9f)
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
                        Text("Panel Admin · JSON Plano", style = MaterialTheme.typography.headlineSmall, color = MuPurple)
                        Text(
                            text = "Versión Local: v${manifestInfo.version} (${manifestInfo.updatedAt}) · Sin SQL",
                            style = MaterialTheme.typography.bodySmall,
                            color = MuTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_admin_dialog")) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(id = R.string.cd_close))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    AdminTabButton("Rates Mapas", selectedTab == 0, Modifier.weight(1f)) { selectedTab = 0 }
                    AdminTabButton("Editor JSON", selectedTab == 1, Modifier.weight(1f)) { selectedTab = 1 }
                    AdminTabButton("Sync Nube", selectedTab == 2, Modifier.weight(1f)) { selectedTab = 2 }
                }

                (feedback ?: syncStatusMessage)?.let { msg ->
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

                when (selectedTab) {
                    0 -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Surface(
                                    color = MuStoneCard,
                                    shape = MaterialTheme.shapes.medium,
                                    border = BorderStroke(1.dp, MuGold.copy(alpha = 0.55f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Editando: ${currentMeta.name} (Nv.${currentMeta.minLevel}+)",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MuAmberBright
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = expText,
                                                onValueChange = { expText = it },
                                                label = { Text("EXP Rate") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = zenText,
                                                onValueChange = { zenText = it },
                                                label = { Text("Zen Rate") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = dropText,
                                                onValueChange = { dropText = it },
                                                label = { Text("Drop (0..1)") },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                val exp = expText.toDoubleOrNull() ?: currentMeta.expRate
                                                val zen = zenText.toDoubleOrNull() ?: currentMeta.dropZenRate
                                                val drop = dropText.toDoubleOrNull() ?: currentMeta.dropItemRate
                                                onUpdateMapRates(editingMapId, exp, zen, drop)
                                                feedback = "Rates de ${currentMeta.name} guardados en mapas.json."
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MuGold, contentColor = Color(0xFF1B1202)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp)
                                                .testTag("admin_save_map_rates")
                                        ) {
                                            Icon(Icons.Rounded.Save, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Guardar Rates en mapas.json")
                                        }
                                    }
                                }
                            }

                            items(MapId.entries, key = { it.wire }) { mid ->
                                val m = mapCatalog[mid] ?: MuCatalog.DEFAULT_MAP_CATALOG.getValue(mid)
                                val isSel = mid == editingMapId
                                Surface(
                                    color = if (isSel) MuPurple.copy(alpha = 0.2f) else MuStoneCard,
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, if (isSel) MuPurple else MuBorderSlate),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${m.name} (Nv.${m.minLevel}+)", style = MaterialTheme.typography.titleMedium, color = MuTextPrimary)
                                            Text(
                                                text = "EXP ${m.expRate} · Zen ${m.dropZenRate} · Drop ${(m.dropItemRate * 100).toInt()}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MuTextSecondary
                                            )
                                        }
                                        OutlinedButton(onClick = { editingMapId = mid }) {
                                            Text(if (isSel) "Editando" else "Editar")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !rawTargetIsNpcs,
                                onClick = { rawTargetIsNpcs = false },
                                label = { Text("mapas.json") }
                            )
                            FilterChip(
                                selected = rawTargetIsNpcs,
                                onClick = { rawTargetIsNpcs = true },
                                label = { Text("npcs.json") }
                            )
                        }

                        val activeRaw = if (rawTargetIsNpcs) npcsRaw else mapsRaw
                        OutlinedTextField(
                            value = activeRaw,
                            onValueChange = { updated ->
                                if (rawTargetIsNpcs) npcsRaw = updated else mapsRaw = updated
                            },
                            label = { Text(if (rawTargetIsNpcs) "Contenido npcs.json" else "Contenido mapas.json") },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            maxLines = 24
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    if (rawTargetIsNpcs) {
                                        onSaveNpcsRawJson(npcsRaw) { res ->
                                            feedback = res.fold(onSuccess = { it }, onFailure = { "JSON inválido: ${it.message}" })
                                        }
                                    } else {
                                        onSaveMapsRawJson(mapsRaw) { res ->
                                            feedback = res.fold(onSuccess = { it }, onFailure = { "JSON inválido: ${it.message}" })
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MuEmerald, contentColor = Color(0xFF022016)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Aplicar JSON")
                            }
                            OutlinedButton(
                                onClick = {
                                    pendingExportText = activeRaw
                                    exportLauncher.launch(if (rawTargetIsNpcs) "npcs.json" else "mapas.json")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null, tint = MuAmberBright)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exportar .json", color = MuAmberBright)
                            }
                        }
                    }

                    else -> {
                        Surface(
                            color = MuStoneCard,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MuSky.copy(alpha = 0.55f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Sincronización Remota de Parches JSON", style = MaterialTheme.typography.titleMedium, color = MuSky)
                                Text(
                                    text = "Por defecto el juego corre 100% offline. Si tienes internet, pega la URL RAW de tu manifest.json (GitHub Pages o GitHub Raw) para descargar mapas.json y npcs.json actualizados.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MuTextSecondary
                                )
                                OutlinedTextField(
                                    value = manifestUrl,
                                    onValueChange = { manifestUrl = it },
                                    label = { Text("URL de manifest.json") },
                                    placeholder = { Text("https://raw.githubusercontent.com/.../manifest.json") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_manifest_url_input")
                                )
                                Button(
                                    onClick = { onSyncManifestUrl(manifestUrl) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MuSky, contentColor = Color(0xFF041C2C)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_sync_manifest_button")
                                ) {
                                    Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Descargar Actualización JSON")
                                }
                                Text(
                                    text = "Última versión instalada: ${manifestInfo.version}\nNotas: ${manifestInfo.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MuTextPrimary
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
private fun AdminTabButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MuPurple else MuStoneCard,
            contentColor = if (selected) Color.White else MuTextPrimary
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        modifier = modifier.heightIn(min = 44.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}
