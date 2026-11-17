package com.muoffline.mobile.ui.dialogs

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.core.content.ContextCompat
import com.muoffline.mobile.R
import com.muoffline.mobile.lan.LanMultiplayerManager
import com.muoffline.mobile.model.*
import com.muoffline.mobile.ui.theme.*

@Composable
fun LanServerDialog(
    localHeroId: String?,
    status: LanConnectionStatus,
    connectedRoom: String?,
    hostEndpoint: String?,
    lanError: String?,
    localIps: List<String>,
    discoveredRooms: List<DiscoveredLanRoom>,
    players: List<LanPlayer>,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onHostRoom: (String) -> Unit,
    onJoinRoom: (String, String) -> Unit,
    onLeaveRoom: () -> Unit,
    onRequestTrade: (String) -> Unit,
    onStartEvent: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var roomCode by remember { mutableStateOf("LORENCIA-01") }
    var targetIpPort by remember { mutableStateOf("192.168.43.1:47777") }

    val btManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val btAdapter: BluetoothAdapter? = remember(btManager) { btManager?.adapter }
    var hasBtPerm by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }
    var pairedBtDevices by remember { mutableStateOf<List<String>>(emptyList()) }
    var btStatusMsg by remember { mutableStateOf<String?>(null) }

    fun refreshBluetoothDevices() {
        if (btAdapter == null) {
            btStatusMsg = "Este dispositivo no tiene adaptador Bluetooth."
            return
        }
        if (!hasBtPerm) {
            btStatusMsg = "Se requieren permisos Bluetooth para listar dispositivos cercanos."
            return
        }
        runCatching {
            val bonded = btAdapter.bondedDevices.orEmpty()
            pairedBtDevices = bonded.mapNotNull { dev ->
                runCatching { "${dev.name ?: "Dispositivo"} (${dev.address})" }.getOrNull()
            }
            btStatusMsg = if (!btAdapter.isEnabled) {
                "Bluetooth apagado. Actívalo para detectar mandos o pares."
            } else if (pairedBtDevices.isEmpty()) {
                "Bluetooth listo (${pairedBtDevices.size} emparejados). Empareja en Ajustes o usa Hotspot Wi-Fi."
            } else {
                "Bluetooth activo: ${pairedBtDevices.size} dispositivo(s) emparejados."
            }
        }.onFailure {
            btStatusMsg = "No se pudo consultar dispositivos Bluetooth."
        }
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasBtPerm = grants.values.all { it }
        refreshBluetoothDevices()
    }

    LaunchedEffect(hasBtPerm) {
        if (hasBtPerm) refreshBluetoothDevices()
    }

    DisposableEffect(Unit) {
        onStartDiscovery()
        onDispose { onStopDiscovery() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MuAbyssSlate,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MuSky.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 700.dp)
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Multijugador Wi-Fi LAN / Bluetooth",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MuSky
                        )
                        Text(
                            text = if (connectedRoom != null) {
                                "Sala: $connectedRoom (${hostEndpoint ?: "local"}) · Máx. ${LanMultiplayerManager.MAX_PLAYERS} jugadores"
                            } else {
                                "Máx. ${LanMultiplayerManager.MAX_PLAYERS} jugadores · IP: ${localIps.joinToString(", ").ifEmpty { "Conecta al Wi-Fi o Hotspot" }}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MuTextSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_lan_dialog")) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(id = R.string.cd_close))
                    }
                }

                if (!lanError.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = lanError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (connectedRoom == null) {
                        item {
                            Surface(
                                color = MuStoneCard,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MuBorderSlate)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Crear o Unirse a una Sala Wi-Fi (hasta 12 jugadores)", style = MaterialTheme.typography.titleMedium, color = MuAmberBright)
                                    OutlinedTextField(
                                        value = roomCode,
                                        onValueChange = { roomCode = it.uppercase() },
                                        label = { Text("Código de Sala") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = { onHostRoom(roomCode) },
                                        enabled = status != LanConnectionStatus.CONNECTING,
                                        colors = ButtonDefaults.buttonColors(containerColor = MuEmerald, contentColor = Color(0xFF022016)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("host_lan_button")
                                    ) {
                                        Icon(Icons.Rounded.Router, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Crear Sala en este Dispositivo (Host)")
                                    }

                                    HorizontalDivider(color = MuBorderSlate)

                                    OutlinedTextField(
                                        value = targetIpPort,
                                        onValueChange = { targetIpPort = it },
                                        label = { Text("IP:Puerto del Host (ej. 192.168.43.1:47777)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = { onJoinRoom(targetIpPort, roomCode) },
                                        enabled = status != LanConnectionStatus.CONNECTING,
                                        colors = ButtonDefaults.buttonColors(containerColor = MuSky, contentColor = Color(0xFF041C2C)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("join_lan_button")
                                    ) {
                                        Icon(Icons.Rounded.Wifi, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Unirse por IP")
                                    }
                                }
                            }
                        }

                        // Bluetooth Local Panel
                        item {
                            Surface(
                                color = MuStoneCard,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MuPurple.copy(alpha = 0.55f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = MuSky)
                                            Text("Bluetooth Local", style = MaterialTheme.typography.titleMedium, color = MuSky)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                if (!hasBtPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    btPermissionLauncher.launch(
                                                        arrayOf(
                                                            Manifest.permission.BLUETOOTH_CONNECT,
                                                            Manifest.permission.BLUETOOTH_SCAN
                                                        )
                                                    )
                                                } else {
                                                    refreshBluetoothDevices()
                                                }
                                            },
                                            modifier = Modifier.testTag("bluetooth_scan_button")
                                        ) {
                                            Text(if (hasBtPerm) "Actualizar" else "Permitir BT")
                                        }
                                    }
                                    btStatusMsg?.let { msg ->
                                        Text(msg, style = MaterialTheme.typography.bodySmall, color = MuTextSecondary)
                                    }
                                    pairedBtDevices.take(5).forEach { dev ->
                                        Text("• $dev", style = MaterialTheme.typography.bodySmall, color = MuTextPrimary)
                                    }
                                }
                            }
                        }

                        item {
                            Text("Salas detectadas automáticamente (NSD)", style = MaterialTheme.typography.titleMedium, color = MuTextPrimary)
                        }
                        if (discoveredRooms.isEmpty()) {
                            item {
                                Text(
                                    text = "Buscando partidas MU Offline en tu red Wi-Fi local...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MuTextSecondary
                                )
                            }
                        } else {
                            items(discoveredRooms, key = { "${it.hostAddress}:${it.port}" }) { room ->
                                Surface(
                                    color = MuStoneCard,
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, MuSky.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(room.roomCode, style = MaterialTheme.typography.titleMedium, color = MuAmberBright)
                                            Text("${room.hostAddress}:${room.port}", style = MaterialTheme.typography.bodySmall, color = MuTextSecondary)
                                        }
                                        Button(
                                            onClick = { onJoinRoom("${room.hostAddress}:${room.port}", room.roomCode) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MuSky, contentColor = Color(0xFF041C2C))
                                        ) {
                                            Text("Conectar")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Surface(
                                color = MuStoneCard,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, MuEmerald.copy(alpha = 0.6f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Eventos Cooperativos LAN (Season 6)", style = MaterialTheme.typography.titleMedium, color = MuAmberBright)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onStartEvent("golden-invasion") },
                                            colors = ButtonDefaults.buttonColors(containerColor = MuGold, contentColor = Color(0xFF1B1202)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Invasión Dorada")
                                        }
                                        Button(
                                            onClick = { onStartEvent("blood-castle") },
                                            colors = ButtonDefaults.buttonColors(containerColor = MuCrimson, contentColor = Color.White),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Blood Castle")
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onStartEvent("devil-square") },
                                            colors = ButtonDefaults.buttonColors(containerColor = MuPurple, contentColor = Color.White),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Devil Square")
                                        }
                                        Button(
                                            onClick = { onStartEvent("chaos-castle") },
                                            colors = ButtonDefaults.buttonColors(containerColor = MuSky, contentColor = Color(0xFF041C2C)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Chaos Castle")
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = onLeaveRoom,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Desconectar de la Sala", color = MuCrimson)
                                    }
                                }
                            }
                        }

                        item {
                            Text("Jugadores en Sala (${players.size}/${LanMultiplayerManager.MAX_PLAYERS})", style = MaterialTheme.typography.titleMedium, color = MuTextPrimary)
                        }
                        items(players, key = { it.id }) { p ->
                            val isSelf = p.id == localHeroId
                            val mapName = MuCatalog.DEFAULT_MAP_CATALOG[p.mapId]?.name ?: p.mapId.name
                            Surface(
                                color = MuStoneCard,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MuBorderSlate)
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
                                            text = if (isSelf) "${p.name} (Tú)" else p.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MuTextPrimary
                                        )
                                        Text(
                                            text = "${MuCatalog.CLASS_CATALOG.getValue(p.classId).name} · Nv.${p.level} · $mapName",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MuTextSecondary
                                        )
                                    }
                                    if (!isSelf) {
                                        Button(
                                            onClick = {
                                                onRequestTrade(p.id)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MuSky, contentColor = Color(0xFF041C2C))
                                        ) {
                                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Intercambiar")
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
