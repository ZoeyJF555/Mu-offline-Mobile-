package com.muoffline.mobile.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.muoffline.mobile.model.ClassId
import com.muoffline.mobile.model.MapId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * 9-Layer Procedural MU Online Audio Engine:
 * 1. Map Background Music (lute / elven chimes / icy pad / dungeon organ / underwater synth)
 * 2. Map Ambient Beds (wind, forest, subterranean drone, ocean waves)
 * 3. Spatial Fire Crackle (bonfires & braziers attenuated by proximity)
 * 4. Spatial Map-Object Loops (fountain ripple & Chaos Goblin cauldron hum)
 * 5. Dynamic Surface Footsteps (stone, grass, snow, water, dungeon)
 * 6. UI Sounds (clicks, equip metallic clink, Zen pickup, bag open)
 * 7. Combat & Distinct Skill Signatures
 * 8. Monster Voices (spider hiss, hound growl, dragon roar, skeleton rattle, bull bellow, goblin cackle)
 * 9. Spatial Listener Attenuation (distance-scaled gain)
 */
class SoundFxPlayer {

    @Volatile
    var muted: Boolean = false

    @Volatile
    var sfxVolume: Float = 0.8f

    @Volatile
    var musicVolume: Float = 0.45f

    @Volatile
    private var currentMapId: MapId = MapId.LORENCIA

    @Volatile
    private var fireProximity: Float = 0f // 0..1 from braziers/bonfires

    @Volatile
    private var objectProximity: Float = 0f // 0..1 from fountain / Chaos cauldron

    @Volatile
    private var engineRunning: Boolean = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ambientJob: Job? = null
    private val sampleRate = 22050
    private var lastFootstepAt = 0L

    fun updateEnvironment(
        mapId: MapId,
        fireProx: Float,
        objProx: Float
    ) {
        currentMapId = mapId
        fireProximity = fireProx.coerceIn(0f, 1f)
        objectProximity = objProx.coerceIn(0f, 1f)
    }

    fun startAmbientEngine() {
        if (engineRunning && ambientJob?.isActive == true) return
        engineRunning = true
        ambientJob = scope.launch {
            val chunkSamples = sampleRate / 10 // 100ms chunks for responsive mute/volume/spatial updates
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuf, chunkSamples * 4))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: return@launch

            runCatching { track.play() }

            val buf = ShortArray(chunkSamples)
            var tSample = 0L
            var noiseState = 0.0
            var fireEnv = 0.0

            // Pentatonic & medieval mode scales per map family (Hz)
            val lorenciaMelody = doubleArrayOf(220.0, 261.63, 293.66, 329.63, 392.0, 329.63, 293.66, 246.94)
            val noriaMelody = doubleArrayOf(329.63, 392.0, 440.0, 523.25, 493.88, 440.0, 392.0, 329.63)
            val deviasMelody = doubleArrayOf(261.63, 311.13, 392.0, 466.16, 392.0, 311.13, 293.66, 233.08)
            val dungeonMelody = doubleArrayOf(110.0, 123.47, 130.81, 146.83, 130.81, 116.54, 110.0, 98.0)

            while (isActive && engineRunning) {
                val mVol = if (muted) 0f else musicVolume.coerceIn(0f, 1f)
                val sVol = if (muted) 0f else sfxVolume.coerceIn(0f, 1f)
                val map = currentMapId
                val fProx = fireProximity
                val oProx = objectProximity

                if (mVol <= 0.01f && sVol <= 0.01f) {
                    buf.fill(0)
                    runCatching { track.write(buf, 0, buf.size) }
                    tSample += chunkSamples
                    continue
                }

                val notes = when (map) {
                    MapId.NORIA, MapId.AIDA -> noriaMelody
                    MapId.DEVIAS, MapId.ICARUS, MapId.ATLANS -> deviasMelody
                    MapId.DUNGEON1, MapId.DUNGEON2, MapId.DUNGEON3, MapId.LOSTTOWER, MapId.KALIMA -> dungeonMelody
                    else -> lorenciaMelody
                }

                val stepSamples = (sampleRate * 0.65).toInt()
                for (i in 0 until chunkSamples) {
                    val idx = tSample + i
                    val timeSec = idx.toDouble() / sampleRate
                    val stepPos = (idx % stepSamples).toDouble() / stepSamples
                    val noteIdx = ((idx / stepSamples) % notes.size).toInt()
                    val freq = notes[noteIdx]
                    val bassFreq = notes[(noteIdx / 2) % notes.size] * 0.5

                    // Layer 1: Map BGM (warm lute / pad pluck + subtle root drone)
                    val pluckEnv = Math.exp(-stepPos * 3.6)
                    val bgmTone = (sin(2.0 * PI * freq * timeSec) * 0.55 +
                        sin(2.0 * PI * freq * 2.0 * timeSec) * 0.18) * pluckEnv +
                        sin(2.0 * PI * bassFreq * timeSec) * 0.25

                    // Layer 2: Ambient Bed (filtered brown wind / dungeon rumble)
                    val white = Random.nextDouble() * 2.0 - 1.0
                    noiseState = noiseState * 0.96 + white * 0.04
                    val windSwell = 0.55 + 0.45 * sin(timeSec * 0.7)
                    val ambientBed = noiseState * windSwell

                    // Layer 3: Spatial Fire Crackle (bonfires & Chaos flame)
                    if (fProx > 0.05f && Random.nextDouble() < 0.008) {
                        fireEnv = 0.6 + Random.nextDouble() * 0.4
                    }
                    fireEnv *= 0.985
                    val fireCrackle = if (fProx > 0.05f) white * fireEnv * fProx * 0.45 else 0.0

                    // Layer 4: Spatial Map-Object Loop (Fountain shimmer / Chaos Goblin cauldron hum)
                    val objWave = if (oProx > 0.05f) {
                        val humFreq = if (map == MapId.NORIA) 142.0 else 310.0 + 40.0 * sin(timeSec * 3.1)
                        (sin(2.0 * PI * humFreq * timeSec) * 0.35 + white * 0.12) * oProx * 0.35
                    } else 0.0

                    val mixed = bgmTone * (mVol * 0.14) +
                        ambientBed * (mVol * 0.18) +
                        (fireCrackle + objWave) * (sVol * 0.32)

                    buf[i] = (mixed * Short.MAX_VALUE)
                        .toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        .toShort()
                }
                tSample += chunkSamples
                runCatching { track.write(buf, 0, buf.size) }
            }

            runCatching {
                track.stop()
                track.release()
            }
        }
    }

    fun pauseAmbientEngine() {
        engineRunning = false
        ambientJob?.cancel()
        ambientJob = null
    }

    fun release() {
        pauseAmbientEngine()
        scope.cancel()
    }

    // Layer 5: Dynamic Surface Footsteps
    fun playFootstep(surface: String) {
        if (muted || sfxVolume <= 0.01f) return
        val now = System.currentTimeMillis()
        if (now - lastFootstepAt < 250L) return
        lastFootstepAt = now

        val (startHz, endHz, dur) = when (surface) {
            "stone" -> Triple(220.0, 95.0, 42)
            "snow" -> Triple(520.0, 260.0, 55)
            "water" -> Triple(420.0, 680.0, 62)
            else -> Triple(165.0, 75.0, 46) // grass / earth
        }
        playNoiseBurst(startHz, endHz, dur, 0.12f)
    }

    // Layer 6: UI Sounds
    fun playUiClick() = playNotes(listOf(780.0 to 26), 0.11f)
    fun playEquip() = playNotes(listOf(440.0 to 40, 880.0 to 65), 0.18f)
    fun playZenPickup() = playNotes(listOf(987.77 to 45, 1318.51 to 75), 0.16f)

    // Layer 7: Combat & Distinct Skill Signatures
    fun playAttack(classId: ClassId) {
        when (classId) {
            ClassId.DK -> playSweep(340.0, 120.0, 95, 0.22f)
            ClassId.DW -> playSweep(620.0, 920.0, 110, 0.18f)
            ClassId.ELF -> playSweep(880.0, 460.0, 85, 0.18f)
            ClassId.MG -> playSweep(420.0, 760.0, 105, 0.21f)
            ClassId.DL -> playSweep(280.0, 620.0, 115, 0.22f)
        }
    }

    fun playSkill(skillId: String? = null) {
        when (skillId) {
            "twisting-slash", "rageful-blow" -> playSweep(260.0, 780.0, 185, 0.25f)
            "evil-spirit" -> playNotes(listOf(587.33 to 55, 440.0 to 60, 329.63 to 80), 0.23f)
            "meteorite" -> playSweep(520.0, 75.0, 240, 0.26f)
            "ice-storm", "ice-arrow" -> playNotes(listOf(1046.5 to 50, 1396.9 to 60, 880.0 to 90), 0.22f)
            "triple-shot" -> playNotes(listOf(920.0 to 35, 980.0 to 35, 1060.0 to 45), 0.20f)
            "heal-aura" -> playNotes(listOf(523.25 to 60, 659.25 to 65, 783.99 to 110), 0.21f)
            else -> playSweep(420.0, 960.0, 170, 0.24f)
        }
    }

    // Layer 8 & 9: Monster Voices with Spatial Distance Attenuation
    fun playMonsterVoice(templateId: String, distanceWorld: Float, isDeath: Boolean = false) {
        if (muted || sfxVolume <= 0.01f) return
        val spatialGain = (1f - (distanceWorld / 460f)).coerceIn(0.15f, 1f)
        val lower = templateId.lowercase()
        val (sHz, eHz, dur) = when {
            lower.contains("spider") || lower.contains("scorpion") ->
                if (isDeath) Triple(950.0, 260.0, 130) else Triple(1150.0, 700.0, 75)
            lower.contains("hound") ->
                if (isDeath) Triple(340.0, 110.0, 160) else Triple(260.0, 420.0, 95)
            lower.contains("dragon") || lower.contains("phoenix") || lower.contains("hydra") ->
                if (isDeath) Triple(220.0, 65.0, 260) else Triple(190.0, 95.0, 165)
            lower.contains("skeleton") ->
                if (isDeath) Triple(640.0, 180.0, 140) else Triple(540.0, 320.0, 70)
            lower.contains("bull") || lower.contains("balrog") || lower.contains("kundun") ->
                if (isDeath) Triple(160.0, 55.0, 240) else Triple(140.0, 85.0, 150)
            else ->
                if (isDeath) Triple(420.0, 140.0, 140) else Triple(360.0, 210.0, 85)
        }
        playSweep(sHz, eHz, dur, 0.17f * spatialGain)
    }

    fun playPotion() = playNotes(listOf(523.25 to 70, 659.25 to 90), 0.20f)
    fun playJewelDrop() = playNotes(listOf(1046.5 to 85, 1318.5 to 110, 1567.98 to 150), 0.24f)
    fun playLevelUp() = playNotes(listOf(523.25 to 90, 659.25 to 90, 783.99 to 100, 1046.5 to 200), 0.26f)
    fun playChaosSuccess() = playNotes(listOf(659.25 to 90, 783.99 to 90, 1046.5 to 130, 1318.5 to 210), 0.26f)
    fun playChaosFail() = playSweep(280.0, 95.0, 260, 0.25f)

    private fun playSweep(startHz: Double, endHz: Double, durationMs: Int, baseVol: Float) {
        if (muted || sfxVolume <= 0.01f) return
        val volume = baseVol * sfxVolume
        scope.launch {
            val count = (sampleRate * durationMs) / 1000
            if (count <= 0) return@launch
            val pcm = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = startHz + (endHz - startHz) * t
                val env = 1.0 - t
                phase += 2.0 * PI * freq / sampleRate
                pcm[i] = (sin(phase) * env * volume * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            emitPcm(pcm)
        }
    }

    private fun playNoiseBurst(startHz: Double, endHz: Double, durationMs: Int, baseVol: Float) {
        if (muted || sfxVolume <= 0.01f) return
        val volume = baseVol * sfxVolume
        scope.launch {
            val count = (sampleRate * durationMs) / 1000
            if (count <= 0) return@launch
            val pcm = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val freq = startHz + (endHz - startHz) * t
                val env = (1.0 - t) * (1.0 - t)
                phase += 2.0 * PI * freq / sampleRate
                val sample = sin(phase) * 0.55 + (Random.nextDouble() * 2.0 - 1.0) * 0.45
                pcm[i] = (sample * env * volume * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            emitPcm(pcm)
        }
    }

    private fun playNotes(notes: List<Pair<Double, Int>>, baseVol: Float) {
        if (muted || sfxVolume <= 0.01f) return
        val volume = baseVol * sfxVolume
        scope.launch {
            val totalSamples = notes.sumOf { (sampleRate * it.second) / 1000 }
            if (totalSamples <= 0) return@launch
            val pcm = ShortArray(totalSamples)
            var cursor = 0
            for ((freq, dur) in notes) {
                val n = (sampleRate * dur) / 1000
                var phase = 0.0
                for (i in 0 until n) {
                    val env = 1.0 - (i.toDouble() / n) * 0.7
                    phase += 2.0 * PI * freq / sampleRate
                    pcm[cursor++] = (sin(phase) * env * volume * Short.MAX_VALUE).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            emitPcm(pcm)
        }
    }

    private fun emitPcm(pcm: ShortArray) {
        runCatching {
            val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, pcm.size * 2))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep((pcm.size * 1000L) / sampleRate + 30L)
            track.stop()
            track.release()
        }
    }
}
