package com.muoffline.mobile.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.muoffline.mobile.audio.SoundFxPlayer
import com.muoffline.mobile.data.GameRepository
import com.muoffline.mobile.lan.LanIncomingEvent
import com.muoffline.mobile.lan.LanMultiplayerManager
import com.muoffline.mobile.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = GameRepository(application)
    val soundFx = SoundFxPlayer()
    val lanManager = LanMultiplayerManager(application)

    private val _accountId = MutableStateFlow(repo.getActiveSessionAccount())
    val accountId = _accountId.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _authBusy = MutableStateFlow(false)
    val authBusy = _authBusy.asStateFlow()

    private val _heroes = MutableStateFlow<List<HeroSave>>(emptyList())
    val heroes = _heroes.asStateFlow()

    private val _vaultItems = MutableStateFlow<List<Item>>(emptyList())
    val vaultItems = _vaultItems.asStateFlow()

    private val _mods = MutableStateFlow<List<ModPackage>>(emptyList())
    val mods = _mods.asStateFlow()

    val activeModRates: StateFlow<ActiveModRates> = _mods
        .map { MuCatalog.computeActiveModRates(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ActiveModRates())

    private val _mapCatalog = MutableStateFlow(MuCatalog.DEFAULT_MAP_CATALOG)
    val mapCatalog = _mapCatalog.asStateFlow()

    private val _npcs = MutableStateFlow(MuCatalog.DEFAULT_NPCS)
    val npcs = _npcs.asStateFlow()

    private val _hudSettings = MutableStateFlow(MobileHudSettings())
    val hudSettings = _hudSettings.asStateFlow()

    private val _manifestInfo = MutableStateFlow(ManifestInfo())
    val manifestInfo = _manifestInfo.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String?>(null)
    val syncStatusMessage = _syncStatusMessage.asStateFlow()

    private val _activeHero = MutableStateFlow<HeroSave?>(null)
    val activeHero = _activeHero.asStateFlow()

    private val _monsters = MutableStateFlow<List<LiveMonster>>(emptyList())
    val monsters = _monsters.asStateFlow()

    private val _drops = MutableStateFlow<List<GroundDrop>>(emptyList())
    val drops = _drops.asStateFlow()

    private val _floatingTexts = MutableStateFlow<List<FloatingText>>(emptyList())
    val floatingTexts = _floatingTexts.asStateFlow()

    private val _vfxList = MutableStateFlow<List<SkillVfx>>(emptyList())
    val vfxList = _vfxList.asStateFlow()

    private val _skillCooldowns = MutableStateFlow<Map<String, Long>>(emptyMap())
    val skillCooldowns = _skillCooldowns.asStateFlow()

    private val _autoHunt = MutableStateFlow(false)
    val autoHunt = _autoHunt.asStateFlow()

    private val _muted = MutableStateFlow(false)
    val muted = _muted.asStateFlow()

    private val _heroFacingAngle = MutableStateFlow(0f)
    val heroFacingAngle = _heroFacingAngle.asStateFlow()

    // LAN UI States
    val lanStatus = lanManager.status
    val connectedRoom = lanManager.connectedRoom
    val hostEndpoint = lanManager.hostEndpoint
    val lanError = lanManager.error
    val discoveredRooms = lanManager.discoveredRooms

    private val _lanPlayers = MutableStateFlow<List<LanPlayer>>(emptyList())
    val lanPlayers = _lanPlayers.asStateFlow()

    private val _lanBosses = MutableStateFlow<List<LanBossState>>(emptyList())
    val lanBosses = _lanBosses.asStateFlow()

    private val _activeEvent = MutableStateFlow<LanEventState?>(null)
    val activeEvent = _activeEvent.asStateFlow()

    private val _incomingTrade = MutableStateFlow<IncomingTradeRequest?>(null)
    val incomingTrade = _incomingTrade.asStateFlow()

    private val _tradeSession = MutableStateFlow<TradeSession?>(null)
    val tradeSession = _tradeSession.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<LanChatMessage>>(emptyList())
    val chatMessages = _chatMessages.asStateFlow()

    @Volatile
    var joystickX: Float = 0f
    @Volatile
    var joystickY: Float = 0f

    private var lastAttackAt = 0L
    private var lastLanSyncAt = 0L
    private var lastAutoSaveAt = 0L

    init {
        viewModelScope.launch {
            _mods.value = repo.loadMods()
            _mapCatalog.value = repo.loadMapsConfig()
            _npcs.value = repo.loadNpcs()
            _manifestInfo.value = repo.loadManifest()
            val settings = repo.loadSettings()
            _hudSettings.value = settings
            soundFx.sfxVolume = settings.sfxVolume
            _accountId.value?.let { acc ->
                _heroes.value = repo.loadHeroes(acc)
                _vaultItems.value = repo.loadVault(acc)
            }
        }
        observeLanEvents()
        startWorldSimulationLoop()
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun login(rawId: String, password: String) {
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            val res = repo.loginAccount(rawId, password)
            _authBusy.value = false
            if (res.ok && res.accountId != null) {
                _accountId.value = res.accountId
                _heroes.value = repo.loadHeroes(res.accountId)
                _vaultItems.value = repo.loadVault(res.accountId)
            } else {
                _authError.value = res.error ?: "Error al iniciar sesión."
            }
        }
    }

    fun register(rawId: String, password: String, confirm: String) {
        if (password != confirm) {
            _authError.value = "Las contraseñas no coinciden."
            return
        }
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            val res = repo.registerAccount(rawId, password)
            _authBusy.value = false
            if (res.ok && res.accountId != null) {
                _accountId.value = res.accountId
                _heroes.value = repo.loadHeroes(res.accountId)
                _vaultItems.value = repo.loadVault(res.accountId)
            } else {
                _authError.value = res.error ?: "No se pudo registrar la cuenta."
            }
        }
    }

    fun logoutAccount() {
        repo.logoutAccount()
        lanManager.leaveRoom()
        _autoHunt.value = false
        _activeHero.value = null
        _accountId.value = null
        _heroes.value = emptyList()
        _vaultItems.value = emptyList()
    }

    fun maxAccountHeroLevel(): Int = _heroes.value.maxOfOrNull { it.level } ?: 1

    fun canCreateClass(classId: ClassId): Boolean {
        val req = MuCatalog.CLASS_CATALOG.getValue(classId).unlockLevel
        return maxAccountHeroLevel() >= req
    }

    fun selectHero(hero: HeroSave) {
        val derived = MuCatalog.computeDerivedHeroStats(hero)
        val synced = hero.copy(
            maxHp = derived.maxHp,
            maxMp = derived.maxMp,
            hp = hero.hp.coerceIn(1, derived.maxHp),
            mp = hero.mp.coerceIn(0, derived.maxMp)
        )
        _activeHero.value = synced
        _monsters.value = MuCatalog.spawnMonstersForMap(synced.mapId, _mods.value)
        _drops.value = emptyList()
        _floatingTexts.value = emptyList()
        _vfxList.value = emptyList()
    }

    fun createHero(rawName: String, classId: ClassId): String? {
        val acc = _accountId.value ?: return "Sin sesión activa"
        if (_heroes.value.size >= 5) {
            return "Límite clásico de 5 slots de personaje alcanzado."
        }
        if (!canCreateClass(classId)) {
            val req = MuCatalog.CLASS_CATALOG.getValue(classId).unlockLevel
            return "Requiere tener un personaje nivel $req+ en esta cuenta."
        }
        val hero = MuCatalog.createStarterHero(acc, rawName, classId)
        viewModelScope.launch {
            repo.saveHero(hero)
            _heroes.value = listOf(hero) + _heroes.value
            selectHero(hero)
        }
        return null
    }

    fun deleteHero(heroId: String) {
        val acc = _accountId.value ?: return
        viewModelScope.launch {
            repo.deleteHero(acc, heroId)
            _heroes.value = _heroes.value.filterNot { it.id == heroId }
        }
    }

    fun exitToTitle() {
        val hero = _activeHero.value
        if (hero != null) {
            viewModelScope.launch {
                repo.saveHero(hero)
                _accountId.value?.let { acc -> _heroes.value = repo.loadHeroes(acc) }
            }
        }
        _autoHunt.value = false
        _tradeSession.value = null
        _activeHero.value = null
    }

    fun changeMap(mapId: MapId): String? {
        val hero = _activeHero.value ?: return null
        val meta = _mapCatalog.value[mapId] ?: MuCatalog.DEFAULT_MAP_CATALOG.getValue(mapId)
        if (hero.level < meta.minLevel) {
            val msg = "${meta.name} requiere nivel ${meta.minLevel}."
            pushFloatingText(hero.x, hero.y - 26f, msg, 0xFFEF4444)
            return msg
        }
        if (mapId == MapId.ICARUS && hero.equipment.wings == null) {
            val msg = "Icarus requiere tener Alas equipadas."
            pushFloatingText(hero.x, hero.y - 26f, msg, 0xFF38BDF8)
            return msg
        }
        val next = hero.copy(
            mapId = mapId,
            x = MuWorldConstants.SAFE_CENTER,
            y = MuWorldConstants.SAFE_CENTER
        )
        _activeHero.value = next
        _monsters.value = MuCatalog.spawnMonstersForMap(mapId, _mods.value)
        _drops.value = emptyList()
        saveHeroAsync(next)
        if (connectedRoom.value != null) {
            lanManager.sendPlayerMove(next, _heroFacingAngle.value)
        }
        return null
    }

    fun updateHudSettings(transform: (MobileHudSettings) -> MobileHudSettings) {
        val next = transform(_hudSettings.value)
        _hudSettings.value = next
        soundFx.sfxVolume = next.sfxVolume
        viewModelScope.launch { repo.saveSettings(next) }
    }

    fun toggleAutoHunt() {
        _autoHunt.value = !_autoHunt.value
    }

    fun toggleMute() {
        val next = !_muted.value
        _muted.value = next
        soundFx.muted = next
    }

    fun moveByDelta(dxNorm: Float, dyNorm: Float) {
        val hero = _activeHero.value ?: return
        val derived = MuCatalog.computeDerivedHeroStats(hero)
        val nx = (hero.x + dxNorm * derived.moveSpeed).coerceIn(20f, MuWorldConstants.WORLD_SIZE - 20f)
        val ny = (hero.y + dyNorm * derived.moveSpeed).coerceIn(20f, MuWorldConstants.WORLD_SIZE - 20f)
        val dir = atan2(dxNorm, dyNorm)
        _heroFacingAngle.value = dir
        val updated = hero.copy(x = nx, y = ny)
        _activeHero.value = updated

        val now = System.currentTimeMillis()
        if (connectedRoom.value != null && now - lastLanSyncAt > 85L) {
            lastLanSyncAt = now
            lanManager.sendPlayerMove(updated, dir)
        }
    }

    fun handleTapWorld(wx: Float, wy: Float) {
        val hero = _activeHero.value ?: return
        val clickedMonster = _monsters.value.firstOrNull {
            it.hp > 0 && hypot((it.x - wx).toDouble(), (it.y - wy).toDouble()) <= it.radius + 28f
        }
        val atkRange = MuCatalog.CLASS_CATALOG.getValue(hero.classId).attackRange
        if (clickedMonster != null && hypot((clickedMonster.x - hero.x).toDouble(), (clickedMonster.y - hero.y).toDouble()) <= atkRange + 32f) {
            executeCombat()
            return
        }
        val dist = hypot((wx - hero.x).toDouble(), (wy - hero.y).toDouble()).toFloat()
        if (dist > 8f) {
            val step = min(dist, 54f)
            val nx = (hero.x + ((wx - hero.x) / dist) * step).coerceIn(20f, MuWorldConstants.WORLD_SIZE - 20f)
            val ny = (hero.y + ((wy - hero.y) / dist) * step).coerceIn(20f, MuWorldConstants.WORLD_SIZE - 20f)
            val dir = atan2(wx - hero.x, wy - hero.y)
            _heroFacingAngle.value = dir
            val updated = hero.copy(x = nx, y = ny)
            _activeHero.value = updated
            if (connectedRoom.value != null) {
                lanManager.sendPlayerMove(updated, dir)
            }
        }
    }

    fun executeCombat(skillId: String? = null) {
        val hero = _activeHero.value ?: return
        val nowMs = System.currentTimeMillis()
        val derived = MuCatalog.computeDerivedHeroStats(hero)
        val classMeta = MuCatalog.CLASS_CATALOG.getValue(hero.classId)

        var dmgMultiplier = 1f
        var range = classMeta.attackRange
        var aoeRadius = 45f
        var mpCost = 0
        var vfxColor = classMeta.accentColor

        if (skillId != null) {
            val skill = MuCatalog.SKILLS_BY_CLASS[hero.classId]?.firstOrNull { it.id == skillId } ?: return
            if ((_skillCooldowns.value[skill.id] ?: 0L) > nowMs) return
            if (hero.mp < skill.mpCost) {
                pushFloatingText(hero.x, hero.y - 22f, "Sin Mana", 0xFF60A5FA)
                return
            }
            mpCost = skill.mpCost
            dmgMultiplier = skill.damageMultiplier
            range = max(range, skill.range)
            aoeRadius = skill.aoeRadius
            vfxColor = skill.colorHex
            _skillCooldowns.value = _skillCooldowns.value + (skill.id to (nowMs + skill.cooldownMs))
            soundFx.playSkill()

            if (skill.id == "heal-aura") {
                val healAmt = (derived.maxHp * 0.45f).roundToInt()
                _activeHero.value = hero.copy(
                    mp = hero.mp - mpCost,
                    hp = min(derived.maxHp, hero.hp + healAmt)
                )
                pushFloatingText(hero.x, hero.y - 20f, "+$healAmt HP", 0xFF34D399, isCrit = true)
                if (_hudSettings.value.animationsEnabled) {
                    pushVfx(skill.id, hero.x, hero.y, hero.x, hero.y, skill.colorHex, skill.aoeRadius)
                }
                return
            }
        } else {
            if (nowMs - lastAttackAt < 320L) return
            lastAttackAt = nowMs
            soundFx.playAttack(hero.classId)
        }

        val alive = _monsters.value.filter { it.hp > 0 }
        var primary: LiveMonster? = null
        var bestDist = Float.MAX_VALUE
        for (m in alive) {
            val d = hypot((m.x - hero.x).toDouble(), (m.y - hero.y).toDouble()).toFloat()
            if (d <= range + m.radius && d < bestDist) {
                bestDist = d
                primary = m
            }
        }

        val targetX = primary?.x ?: hero.x
        val targetY = primary?.y ?: hero.y
        if (_hudSettings.value.animationsEnabled) {
            pushVfx(skillId ?: "basic", hero.x, hero.y, targetX, targetY, vfxColor, aoeRadius)
        }

        if (connectedRoom.value != null) {
            lanManager.sendSkillCast(skillId ?: "basic", hero.x, hero.y, targetX, targetY)
        }

        for (lb in _lanBosses.value) {
            if (lb.mapId == hero.mapId && lb.hp > 0 && hypot((lb.x - targetX).toDouble(), (lb.y - targetY).toDouble()) <= aoeRadius + 32f) {
                val dmg = max(10, (derived.attackPower * dmgMultiplier).roundToInt())
                lanManager.sendBossHit(lb.id, dmg)
                pushFloatingText(lb.x, lb.y - 24f, "-$dmg", 0xFF38BDF8, isCrit = true)
            }
        }

        if (primary == null) {
            if (mpCost > 0) {
                _activeHero.value = hero.copy(mp = max(0, hero.mp - mpCost))
            }
            return
        }

        val rates = activeModRates.value
        val mapMeta = _mapCatalog.value[hero.mapId] ?: MuCatalog.DEFAULT_MAP_CATALOG.getValue(hero.mapId)
        var totalExp = 0
        var totalZen = 0
        val killed = mutableListOf<LiveMonster>()

        _monsters.value = _monsters.value.map { m ->
            if (m.hp <= 0) return@map m
            val distToImpact = hypot((m.x - targetX).toDouble(), (m.y - targetY).toDouble()).toFloat()
            val hit = m.uid == primary.uid || (skillId != null && distToImpact <= aoeRadius + m.radius)
            if (!hit) return@map m

            val isCrit = Random.nextFloat() < derived.critChance
            val rawDmg = derived.attackPower * dmgMultiplier - m.defense * 0.45f
            val variance = if (isCrit) 1.65f else (0.92f + Random.nextFloat() * 0.16f)
            val finalDmg = max(4, (rawDmg * variance).roundToInt())
            val newHp = max(0, m.hp - finalDmg)

            pushFloatingText(
                m.x,
                m.y - m.radius,
                if (isCrit) "$finalDmg!" else "$finalDmg",
                if (isCrit) 0xFF38BDF8 else 0xFFFBBF24,
                isCrit = isCrit
            )

            if (newHp == 0) {
                totalExp += max(5, (m.expReward * mapMeta.expRate * rates.expMul).roundToInt())
                totalZen += max(10, (m.zenReward * 0.35 * mapMeta.dropZenRate * rates.zenMul).roundToInt())
                killed += m
                m.copy(hp = 0, respawnAt = nowMs + if (m.isBoss) 18_000L else 6_500L)
            } else {
                m.copy(hp = newHp, hitFlashUntil = nowMs + 130L)
            }
        }

        for (km in killed) {
            spawnDropAt(km.x, km.y, km, mapMeta)
        }

        var nextHero = hero.copy(mp = max(0, hero.mp - mpCost))
        if (totalExp > 0 || totalZen > 0) {
            val (rewarded, leveledUp) = applyRewards(nextHero, totalExp, totalZen, killed.size)
            nextHero = rewarded
            if (leveledUp) {
                soundFx.playLevelUp()
                pushFloatingText(nextHero.x, nextHero.y - 36f, "LEVEL UP! Nv ${nextHero.level}", 0xFFFDE68A, isCrit = true)
            }
        }
        _activeHero.value = nextHero
    }

    fun usePotion(kind: PotionType) {
        val hero = _activeHero.value ?: return
        val idx = hero.inventory.indexOfFirst { it.potionType == kind }
        if (idx < 0) return
        soundFx.playPotion()
        val inv = hero.inventory.toMutableList()
        val pot = inv[idx]
        if (pot.quantity > 1) {
            inv[idx] = pot.copy(quantity = pot.quantity - 1)
        } else {
            inv.removeAt(idx)
        }
        _activeHero.value = if (kind == PotionType.HP) {
            val heal = (hero.maxHp * 0.45f).roundToInt()
            pushFloatingText(hero.x, hero.y - 24f, "+$heal HP", 0xFF34D399)
            hero.copy(inventory = inv, hp = min(hero.maxHp, hero.hp + heal))
        } else {
            val mana = (hero.maxMp * 0.50f).roundToInt()
            pushFloatingText(hero.x, hero.y - 24f, "+$mana MP", 0xFF60A5FA)
            hero.copy(inventory = inv, mp = min(hero.maxMp, hero.mp + mana))
        }
    }

    fun addStat(kind: StatKind, amount: Int) {
        val hero = _activeHero.value ?: return
        if (hero.statPoints < amount) return
        val nextStats = when (kind) {
            StatKind.STR -> hero.stats.copy(str = hero.stats.str + amount)
            StatKind.AGI -> hero.stats.copy(agi = hero.stats.agi + amount)
            StatKind.VIT -> hero.stats.copy(vit = hero.stats.vit + amount)
            StatKind.ENE -> hero.stats.copy(ene = hero.stats.ene + amount)
        }
        val baseUpdated = hero.copy(statPoints = hero.statPoints - amount, stats = nextStats)
        val derived = MuCatalog.computeDerivedHeroStats(baseUpdated)
        val finalHero = baseUpdated.copy(maxHp = derived.maxHp, maxMp = derived.maxMp)
        _activeHero.value = finalHero
        saveHeroAsync(finalHero)
    }

    fun advanceEvolutionQuest(): Pair<Boolean, String> {
        val hero = _activeHero.value ?: return false to "Sin héroe activo."
        val meta = MuCatalog.CLASS_CATALOG.getValue(hero.classId)
        return when {
            hero.evolutionTier == 0 && hero.level >= 150 -> {
                val upgraded = hero.copy(evolutionTier = 1, statPoints = hero.statPoints + 25)
                val derived = MuCatalog.computeDerivedHeroStats(upgraded)
                val finalHero = upgraded.copy(maxHp = derived.maxHp, maxMp = derived.maxMp, hp = derived.maxHp, mp = derived.maxMp)
                soundFx.playLevelUp()
                _activeHero.value = finalHero
                saveHeroAsync(finalHero)
                true to "¡Evolucionaste a ${meta.evo150Title}! (+25 puntos y +15% poder)"
            }
            hero.evolutionTier == 1 && hero.level >= 400 -> {
                val upgraded = hero.copy(evolutionTier = 2, statPoints = hero.statPoints + 50)
                val derived = MuCatalog.computeDerivedHeroStats(upgraded)
                val finalHero = upgraded.copy(maxHp = derived.maxHp, maxMp = derived.maxMp, hp = derived.maxHp, mp = derived.maxMp)
                soundFx.playLevelUp()
                _activeHero.value = finalHero
                saveHeroAsync(finalHero)
                true to "¡Evolucionaste a ${meta.evo400Title}! (+50 puntos y +30% poder)"
            }
            hero.evolutionTier >= 2 -> false to "Ya alcanzaste tu evolución máxima (${meta.evo400Title})."
            hero.evolutionTier == 0 -> false to "Necesitas Nivel 150 para evolucionar a ${meta.evo150Title}."
            else -> false to "Necesitas Nivel 400 para evolucionar a ${meta.evo400Title}."
        }
    }

    fun resetHero() {
        val hero = _activeHero.value ?: return
        if (hero.level < activeModRates.value.resetLevel) return
        val meta = MuCatalog.CLASS_CATALOG.getValue(hero.classId)
        val nextResets = hero.resets + 1
        val resetBase = hero.copy(
            level = 1,
            resets = nextResets,
            exp = 0,
            expToNext = 120,
            statPoints = nextResets * 150,
            stats = meta.baseStats,
            mapId = MapId.LORENCIA,
            x = MuWorldConstants.SAFE_CENTER,
            y = MuWorldConstants.SAFE_CENTER
        )
        val derived = MuCatalog.computeDerivedHeroStats(resetBase)
        val finalHero = resetBase.copy(
            maxHp = derived.maxHp,
            hp = derived.maxHp,
            maxMp = derived.maxMp,
            mp = derived.maxMp
        )
        soundFx.playLevelUp()
        _activeHero.value = finalHero
        _monsters.value = MuCatalog.spawnMonstersForMap(MapId.LORENCIA, _mods.value)
        _drops.value = emptyList()
        saveHeroAsync(finalHero)
    }

    fun equipItem(item: Item): String? {
        if (item.slot == ItemSlot.JEWEL || item.slot == ItemSlot.POTION || item.slot == ItemSlot.TALISMAN) return null
        val hero = _activeHero.value ?: return null
        val meta = MuCatalog.CLASS_CATALOG.getValue(hero.classId)
        if (item.slot == ItemSlot.HELM && !meta.canEquipHelm) {
            return "Magic Gladiator no puede equipar Casco."
        }
        if (item.classReq != "all" && item.classReq != hero.classId.wire) {
            return "Este ítem es exclusivo de clase ${item.classReq.uppercase()}."
        }
        val currentEq = hero.equipment.getSlot(item.slot)
        val nextInv = hero.inventory.filterNot { it.id == item.id }.toMutableList()
        if (currentEq != null) nextInv.add(currentEq)
        val updated = hero.copy(
            equipment = hero.equipment.withSlot(item.slot, item),
            inventory = nextInv
        )
        val derived = MuCatalog.computeDerivedHeroStats(updated)
        val finalHero = updated.copy(maxHp = derived.maxHp, maxMp = derived.maxMp)
        _activeHero.value = finalHero
        saveHeroAsync(finalHero)
        return null
    }

    fun unequipSlot(slot: ItemSlot) {
        val hero = _activeHero.value ?: return
        val eq = hero.equipment.getSlot(slot) ?: return
        if (hero.inventory.size >= 24) return
        val updated = hero.copy(
            equipment = hero.equipment.withSlot(slot, null),
            inventory = hero.inventory + eq
        )
        val derived = MuCatalog.computeDerivedHeroStats(updated)
        val finalHero = updated.copy(maxHp = derived.maxHp, maxMp = derived.maxMp)
        _activeHero.value = finalHero
        saveHeroAsync(finalHero)
    }

    fun depositToVault(item: Item): String {
        val acc = _accountId.value ?: return "Error"
        val hero = _activeHero.value ?: return "Error"
        if (_vaultItems.value.size >= 40) return "Baúl lleno (máx. 40)."
        val nextInv = hero.inventory.filterNot { it.id == item.id }
        val nextVault = _vaultItems.value + item
        _vaultItems.value = nextVault
        val nextHero = hero.copy(inventory = nextInv)
        _activeHero.value = nextHero
        viewModelScope.launch {
            repo.saveVault(acc, nextVault)
            repo.saveHero(nextHero)
        }
        return "${item.name} guardado en el Baúl."
    }

    fun withdrawFromVault(item: Item): String {
        val acc = _accountId.value ?: return "Error"
        val hero = _activeHero.value ?: return "Error"
        if (hero.inventory.size >= 24) return "Mochila llena (máx. 24)."
        val nextVault = _vaultItems.value.filterNot { it.id == item.id }
        val nextHero = addItemToHero(hero, item)
        _vaultItems.value = nextVault
        _activeHero.value = nextHero
        viewModelScope.launch {
            repo.saveVault(acc, nextVault)
            repo.saveHero(nextHero)
        }
        return "${item.name} retirado a tu mochila."
    }

    fun sellItem(itemId: String): String {
        val hero = _activeHero.value ?: return "Error"
        val item = hero.inventory.firstOrNull { it.id == itemId } ?: return "Ítem no encontrado"
        val gain = item.valueZen * max(1, item.quantity)
        val next = hero.copy(
            zen = hero.zen + gain,
            inventory = hero.inventory.filterNot { it.id == itemId }
        )
        _activeHero.value = next
        saveHeroAsync(next)
        return "Vendiste ${item.name} por $gain Zen."
    }

    fun buyShopItem(template: Item, costZen: Int): String {
        val hero = _activeHero.value ?: return "No hay héroe activo."
        if (hero.zen < costZen) return "No tienes suficiente Zen."
        if (hero.inventory.size >= 24 && template.slot != ItemSlot.POTION && template.slot != ItemSlot.JEWEL && template.slot != ItemSlot.TALISMAN) {
            return "Tu mochila está llena (máx. 24 espacios)."
        }
        val newItem = template.copy(
            id = MuCatalog.newUid("shop"),
            quantity = if (template.slot == ItemSlot.POTION) 5 else 1
        )
        val next = addItemToHero(hero.copy(zen = hero.zen - costZen), newItem)
        _activeHero.value = next
        saveHeroAsync(next)
        soundFx.playJewelDrop()
        return "Compraste ${template.name} por $costZen Zen."
    }

    fun upgradeWithJewel(targetItemId: String, jewelType: JewelType): String {
        val hero = _activeHero.value ?: return "Error interno"
        val nextInv = consumeOneJewel(hero.inventory, jewelType)
            ?: return "Necesitas 1 Jewel of ${jewelType.name.lowercase().replaceFirstChar { it.uppercase() }}."

        if (jewelType == JewelType.LIFE) {
            var itemName = ""
            val mutate: (Item) -> Item = { item ->
                if (item.id != targetItemId) item
                else {
                    itemName = item.name
                    item.copy(
                        attack = if (item.attack > 0) item.attack + 4 else 0,
                        magic = if (item.magic > 0) item.magic + 4 else 0,
                        defense = if (item.defense > 0) item.defense + 4 else 0
                    )
                }
            }
            val baseNext = hero.copy(equipment = hero.equipment.mapItems(mutate), inventory = nextInv.map(mutate))
            val derived = MuCatalog.computeDerivedHeroStats(baseNext)
            val nextHero = baseNext.copy(maxHp = derived.maxHp, maxMp = derived.maxMp)
            soundFx.playChaosSuccess()
            _activeHero.value = nextHero
            saveHeroAsync(nextHero)
            return "¡Jewel of Life aplicada! +4 ATK/DEF adicional a $itemName."
        }

        val success = if (jewelType == JewelType.BLESS) true else Random.nextFloat() < 0.72f
        var upgradedName = ""
        var newPlus = 0

        val mutate: (Item) -> Item = { item ->
            if (item.id != targetItemId) item
            else {
                upgradedName = item.name
                newPlus = if (success) min(9, item.plus + 1) else max(0, item.plus - 1)
                item.copy(plus = newPlus)
            }
        }

        val nextEq = hero.equipment.mapItems(mutate)
        val finalInv = nextInv.map(mutate)
        if (success) soundFx.playChaosSuccess() else soundFx.playChaosFail()

        val baseNext = hero.copy(equipment = nextEq, inventory = finalInv)
        val derived = MuCatalog.computeDerivedHeroStats(baseNext)
        val nextHero = baseNext.copy(maxHp = derived.maxHp, maxMp = derived.maxMp)
        _activeHero.value = nextHero
        saveHeroAsync(nextHero)
        return if (success) "¡Éxito! $upgradedName subió a +$newPlus."
        else "Falló la joya: $upgradedName bajó a +$newPlus."
    }

    fun chaosUpgrade(targetItemId: String): Pair<Boolean, String> {
        val hero = _activeHero.value ?: return false to "Error interno"
        if (hero.zen < 3000) return false to "Necesitas 3,000 Zen."
        val afterChaos = consumeOneJewel(hero.inventory, JewelType.CHAOS) ?: return false to "Falta Jewel of Chaos."
        val afterSoul = consumeOneJewel(afterChaos, JewelType.SOUL) ?: return false to "Falta Jewel of Soul."

        val allItems = hero.equipment.allEquipped() + afterSoul
        val target = allItems.firstOrNull { it.id == targetItemId } ?: return false to "Ítem no encontrado."
        val rate = (75 - (target.plus - 9) * 10 + activeModRates.value.chaosBonus).coerceIn(35, 95) / 100f
        val ok = Random.nextFloat() < rate
        val newPlus = if (ok) min(13, target.plus + 1) else max(0, target.plus - 2)

        val mutate: (Item) -> Item = { it -> if (it.id == targetItemId) it.copy(plus = newPlus) else it }
        if (ok) soundFx.playChaosSuccess() else soundFx.playChaosFail()

        val baseNext = hero.copy(
            zen = hero.zen - 3000,
            equipment = hero.equipment.mapItems(mutate),
            inventory = afterSoul.map(mutate)
        )
        val derived = MuCatalog.computeDerivedHeroStats(baseNext)
        val nextHero = baseNext.copy(maxHp = derived.maxHp, maxMp = derived.maxMp)
        _activeHero.value = nextHero
        saveHeroAsync(nextHero)
        return ok to if (ok) "¡Chaos Machine Exitosa! ${target.name} ahora es +$newPlus."
        else "Combinación fallida: ${target.name} bajó a +$newPlus."
    }

    fun craftWingsTier(tier: Int = 2): Pair<Boolean, String> {
        val hero = _activeHero.value ?: return false to "Error interno"
        val zenCost = when (tier) {
            1 -> 2500
            3 -> 12000
            else -> 5000
        }
        if (hero.zen < zenCost) return false to "Necesitas $zenCost Zen."
        var inv = consumeOneJewel(hero.inventory, JewelType.CHAOS) ?: return false to "Necesitas 1 Jewel of Chaos."
        if (tier >= 2) {
            inv = consumeOneJewel(inv, JewelType.BLESS) ?: return false to "Alas S$tier requieren 1 Jewel of Bless."
        }
        if (tier >= 3) {
            inv = consumeOneJewel(inv, JewelType.CREATION) ?: return false to "Alas S3 requieren 1 Jewel of Creation."
        }

        val baseRate = when (tier) {
            1 -> 82
            3 -> 55
            else -> 70
        }
        val rate = (baseRate + activeModRates.value.chaosBonus).coerceIn(30, 95) / 100f
        val ok = Random.nextFloat() < rate
        var nextHero = hero.copy(zen = hero.zen - zenCost, inventory = inv)

        if (ok) {
            soundFx.playChaosSuccess()
            val newWings = when (tier) {
                1 -> Item(MuCatalog.newUid("wings"), "crafted-wings-s1", "Wings of Satan (S1)", ItemSlot.WINGS, ItemRarity.NORMAL, plus = 1, attack = 22, magic = 22, defense = 18, speedBonus = 14, wingsTier = 1, colorHex = 0xFFA855F7, valueZen = 18000)
                3 -> Item(MuCatalog.newUid("wings"), "crafted-wings-s3", "Wings of Storm (S3)", ItemSlot.WINGS, ItemRarity.MYTHIC, plus = 5, attack = 74, magic = 74, defense = 58, hpBonus = 260, speedBonus = 30, wingsTier = 3, colorHex = 0xFFFBBF24, valueZen = 68000)
                else -> {
                    val wingsName = when (hero.classId) {
                        ClassId.DK, ClassId.MG -> "Wings of Dragon (S2)"
                        ClassId.DW -> "Wings of Soul (S2)"
                        ClassId.ELF -> "Wings of Spirits (S2)"
                        ClassId.DL -> "Cape of Lord (S2)"
                    }
                    Item(MuCatalog.newUid("wings"), "crafted-wings-s2", wingsName, ItemSlot.WINGS, ItemRarity.EXCELLENT, plus = 3, attack = 42, magic = 42, defense = 34, hpBonus = 150, speedBonus = 22, wingsTier = 2, colorHex = 0xFF38BDF8, valueZen = 32000)
                }
            }
            nextHero = addItemToHero(nextHero, newWings)
            _activeHero.value = nextHero
            saveHeroAsync(nextHero)
            return true to "¡Forjaste ${newWings.name} +${newWings.plus}! Equípalas desde tu mochila."
        }

        soundFx.playChaosFail()
        _activeHero.value = nextHero
        saveHeroAsync(nextHero)
        return false to "La Chaos Machine falló al forjar las alas S$tier."
    }

    fun redeemGoldenArcherWings(): Pair<Boolean, String> {
        val hero = _activeHero.value ?: return false to "Sin héroe activo."
        val idx = hero.inventory.indexOfFirst { it.slot == ItemSlot.TALISMAN && it.quantity >= 3 }
        if (idx < 0) return false to "Golden Archer requiere 3 Talisman / Rena."
        if (hero.zen < 3000) return false to "Necesitas 3,000 Zen."
        val inv = hero.inventory.toMutableList()
        val tal = inv[idx]
        if (tal.quantity > 3) inv[idx] = tal.copy(quantity = tal.quantity - 3) else inv.removeAt(idx)

        val reward = Item(
            id = MuCatalog.newUid("archer-wings"),
            templateId = "wings-s2-archer",
            name = "Wings of Spirits (S2 · Archer)",
            slot = ItemSlot.WINGS,
            rarity = ItemRarity.EXCELLENT,
            plus = 3,
            attack = 44,
            magic = 44,
            defense = 34,
            hpBonus = 160,
            speedBonus = 24,
            wingsTier = 2,
            colorHex = 0xFFFDE047,
            valueZen = 35000
        )
        val nextHero = addItemToHero(hero.copy(zen = hero.zen - 3000, inventory = inv), reward)
        soundFx.playChaosSuccess()
        _activeHero.value = nextHero
        saveHeroAsync(nextHero)
        return true to "¡Golden Archer te entregó ${reward.name}!"
    }

    // Admin JSON & Remote Manifest Sync
    fun updateMapRates(mapId: MapId, expRate: Double, dropZen: Double, dropItems: Double) {
        val current = _mapCatalog.value.toMutableMap()
        val existing = current[mapId] ?: return
        current[mapId] = existing.copy(
            expRate = expRate.coerceIn(0.05, 50.0),
            dropZenRate = dropZen.coerceIn(0.1, 50.0),
            dropItemRate = dropItems.coerceIn(0.05, 1.0)
        )
        _mapCatalog.value = current
        viewModelScope.launch { repo.saveMapsConfig(current) }
    }

    suspend fun loadMapsRawJson(): String = repo.loadMapsRawJson()

    fun saveMapsRawJson(raw: String, onDone: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val res = repo.saveMapsRawJson(raw)
            res.onSuccess { updated ->
                _mapCatalog.value = updated
                onDone(Result.success("mapas.json guardado y aplicado."))
            }.onFailure { err ->
                onDone(Result.failure(err))
            }
        }
    }

    suspend fun loadNpcsRawJson(): String = repo.loadNpcsRawJson()

    fun saveNpcsRawJson(raw: String, onDone: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val res = repo.saveNpcsRawJson(raw)
            res.onSuccess { updated ->
                _npcs.value = updated
                onDone(Result.success("npcs.json guardado y aplicado."))
            }.onFailure { err ->
                onDone(Result.failure(err))
            }
        }
    }

    fun syncManifestFromUrl(url: String) {
        updateHudSettings { it.copy(manifestUrl = url.trim()) }
        viewModelScope.launch {
            _syncStatusMessage.value = "Descargando manifest.json..."
            val res = repo.syncFromManifestUrl(url)
            _syncStatusMessage.value = res.message
            if (res.ok) {
                res.manifest?.let { _manifestInfo.value = it }
                _mapCatalog.value = repo.loadMapsConfig()
                _npcs.value = repo.loadNpcs()
            }
        }
    }

    // Mods management
    fun toggleMod(modId: String) {
        val next = _mods.value.map { if (it.id == modId) it.copy(enabled = !it.enabled) else it }
        _mods.value = next
        viewModelScope.launch { repo.saveMods(next) }
        _activeHero.value?.let { h -> _monsters.value = MuCatalog.spawnMonstersForMap(h.mapId, next) }
    }

    fun addMod(mod: ModPackage) {
        val next = listOf(mod) + _mods.value
        _mods.value = next
        viewModelScope.launch { repo.saveMods(next) }
        _activeHero.value?.let { h -> _monsters.value = MuCatalog.spawnMonstersForMap(h.mapId, next) }
    }

    fun deleteMod(modId: String) {
        val next = _mods.value.filterNot { it.id == modId }
        _mods.value = next
        viewModelScope.launch { repo.saveMods(next) }
        _activeHero.value?.let { h -> _monsters.value = MuCatalog.spawnMonstersForMap(h.mapId, next) }
    }

    fun exportModJson(mod: ModPackage): String = repo.exportModJson(mod)

    fun importModJson(rawJson: String): Result<ModPackage> {
        val res = repo.parseImportedModJson(rawJson)
        res.getOrNull()?.let { addMod(it) }
        return res
    }

    // LAN Actions
    fun hostLanRoom(roomCode: String) {
        val hero = _activeHero.value ?: _heroes.value.firstOrNull() ?: return
        lanManager.hostRoom(roomCode, hero)
    }

    fun joinLanRoom(hostIpPort: String, roomCode: String) {
        val hero = _activeHero.value ?: _heroes.value.firstOrNull() ?: return
        lanManager.joinRoom(hostIpPort, roomCode, hero)
    }

    fun leaveLanRoom() {
        lanManager.leaveRoom()
        _lanPlayers.value = emptyList()
        _lanBosses.value = emptyList()
        _activeEvent.value = null
        _incomingTrade.value = null
        _tradeSession.value = null
    }

    fun acceptIncomingTrade() {
        val inc = _incomingTrade.value ?: return
        lanManager.sendTradeAccept(inc.tradeId)
    }

    fun declineIncomingTrade() {
        val inc = _incomingTrade.value ?: return
        lanManager.sendTradeDecline(inc.tradeId)
        _incomingTrade.value = null
    }

    fun updateTradeOffer(items: List<Item>, zen: Int) {
        val tr = _tradeSession.value ?: return
        _tradeSession.value = tr.copy(myItems = items, myZen = zen, myConfirmed = false, partnerConfirmed = false)
        lanManager.sendTradeUpdate(tr.tradeId, items, zen)
    }

    fun confirmTrade() {
        val tr = _tradeSession.value ?: return
        _tradeSession.value = tr.copy(myConfirmed = true)
        lanManager.sendTradeConfirm(tr.tradeId)
    }

    fun cancelTrade() {
        val tr = _tradeSession.value ?: return
        lanManager.sendTradeCancel(tr.tradeId)
        _tradeSession.value = null
    }

    private fun observeLanEvents() {
        viewModelScope.launch {
            lanManager.events.collect { ev ->
                when (ev) {
                    is LanIncomingEvent.RoomState -> {
                        _lanPlayers.value = ev.players
                        _lanBosses.value = ev.bosses
                        _activeEvent.value = ev.activeEvent
                    }
                    is LanIncomingEvent.PlayerJoined -> {
                        if (_lanPlayers.value.none { it.id == ev.player.id }) {
                            _lanPlayers.value = _lanPlayers.value + ev.player
                        }
                    }
                    is LanIncomingEvent.PlayerUpdated -> {
                        _lanPlayers.value = _lanPlayers.value.map { if (it.id == ev.player.id) ev.player else it }
                    }
                    is LanIncomingEvent.PlayerLeft -> {
                        _lanPlayers.value = _lanPlayers.value.filterNot { it.id == ev.playerId }
                    }
                    is LanIncomingEvent.PlayerCast -> {
                        val hero = _activeHero.value
                        if (hero != null && ev.mapId == hero.mapId && _hudSettings.value.animationsEnabled) {
                            pushVfx(ev.skillId, ev.x, ev.y, ev.targetX, ev.targetY, 0xFF38BDF8, 80f)
                        }
                    }
                    is LanIncomingEvent.EventStarted -> {
                        _activeEvent.value = ev.event
                        pushFloatingText(MuWorldConstants.SAFE_CENTER, MuWorldConstants.SAFE_CENTER - 42f, "Evento: ${ev.event.name}", 0xFFFBBF24, isCrit = true)
                    }
                    is LanIncomingEvent.EventCompleted -> {
                        _activeEvent.value = null
                        _activeHero.value?.let { cur ->
                            val next = cur.copy(zen = cur.zen + ev.rewardZen)
                            _activeHero.value = next
                            saveHeroAsync(next)
                        }
                        soundFx.playLevelUp()
                        pushFloatingText(
                            MuWorldConstants.SAFE_CENTER,
                            MuWorldConstants.SAFE_CENTER - 48f,
                            "¡${ev.eventName} completado! +${ev.rewardZen} Zen",
                            0xFF34D399,
                            isCrit = true
                        )
                    }
                    is LanIncomingEvent.BossUpdated -> {
                        _lanBosses.value = _lanBosses.value.map { if (it.id == ev.bossId) it.copy(hp = ev.hp) else it }
                    }
                    is LanIncomingEvent.BossKilled -> {
                        _lanBosses.value = _lanBosses.value.map { if (it.id == ev.bossId) it.copy(hp = 0) else it }
                        _activeHero.value?.let { cur ->
                            val next = cur.copy(zen = cur.zen + 5000)
                            _activeHero.value = next
                            saveHeroAsync(next)
                        }
                        soundFx.playJewelDrop()
                        pushFloatingText(MuWorldConstants.SAFE_CENTER, MuWorldConstants.SAFE_CENTER - 40f, "${ev.killerName} derrotó al Jefe LAN (+5,000 Zen)!", 0xFFFBBF24, isCrit = true)
                    }
                    is LanIncomingEvent.BossRespawned -> {
                        _lanBosses.value = _lanBosses.value.map { if (it.id == ev.boss.id) ev.boss else it }
                    }
                    is LanIncomingEvent.TradeIncoming -> _incomingTrade.value = ev.request
                    is LanIncomingEvent.TradeStarted -> {
                        _incomingTrade.value = null
                        _tradeSession.value = TradeSession(ev.tradeId, ev.partnerId, ev.partnerName)
                    }
                    is LanIncomingEvent.TradePartnerUpdated -> {
                        val tr = _tradeSession.value
                        if (tr != null && tr.tradeId == ev.tradeId) {
                            _tradeSession.value = tr.copy(
                                partnerItems = ev.items,
                                partnerZen = ev.zen,
                                myConfirmed = false,
                                partnerConfirmed = false
                            )
                        }
                    }
                    is LanIncomingEvent.TradePartnerConfirmed -> {
                        val tr = _tradeSession.value
                        if (tr != null && tr.tradeId == ev.tradeId) {
                            _tradeSession.value = tr.copy(partnerConfirmed = true)
                        }
                    }
                    is LanIncomingEvent.TradeCancelled -> {
                        _incomingTrade.value = null
                        _tradeSession.value = null
                    }
                    is LanIncomingEvent.TradeCompleted -> {
                        val givenIds = ev.givenItems.map { it.id }.toSet()
                        _activeHero.value?.let { cur ->
                            var next = cur.copy(
                                zen = max(0, cur.zen - ev.givenZen + ev.receivedZen),
                                inventory = cur.inventory.filterNot { it.id in givenIds }
                            )
                            for (rec in ev.receivedItems) {
                                next = addItemToHero(next, rec.copy(id = MuCatalog.newUid("tr")))
                            }
                            _activeHero.value = next
                            saveHeroAsync(next)
                        }
                        soundFx.playChaosSuccess()
                        _tradeSession.value = null
                        _activeHero.value?.let { cur ->
                            pushFloatingText(cur.x, cur.y - 30f, "¡Intercambio Completado!", 0xFF34D399, isCrit = true)
                        }
                    }
                    is LanIncomingEvent.ChatMessage -> {
                        _chatMessages.value = (_chatMessages.value.takeLast(29) + ev.msg)
                    }
                }
            }
        }
    }

    private fun startWorldSimulationLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(100L)
                val hero = _activeHero.value ?: continue
                val now = System.currentTimeMillis()
                val derived = MuCatalog.computeDerivedHeroStats(hero)

                val jx = joystickX
                val jy = joystickY
                val mag = hypot(jx.toDouble(), jy.toDouble()).toFloat()
                if (mag > 0.08f) {
                    val clamped = min(1f, mag)
                    moveByDelta((jx / mag) * clamped * 0.1f, (jy / mag) * clamped * 0.1f)
                }

                val currentHero = _activeHero.value ?: continue
                val inSafeZone = hypot(
                    (currentHero.x - MuWorldConstants.SAFE_CENTER).toDouble(),
                    (currentHero.y - MuWorldConstants.SAFE_CENTER).toDouble()
                ) <= MuWorldConstants.SAFE_RADIUS

                val collected = mutableListOf<GroundDrop>()
                val remaining = mutableListOf<GroundDrop>()
                for (d in _drops.value) {
                    if (hypot((d.x - currentHero.x).toDouble(), (d.y - currentHero.y).toDouble()) <= 44f) {
                        collected += d
                    } else if (now - d.createdAt < 45_000L) {
                        remaining += d
                    }
                }

                var updatedHero = currentHero
                if (collected.isNotEmpty()) {
                    _drops.value = remaining
                    for (d in collected) {
                        d.zen?.let { z ->
                            updatedHero = updatedHero.copy(zen = updatedHero.zen + z)
                            pushFloatingText(updatedHero.x, updatedHero.y - 26f, "+$z Zen", 0xFFFBBF24)
                        }
                        d.item?.let { item ->
                            updatedHero = addItemToHero(updatedHero, item)
                            pushFloatingText(updatedHero.x, updatedHero.y - 34f, "+${item.name}", item.colorHex)
                        }
                    }
                }

                var damageToHero = 0
                _monsters.value = _monsters.value.map { m ->
                    if (m.hp <= 0) {
                        if (m.respawnAt > 0L && now >= m.respawnAt) {
                            return@map m.copy(hp = m.maxHp, x = m.homeX, y = m.homeY, respawnAt = 0L)
                        }
                        return@map m
                    }
                    val distHero = hypot((updatedHero.x - m.x).toDouble(), (updatedHero.y - m.y).toDouble()).toFloat()
                    var nx = m.x
                    var ny = m.y
                    if (!inSafeZone && distHero < 250f && distHero > m.radius + 20f) {
                        val step = m.speed * 0.1f
                        val candX = m.x + ((updatedHero.x - m.x) / distHero) * step
                        val candY = m.y + ((updatedHero.y - m.y) / distHero) * step
                        if (hypot((candX - MuWorldConstants.SAFE_CENTER).toDouble(), (candY - MuWorldConstants.SAFE_CENTER).toDouble()) >= MuWorldConstants.SAFE_RADIUS + m.radius) {
                            nx = candX
                            ny = candY
                        }
                    } else if (inSafeZone || distHero >= 250f) {
                        val distHome = hypot((m.homeX - m.x).toDouble(), (m.homeY - m.y).toDouble()).toFloat()
                        if (distHome > 12f) {
                            nx = m.x + ((m.homeX - m.x) / distHome) * 4f
                            ny = m.y + ((m.homeY - m.y) / distHome) * 4f
                        }
                    }

                    var nextAtkCd = m.attackCooldownUntil
                    if (!inSafeZone && distHero <= m.radius + 26f && now >= m.attackCooldownUntil) {
                        val mitigated = max(1, (m.attack - derived.defense * 0.45f).roundToInt())
                        damageToHero += mitigated
                        nextAtkCd = now + 1150L
                    }
                    m.copy(x = nx, y = ny, attackCooldownUntil = nextAtkCd)
                }

                var hp = updatedHero.hp - damageToHero
                var mp = min(derived.maxMp, updatedHero.mp + if (inSafeZone) 8 else 2)
                var hx = updatedHero.x
                var hy = updatedHero.y
                if (damageToHero > 0) {
                    pushFloatingText(hx, hy - 22f, "-$damageToHero", 0xFFEF4444)
                }
                if (inSafeZone) {
                    hp = min(derived.maxHp, hp + 12)
                }
                if (hp <= 0) {
                    hp = derived.maxHp
                    mp = derived.maxMp
                    hx = MuWorldConstants.SAFE_CENTER
                    hy = MuWorldConstants.SAFE_CENTER
                    pushFloatingText(hx, hy - 30f, "Regresaste a Zona Segura", 0xFFFBBF24)
                }

                updatedHero = updatedHero.copy(hp = hp, mp = mp, maxHp = derived.maxHp, maxMp = derived.maxMp, x = hx, y = hy)
                _activeHero.value = updatedHero

                if (_autoHunt.value) {
                    val targetDrop = _drops.value.firstOrNull()
                    val alive = _monsters.value.filter { it.hp > 0 }
                    val nearest = alive.minByOrNull { hypot((it.x - hx).toDouble(), (it.y - hy).toDouble()) }
                    val dropDist = targetDrop?.let { hypot((it.x - hx).toDouble(), (it.y - hy).toDouble()).toFloat() } ?: Float.MAX_VALUE
                    val goalX: Float?
                    val goalY: Float?
                    if (targetDrop != null && dropDist < 160f) {
                        goalX = targetDrop.x
                        goalY = targetDrop.y
                    } else {
                        goalX = nearest?.x
                        goalY = nearest?.y
                    }

                    if (goalX != null && goalY != null) {
                        val distGoal = hypot((goalX - hx).toDouble(), (goalY - hy).toDouble()).toFloat()
                        val atkRange = MuCatalog.CLASS_CATALOG.getValue(updatedHero.classId).attackRange
                        val distNearest = nearest?.let { hypot((it.x - hx).toDouble(), (it.y - hy).toDouble()).toFloat() } ?: Float.MAX_VALUE
                        if (nearest != null && distNearest <= atkRange) {
                            val firstSkill = MuCatalog.SKILLS_BY_CLASS[updatedHero.classId]?.firstOrNull()
                            if (firstSkill != null && updatedHero.mp >= firstSkill.mpCost) {
                                executeCombat(firstSkill.id)
                            } else {
                                executeCombat()
                            }
                        } else if (distGoal > 14f) {
                            val step = derived.moveSpeed * 0.1f
                            val nx = (hx + ((goalX - hx) / distGoal) * step).coerceIn(24f, MuWorldConstants.WORLD_SIZE - 24f)
                            val ny = (hy + ((goalY - hy) / distGoal) * step).coerceIn(24f, MuWorldConstants.WORLD_SIZE - 24f)
                            _heroFacingAngle.value = atan2(goalX - hx, goalY - hy)
                            _activeHero.value = updatedHero.copy(x = nx, y = ny)
                        }
                    }
                }

                _floatingTexts.value = _floatingTexts.value.filter { now - it.createdAt < 950L }
                _vfxList.value = _vfxList.value.filter { now - it.createdAt < it.durationMs }

                if (now - lastAutoSaveAt > 3500L) {
                    lastAutoSaveAt = now
                    _activeHero.value?.let { saveHeroAsync(it) }
                }
            }
        }
    }

    private fun spawnDropAt(x: Float, y: Float, monster: LiveMonster, mapMeta: MapMeta) {
        val rates = activeModRates.value
        val zenAmount = max(15, (monster.zenReward * mapMeta.dropZenRate * rates.zenMul).roundToInt())
        val now = System.currentTimeMillis()
        val spawned = mutableListOf(
            GroundDrop(MuCatalog.newUid("zen"), x - 14f, y + 8f, zen = zenAmount, createdAt = now)
        )

        val modItems = _mods.value.filter { it.enabled }.flatMap { m ->
            m.customItems.map { ci ->
                Item(
                    id = "mod-tpl",
                    templateId = ci.templateId,
                    name = ci.name,
                    slot = ci.slot,
                    rarity = ci.rarity,
                    plus = ci.basePlus,
                    attack = ci.attack,
                    defense = ci.defense,
                    magic = ci.magic,
                    hpBonus = ci.hpBonus,
                    speedBonus = ci.speedBonus,
                    wingsTier = ci.wingsTier,
                    classReq = ci.classReq,
                    colorHex = ci.colorHex,
                    valueZen = ci.valueZen
                )
            }
        }

        val baseChance = if (monster.guaranteedJewel) 1.0 else min(0.95, mapMeta.dropItemRate * rates.dropMul)
        if (Random.nextDouble() <= baseChance) {
            val pool = MuCatalog.DROP_ITEM_POOL + modItems
            val picked = if (monster.guaranteedJewel) {
                val special = pool.filter { it.slot == ItemSlot.JEWEL || it.rarity == ItemRarity.MYTHIC || it.slot == ItemSlot.WINGS }
                special.randomOrNull() ?: pool.random()
            } else {
                pool.random()
            }
            val item = picked.copy(id = MuCatalog.newUid("item"), quantity = 1)
            if (item.slot == ItemSlot.JEWEL) soundFx.playJewelDrop()
            spawned += GroundDrop(MuCatalog.newUid("drop"), x + 16f, y - 6f, item = item, createdAt = now)
        }

        _drops.value = (_drops.value.takeLast(28) + spawned)
    }

    private fun applyRewards(hero: HeroSave, gainedExp: Int, gainedZen: Int, killDelta: Int): Pair<HeroSave, Boolean> {
        var exp = hero.exp + gainedExp
        var level = hero.level
        var expToNext = hero.expToNext
        var statPoints = hero.statPoints
        var leveledUp = false
        val ptsPerLevel = MuCatalog.CLASS_CATALOG.getValue(hero.classId).pointsPerLevel

        while (exp >= expToNext && level < 400) {
            exp -= expToNext
            level += 1
            statPoints += ptsPerLevel
            expToNext = MuCatalog.expNeededForLevel(level)
            leveledUp = true
        }

        val next = hero.copy(
            level = level,
            exp = exp,
            expToNext = expToNext,
            statPoints = statPoints,
            zen = hero.zen + gainedZen,
            kills = hero.kills + killDelta
        )
        val derived = MuCatalog.computeDerivedHeroStats(next)
        val finalHero = if (leveledUp) {
            next.copy(maxHp = derived.maxHp, maxMp = derived.maxMp, hp = derived.maxHp, mp = derived.maxMp)
        } else {
            next.copy(maxHp = derived.maxHp, maxMp = derived.maxMp)
        }
        return finalHero to leveledUp
    }

    private fun addItemToHero(hero: HeroSave, item: Item): HeroSave {
        val inv = hero.inventory.toMutableList()
        if (item.slot == ItemSlot.POTION || item.slot == ItemSlot.JEWEL || item.slot == ItemSlot.TALISMAN) {
            val idx = inv.indexOfFirst { it.templateId == item.templateId }
            if (idx >= 0) {
                inv[idx] = inv[idx].copy(quantity = inv[idx].quantity + max(1, item.quantity))
                return hero.copy(inventory = inv)
            }
        }
        if (inv.size >= 24) return hero
        inv.add(item)
        return hero.copy(inventory = inv)
    }

    private fun consumeOneJewel(inv: List<Item>, jewelType: JewelType): List<Item>? {
        val idx = inv.indexOfFirst { it.jewelType == jewelType }
        if (idx < 0) return null
        val copy = inv.toMutableList()
        val target = copy[idx]
        if (target.quantity > 1) {
            copy[idx] = target.copy(quantity = target.quantity - 1)
        } else {
            copy.removeAt(idx)
        }
        return copy
    }

    private fun pushFloatingText(x: Float, y: Float, text: String, colorHex: Long, isCrit: Boolean = false) {
        val ft = FloatingText(MuCatalog.newUid("ft"), x, y, text, colorHex, isCrit, System.currentTimeMillis())
        _floatingTexts.value = (_floatingTexts.value.takeLast(24) + ft)
    }

    private fun pushVfx(skillId: String, x: Float, y: Float, targetX: Float, targetY: Float, colorHex: Long, radius: Float) {
        val vfx = SkillVfx(MuCatalog.newUid("vfx"), skillId, x, y, targetX, targetY, colorHex, radius, System.currentTimeMillis())
        _vfxList.value = (_vfxList.value.takeLast(16) + vfx)
    }

    private fun saveHeroAsync(hero: HeroSave) {
        viewModelScope.launch { repo.saveHero(hero) }
    }

    override fun onCleared() {
        super.onCleared()
        lanManager.shutdown()
    }
}
