package com.muoffline.mobile.lan

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import com.muoffline.mobile.data.JsonDataStore
import com.muoffline.mobile.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

sealed class LanIncomingEvent {
    data class RoomState(
        val roomCode: String,
        val players: List<LanPlayer>,
        val bosses: List<LanBossState>,
        val activeEvent: LanEventState?
    ) : LanIncomingEvent()

    data class PlayerJoined(val player: LanPlayer) : LanIncomingEvent()
    data class PlayerUpdated(val player: LanPlayer) : LanIncomingEvent()
    data class PlayerLeft(val playerId: String) : LanIncomingEvent()
    data class PlayerCast(val skillId: String, val mapId: MapId, val x: Float, val y: Float, val targetX: Float, val targetY: Float) : LanIncomingEvent()
    data class EventStarted(val event: LanEventState) : LanIncomingEvent()
    data class EventCompleted(val eventName: String, val rewardZen: Int, val killerName: String) : LanIncomingEvent()
    data class BossUpdated(val bossId: String, val hp: Int) : LanIncomingEvent()
    data class BossKilled(val bossId: String, val killerName: String) : LanIncomingEvent()
    data class BossRespawned(val boss: LanBossState) : LanIncomingEvent()
    data class TradeIncoming(val request: IncomingTradeRequest) : LanIncomingEvent()
    data class TradeStarted(val tradeId: String, val partnerId: String, val partnerName: String) : LanIncomingEvent()
    data class TradePartnerUpdated(val tradeId: String, val items: List<Item>, val zen: Int) : LanIncomingEvent()
    data class TradePartnerConfirmed(val tradeId: String) : LanIncomingEvent()
    data class TradeCancelled(val tradeId: String) : LanIncomingEvent()
    data class TradeCompleted(
        val receivedItems: List<Item>,
        val receivedZen: Int,
        val givenItems: List<Item>,
        val givenZen: Int
    ) : LanIncomingEvent()
    data class ChatMessage(val msg: LanChatMessage) : LanIncomingEvent()
}

class LanMultiplayerManager(context: Context) {

    companion object {
        const val DEFAULT_PORT = 47777
        const val MAX_PLAYERS = 12
        const val SERVICE_TYPE = "_muoffline._tcp."
    }

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val nsdManager: NsdManager? = appContext.getSystemService(Context.NSD_SERVICE) as? NsdManager

    private val _status = MutableStateFlow(LanConnectionStatus.DISCONNECTED)
    val status = _status.asStateFlow()

    private val _connectedRoom = MutableStateFlow<String?>(null)
    val connectedRoom = _connectedRoom.asStateFlow()

    private val _hostEndpoint = MutableStateFlow<String?>(null)
    val hostEndpoint = _hostEndpoint.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _discoveredRooms = MutableStateFlow<List<DiscoveredLanRoom>>(emptyList())
    val discoveredRooms = _discoveredRooms.asStateFlow()

    private val _events = MutableSharedFlow<LanIncomingEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    // Client socket state
    private var clientSocket: Socket? = null
    private var clientWriter: BufferedWriter? = null

    // Embedded host server state
    private var serverSocket: ServerSocket? = null
    private var hostJob: Job? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private data class ConnectedPeer(
        val id: String,
        val writer: BufferedWriter,
        var state: LanPlayer
    )

    private data class ServerTrade(
        val tradeId: String,
        val playerA: String,
        val playerB: String,
        var itemsA: JSONArray = JSONArray(),
        var zenA: Int = 0,
        var confirmedA: Boolean = false,
        var itemsB: JSONArray = JSONArray(),
        var zenB: Int = 0,
        var confirmedB: Boolean = false
    )

    private var hostedRoomCode: String = "LORENCIA-01"
    private val peers = ConcurrentHashMap<String, ConnectedPeer>()
    private val serverBosses = ConcurrentHashMap<String, LanBossState>()
    private val serverTrades = ConcurrentHashMap<String, ServerTrade>()
    @Volatile
    private var serverActiveEvent: LanEventState? = null

    fun getLocalIpv4Addresses(): List<String> {
        val result = mutableListOf<String>()
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        addr.hostAddress?.let { result += it }
                    }
                }
            }
        }
        return result
    }

    fun startDiscovery() {
        val mgr = nsdManager ?: return
        stopDiscovery()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != SERVICE_TYPE) return
                runCatching {
                    @Suppress("DEPRECATION")
                    mgr.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(si: NsdServiceInfo, errorCode: Int) = Unit
                        override fun onServiceResolved(si: NsdServiceInfo) {
                            val host = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                si.hostAddresses.firstOrNull { it is Inet4Address }?.hostAddress
                            } else {
                                @Suppress("DEPRECATION")
                                si.host?.hostAddress
                            } ?: return
                            val roomCode = si.serviceName.removePrefix("MuOffline-").ifBlank { "LORENCIA-01" }
                            val entry = DiscoveredLanRoom(si.serviceName, roomCode, host, si.port)
                            _discoveredRooms.value = (_discoveredRooms.value.filterNot {
                                it.hostAddress == host && it.port == si.port
                            } + entry)
                        }
                    })
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                _discoveredRooms.value = _discoveredRooms.value.filterNot { it.serviceName == serviceInfo.serviceName }
            }
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }
        discoveryListener = listener
        runCatching {
            mgr.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        }
    }

    fun stopDiscovery() {
        val mgr = nsdManager ?: return
        discoveryListener?.let { l ->
            runCatching { mgr.stopServiceDiscovery(l) }
        }
        discoveryListener = null
    }

    fun hostRoom(roomCodeRaw: String, hero: HeroSave) {
        leaveRoom()
        val code = roomCodeRaw.trim().uppercase().take(24).ifEmpty { "LORENCIA-01" }
        hostedRoomCode = code
        _error.value = null
        _status.value = LanConnectionStatus.CONNECTING

        scope.launch {
            try {
                val ss = runCatching { ServerSocket(DEFAULT_PORT) }.getOrElse { ServerSocket(0) }
                serverSocket = ss
                val port = ss.localPort
                val ip = getLocalIpv4Addresses().firstOrNull() ?: "127.0.0.1"
                _hostEndpoint.value = "$ip:$port"
                initDefaultBosses()
                serverActiveEvent = null
                peers.clear()
                serverTrades.clear()
                registerNsdService(code, port)

                hostJob = scope.launch {
                    while (isActive && !ss.isClosed) {
                        val socket = runCatching { ss.accept() }.getOrNull() ?: break
                        launch { handleServerClient(socket) }
                    }
                }

                connectInternal("127.0.0.1", port, code, hero, hosting = true)
            } catch (e: Exception) {
                _status.value = LanConnectionStatus.ERROR
                _error.value = "No se pudo iniciar la sala LAN: ${e.message ?: "Error de red"}"
            }
        }
    }

    fun joinRoom(hostIpPort: String, roomCodeRaw: String, hero: HeroSave) {
        leaveRoom()
        val parts = hostIpPort.trim().split(":")
        val host = parts[0].trim()
        val port = parts.getOrNull(1)?.toIntOrNull() ?: DEFAULT_PORT
        val code = roomCodeRaw.trim().uppercase().take(24).ifEmpty { "LORENCIA-01" }
        if (host.isEmpty()) {
            _status.value = LanConnectionStatus.ERROR
            _error.value = "Ingresa la dirección IP del anfitrión (ej. 192.168.43.1)."
            return
        }
        _error.value = null
        _status.value = LanConnectionStatus.CONNECTING
        _hostEndpoint.value = "$host:$port"
        scope.launch {
            connectInternal(host, port, code, hero, hosting = false)
        }
    }

    private suspend fun connectInternal(host: String, port: Int, code: String, hero: HeroSave, hosting: Boolean) {
        try {
            val socket = Socket(host, port)
            socket.tcpNoDelay = true
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            clientSocket = socket
            clientWriter = writer

            val joinPayload = JSONObject().apply {
                put("type", "room:join")
                put("roomCode", code)
                put("player", encodeHeroAsLanPlayer(hero))
            }
            writer.write(joinPayload.toString())
            writer.newLine()
            writer.flush()

            _status.value = if (hosting) LanConnectionStatus.HOSTING else LanConnectionStatus.CONNECTED

            while (currentCoroutineContext().isActive && !socket.isClosed) {
                val line = reader.readLine() ?: break
                handleIncomingServerMessage(line)
            }
        } catch (e: Exception) {
            if (_status.value != LanConnectionStatus.DISCONNECTED) {
                _status.value = LanConnectionStatus.ERROR
                _error.value = "Conexión LAN cerrada o inaccesible (${e.message ?: "sin ruta"})."
            }
        } finally {
            _connectedRoom.value = null
            if (_status.value != LanConnectionStatus.ERROR) {
                _status.value = LanConnectionStatus.DISCONNECTED
            }
        }
    }

    fun sendJson(payload: JSONObject) {
        val writer = clientWriter ?: return
        scope.launch {
            runCatching {
                synchronized(writer) {
                    writer.write(payload.toString())
                    writer.newLine()
                    writer.flush()
                }
            }
        }
    }

    fun sendPlayerMove(hero: HeroSave, dir: Float) {
        sendJson(JSONObject().apply {
            put("type", "player:move")
            put("x", hero.x.toDouble())
            put("y", hero.y.toDouble())
            put("dir", dir.toDouble())
            put("mapId", hero.mapId.wire)
            put("level", hero.level)
            put("resets", hero.resets)
            put("wingsTier", hero.equipment.wings?.wingsTier?.coerceAtLeast(1) ?: 0)
            put("weaponPlus", hero.equipment.weapon?.plus ?: 0)
            put("hp", hero.hp)
            put("maxHp", hero.maxHp)
        })
    }

    fun sendSkillCast(skillId: String, x: Float, y: Float, targetX: Float, targetY: Float) {
        sendJson(JSONObject().apply {
            put("type", "player:skill")
            put("skillId", skillId)
            put("x", x.toDouble())
            put("y", y.toDouble())
            put("targetX", targetX.toDouble())
            put("targetY", targetY.toDouble())
        })
    }

    fun sendBossHit(bossId: String, damage: Int) {
        sendJson(JSONObject().apply {
            put("type", "boss:hit")
            put("bossId", bossId)
            put("damage", damage)
        })
    }

    fun sendStartEvent(eventType: String) {
        sendJson(JSONObject().apply {
            put("type", "event:start")
            put("eventType", eventType)
        })
    }

    fun sendChat(text: String) {
        sendJson(JSONObject().apply {
            put("type", "chat:send")
            put("text", text)
        })
    }

    fun sendTradeRequest(targetPlayerId: String) {
        sendJson(JSONObject().apply {
            put("type", "trade:request")
            put("targetPlayerId", targetPlayerId)
        })
    }

    fun sendTradeAccept(tradeId: String) {
        sendJson(JSONObject().apply {
            put("type", "trade:accept")
            put("tradeId", tradeId)
        })
    }

    fun sendTradeDecline(tradeId: String) {
        sendJson(JSONObject().apply {
            put("type", "trade:decline")
            put("tradeId", tradeId)
        })
    }

    fun sendTradeUpdate(tradeId: String, items: List<Item>, zen: Int) {
        sendJson(JSONObject().apply {
            put("type", "trade:update")
            put("tradeId", tradeId)
            put("items", JSONArray().apply { items.forEach { put(JsonDataStore.encodeItem(it)) } })
            put("zen", zen)
        })
    }

    fun sendTradeConfirm(tradeId: String) {
        sendJson(JSONObject().apply {
            put("type", "trade:confirm")
            put("tradeId", tradeId)
        })
    }

    fun sendTradeCancel(tradeId: String) {
        sendJson(JSONObject().apply {
            put("type", "trade:cancel")
            put("tradeId", tradeId)
        })
    }

    fun leaveRoom() {
        runCatching {
            clientWriter?.let { w ->
                synchronized(w) {
                    w.write(JSONObject().put("type", "room:leave").toString())
                    w.newLine()
                    w.flush()
                }
            }
        }
        runCatching { clientSocket?.close() }
        clientSocket = null
        clientWriter = null

        unregisterNsdService()
        hostJob?.cancel()
        hostJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        peers.clear()
        serverTrades.clear()
        serverActiveEvent = null
        _connectedRoom.value = null
        _hostEndpoint.value = null
        _status.value = LanConnectionStatus.DISCONNECTED
    }

    private fun registerNsdService(roomCode: String, port: Int) {
        val mgr = nsdManager ?: return
        unregisterNsdService()
        val info = NsdServiceInfo().apply {
            serviceName = "MuOffline-$roomCode"
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        registrationListener = listener
        runCatching { mgr.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
    }

    private fun unregisterNsdService() {
        val mgr = nsdManager ?: return
        registrationListener?.let { l -> runCatching { mgr.unregisterService(l) } }
        registrationListener = null
    }

    private fun initDefaultBosses() {
        serverBosses.clear()
        val defaults = listOf(
            LanBossState("lan-boss-lorencia", "Golden Dragon", MapId.LORENCIA, 1800, 1800, 780f, 680f, 0L),
            LanBossState("lan-boss-dungeon", "Arcángel Caído (Blood Castle)", MapId.DUNGEON1, 4200, 4200, 640f, 520f, 0L),
            LanBossState("lan-boss-losttower", "Balrog Supremo (Devil Square)", MapId.LOSTTOWER, 7200, 7200, 700f, 500f, 0L),
            LanBossState("lan-boss-kanturu", "Guardián del Caos (Chaos Castle)", MapId.KANTURU, 11500, 11500, 680f, 640f, 0L)
        )
        defaults.forEach { serverBosses[it.id] = it }
    }

    private suspend fun handleServerClient(socket: Socket) {
        var playerId: String? = null
        try {
            socket.tcpNoDelay = true
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

            while (!socket.isClosed) {
                val raw = reader.readLine() ?: break
                val msg = runCatching { JSONObject(raw) }.getOrNull() ?: continue
                val type = msg.optString("type")

                if (type == "room:join") {
                    if (peers.size >= MAX_PLAYERS) {
                        sendPeer(writer, JSONObject().apply {
                            put("type", "room:full")
                            put("maxPlayers", MAX_PLAYERS)
                        })
                        break
                    }
                    val pObj = msg.optJSONObject("player") ?: JSONObject()
                    val pid = pObj.optString("id").ifBlank { MuCatalog.newUid("p") }
                    val state = decodeLanPlayer(pObj.put("id", pid))
                    playerId = pid
                    peers[pid] = ConnectedPeer(pid, writer, state)

                    sendPeer(writer, JSONObject().apply {
                        put("type", "room:state")
                        put("roomCode", hostedRoomCode)
                        put("players", JSONArray().apply { peers.values.forEach { put(encodeLanPlayer(it.state)) } })
                        put("bosses", JSONArray().apply { serverBosses.values.forEach { put(encodeLanBoss(it)) } })
                        serverActiveEvent?.let { put("activeEvent", encodeLanEvent(it)) }
                    })

                    broadcastServer(JSONObject().apply {
                        put("type", "player:joined")
                        put("player", encodeLanPlayer(state))
                    }, excludeId = pid)
                    continue
                }

                val currentId = playerId ?: continue
                val currentPeer = peers[currentId] ?: continue

                when (type) {
                    "player:move" -> {
                        val updated = currentPeer.state.copy(
                            x = msg.optDouble("x", currentPeer.state.x.toDouble()).toFloat(),
                            y = msg.optDouble("y", currentPeer.state.y.toDouble()).toFloat(),
                            dir = msg.optDouble("dir", currentPeer.state.dir.toDouble()).toFloat(),
                            mapId = MapId.fromWire(msg.optString("mapId", currentPeer.state.mapId.wire)),
                            level = msg.optInt("level", currentPeer.state.level),
                            resets = msg.optInt("resets", currentPeer.state.resets),
                            wingsTier = msg.optInt("wingsTier", currentPeer.state.wingsTier),
                            weaponPlus = msg.optInt("weaponPlus", currentPeer.state.weaponPlus),
                            hp = msg.optInt("hp", currentPeer.state.hp),
                            maxHp = msg.optInt("maxHp", currentPeer.state.maxHp)
                        )
                        currentPeer.state = updated
                        broadcastServer(JSONObject().apply {
                            put("type", "player:updated")
                            put("player", encodeLanPlayer(updated))
                        }, excludeId = currentId)
                    }
                    "player:skill" -> {
                        broadcastServer(JSONObject().apply {
                            put("type", "player:cast")
                            put("playerId", currentId)
                            put("skillId", msg.optString("skillId", "slash"))
                            put("mapId", currentPeer.state.mapId.wire)
                            put("x", msg.optDouble("x", currentPeer.state.x.toDouble()))
                            put("y", msg.optDouble("y", currentPeer.state.y.toDouble()))
                            put("targetX", msg.optDouble("targetX", currentPeer.state.x.toDouble()))
                            put("targetY", msg.optDouble("targetY", currentPeer.state.y.toDouble()))
                        }, excludeId = currentId)
                    }
                    "boss:hit" -> {
                        val bossId = msg.optString("bossId")
                        val dmg = msg.optInt("damage", 0).coerceIn(1, 50000)
                        val boss = serverBosses[bossId] ?: continue
                        val now = System.currentTimeMillis()
                        if (boss.respawnAt > now) continue
                        val newHp = maxOf(0, boss.hp - dmg)
                        if (newHp == 0) {
                            val dead = boss.copy(hp = 0, respawnAt = now + 20_000L)
                            serverBosses[bossId] = dead
                            broadcastServer(JSONObject().apply {
                                put("type", "boss:killed")
                                put("bossId", bossId)
                                put("killerName", currentPeer.state.name)
                            })
                            val activeEv = serverActiveEvent
                            if (activeEv != null && activeEv.active && activeEv.endsAt > now && activeEv.mapId == boss.mapId) {
                                serverActiveEvent = null
                                broadcastServer(JSONObject().apply {
                                    put("type", "event:completed")
                                    put("eventName", activeEv.name)
                                    put("rewardZen", activeEv.rewardZen)
                                    put("killerName", currentPeer.state.name)
                                })
                            }
                            scope.launch {
                                delay(20_000L)
                                val b = serverBosses[bossId] ?: return@launch
                                val respawned = b.copy(hp = b.maxHp, respawnAt = 0L)
                                serverBosses[bossId] = respawned
                                broadcastServer(JSONObject().apply {
                                    put("type", "boss:respawned")
                                    put("boss", encodeLanBoss(respawned))
                                })
                            }
                        } else {
                            serverBosses[bossId] = boss.copy(hp = newHp)
                            broadcastServer(JSONObject().apply {
                                put("type", "boss:updated")
                                put("bossId", bossId)
                                put("hp", newHp)
                            })
                        }
                    }
                    "event:start" -> {
                        val kind = msg.optString("eventType")
                        val now = System.currentTimeMillis()
                        val (ev, bossId) = when (kind) {
                            "blood-castle" -> LanEventState("ev-$now", "Blood Castle", MapId.DUNGEON1, true, now + 90_000L, currentPeer.state.name, 12000) to "lan-boss-dungeon"
                            "devil-square" -> LanEventState("ev-$now", "Devil Square", MapId.LOSTTOWER, true, now + 90_000L, currentPeer.state.name, 18000) to "lan-boss-losttower"
                            "chaos-castle" -> LanEventState("ev-$now", "Chaos Castle", MapId.KANTURU, true, now + 90_000L, currentPeer.state.name, 25000) to "lan-boss-kanturu"
                            else -> LanEventState("ev-$now", "Invasión Dorada", MapId.LORENCIA, true, now + 90_000L, currentPeer.state.name, 8500) to "lan-boss-lorencia"
                        }
                        serverActiveEvent = ev
                        serverBosses[bossId]?.let { b ->
                            val reset = b.copy(hp = b.maxHp, respawnAt = 0L)
                            serverBosses[bossId] = reset
                            broadcastServer(JSONObject().apply {
                                put("type", "boss:respawned")
                                put("boss", encodeLanBoss(reset))
                            })
                        }
                        broadcastServer(JSONObject().apply {
                            put("type", "event:started")
                            put("event", encodeLanEvent(ev))
                        })
                    }
                    "trade:request" -> {
                        val targetId = msg.optString("targetPlayerId")
                        val target = peers[targetId]
                        if (target != null && targetId != currentId) {
                            val tid = MuCatalog.newUid("tr")
                            serverTrades[tid] = ServerTrade(tid, currentId, targetId)
                            sendPeer(target.writer, JSONObject().apply {
                                put("type", "trade:incoming")
                                put("tradeId", tid)
                                put("fromPlayerId", currentId)
                                put("fromPlayerName", currentPeer.state.name)
                            })
                        }
                    }
                    "trade:accept" -> {
                        val tid = msg.optString("tradeId")
                        val tr = serverTrades[tid] ?: continue
                        if (tr.playerB != currentId) continue
                        val pA = peers[tr.playerA] ?: continue
                        val pB = peers[tr.playerB] ?: continue
                        sendPeer(pA.writer, JSONObject().apply {
                            put("type", "trade:started")
                            put("tradeId", tid)
                            put("partnerId", pB.id)
                            put("partnerName", pB.state.name)
                        })
                        sendPeer(pB.writer, JSONObject().apply {
                            put("type", "trade:started")
                            put("tradeId", tid)
                            put("partnerId", pA.id)
                            put("partnerName", pA.state.name)
                        })
                    }
                    "trade:decline", "trade:cancel" -> {
                        val tid = msg.optString("tradeId")
                        val tr = serverTrades.remove(tid) ?: continue
                        listOfNotNull(peers[tr.playerA], peers[tr.playerB]).forEach { p ->
                            sendPeer(p.writer, JSONObject().apply {
                                put("type", "trade:cancelled")
                                put("tradeId", tid)
                            })
                        }
                    }
                    "trade:update" -> {
                        val tid = msg.optString("tradeId")
                        val tr = serverTrades[tid] ?: continue
                        val items = msg.optJSONArray("items") ?: JSONArray()
                        val zen = maxOf(0, msg.optInt("zen", 0))
                        if (tr.playerA == currentId) {
                            tr.itemsA = items
                            tr.zenA = zen
                            tr.confirmedA = false
                            tr.confirmedB = false
                            peers[tr.playerB]?.let { pb ->
                                sendPeer(pb.writer, JSONObject().apply {
                                    put("type", "trade:partner_updated")
                                    put("tradeId", tid)
                                    put("items", items)
                                    put("zen", zen)
                                })
                            }
                        } else if (tr.playerB == currentId) {
                            tr.itemsB = items
                            tr.zenB = zen
                            tr.confirmedA = false
                            tr.confirmedB = false
                            peers[tr.playerA]?.let { pa ->
                                sendPeer(pa.writer, JSONObject().apply {
                                    put("type", "trade:partner_updated")
                                    put("tradeId", tid)
                                    put("items", items)
                                    put("zen", zen)
                                })
                            }
                        }
                    }
                    "trade:confirm" -> {
                        val tid = msg.optString("tradeId")
                        val tr = serverTrades[tid] ?: continue
                        if (tr.playerA == currentId) tr.confirmedA = true
                        else if (tr.playerB == currentId) tr.confirmedB = true

                        val pA = peers[tr.playerA]
                        val pB = peers[tr.playerB]
                        if (tr.confirmedA && tr.confirmedB) {
                            serverTrades.remove(tid)
                            pA?.let {
                                sendPeer(it.writer, JSONObject().apply {
                                    put("type", "trade:completed")
                                    put("tradeId", tid)
                                    put("receivedItems", tr.itemsB)
                                    put("receivedZen", tr.zenB)
                                    put("givenItems", tr.itemsA)
                                    put("givenZen", tr.zenA)
                                })
                            }
                            pB?.let {
                                sendPeer(it.writer, JSONObject().apply {
                                    put("type", "trade:completed")
                                    put("tradeId", tid)
                                    put("receivedItems", tr.itemsA)
                                    put("receivedZen", tr.zenA)
                                    put("givenItems", tr.itemsB)
                                    put("givenZen", tr.zenB)
                                })
                            }
                        } else {
                            val partner = if (tr.playerA == currentId) pB else pA
                            partner?.let {
                                sendPeer(it.writer, JSONObject().apply {
                                    put("type", "trade:partner_confirmed")
                                    put("tradeId", tid)
                                })
                            }
                        }
                    }
                    "chat:send" -> {
                        val text = msg.optString("text").trim().take(180)
                        if (text.isNotEmpty()) {
                            broadcastServer(JSONObject().apply {
                                put("type", "chat:message")
                                put("id", MuCatalog.newUid("msg"))
                                put("senderId", currentId)
                                put("senderName", currentPeer.state.name)
                                put("classId", currentPeer.state.classId.wire)
                                put("text", text)
                                put("timestamp", System.currentTimeMillis())
                            })
                        }
                    }
                    "room:leave" -> break
                }
            }
        } catch (_: Exception) {
        } finally {
            playerId?.let { pid ->
                peers.remove(pid)
                broadcastServer(JSONObject().apply {
                    put("type", "player:left")
                    put("playerId", pid)
                })
            }
            runCatching { socket.close() }
        }
    }

    private fun sendPeer(writer: BufferedWriter, payload: JSONObject) {
        runCatching {
            synchronized(writer) {
                writer.write(payload.toString())
                writer.newLine()
                writer.flush()
            }
        }
    }

    private fun broadcastServer(payload: JSONObject, excludeId: String? = null) {
        for ((id, peer) in peers.entries) {
            if (excludeId != null && id == excludeId) continue
            sendPeer(peer.writer, payload)
        }
    }

    private fun handleIncomingServerMessage(raw: String) {
        val msg = runCatching { JSONObject(raw) }.getOrNull() ?: return
        when (msg.optString("type")) {
            "room:state" -> {
                val code = msg.optString("roomCode", "LORENCIA-01")
                _connectedRoom.value = code
                val playersArr = msg.optJSONArray("players") ?: JSONArray()
                val players = (0 until playersArr.length()).mapNotNull { playersArr.optJSONObject(it)?.let(::decodeLanPlayer) }
                val bossesArr = msg.optJSONArray("bosses") ?: JSONArray()
                val bosses = (0 until bossesArr.length()).mapNotNull { bossesArr.optJSONObject(it)?.let(::decodeLanBoss) }
                val ev = msg.optJSONObject("activeEvent")?.let(::decodeLanEvent)
                _events.tryEmit(LanIncomingEvent.RoomState(code, players, bosses, ev))
            }
            "player:joined" -> msg.optJSONObject("player")?.let { _events.tryEmit(LanIncomingEvent.PlayerJoined(decodeLanPlayer(it))) }
            "player:updated" -> msg.optJSONObject("player")?.let { _events.tryEmit(LanIncomingEvent.PlayerUpdated(decodeLanPlayer(it))) }
            "player:left" -> _events.tryEmit(LanIncomingEvent.PlayerLeft(msg.optString("playerId")))
            "player:cast" -> _events.tryEmit(
                LanIncomingEvent.PlayerCast(
                    skillId = msg.optString("skillId", "basic"),
                    mapId = MapId.fromWire(msg.optString("mapId")),
                    x = msg.optDouble("x", 0.0).toFloat(),
                    y = msg.optDouble("y", 0.0).toFloat(),
                    targetX = msg.optDouble("targetX", 0.0).toFloat(),
                    targetY = msg.optDouble("targetY", 0.0).toFloat()
                )
            )
            "event:started" -> msg.optJSONObject("event")?.let { _events.tryEmit(LanIncomingEvent.EventStarted(decodeLanEvent(it))) }
            "event:completed" -> _events.tryEmit(
                LanIncomingEvent.EventCompleted(
                    eventName = msg.optString("eventName", "Evento LAN"),
                    rewardZen = msg.optInt("rewardZen", 5000),
                    killerName = msg.optString("killerName", "Héroe")
                )
            )
            "boss:updated" -> _events.tryEmit(LanIncomingEvent.BossUpdated(msg.optString("bossId"), msg.optInt("hp", 0)))
            "boss:killed" -> _events.tryEmit(LanIncomingEvent.BossKilled(msg.optString("bossId"), msg.optString("killerName", "Héroe")))
            "boss:respawned" -> msg.optJSONObject("boss")?.let { _events.tryEmit(LanIncomingEvent.BossRespawned(decodeLanBoss(it))) }
            "trade:incoming" -> _events.tryEmit(
                LanIncomingEvent.TradeIncoming(
                    IncomingTradeRequest(
                        tradeId = msg.optString("tradeId"),
                        fromPlayerId = msg.optString("fromPlayerId"),
                        fromPlayerName = msg.optString("fromPlayerName", "Jugador")
                    )
                )
            )
            "trade:started" -> _events.tryEmit(
                LanIncomingEvent.TradeStarted(
                    tradeId = msg.optString("tradeId"),
                    partnerId = msg.optString("partnerId"),
                    partnerName = msg.optString("partnerName", "Jugador")
                )
            )
            "room:full" -> {
                _status.value = LanConnectionStatus.ERROR
                _error.value = "Sala llena (máximo $MAX_PLAYERS jugadores)."
            }
            "trade:partner_updated" -> {
                val arr = msg.optJSONArray("items") ?: JSONArray()
                val items = (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(JsonDataStore::decodeItem) }
                _events.tryEmit(LanIncomingEvent.TradePartnerUpdated(msg.optString("tradeId"), items, msg.optInt("zen", 0)))
            }
            "trade:partner_confirmed" -> _events.tryEmit(LanIncomingEvent.TradePartnerConfirmed(msg.optString("tradeId")))
            "trade:cancelled" -> _events.tryEmit(LanIncomingEvent.TradeCancelled(msg.optString("tradeId")))
            "trade:completed" -> {
                val recArr = msg.optJSONArray("receivedItems") ?: JSONArray()
                val givArr = msg.optJSONArray("givenItems") ?: JSONArray()
                val rec = (0 until recArr.length()).mapNotNull { recArr.optJSONObject(it)?.let(JsonDataStore::decodeItem) }
                val giv = (0 until givArr.length()).mapNotNull { givArr.optJSONObject(it)?.let(JsonDataStore::decodeItem) }
                _events.tryEmit(
                    LanIncomingEvent.TradeCompleted(
                        receivedItems = rec,
                        receivedZen = msg.optInt("receivedZen", 0),
                        givenItems = giv,
                        givenZen = msg.optInt("givenZen", 0)
                    )
                )
            }
            "chat:message" -> _events.tryEmit(
                LanIncomingEvent.ChatMessage(
                    LanChatMessage(
                        id = msg.optString("id", MuCatalog.newUid("m")),
                        senderId = msg.optString("senderId"),
                        senderName = msg.optString("senderName", "Héroe"),
                        classId = ClassId.fromWire(msg.optString("classId")),
                        text = msg.optString("text"),
                        timestamp = msg.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            )
        }
    }

    private fun encodeHeroAsLanPlayer(hero: HeroSave): JSONObject = encodeLanPlayer(
        LanPlayer(
            id = hero.id,
            name = hero.name,
            classId = hero.classId,
            level = hero.level,
            resets = hero.resets,
            mapId = hero.mapId,
            x = hero.x,
            y = hero.y,
            dir = 0f,
            wingsTier = hero.equipment.wings?.wingsTier?.coerceAtLeast(1) ?: 0,
            weaponPlus = hero.equipment.weapon?.plus ?: 0,
            hp = hero.hp,
            maxHp = hero.maxHp
        )
    )

    private fun encodeLanPlayer(p: LanPlayer): JSONObject = JSONObject().apply {
        put("id", p.id)
        put("name", p.name)
        put("classId", p.classId.wire)
        put("level", p.level)
        put("resets", p.resets)
        put("mapId", p.mapId.wire)
        put("x", p.x.toDouble())
        put("y", p.y.toDouble())
        put("dir", p.dir.toDouble())
        put("wingsTier", p.wingsTier)
        put("weaponPlus", p.weaponPlus)
        put("hp", p.hp)
        put("maxHp", p.maxHp)
    }

    private fun decodeLanPlayer(o: JSONObject): LanPlayer = LanPlayer(
        id = o.optString("id"),
        name = o.optString("name", "Guerrero"),
        classId = ClassId.fromWire(o.optString("classId")),
        level = o.optInt("level", 1),
        resets = o.optInt("resets", 0),
        mapId = MapId.fromWire(o.optString("mapId")),
        x = o.optDouble("x", MuWorldConstants.SAFE_CENTER.toDouble()).toFloat(),
        y = o.optDouble("y", MuWorldConstants.SAFE_CENTER.toDouble()).toFloat(),
        dir = o.optDouble("dir", 0.0).toFloat(),
        wingsTier = o.optInt("wingsTier", 0),
        weaponPlus = o.optInt("weaponPlus", 0),
        hp = o.optInt("hp", 180),
        maxHp = o.optInt("maxHp", 180)
    )

    private fun encodeLanBoss(b: LanBossState): JSONObject = JSONObject().apply {
        put("id", b.id)
        put("name", b.name)
        put("mapId", b.mapId.wire)
        put("hp", b.hp)
        put("maxHp", b.maxHp)
        put("x", b.x.toDouble())
        put("y", b.y.toDouble())
        put("respawnAt", b.respawnAt)
    }

    private fun decodeLanBoss(o: JSONObject): LanBossState = LanBossState(
        id = o.optString("id"),
        name = o.optString("name", "Boss"),
        mapId = MapId.fromWire(o.optString("mapId")),
        hp = o.optInt("hp", 1000),
        maxHp = o.optInt("maxHp", 1000),
        x = o.optDouble("x", 700.0).toFloat(),
        y = o.optDouble("y", 600.0).toFloat(),
        respawnAt = o.optLong("respawnAt", 0L)
    )

    private fun encodeLanEvent(e: LanEventState): JSONObject = JSONObject().apply {
        put("id", e.id)
        put("name", e.name)
        put("mapId", e.mapId.wire)
        put("active", e.active)
        put("endsAt", e.endsAt)
        put("startedBy", e.startedBy)
        put("rewardZen", e.rewardZen)
    }

    private fun decodeLanEvent(o: JSONObject): LanEventState = LanEventState(
        id = o.optString("id"),
        name = o.optString("name", "Evento"),
        mapId = MapId.fromWire(o.optString("mapId")),
        active = o.optBoolean("active", false),
        endsAt = o.optLong("endsAt", 0L),
        startedBy = o.optString("startedBy", "Host"),
        rewardZen = o.optInt("rewardZen", 8000)
    )

    fun shutdown() {
        stopDiscovery()
        leaveRoom()
        scope.cancel()
    }
}
